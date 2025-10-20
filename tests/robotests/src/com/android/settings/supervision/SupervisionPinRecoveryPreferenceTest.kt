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
import android.app.settings.SettingsEnums.ACTION_SUPERVISION_FORGOT_PIN
import android.app.supervision.SupervisionManager
import android.app.supervision.SupervisionRecoveryInfo
import android.app.supervision.SupervisionRecoveryInfo.STATE_PENDING
import android.app.supervision.SupervisionRecoveryInfo.STATE_VERIFIED
import android.app.supervision.flags.Flags
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
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
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.shadows.ShadowToast

@RunWith(AndroidJUnit4::class)
class SupervisionPinRecoveryPreferenceTest {

    private val appContext: Context = ApplicationProvider.getApplicationContext()
    private val mockSupervisionManager = mock<SupervisionManager>()
    private val mockPinRecoveryLauncher = mock<ActivityResultLauncher<Intent>>()
    private val preference = SupervisionPinRecoveryPreference()
    private val mContext =
        object : ContextWrapper(appContext) {
            override fun getSystemService(name: String): Any =
                when (name) {
                    SUPERVISION_SERVICE -> mockSupervisionManager
                    else -> super.getSystemService(name)
                }
        }

    private val mockLifeCycleContext =
        mock<PreferenceLifecycleContext>(
            useConstructor = withArguments(mContext),
            defaultAnswer = CALLS_REAL_METHODS,
        )

    @get:Rule val setFlagsRule = SetFlagsRule()
    @get:Rule val metricsRule = MetricsRule()

    @Before
    fun setUp() {
        whenever(
                mockLifeCycleContext.registerForActivityResult(
                    any<ActivityResultContracts.StartActivityForResult>(),
                    any(),
                )
            )
            .thenReturn(mockPinRecoveryLauncher)
        preference.onCreate(mockLifeCycleContext)
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
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_PIN_RECOVERY_SCREEN)
    fun flagEnabled_recoveryInfoExist_isAvailable() {
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
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_PIN_RECOVERY_SCREEN)
    fun flagEnabled_recoveryVerified_isAvailable() {
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
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_PIN_SNACKBARS_TOAST_MESSAGE)
    fun onRecoveryFlowComplete_showsCorrectToastString() {
        val expectedToastMessage = mContext.getString(R.string.supervision_pin_updated)
        preference.onRecoveryFlowComplete(ActivityResult(Activity.RESULT_OK, null))
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(expectedToastMessage)
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_PIN_SNACKBARS_TOAST_MESSAGE)
    fun onRecoveryFlowComplete_flagDisabled_noToast() {
        preference.onRecoveryFlowComplete(ActivityResult(Activity.RESULT_OK, null))
        assertThat(ShadowToast.getTextOfLatestToast()).isNull()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_PIN_SNACKBARS_TOAST_MESSAGE)
    fun onRecoveryFlowComplete_flowCanceled_noToast() {
        preference.onRecoveryFlowComplete(ActivityResult(Activity.RESULT_CANCELED, null))
        assertThat(ShadowToast.getTextOfLatestToast()).isNull()
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
}
