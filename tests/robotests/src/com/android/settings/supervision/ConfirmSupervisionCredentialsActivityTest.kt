/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.supervision

import android.app.Activity
import android.app.ActivityManager
import android.app.KeyguardManager
import android.app.role.RoleManager.ROLE_SYSTEM_SUPERVISION
import android.app.supervision.SupervisionManager
import android.app.supervision.SupervisionRecoveryInfo
import android.app.supervision.SupervisionRecoveryInfo.STATE_PENDING
import android.content.Context
import android.content.Intent
import android.content.pm.UserInfo
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.PromptContentViewWithMoreOptionsButton
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.os.UserManager.USER_TYPE_PROFILE_SUPERVISING
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.supervision.ConfirmSupervisionCredentialsActivity.Companion.EXTRA_FORCE_CONFIRMATION
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowActivity
import org.robolectric.shadows.ShadowBinder
import org.robolectric.shadows.ShadowContextImpl
import org.robolectric.shadows.ShadowKeyguardManager
import org.robolectric.shadows.ShadowRoleManager

@RunWith(AndroidJUnit4::class)
class ConfirmSupervisionCredentialsActivityTest {
    private val mockUserManager = mock<UserManager>()
    private val mockActivityManager = mock<ActivityManager>()
    private val mockSupervisionManager = mock<SupervisionManager>()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val currentUser = context.user

    private lateinit var mActivity: ConfirmSupervisionCredentialsActivity
    private lateinit var mActivityController:
        ActivityController<ConfirmSupervisionCredentialsActivity>

    private lateinit var shadowActivity: ShadowActivity
    private lateinit var shadowKeyguardManager: ShadowKeyguardManager

    private val callingPackage = "com.example.caller"

    @Before
    fun setUp() {
        ShadowRoleManager.reset()
        setUpActivity(forceConfirm = false)
        SupervisionAuthController.sInstance = null
    }

    @Test
    fun onCreate_callerHasSupervisionRole_doesNotFinish() {
        ShadowRoleManager.addRoleHolder(ROLE_SYSTEM_SUPERVISION, callingPackage, currentUser)
        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        mockActivityManager.stub { on { startProfile(any()) } doReturn true }
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, true)

        mActivityController.setup()

        assertThat(mActivity.isFinishing).isFalse()

