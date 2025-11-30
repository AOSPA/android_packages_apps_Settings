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
import android.app.Application
import android.app.KeyguardManager
import android.app.role.RoleManager.ROLE_SYSTEM_SUPERVISION
import android.app.settings.SettingsEnums
import android.app.supervision.ISupervisionManager
import android.app.supervision.SupervisionManager
import android.app.supervision.SupervisionRecoveryInfo
import android.app.supervision.SupervisionRecoveryInfo.STATE_PENDING
import android.app.supervision.flags.Flags
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.content.pm.UserInfo
import android.hardware.biometrics.BiometricManager
import android.os.Bundle
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.os.UserManager.USER_TYPE_PROFILE_SUPERVISING
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.supervision.ConfirmSupervisionCredentialsActivity.Companion.EXTRA_FORCE_CONFIRMATION
import com.android.settings.testutils.MetricsRule
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowActivity
import org.robolectric.shadows.ShadowBinder
import org.robolectric.shadows.ShadowContextImpl
import org.robolectric.shadows.ShadowKeyguardManager
import org.robolectric.shadows.ShadowRoleManager
import org.robolectric.shadows.ShadowServiceManager

@RunWith(AndroidJUnit4::class)
class ConfirmSupervisionCredentialsActivityTest {
    @get:Rule val metricsRule = MetricsRule()
    private val mockUserManager = mock<UserManager>()
    private val mockActivityManager = mock<ActivityManager>()
    private val mockSupervisionManager = mock<SupervisionManager>()
    private val mockISupervisionManager = mock<ISupervisionManager>()

    @get:Rule val setFlagsRule = SetFlagsRule()
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
        ShadowServiceManager.addBinderService(
            Context.SUPERVISION_SERVICE,
            ISupervisionManager::class.java,
            mockISupervisionManager,
        )
        whenever(mockISupervisionManager.querySupervisionApprovalActivities(any()))
            .thenReturn(emptyList())
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
    fun onCreate_userIsRunning_savesPromptShownState() {
        ShadowRoleManager.addRoleHolder(ROLE_SYSTEM_SUPERVISION, callingPackage, currentUser)
        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        mockActivityManager.stub {
            on { startProfile(any()) } doReturn true
            on { isUserRunning(SUPERVISING_USER_ID) } doReturn true
        }
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, true)

        mActivityController.setup()
        assertThat(mActivity.isFinishing).isFalse()

