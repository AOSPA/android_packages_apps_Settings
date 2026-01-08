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
package com.android.settings.supervision.credentialmanagement

import android.app.Activity
import android.app.KeyguardManager
import android.app.settings.SettingsEnums.ACTION_SUPERVISION_FORGOT_PIN
import android.app.supervision.ISupervisionManager
import android.app.supervision.SupervisionManager
import android.app.supervision.SupervisionRecoveryInfo
import android.app.supervision.SupervisionRecoveryInfo.STATE_PENDING
import android.app.supervision.SupervisionRecoveryInfo.STATE_VERIFIED
import android.app.supervision.flags.Flags
import android.content.Context
import android.content.Context.KEYGUARD_SERVICE
import android.content.Context.USER_SERVICE
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.UserInfo
import android.os.UserManager
import android.os.UserManager.USER_TYPE_PROFILE_SUPERVISING
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.testutils.MetricsRule
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers.CALLS_REAL_METHODS
import org.mockito.kotlin.UseConstructor.Companion.withArguments
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.shadows.ShadowServiceManager
import org.robolectric.shadows.ShadowToast

@RunWith(AndroidJUnit4::class)
class SupervisionPinRecoveryPreferenceTest {

    private val appContext: Context = ApplicationProvider.getApplicationContext()
    private val mockKeyguardManager = mock<KeyguardManager>()
    private val mockUserManager = mock<UserManager>()
    private val mockSupervisionManager = mock<SupervisionManager>()
    private val mockISupervisionManager = mock<ISupervisionManager>()
    private val mockPinRecoveryLauncher = mock<ActivityResultLauncher<Intent>>()
    private val preference = SupervisionPinRecoveryPreference()
    private val mContext =
        object : ContextWrapper(appContext) {
            override fun getSystemService(name: String): Any =
                when (name) {
                    KEYGUARD_SERVICE -> mockKeyguardManager
                    USER_SERVICE -> mockUserManager
                    SUPERVISION_SERVICE -> mockSupervisionManager
                    else -> super.getSystemService(name)
                }
        }

    private val mockLifeCycleContext =
        mock<PreferenceLifecycleContext>(
            useConstructor = withArguments(mContext),
            defaultAnswer = CALLS_REAL_METHODS,
        )
    private var backPressedCalled = false

    @get:Rule val setFlagsRule = SetFlagsRule()
    @get:Rule val metricsRule = MetricsRule()

    @Before
    fun setUp() {
        backPressedCalled = false
        val activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        val activity = activityController.create().start().resume().get()
        activity.setTheme(R.style.Theme_AppCompat)
        val onBackPressedCallback =
            object : OnBackPressedCallback(enabled = true) {
                override fun handleOnBackPressed() {
                    backPressedCalled = true
                }
            }
        activity.onBackPressedDispatcher.addCallback(activity, onBackPressedCallback)
        whenever(mockLifeCycleContext.baseContext).thenReturn(activity)
        whenever(
                mockLifeCycleContext.registerForActivityResult(
                    any<ActivityResultContracts.StartActivityForResult>(),
                    any(),
                )
            )
            .thenReturn(mockPinRecoveryLauncher)
        preference.onCreate(mockLifeCycleContext)
        ShadowServiceManager.addBinderService(
            Context.SUPERVISION_SERVICE,
            ISupervisionManager::class.java,
            mockISupervisionManager,
        )
    }

