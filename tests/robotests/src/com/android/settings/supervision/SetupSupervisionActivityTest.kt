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

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.app.Application
import android.app.KeyguardManager
import android.app.settings.SettingsEnums.ACTION_SUPERVISION_ENABLE_SUPERVISION
import android.app.settings.SettingsEnums.ACTION_SUPERVISION_PIN_SET_UP
import android.app.settings.SettingsEnums.ACTION_SUPERVISION_SKIP_PIN_RECOVERY
import android.app.supervision.ISupervisionManager
import android.app.supervision.SupervisionManager
import android.app.supervision.flags.Flags
import android.content.Context
import android.content.pm.UserInfo
import android.os.UserHandle
import android.os.UserManager
import android.os.UserManager.USER_TYPE_FULL_SYSTEM
import android.os.UserManager.USER_TYPE_PROFILE_MANAGED
import android.os.UserManager.USER_TYPE_PROFILE_SUPERVISING
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.password.ChooseLockGeneric
import com.android.settings.supervision.credentialmanagement.SupervisionPinRecoveryActivity
import com.android.settings.testutils.MetricsRule
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupdesign.GlifLayout
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowContextImpl
import org.robolectric.shadows.ShadowKeyguardManager
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowServiceManager

@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowAlertDialogCompat::class])
class SetupSupervisionActivityTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val mockSupervisionManager = mock<SupervisionManager>()
    private val mockUserManager = mock<UserManager>()

    private lateinit var shadowKeyguardManager: ShadowKeyguardManager
    private val mockISupervisionManager = mock<ISupervisionManager>()

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule val metricsRule = MetricsRule()
    @get:Rule val setFlagsRule = SetFlagsRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        SetupSupervisionActivity.ioDispatcher = testDispatcher
        shadowKeyguardManager = shadowOf(context.getSystemService(KeyguardManager::class.java))
        Shadow.extract<ShadowContextImpl>((context as Application).baseContext).apply {
            setSystemService(Context.SUPERVISION_SERVICE, mockSupervisionManager)
            setSystemService(Context.USER_SERVICE, mockUserManager)
        }

        mockUserManager.stub { on { users } doReturn listOf(SUPERVISING_USER_INFO) }
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, false)
        ShadowServiceManager.addBinderService(
            Context.SUPERVISION_SERVICE,
            ISupervisionManager::class.java,
            mockISupervisionManager,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        SetupSupervisionActivity.ioDispatcher = Dispatchers.IO
    }

    @Test
    fun onCreate_noSupervisingUser_loadProgressBar() {
        mockUserManager.stub { on { users } doReturn emptyList() }

        ActivityScenario.launch(SetupSupervisionActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val layout = activity.findViewById<GlifLayout>(R.id.supervision_setup_introduction)
                val footer = layout.getMixin(FooterBarMixin::class.java)
                footer.getPrimaryButtonView().performClick()

                assertThat(layout.isProgressBarShown).isTrue()
            }
        }
    }

    @Test
    fun onCreate_noSupervisingUser_createProfile_startSetPinActivity() = runTest {
        mockUserManager.stub {
            on { users } doReturn emptyList()
            on {
                createProfileForUserEvenWhenDisallowed(any(), any(), any(), any(), anyOrNull())
            } doReturn SUPERVISING_USER_INFO
        }
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, true)

        ActivityScenario.launch(SetupSupervisionActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                clickNextOnIntroductionScreen(activity)

                assertThat(shadowOf(activity).nextStartedActivity.component?.className)
                    .isEqualTo(ChooseLockGeneric::class.java.name)

                assertThat(activity.isFinishing).isFalse()
            }
        }

        verify(mockUserManager)
            .createProfileForUserEvenWhenDisallowed(
                "Supervising",
                USER_TYPE_PROFILE_SUPERVISING,
                /* flags= */ 0,
                UserHandle.USER_NULL,
                null,
            )
    }

    @Test
    fun onCreate_existingSupervisingUser_startSetPinActivity() = runTest {
        ActivityScenario.launch(SetupSupervisionActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                clickNextOnIntroductionScreen(activity)

                assertThat(shadowOf(activity).nextStartedActivity.component?.className)
                    .isEqualTo(ChooseLockGeneric::class.java.name)

                assertThat(activity.isFinishing).isFalse()
            }
        }

        verify(mockUserManager, never())
            .createProfileForUserEvenWhenDisallowed(any(), any(), any(), any(), any())
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun onCreate_withoutUiUpdates_existingSupervisingLock_canceled() {
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, true)

        ActivityScenario.launchActivityForResult(SetupSupervisionActivity::class.java).use {
            scenario ->
            assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
            assertThat(scenario.result.resultCode).isEqualTo(RESULT_CANCELED)
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun onCreate_existingSupervisedUserAndExistingSupervisingLock_canceled() {
        mockSupervisionManager.stub { on { isSupervisionEnabled } doReturn true }
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, true)

        ActivityScenario.launchActivityForResult(SetupSupervisionActivity::class.java).use {
            scenario ->
            assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
            assertThat(scenario.result.resultCode).isEqualTo(RESULT_CANCELED)
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun onCreate_existingSupervisingLock_startsConfirmCredentialsActivity() {
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, true)

        ActivityScenario.launch(SetupSupervisionActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val nextActivity = shadowOf(activity).nextStartedActivity
                assertThat(nextActivity.component?.className)
                    .isEqualTo(ConfirmSupervisionCredentialsActivity::class.java.name)
            }
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun onConfirmCredentialsResult_ok_setsResultOkAndEnablesSupervision() {
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, true)

        ActivityScenario.launchActivityForResult(SetupSupervisionActivity::class.java).use {
            scenario ->
            scenario.onActivity { activity ->
                val shadowActivity = shadowOf(activity)
                shadowActivity.receiveResult(
                    shadowActivity.nextStartedActivityForResult.intent,
                    RESULT_OK,
                    null,
                )

                assertThat(activity.isFinishing).isTrue()
            }
            assertThat(scenario.result.resultCode).isEqualTo(RESULT_OK)
        }
        verify(mockSupervisionManager).setSupervisionEnabled(true)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun onConfirmCredentialsResult_ok_logsMetrics() {
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, true)

        ActivityScenario.launchActivityForResult(SetupSupervisionActivity::class.java).use {
            scenario ->
            scenario.onActivity { activity ->
                val shadowActivity = shadowOf(activity)
                shadowActivity.receiveResult(
                    shadowActivity.nextStartedActivityForResult.intent,
                    RESULT_OK,
                    null,
                )

                verify(metricsRule.metricsFeatureProvider)
                    .action(eq(activity), eq(ACTION_SUPERVISION_ENABLE_SUPERVISION))
                verify(metricsRule.metricsFeatureProvider, never())
                    .action(eq(activity), eq(ACTION_SUPERVISION_PIN_SET_UP))
            }
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun onConfirmCredentialsResult_canceled_setsResultCanceled() {
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, true)

        ActivityScenario.launchActivityForResult(SetupSupervisionActivity::class.java).use {
            scenario ->
            scenario.onActivity { activity ->
                val shadowActivity = shadowOf(activity)
                shadowActivity.receiveResult(
                    shadowActivity.nextStartedActivityForResult.intent,
                    RESULT_CANCELED,
                    null,
                )

                assertThat(activity.isFinishing).isTrue()
            }
            assertThat(scenario.result.resultCode).isEqualTo(RESULT_CANCELED)
        }
        verify(mockSupervisionManager, never()).setSupervisionEnabled(true)
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun onResume_withoutUiUpdates_existingSupervisingLock_finishes() {
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, false)

        ActivityScenario.launchActivityForResult(SetupSupervisionActivity::class.java).use {
            scenario ->
            scenario.moveToState(Lifecycle.State.STARTED)

            shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, true)
            scenario.moveToState(Lifecycle.State.RESUMED)

            scenario.onActivity { activity -> assertThat(activity.isFinishing).isTrue() }
            assertThat(scenario.result.resultCode).isEqualTo(RESULT_OK)
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun onResume_existingSupervisingLock_doesNotFinish() {
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, false)

        ActivityScenario.launchActivityForResult(SetupSupervisionActivity::class.java).use {
            scenario ->
            scenario.moveToState(Lifecycle.State.STARTED)

            shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, true)
            scenario.moveToState(Lifecycle.State.RESUMED)

            scenario.onActivity { activity -> assertThat(activity.isFinishing).isFalse() }
        }
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_SUPERVISION_PIN_RECOVERY_SCREEN,
        Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES,
    )
    fun onSetLockResult_startsRecoveryActivity() = runTest {
        whenever(mockISupervisionManager.canLaunchPinRecovery(any())).thenReturn(false)
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, false)

        ActivityScenario.launch(SetupSupervisionActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                clickNextOnIntroductionScreen(activity)
                val shadowActivity = shadowOf(activity)
                shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, true)
                shadowActivity.receiveResult(
                    shadowActivity.nextStartedActivityForResult.intent,
                    RESULT_OK,
                    null,
                )

                assertThat(shadowActivity.nextStartedActivity.component?.className)
                    .isEqualTo(SupervisionPinRecoveryActivity::class.java.name)
                assertThat(activity.isFinishing).isFalse()
            }
        }
        verify(mockSupervisionManager).setSupervisionEnabled(true)
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_SUPERVISION_PIN_RECOVERY_SCREEN,
        Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES,
    )
    fun onSetLockResult_logsMetrics() = runTest {
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, false)

        ActivityScenario.launch(SetupSupervisionActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                clickNextOnIntroductionScreen(activity)
                val shadowActivity = shadowOf(activity)
                shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, true)
                shadowActivity.receiveResult(
                    shadowActivity.nextStartedActivityForResult.intent,
                    RESULT_OK,
                    null,
                )

                verify(metricsRule.metricsFeatureProvider)
                    .action(eq(activity), eq(ACTION_SUPERVISION_ENABLE_SUPERVISION))
                verify(metricsRule.metricsFeatureProvider)
                    .action(eq(activity), eq(ACTION_SUPERVISION_PIN_SET_UP))
            }
        }
    }

    @Test
    fun onSetLockResult_supervisingUserNull_canceled() = runTest {
        ActivityScenario.launchActivityForResult(SetupSupervisionActivity::class.java).use {
            scenario ->
            scenario.onActivity { activity ->
                clickNextOnIntroductionScreen(activity)
                mockUserManager.stub { on { users } doReturn emptyList() }

                val shadowActivity = shadowOf(activity)
                shadowActivity.receiveResult(
                    shadowActivity.nextStartedActivityForResult.intent,
                    RESULT_OK,
                    null,
                )

                assertThat(activity.isFinishing).isTrue()
            }
            assertThat(scenario.result.resultCode).isEqualTo(RESULT_CANCELED)
        }
        verify(mockSupervisionManager, never()).setSupervisionEnabled(any())
    }

    @Test
    fun onSetLockResult_supervisingUserNotSecure_canceled() = runTest {
        ActivityScenario.launchActivityForResult(SetupSupervisionActivity::class.java).use {
            scenario ->
            scenario.onActivity { activity ->
                clickNextOnIntroductionScreen(activity)
                val shadowActivity = shadowOf(activity)
                shadowActivity.receiveResult(
                    shadowActivity.nextStartedActivityForResult.intent,
                    RESULT_OK,
                    null,
                )

                assertThat(activity.isFinishing).isTrue()
            }
            assertThat(scenario.result.resultCode).isEqualTo(RESULT_CANCELED)
        }
        verify(mockSupervisionManager, never()).setSupervisionEnabled(any())
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_SUPERVISION_PIN_RECOVERY_SCREEN,
        Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES,
    )
    fun canLaunchPinRecovery_onPinRecoveryResultOk_finishesOk() = runTest {
        whenever(mockISupervisionManager.canLaunchPinRecovery(any())).thenReturn(false)
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, false)

        ActivityScenario.launchActivityForResult(SetupSupervisionActivity::class.java).use {
            scenario ->
            scenario.onActivity { activity ->
                clickNextOnIntroductionScreen(activity)
                val shadowActivity = shadowOf(activity)
                shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, true)
                // Set PIN result.
                shadowActivity.receiveResult(
                    shadowActivity.nextStartedActivityForResult.intent,
                    RESULT_OK,
                    null,
                )

                // PIN recovery set up activity was invoked
                val nextActivity = shadowActivity.nextStartedActivity
                assertThat(nextActivity.component?.className)
                    .isEqualTo(SupervisionPinRecoveryActivity::class.java.name)

                // PIN recovery result.
                shadowActivity.receiveResult(
                    shadowActivity.nextStartedActivityForResult.intent,
                    RESULT_OK,
                    null,
                )

                verify(metricsRule.metricsFeatureProvider, never())
                    .action(eq(activity), eq(ACTION_SUPERVISION_SKIP_PIN_RECOVERY))
                assertThat(activity.isFinishing).isTrue()
            }
            assertThat(scenario.result.resultCode).isEqualTo(RESULT_OK)
        }
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_SUPERVISION_PIN_RECOVERY_SCREEN,
        Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES,
    )
    fun canLaunchPinRecovery_onPinRecoveryResultCancelled_finishesOkWithLog() = runTest {
        whenever(mockISupervisionManager.canLaunchPinRecovery(any())).thenReturn(false)
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, false)

        ActivityScenario.launchActivityForResult(SetupSupervisionActivity::class.java).use {
            scenario ->
            scenario.onActivity { activity ->
                clickNextOnIntroductionScreen(activity)
                val shadowActivity = shadowOf(activity)
                shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, true)
                // Set PIN result.
                shadowActivity.receiveResult(
                    shadowActivity.nextStartedActivityForResult.intent,
                    RESULT_OK,
                    null,
                )

                // PIN recovery set up activity was invoked
                val nextActivity = shadowActivity.nextStartedActivity
                assertThat(nextActivity.component?.className)
                    .isEqualTo(SupervisionPinRecoveryActivity::class.java.name)

                // PIN recovery result.
                shadowActivity.receiveResult(
                    shadowActivity.nextStartedActivityForResult.intent,
                    RESULT_CANCELED,
                    null,
                )

                verify(metricsRule.metricsFeatureProvider)
                    .action(eq(activity), eq(ACTION_SUPERVISION_SKIP_PIN_RECOVERY))
                assertThat(activity.isFinishing).isTrue()
            }
            assertThat(scenario.result.resultCode).isEqualTo(RESULT_OK)
        }
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_SUPERVISION_PIN_RECOVERY_SCREEN,
        Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES,
    )
    fun canNotLaunchPinRecovery_finishesWithoutStartingPinRecovery() = runTest {
        whenever(mockISupervisionManager.canLaunchPinRecovery(any())).thenReturn(true)
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, false)

        ActivityScenario.launchActivityForResult(SetupSupervisionActivity::class.java).use {
            scenario ->
            scenario.onActivity { activity ->
                clickNextOnIntroductionScreen(activity)
                val shadowActivity = shadowOf(activity)
                shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, true)
                // Set PIN result.
                shadowActivity.receiveResult(
                    shadowActivity.nextStartedActivityForResult.intent,
                    RESULT_OK,
                    null,
                )

                assertThat(activity.isFinishing).isTrue()
            }
            assertThat(scenario.result.resultCode).isEqualTo(RESULT_OK)
        }
    }

    @Test
    fun onCreate_createUserFails_showsErrorScreen() = runTest {
        mockUserManager.stub {
            on { users } doReturn emptyList()
            on {
                createProfileForUserEvenWhenDisallowed(any(), any(), any(), any(), anyOrNull())
            } doReturn null
        }

        ActivityScenario.launchActivityForResult(SetupSupervisionActivity::class.java).use {
            scenario ->
            scenario.onActivity { activity ->
                clickNextOnIntroductionScreen(activity)
                val nextStartedActivity = shadowOf(activity).nextStartedActivity
                assertThat(nextStartedActivity.component?.className)
                    .isEqualTo(SupervisionErrorActivity::class.java.name)

                assertThat(activity.isFinishing).isTrue()
            }
            assertThat(scenario.result.resultCode).isEqualTo(RESULT_CANCELED)
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun onCreate_multipleProfilesExist_showsErrorDialog() {
        mockUserManager.stub {
            on { userProfiles } doReturn
                listOf(
                    MAIN_USER.userHandle,
                    WORK_PROFILE.userHandle,
                    SUPERVISING_USER_INFO.userHandle,
                )
            on { getUserInfo(MAIN_USER.id) } doReturn MAIN_USER
            on { getUserInfo(WORK_PROFILE.id) } doReturn WORK_PROFILE
            on { getUserInfo(SUPERVISING_USER_INFO.id) } doReturn SUPERVISING_USER_INFO
        }

        ActivityScenario.launch(SetupSupervisionActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val dialog = ShadowAlertDialogCompat.getLatestAlertDialog()
                assertThat(dialog).isNotNull()
                assertThat(ShadowAlertDialogCompat.shadowOf(dialog).title)
                    .isEqualTo(
                        context.getString(R.string.supervision_setup_multi_profile_error_title)
                    )

                dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.performClick()
                ShadowLooper.idleMainLooper()

                assertThat(activity.isFinishing).isTrue()
            }
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun onIntroductionScreen_nextButton_enablesSupervision() = runTest {
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, false)
        mockUserManager.stub {
            on { users } doReturn emptyList()
            on {
                createProfileForUserEvenWhenDisallowed(any(), any(), any(), any(), anyOrNull())
            } doReturn SUPERVISING_USER_INFO
        }

        ActivityScenario.launch(SetupSupervisionActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                clickNextOnIntroductionScreen(activity)

                val layout =
                    activity.findViewById<com.google.android.setupdesign.GlifLayout>(
                        R.id.supervision_setup_introduction
                    )
                assertThat(layout.isProgressBarShown).isTrue()

                assertThat(shadowOf(activity).nextStartedActivity.component?.className)
                    .isEqualTo(ChooseLockGeneric::class.java.name)
            }
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun onIntroductionScreen_cancelButton_finishesActivity() {
        shadowKeyguardManager.setIsDeviceSecure(SUPERVISING_USER_ID, false)
        mockUserManager.stub { on { users } doReturn emptyList() }

        ActivityScenario.launchActivityForResult(SetupSupervisionActivity::class.java).use {
            scenario ->
            scenario.onActivity { activity ->
                val layout =
                    activity.findViewById<com.google.android.setupdesign.GlifLayout>(
                        R.id.supervision_setup_introduction
                    )
                val footer =
                    layout.getMixin(
                        com.google.android.setupcompat.template.FooterBarMixin::class.java
                    )
                footer.getSecondaryButtonView().performClick()

                assertThat(activity.isFinishing).isTrue()
            }
            assertThat(scenario.result.resultCode).isEqualTo(RESULT_CANCELED)
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun onCreate_onScreenRotation_keepsPinIntroductionUi() {
        mockUserManager.stub { on { users } doReturn emptyList() }
        ActivityScenario.launch(SetupSupervisionActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val layout = activity.findViewById<GlifLayout>(R.id.supervision_setup_introduction)
                val footer = layout.getMixin(FooterBarMixin::class.java)
                footer.getPrimaryButtonView().performClick()
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                val layout = activity.findViewById<GlifLayout>(R.id.supervision_setup_introduction)
                val footer = layout.getMixin(FooterBarMixin::class.java)
                assertThat(layout.isProgressBarShown).isTrue()
                assertThat(footer.buttonContainer.visibility).isEqualTo(View.GONE)
            }
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun onCreate_setsContentDescriptionForIntro() {
        ActivityScenario.launch(SetupSupervisionActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val layout = activity.findViewById<GlifLayout>(R.id.supervision_setup_introduction)
                val descriptionView = layout.getDescriptionTextView()
                assertThat(descriptionView.contentDescription)
                    .isEqualTo(context.getString(R.string.supervision_intro_content_description))
            }
        }
    }

    private fun clickNextOnIntroductionScreen(activity: SetupSupervisionActivity) {
        val layout = activity.findViewById<GlifLayout>(R.id.supervision_setup_introduction)
        val footer = layout.getMixin(FooterBarMixin::class.java)
        footer.getPrimaryButtonView().performClick()
        ShadowLooper.idleMainLooper()
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
        private val MAIN_USER = UserInfo(0, "Main", null, 0, USER_TYPE_FULL_SYSTEM)
        private val WORK_PROFILE = UserInfo(11, "Work", null, 0, USER_TYPE_PROFILE_MANAGED)
    }
}
