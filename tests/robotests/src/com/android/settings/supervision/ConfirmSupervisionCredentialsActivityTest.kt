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
import android.app.role.RoleManager
import android.app.supervision.SupervisionManager
import android.app.supervision.SupervisionRecoveryInfo
import android.app.supervision.SupervisionRecoveryInfo.STATE_PENDING
import android.content.pm.UserInfo
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.PromptContentViewWithMoreOptionsButton
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.os.UserManager.USER_TYPE_PROFILE_SUPERVISING
import android.os.UserManager.USER_TYPE_PROFILE_TEST
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBinder

@RunWith(RobolectricTestRunner::class)
class ConfirmSupervisionCredentialsActivityTest {
    private val mockRoleManager = mock<RoleManager>()
    private val mockUserManager = mock<UserManager>()
    private val mockActivityManager = mock<ActivityManager>()
    private val mockKeyguardManager = mock<KeyguardManager>()
    private val mockSupervisionManager = mock<SupervisionManager>()

    private lateinit var mActivity: ConfirmSupervisionCredentialsActivity

    private val callingPackage = "com.example.caller"

    @Before
    fun setUp() {
        mActivity =
            spy(
                Robolectric.buildActivity(ConfirmSupervisionCredentialsActivity::class.java).get()
            ) {
                on { getSystemService(RoleManager::class.java) } doReturn mockRoleManager
                on { getSystemService(UserManager::class.java) } doReturn mockUserManager
                on { getSystemService(ActivityManager::class.java) } doReturn mockActivityManager
                on { getSystemService(KeyguardManager::class.java) } doReturn mockKeyguardManager
                on { getSystemService(SupervisionManager::class.java) } doReturn
                    mockSupervisionManager
                on { callingPackage } doReturn callingPackage
            }
    }

    @Test
    fun onCreate_callerHasSupervisionRole_doesNotFinish() {
        mockRoleManager.stub { on { getRoleHolders(any()) } doReturn listOf(callingPackage) }
        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        mockActivityManager.stub { on { startProfile(any()) } doReturn true }
        mockKeyguardManager.stub { on { isDeviceSecure(SUPERVISING_USER_ID) } doReturn true }

        mActivity.onCreate(null)

        verify(mActivity, never()).finish()

        // Ensure that the supervising profile is started
        val userCaptor = argumentCaptor<UserHandle>()
        verify(mockActivityManager).startProfile(userCaptor.capture())
        assert(userCaptor.lastValue.identifier == SUPERVISING_USER_ID)
    }

    @Test
    fun onCreate_failsToStartSupervisingProfile_finish() {
        mockRoleManager.stub { on { getRoleHolders(any()) } doReturn listOf(callingPackage) }
        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        mockActivityManager.stub { on { startProfile(any()) } doReturn false }
        mockKeyguardManager.stub { on { isDeviceSecure(SUPERVISING_USER_ID) } doReturn true }

        mActivity.onCreate(null)

        verify(mActivity).setResult(Activity.RESULT_CANCELED)
        verify(mActivity).finish()
    }

    @Test
    fun onCreate_callerNotHasSupervisionRole_finish() {
        val otherPackage = "com.example.other"
        mockRoleManager.stub { on { getRoleHolders(any()) } doReturn listOf(otherPackage) }
        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        mockActivityManager.stub { on { startProfile(any()) } doReturn true }
        mockKeyguardManager.stub { on { isDeviceSecure(SUPERVISING_USER_ID) } doReturn true }

        mActivity.onCreate(null)

        verify(mActivity).setResult(Activity.RESULT_CANCELED)
        verify(mActivity).finish()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.BAKLAVA])
    fun onCreate_callerIsSystemUid_doesNotFinish() {
        ShadowBinder.setCallingUid(Process.SYSTEM_UID)
        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        mockActivityManager.stub { on { startProfile(any()) } doReturn true }
        mockKeyguardManager.stub { on { isDeviceSecure(SUPERVISING_USER_ID) } doReturn true }

        mActivity.onCreate(null)

        verify(mActivity, never()).finish()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.BAKLAVA])
    fun onCreate_callerIsUnknownUid_finish() {
        ShadowBinder.setCallingUid(Process.NOBODY_UID)
        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        mockActivityManager.stub { on { startProfile(any()) } doReturn true }
        mockKeyguardManager.stub { on { isDeviceSecure(SUPERVISING_USER_ID) } doReturn true }

        mActivity.onCreate(null)

        verify(mActivity).setResult(Activity.RESULT_CANCELED)
        verify(mActivity).finish()
    }

    @Test
    fun onCreate_noSupervisingCredential_finish() {
        mockRoleManager.stub { on { getRoleHolders(any()) } doReturn listOf(callingPackage) }
        mockUserManager.stub { on { users } doReturn listOf(TESTING_USER_INFO) }
        mockActivityManager.stub { on { startProfile(any()) } doReturn true }
        mockKeyguardManager.stub { on { isDeviceSecure(TESTING_USER_ID) } doReturn false }

        mActivity.onCreate(null)

        verify(mActivity).setResult(Activity.RESULT_CANCELED)
        verify(mActivity).finish()
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
        const val TESTING_USER_ID = 6
        val TESTING_USER_INFO =
            UserInfo(
                TESTING_USER_ID,
                /* name */ "testing",
                /* iconPath */ "",
                /* flags */ 0,
                USER_TYPE_PROFILE_TEST,
            )
    }
}