        val outState = Bundle()
        mActivityController.saveInstanceState(outState)
        assertThat(
                outState.getBoolean(
                    ConfirmSupervisionCredentialsActivity.KEY_BIOMETRIC_PROMPT_SHOWN
                )
            )
            .isTrue()
    }

    @Test
    fun onCreate_startsConfirmationActivity_activityFinishing_stopsProfile() {
        ShadowRoleManager.addRoleHolder(ROLE_SYSTEM_SUPERVISION, callingPackage, currentUser)
        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        mockActivityManager.stub {
            on { startProfile(any()) } doReturn true
            on { stopProfile(any()) } doReturn true
        }
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, true)

        mActivityController.setup()

        // Ensure that the supervising profile is started
        val userCaptor = argumentCaptor<UserHandle>()
        verify(mockActivityManager).startProfile(userCaptor.capture())
        assert(userCaptor.lastValue.identifier == SUPERVISING_USER_ID)
        assertThat(mActivity.mProfileStarted).isTrue()

        mActivity.mAuthenticationCallback.onAuthenticationSucceeded(null)
        assertThat(mActivity.isFinishing).isTrue()
        mActivity.onDestroy()
        verify(mockActivityManager).stopProfile(any())
        assertThat(mActivity.mProfileStarted).isFalse()
    }

    @Test
    fun configurationChange_doesNotStopProfile() {
        ShadowRoleManager.addRoleHolder(ROLE_SYSTEM_SUPERVISION, callingPackage, currentUser)
        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        mockActivityManager.stub {
            on { startProfile(any()) } doReturn true
            on { stopProfile(any()) } doReturn true
        }
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, true)

        mActivityController.setup()

        // Ensure that the supervising profile is started
        val userCaptor = argumentCaptor<UserHandle>()
        verify(mockActivityManager).startProfile(userCaptor.capture())
        assert(userCaptor.lastValue.identifier == SUPERVISING_USER_ID)
        assertThat(mActivity.mProfileStarted).isTrue()

        mActivityController.recreate()
        verify(mockActivityManager, never()).stopProfile(any())
        assertThat(mActivity.mProfileStarted).isTrue()
    }

    @Test
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
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun onCreate_noSupervisingCredential_flagDisabled_startSetupActivity() {
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
    fun onCreate_noSupervisingCredential_noApprovalMethods_finish() {
        ShadowRoleManager.addRoleHolder(ROLE_SYSTEM_SUPERVISION, callingPackage, currentUser)
        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        mockActivityManager.stub { on { startProfile(any()) } doReturn true }
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, false)

        mActivityController.setup()

        assertThat(mActivity.isFinishing).isTrue()
        assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_CANCELED)
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun onCreate_noSupervisingCredential_noApprovalMethods_startsSetup() {
        ShadowRoleManager.addRoleHolder(ROLE_SYSTEM_SUPERVISION, callingPackage, currentUser)
        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, false)
        whenever(mockISupervisionManager.querySupervisionApprovalActivities(any()))
            .thenReturn(emptyList())

        mActivityController.setup()

        assertThat(mActivity.isFinishing).isFalse()
        val nextActivity = shadowActivity.nextStartedActivity
        assertThat(nextActivity.component?.className)
            .isEqualTo(SetupSupervisionActivity::class.java.name)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun onCreate_noSupervisingCredential_oneApprovalMethod_launchesMethodAndHandlesResult() {
        ShadowRoleManager.addRoleHolder(ROLE_SYSTEM_SUPERVISION, callingPackage, currentUser)
        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, false)
        val resolveInfo =
            ResolveInfo().apply {
                activityInfo =
                    ActivityInfo().apply {
                        packageName = "com.example.approval"
                        name = "ApprovalActivity"
                    }
            }
        whenever(mockISupervisionManager.querySupervisionApprovalActivities(any()))
            .thenReturn(listOf(resolveInfo))

        mActivityController.setup()

        assertThat(mActivity.isFinishing).isFalse()
        val nextActivity = shadowActivity.nextStartedActivity
        assertThat(nextActivity.action)
            .isEqualTo(SupervisionManager.ACTION_CONFIRM_SUPERVISION_APPROVAL)
        assertThat(nextActivity.component?.className).isEqualTo("ApprovalActivity")

        shadowActivity.receiveResult(nextActivity, Activity.RESULT_OK, null)

        assertThat(mActivity.isFinishing).isTrue()
        assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_OK)
        assertThat(SupervisionAuthController.getInstance(context).isSessionActive(mActivity.taskId))
            .isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun onCreate_noSupervisingCredential_oneApprovalMethod_handlingFailureResult() {
        ShadowRoleManager.addRoleHolder(ROLE_SYSTEM_SUPERVISION, callingPackage, currentUser)
        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, false)
        val resolveInfo =
            ResolveInfo().apply {
                activityInfo =
                    ActivityInfo().apply {
                        packageName = "com.example.approval"
                        name = "ApprovalActivity"
                    }
            }
        whenever(mockISupervisionManager.querySupervisionApprovalActivities(any()))
            .thenReturn(listOf(resolveInfo))

        mActivityController.setup()

        val nextActivity = shadowActivity.nextStartedActivity
        assertThat(nextActivity).isNotNull()

        shadowActivity.receiveResult(nextActivity, Activity.RESULT_CANCELED, null)

        assertThat(mActivity.isFinishing).isTrue()
        assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_CANCELED)
        assertThat(SupervisionAuthController.getInstance(context).isSessionActive(mActivity.taskId))
            .isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun onCreate_noSupervisingCredential_multipleApprovalMethods_showsChooserAndHandlesResult() {
        ShadowRoleManager.addRoleHolder(ROLE_SYSTEM_SUPERVISION, callingPackage, currentUser)
        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, false)
        val applicationInfo = ApplicationInfo().apply { packageName = "com.example.approval" }
        val resolveInfo1 =
            ResolveInfo().apply {
                activityInfo =
                    ActivityInfo().apply {
                        packageName = "com.example.approval"
                        name = "ApprovalActivity1"
                        nonLocalizedLabel = "method 1"
                        this.applicationInfo = applicationInfo
                    }
            }
        val resolveInfo2 =
            ResolveInfo().apply {
                activityInfo =
                    ActivityInfo().apply {
                        packageName = "com.example.approval"
                        name = "ApprovalActivity2"
                        nonLocalizedLabel = "method 2"
                        this.applicationInfo = applicationInfo
                    }
            }
        whenever(mockISupervisionManager.querySupervisionApprovalActivities(any()))
            .thenReturn(listOf(resolveInfo1, resolveInfo2))

        mActivityController.setup()

        assertThat(mActivity.isFinishing).isFalse()
        val dialog = mActivity.supportFragmentManager.findFragmentByTag("ApprovalMethodChooser")
        assertThat(dialog).isInstanceOf(ApprovalMethodChooserDialogFragment::class.java)
        assertThat(dialog?.isAdded).isTrue()

        val resultBundle =
            Bundle().apply {
                putInt(
                    ApprovalMethodChooserDialogFragment.BUNDLE_KEY_RESULT_CODE,
                    Activity.RESULT_OK,
                )
            }
        mActivity.supportFragmentManager.setFragmentResult(
            ApprovalMethodChooserDialogFragment.REQUEST_KEY_APPROVAL_RESULT,
            resultBundle,
        )

        assertThat(mActivity.isFinishing).isTrue()
        assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_OK)
        assertThat(SupervisionAuthController.getInstance(context).isSessionActive(mActivity.taskId))
            .isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun onCreate_noSupervisingCredential_multipleApprovalMethods_handlingFailureResult() {
        ShadowRoleManager.addRoleHolder(ROLE_SYSTEM_SUPERVISION, callingPackage, currentUser)
        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, false)
        val applicationInfo = ApplicationInfo().apply { packageName = "com.example.approval" }
        val resolveInfo1 =
            ResolveInfo().apply {
                activityInfo =
                    ActivityInfo().apply {
                        packageName = "com.example.approval"
                        name = "ApprovalActivity1"
                        this.applicationInfo = applicationInfo
                    }
            }
        val resolveInfo2 =
            ResolveInfo().apply {
                activityInfo =
                    ActivityInfo().apply {
                        packageName = "com.example.approval"
                        name = "ApprovalActivity2"
                        this.applicationInfo = applicationInfo
                    }
            }
        whenever(mockISupervisionManager.querySupervisionApprovalActivities(any()))
            .thenReturn(listOf(resolveInfo1, resolveInfo2))

        mActivityController.setup()

        val resultBundle =
            Bundle().apply {
                putInt(
                    ApprovalMethodChooserDialogFragment.BUNDLE_KEY_RESULT_CODE,
                    Activity.RESULT_CANCELED,
                )
            }
        mActivity.supportFragmentManager.setFragmentResult(
            ApprovalMethodChooserDialogFragment.REQUEST_KEY_APPROVAL_RESULT,
            resultBundle,
        )

        assertThat(mActivity.isFinishing).isTrue()
        assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_CANCELED)
        assertThat(SupervisionAuthController.getInstance(context).isSessionActive(mActivity.taskId))
            .isFalse()
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun onCreate_onStartSetupActivity_onDestroy_notStopProfile() {
        ShadowRoleManager.addRoleHolder(ROLE_SYSTEM_SUPERVISION, callingPackage, currentUser)
        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        mockActivityManager.stub { on { startProfile(any()) } doReturn true }
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, false)

        mActivityController.setup()

        assertThat(mActivity.isFinishing).isFalse()
        assertThat(shadowActivity.nextStartedActivity.component?.className)
            .isEqualTo(SetupSupervisionActivity::class.java.name)
        assertThat(mActivity.mProfileStarted).isFalse()

        mActivity.onDestroy()
        verify(mockActivityManager, never()).stopProfile(any())
    }

    @Test
    fun userStateChangeReceiver_receivesUserStopped_restartsProfileAndShowsPrompt() {
        // Arrange: Set up activity but ensure prompt is not shown in onCreate
        ShadowRoleManager.addRoleHolder(ROLE_SYSTEM_SUPERVISION, callingPackage, currentUser)
        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        mockActivityManager.stub {
            on { startProfile(any()) } doReturn true
            on { isUserRunning(SUPERVISING_USER_ID) } doReturn false
        }
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, true)
        mActivityController.setup()

        // Act: Simulate the USER_STOPPED broadcast for the supervising user
        val intent =
            Intent(Intent.ACTION_USER_STOPPED)
                .putExtra(Intent.EXTRA_USER_HANDLE, SUPERVISING_USER_ID)
        shadowOf(ApplicationProvider.getApplicationContext<Application>())
            .getReceiversForIntent(intent)
            .first()
            .onReceive(context, intent)

        // Assert: Profile is restarted and prompt state is updated
        verify(mockActivityManager, times(2)).startProfile(SUPERVISING_USER_HANDLE)
        assertThat(mActivity.mProfileStarted).isTrue()
        assertThat(mActivity.isFinishing).isFalse()

        val outState = Bundle()
        mActivityController.saveInstanceState(outState)
        assertThat(
                outState.getBoolean(
                    ConfirmSupervisionCredentialsActivity.KEY_BIOMETRIC_PROMPT_SHOWN
                )
            )
            .isTrue()
    }

    @Test
    fun userStateChangeReceiver_receivesUserStopped_startProfileFails_finishesActivity() {
        // Arrange: Set up activity, mock startProfile to fail on the second call
        ShadowRoleManager.addRoleHolder(ROLE_SYSTEM_SUPERVISION, callingPackage, currentUser)
        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        whenever(mockActivityManager.startProfile(any()))
            .thenReturn(true) // First call in onCreate succeeds
            .thenReturn(false) // Second call in receiver fails
        whenever(mockActivityManager.isUserRunning(SUPERVISING_USER_ID)).thenReturn(false)
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, true)
        mActivityController.setup()
        assertThat(mActivity.isFinishing).isFalse()

        // Act: Simulate the USER_STOPPED broadcast
        val intent =
            Intent(Intent.ACTION_USER_STOPPED)
                .putExtra(Intent.EXTRA_USER_HANDLE, SUPERVISING_USER_ID)
        shadowOf(ApplicationProvider.getApplicationContext<Application>())
            .getReceiversForIntent(intent)
            .first()
            .onReceive(context, intent)

        // Assert: Activity finishes with RESULT_CANCELED
        verify(mockActivityManager, times(2)).startProfile(SUPERVISING_USER_HANDLE)
        assertThat(mActivity.isFinishing).isTrue()
        assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_CANCELED)
    }

    @Test
    fun userStateChangeReceiver_receivesUserStopped_forDifferentUser_doesNothing() {
        // Arrange: Set up activity
        ShadowRoleManager.addRoleHolder(ROLE_SYSTEM_SUPERVISION, callingPackage, currentUser)
        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        mockActivityManager.stub {
            on { startProfile(any()) } doReturn true
            on { isUserRunning(SUPERVISING_USER_ID) } doReturn false
        }
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, true)
        mActivityController.setup()

        // Act: Simulate USER_STOPPED broadcast for a different user
        val intent =
            Intent(Intent.ACTION_USER_STOPPED)
                .putExtra(Intent.EXTRA_USER_HANDLE, SUPERVISING_USER_ID + 1)
        shadowOf(ApplicationProvider.getApplicationContext<Application>())
            .getReceiversForIntent(intent)
            .first()
            .onReceive(context, intent)

        // Assert: No further action is taken
        verify(mockActivityManager, times(1)).startProfile(SUPERVISING_USER_HANDLE)
        assertThat(mActivity.isFinishing).isFalse()
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun getBiometricPrompt_recoveryEmailExist_showForgotPinButton_flagDisabled() {
        ShadowRoleManager.addRoleHolder(ROLE_SYSTEM_SUPERVISION, callingPackage, currentUser)
        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        val recoveryInfo = SupervisionRecoveryInfo("email", "default", STATE_PENDING, null)
        whenever(mockSupervisionManager.supervisionRecoveryInfo).thenReturn(recoveryInfo)
        mActivityController.setup()

        val biometricPrompt = mActivity.getBiometricPrompt()

        assertThat(biometricPrompt.title)
            .isEqualTo(mActivity.getString(R.string.supervision_full_screen_pin_verification_title))
        assertThat(biometricPrompt.isConfirmationRequired).isTrue()
        assertThat(biometricPrompt.allowedAuthenticators)
            .isEqualTo(BiometricManager.Authenticators.DEVICE_CREDENTIAL)

        val fallbackOptions = biometricPrompt.getFallbackOptions()
        assertThat(fallbackOptions).isNotNull()
        assertThat(fallbackOptions).hasSize(1)

        val forgotPinOption =
            fallbackOptions.find {
                it.getText().toString() ==
                    mActivity.getString(R.string.supervision_auth_prompt_forgot_pin_button_label)
            }
        assertThat(forgotPinOption).isNotNull()
        assertThat(forgotPinOption!!.getIconType()).isEqualTo(BiometricManager.ICON_TYPE_ACCOUNT)

        mActivity.onForgotPinFallbackClicked()
        verify(metricsRule.metricsFeatureProvider)
            .action(mActivity, SettingsEnums.ACTION_SUPERVISION_FORGOT_PIN_DURING_PIN_INVOCATION)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun getBiometricPrompt_recoveryEmailExist_showForgotPinButton_flagEnabled() {
        ShadowRoleManager.addRoleHolder(ROLE_SYSTEM_SUPERVISION, callingPackage, currentUser)
        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        setCanLaunchPinRecovery(true)
        mActivityController.setup()

        val biometricPrompt = mActivity.getBiometricPrompt()

        assertThat(biometricPrompt.title)
            .isEqualTo(mActivity.getString(R.string.supervision_full_screen_pin_verification_title))
        assertThat(biometricPrompt.isConfirmationRequired).isTrue()
        assertThat(biometricPrompt.allowedAuthenticators)
            .isEqualTo(BiometricManager.Authenticators.DEVICE_CREDENTIAL)

        val fallbackOptions = biometricPrompt.getFallbackOptions()
        assertThat(fallbackOptions).isNotNull()
        assertThat(fallbackOptions).hasSize(1)

        val forgotPinOption =
            fallbackOptions.find {
                it.getText().toString() ==
                    mActivity.getString(R.string.supervision_auth_prompt_forgot_pin_button_label)
            }
        assertThat(forgotPinOption).isNotNull()
        assertThat(forgotPinOption!!.getIconType()).isEqualTo(BiometricManager.ICON_TYPE_ACCOUNT)

        mActivity.onForgotPinFallbackClicked()
        verify(metricsRule.metricsFeatureProvider)
            .action(mActivity, SettingsEnums.ACTION_SUPERVISION_FORGOT_PIN_DURING_PIN_INVOCATION)
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun getBiometricPrompt_recoveryInfoEmpty_noForgotPinButton_flagDisabled() {
        ShadowRoleManager.addRoleHolder(ROLE_SYSTEM_SUPERVISION, callingPackage, currentUser)
        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        whenever(mockSupervisionManager.supervisionRecoveryInfo).thenReturn(null)
        mActivityController.setup()

        val biometricPrompt = mActivity.getBiometricPrompt()

        assertThat(biometricPrompt.title)
            .isEqualTo(mActivity.getString(R.string.supervision_full_screen_pin_verification_title))
        assertThat(biometricPrompt.isConfirmationRequired).isTrue()
        assertThat(biometricPrompt.allowedAuthenticators)
            .isEqualTo(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        assertThat(biometricPrompt.getFallbackOptions()).isEmpty()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun getBiometricPrompt_recoveryInfoEmpty_noForgotPinButton_flagEnabled() {
        ShadowRoleManager.addRoleHolder(ROLE_SYSTEM_SUPERVISION, callingPackage, currentUser)
        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        setCanLaunchPinRecovery(false)
        mActivityController.setup()

        val biometricPrompt = mActivity.getBiometricPrompt()

        assertThat(biometricPrompt.title)
            .isEqualTo(mActivity.getString(R.string.supervision_full_screen_pin_verification_title))
        assertThat(biometricPrompt.isConfirmationRequired).isTrue()
        assertThat(biometricPrompt.allowedAuthenticators)
            .isEqualTo(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        assertThat(biometricPrompt.getFallbackOptions()).isEmpty()
    }

    @Test
    fun getBiometricPrompt_withApprovalMethods_showsFallbackOptions() {
        ShadowRoleManager.addRoleHolder(ROLE_SYSTEM_SUPERVISION, callingPackage, currentUser)
        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, true)
        val activityInfo1 =
            ActivityInfo().apply {
                packageName = "pkg"
                name = "Activity1"
            }
        val resolveInfo1 =
            ResolveInfo().apply {
                activityInfo = activityInfo1
                nonLocalizedLabel = "Method 1"
            }
        val activityInfo2 =
            ActivityInfo().apply {
                packageName = "pkg"
                name = "Activity2"
            }
        val resolveInfo2 =
            ResolveInfo().apply {
                activityInfo = activityInfo2
                nonLocalizedLabel = "Method 2"
            }
        val approvalMethods = listOf(resolveInfo1, resolveInfo2)
        val componentName = ComponentName("pkg", "test")
        val extras = Bundle().apply { putBoolean("key_boolean", true) }
        val intentWithExtras = Intent().putExtras(extras)
        whenever(mockISupervisionManager.querySupervisionApprovalActivities(any()))
            .thenReturn(approvalMethods)
        setUpActivity(forceConfirm = false, intent = intentWithExtras)
        mActivityController.setup()

        val biometricPrompt = mActivity.getBiometricPrompt()

        val fallbackOptions = biometricPrompt.getFallbackOptions()
        assertThat(fallbackOptions).isNotNull()
        assertThat(fallbackOptions).hasSize(2)
        assertThat(fallbackOptions.map { it.getText().toString() })
            .containsExactly("Method 1", "Method 2")

        mActivity.onApprovalMethodsClicked(componentName)

        val startedActivity = shadowActivity.nextStartedActivity
        assertThat(startedActivity).isNotNull()
        assertThat(startedActivity.action)
            .isEqualTo(SupervisionManager.ACTION_CONFIRM_SUPERVISION_APPROVAL)
        assertThat(startedActivity.extras?.getBoolean("key_boolean")).isTrue()
    }

    @Test
    fun getBiometricPrompt_noApprovalMethods_showsNoFallbackOptions() {
        ShadowRoleManager.addRoleHolder(ROLE_SYSTEM_SUPERVISION, callingPackage, currentUser)
        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        mActivityController.setup()
        val biometricPrompt = mActivity.getBiometricPrompt()

        val fallbackOptions = biometricPrompt.getFallbackOptions()
        assertThat(fallbackOptions).isEmpty()
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun getBiometricPrompt_flagDisabled_withApprovalMethods_showsNoFallbackOptions() {
        ShadowRoleManager.addRoleHolder(ROLE_SYSTEM_SUPERVISION, callingPackage, currentUser)
        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        val activityInfo1 =
            ActivityInfo().apply {
                packageName = "pkg"
                name = "Activity1"
            }
        val resolveInfo1 =
            ResolveInfo().apply {
                activityInfo = activityInfo1
                nonLocalizedLabel = "Method 1"
            }
        whenever(mockISupervisionManager.querySupervisionApprovalActivities(any()))
            .thenReturn(listOf(resolveInfo1))
        mActivityController.setup()
        val biometricPrompt = mActivity.getBiometricPrompt()
        val fallbackOptions = biometricPrompt.getFallbackOptions()

        assertThat(fallbackOptions).isEmpty()
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

    private fun setUpActivity(forceConfirm: Boolean, intent: Intent = Intent()) {
        // Note, we have to use ActivityController (instead of ActivityScenario) in order to access
        // the activity before it is created, so we can set up various mocked responses before they
        // are referenced in onCreate.
        if (forceConfirm) {
            intent.putExtra(EXTRA_FORCE_CONFIRMATION, true)
        }
        mActivityController =
            Robolectric.buildActivity(ConfirmSupervisionCredentialsActivity::class.java, intent)
        mActivity = mActivityController.get()

        shadowActivity = shadowOf(mActivity)
        shadowActivity.setCallingPackage(callingPackage)
        shadowKeyguardManager = shadowOf(mActivity.getSystemService(KeyguardManager::class.java))
        Shadow.extract<ShadowContextImpl>(mActivity.baseContext).apply {
            // Mock the ISupervisionManager service
            setSystemService(Context.SUPERVISION_SERVICE, mockISupervisionManager.asBinder())
            setSystemService(Context.ACTIVITY_SERVICE, mockActivityManager)
            setSystemService(Context.SUPERVISION_SERVICE, mockSupervisionManager)
            setSystemService(Context.USER_SERVICE, mockUserManager)
        }
    }

    private fun setCanLaunchPinRecovery(canLaunch: Boolean) {
        whenever(mockISupervisionManager.canLaunchPinRecovery(any())).thenReturn(canLaunch)
    }

    private companion object {
        const val SUPERVISING_USER_ID = 5
        val SUPERVISING_USER_HANDLE = UserHandle.of(SUPERVISING_USER_ID)
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