    @Test
    fun getTitle() {
        assertThat(preference.title).isEqualTo(R.string.supervision_add_forgot_pin_preference_title)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_PIN_RECOVERY_SCREEN)
    fun flagEnabled_recoveryNotExist_notAvailable() {
        whenever(mockSupervisionManager.supervisionRecoveryInfo).thenReturn(null)

        assertThat(preference.isAvailable(mContext)).isFalse()
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun flagDisabled_SettingsUiUpdates_recoveryNotExist_notAvailable() {
        whenever(mockSupervisionManager.supervisionRecoveryInfo).thenReturn(null)

        assertThat(preference.isAvailable(mContext)).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun flagEnabled_SettingsUiUpdates_recoveryNotExist_notAvailable() {
        setCanLaunchPinRecovery(false)

        assertThat(preference.isAvailable(mContext)).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_PIN_RECOVERY_SCREEN)
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun pinRecoveryScreenEnabled_uiUpdatesDisabled_recoveryInfoExist_isAvailable() {
        val recoveryInfo =
            SupervisionRecoveryInfo(
                /* accountName */ "email",
                /* accountType */ "default",
                /* state */ STATE_PENDING,
                /* accountData */ null,
            )
        whenever(mockSupervisionManager.supervisionRecoveryInfo).thenReturn(recoveryInfo)

        assertThat(preference.isAvailable(mContext)).isTrue()
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun flagDisabled_SettingsUiUpdates_recoveryInfoExist_isAvailable() {
        val recoveryInfo =
            SupervisionRecoveryInfo(
                /* accountName */ "email",
                /* accountType */ "default",
                /* state */ STATE_PENDING,
                /* accountData */ null,
            )
        whenever(mockSupervisionManager.supervisionRecoveryInfo).thenReturn(recoveryInfo)

        assertThat(preference.isAvailable(mContext)).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun flagEnabled_SettingsUiUpdates_recoveryInfoExist_isAvailable() {
        val recoveryInfo =
            SupervisionRecoveryInfo(
                /* accountName */ "email",
                /* accountType */ "default",
                /* state */ STATE_PENDING,
                /* accountData */ null,
            )
        setCanLaunchPinRecovery(true)

        assertThat(preference.isAvailable(mContext)).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_PIN_RECOVERY_SCREEN)
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun pinRecoveryScreenEnabled_uiUpdatesDisabled_recoveryVerified_isAvailable() {
        val recoveryInfo =
            SupervisionRecoveryInfo(
                /* accountName */ "email",
                /* accountType */ "default",
                /* state */ STATE_VERIFIED,
                /* accountData */ null,
            )
        whenever(mockSupervisionManager.supervisionRecoveryInfo).thenReturn(recoveryInfo)

        assertThat(preference.isAvailable(mContext)).isTrue()
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun flagDisabled_SettingsUiUpdates_recoveryVerified_isAvailable() {
        val recoveryInfo =
            SupervisionRecoveryInfo(
                /* accountName */ "email",
                /* accountType */ "default",
                /* state */ STATE_VERIFIED,
                /* accountData */ null,
            )
        whenever(mockSupervisionManager.supervisionRecoveryInfo).thenReturn(recoveryInfo)

        assertThat(preference.isAvailable(mContext)).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun flagEnabled_SettingsUiUpdates_recoveryVerified_isAvailable() {
        val recoveryInfo =
            SupervisionRecoveryInfo(
                /* accountName */ "email",
                /* accountType */ "default",
                /* state */ STATE_VERIFIED,
                /* accountData */ null,
            )
        setCanLaunchPinRecovery(true)

        assertThat(preference.isAvailable(mContext)).isTrue()
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_PIN_RECOVERY_SCREEN)
    fun flagDisabled_recoveryInfoExist_notAvailable() {
        val recoveryInfo =
            SupervisionRecoveryInfo(
                /* accountName */ "email",
                /* accountType */ "default",
                /* state */ STATE_VERIFIED,
                /* accountData */ null,
            )
        whenever(mockSupervisionManager.supervisionRecoveryInfo).thenReturn(recoveryInfo)

        assertThat(preference.isAvailable(mContext)).isFalse()
    }

    @Test
    fun onPreferenceClick_launchesCorrectIntentAndLogsMetric() {
        val widget: Preference = preference.createAndBindWidget(mContext)

        mockLifeCycleContext.stub {
            on { findPreference<Preference>(SupervisionPinRecoveryPreference.KEY) } doReturn widget
        }
        val result = preference.onPreferenceClick(widget)
        assertThat(result).isTrue()
        verifyPinRecoveryActivityStarted(SupervisionPinRecoveryActivity.ACTION_RECOVERY)
        verify(metricsRule.metricsFeatureProvider)
            .action(widget.context, ACTION_SUPERVISION_FORGOT_PIN)
    }

    @Test
    fun onRecoveryFlowComplete_showsCorrectToastString() {
        val expectedToastMessage = mContext.getString(R.string.supervision_pin_updated)
        preference.onRecoveryFlowComplete(ActivityResult(Activity.RESULT_OK, null))
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(expectedToastMessage)
    }

    @Test
    fun onRecoveryFlowComplete_flowCanceled_noToast() {
        preference.onRecoveryFlowComplete(ActivityResult(Activity.RESULT_CANCELED, null))
        assertThat(ShadowToast.getTextOfLatestToast()).isNull()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun onRecoveryFlowComplete_uiUpdatesFlagEnabled_notifiesPreferenceChange() {
        // This test verifies that when the UI updates flag is enabled, a preference change
        // notification is sent after the recovery flow completes.

        // Trigger the completion callback.
        preference.onRecoveryFlowComplete(ActivityResult(Activity.RESULT_OK, null))

        // Verify that notifyPreferenceChange is called with the correct key.
        verify(mockLifeCycleContext).notifyPreferenceChange(SupervisionPinRecoveryPreference.KEY)
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun onRecoveryFlowComplete_uiUpdatesFlagDisabled_doesNotNotifyPreferenceChange() {
        // This test verifies that when the UI updates flag is disabled, no preference change
        // notification is sent after the recovery flow completes.

        // Trigger the completion callback.
        preference.onRecoveryFlowComplete(ActivityResult(Activity.RESULT_OK, null))

        // Verify that notifyPreferenceChange is never called.
        verify(mockLifeCycleContext, never())
            .notifyPreferenceChange(SupervisionPinRecoveryPreference.KEY)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun onRecoveryFlowComplete_flowCanceledAndPinNotDeleted_backPressedIsNotCalled() {
        setHasSupervisingCredential(true)
        preference.onRecoveryFlowComplete(ActivityResult(Activity.RESULT_CANCELED, null))
        assertThat(backPressedCalled).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun onRecoveryFlowComplete_flowCanceledAndPinDeleted_backPressedIsCalled() {
        setHasSupervisingCredential(false)
        preference.onRecoveryFlowComplete(ActivityResult(Activity.RESULT_CANCELED, null))
        assertThat(backPressedCalled).isTrue()
    }

    private fun setCanLaunchPinRecovery(canLaunch: Boolean) {
        whenever(mockISupervisionManager.canLaunchPinRecovery(any())).thenReturn(canLaunch)
    }

    private fun verifyPinRecoveryActivityStarted(expectedAction: String) {
        val intentCaptor = argumentCaptor<Intent>()
        verify(mockPinRecoveryLauncher).launch(intentCaptor.capture())
        assertThat(intentCaptor.allValues.size).isEqualTo(1)
        val intent = intentCaptor.firstValue
        assertThat(intent.component?.className)
            .isEqualTo(SupervisionPinRecoveryActivity::class.java.name)
        assertThat(intent.action).isEqualTo(expectedAction)
    }

    private fun setHasSupervisingCredential(isCredentialSet: Boolean) {
        whenever(mockUserManager.getUsers()).thenReturn(listOf(SUPERVISING_USER_INFO))
        whenever(mockKeyguardManager.isDeviceSecure(SUPERVISING_USER_ID))
            .thenReturn(isCredentialSet)
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