        // Ensure that the supervising profile is started
        val userCaptor = argumentCaptor<UserHandle>()
        verify(mockActivityManager).startProfile(userCaptor.capture())
        assert(userCaptor.lastValue.identifier == SUPERVISING_USER_ID)
    }

    @Test
    fun onCreate_failsToStartSupervisingProfile_finish() {
        ShadowRoleManager.addRoleHolder(ROLE_SYSTEM_SUPERVISION, callingPackage, currentUser)
        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        mockActivityManager.stub { on { startProfile(any()) } doReturn false }
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, true)

        mActivityController.setup()

        assertThat(mActivity.isFinishing).isTrue()
        assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_CANCELED)
    }

    @Test
    fun onCreate_callerNotHasSupervisionRole_finish() {
        val otherPackage = "com.example.other"
        ShadowRoleManager.addRoleHolder(ROLE_SYSTEM_SUPERVISION, otherPackage, currentUser)
        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        mockActivityManager.stub { on { startProfile(any()) } doReturn true }
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, true)

        mActivityController.setup()

        assertThat(mActivity.isFinishing).isTrue()
        assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_CANCELED)
    }

    @Test
    fun onCreate_authSessionActive_finishWithResultOK() {
        ShadowRoleManager.addRoleHolder(ROLE_SYSTEM_SUPERVISION, callingPackage, currentUser)
        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        mockActivityManager.stub { on { startProfile(any()) } doReturn true }
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, true)
        SupervisionAuthController.getInstance(context).startSession(mActivity.taskId)

        mActivityController.setup()

        assertThat(mActivity.isFinishing).isTrue()
        assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_OK)
    }

    @Test
    fun onCreate_authSessionActive_forceConfirmation_doesNotFinish() {
        ShadowRoleManager.addRoleHolder(ROLE_SYSTEM_SUPERVISION, callingPackage, currentUser)
        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        mockActivityManager.stub { on { startProfile(any()) } doReturn true }
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, true)
        SupervisionAuthController.getInstance(context).startSession(mActivity.taskId)

        setUpActivity(forceConfirm = true)
        mActivityController.setup()

        assertThat(mActivity.isFinishing).isFalse()

        // Ensure that the supervising profile is started
        val userCaptor = argumentCaptor<UserHandle>()
        verify(mockActivityManager).startProfile(userCaptor.capture())
        assert(userCaptor.lastValue.identifier == SUPERVISING_USER_ID)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.BAKLAVA])
    fun onCreate_callerIsSystemUid_doesNotFinish() {
        ShadowBinder.setCallingUid(
            UserHandle.getUid(/* userId= */ 2, /* appId= */ Process.SYSTEM_UID)
        )
        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        mockActivityManager.stub { on { startProfile(any()) } doReturn true }
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, true)

        mActivityController.setup()

        assertThat(mActivity.isFinishing).isFalse()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.BAKLAVA])
    fun onCreate_callerIsUnknownUid_finish() {
        ShadowBinder.setCallingUid(Process.NOBODY_UID)
        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        mockActivityManager.stub { on { startProfile(any()) } doReturn true }
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, true)

        mActivityController.setup()

        assertThat(mActivity.isFinishing).isTrue()
        assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_CANCELED)
    }

    @Test
    fun onCreate_noSupervisingCredential_startSetupActivity() {
        ShadowRoleManager.addRoleHolder(ROLE_SYSTEM_SUPERVISION, callingPackage, currentUser)
        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        mockActivityManager.stub { on { startProfile(any()) } doReturn true }
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, false)

        mActivityController.setup()

        assertThat(mActivity.isFinishing).isFalse()
        assertThat(shadowActivity.nextStartedActivity.component?.className)
            .isEqualTo(SetupSupervisionActivity::class.java.name)
    }

    @Test
    fun getBiometricPrompt_recoveryEmailExist_showMoreOptionsButton() {
        val recoveryInfo = SupervisionRecoveryInfo("email", "default", STATE_PENDING, null)
        whenever(mockSupervisionManager.supervisionRecoveryInfo).thenReturn(recoveryInfo)

        val biometricPrompt = mActivity.getBiometricPrompt()

        assertThat(biometricPrompt.title)
            .isEqualTo(mActivity.getString(R.string.supervision_full_screen_pin_verification_title))
        assertThat(biometricPrompt.isConfirmationRequired).isTrue()
        assertThat(biometricPrompt.allowedAuthenticators)
            .isEqualTo(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        assertThat(biometricPrompt.contentView)
            .isInstanceOf(PromptContentViewWithMoreOptionsButton::class.java)
    }

    @Test
    fun getBiometricPrompt_recoveryInfoEmpty_noMoreOptionsButton() {
        whenever(mockSupervisionManager.supervisionRecoveryInfo).thenReturn(null)

        val biometricPrompt = mActivity.getBiometricPrompt()

        assertThat(biometricPrompt.title)
            .isEqualTo(mActivity.getString(R.string.supervision_full_screen_pin_verification_title))
        assertThat(biometricPrompt.isConfirmationRequired).isTrue()
        assertThat(biometricPrompt.allowedAuthenticators)
            .isEqualTo(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        assertThat(biometricPrompt.contentView).isNull()
    }

    @Test
    fun onAuthenticationSucceeded_startsAuthSession_returnsResultOK() {
        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, true)

        mActivity.mAuthenticationCallback.onAuthenticationSucceeded(null)

        assertThat(SupervisionAuthController.getInstance(context).isSessionActive(mActivity.taskId))
            .isTrue()
        assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_OK)
    }

    private fun setUpActivity(forceConfirm: Boolean) {
        // Note, we have to use ActivityController (instead of ActivityScenario) in order to access
        // the activity before it is created, so we can set up various mocked responses before they
        // are referenced in onCreate.
        if (forceConfirm) {
            val intent = Intent().putExtra(EXTRA_FORCE_CONFIRMATION, true)
            mActivityController =
                Robolectric.buildActivity(ConfirmSupervisionCredentialsActivity::class.java, intent)
        } else {
            mActivityController =
                Robolectric.buildActivity(ConfirmSupervisionCredentialsActivity::class.java)
        }
        mActivity = mActivityController.get()

        shadowActivity = shadowOf(mActivity)
        shadowActivity.setCallingPackage(callingPackage)
        shadowKeyguardManager = shadowOf(mActivity.getSystemService(KeyguardManager::class.java))
        Shadow.extract<ShadowContextImpl>(mActivity.baseContext).apply {
            setSystemService(Context.ACTIVITY_SERVICE, mockActivityManager)
            setSystemService(Context.SUPERVISION_SERVICE, mockSupervisionManager)
            setSystemService(Context.USER_SERVICE, mockUserManager)
        }
    }

    private companion object {
        const val SUPERVISING_USER_ID = 5
        val SUPERVISING_USER_INFO =
            UserInfo(
                SUPERVISING_USER_ID,
                /* name */ "supervising",
                /* iconPath */ "",
                /* flags */ 0,
                USER_TYPE_PROFILE_SUPERVISING,
            )
    }
}
