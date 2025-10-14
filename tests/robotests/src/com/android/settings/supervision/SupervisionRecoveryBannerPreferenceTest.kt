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

import android.app.KeyguardManager
import android.app.supervision.SupervisionManager
import android.app.supervision.SupervisionRecoveryInfo
import android.app.supervision.SupervisionRecoveryInfo.STATE_PENDING
import android.app.supervision.SupervisionRecoveryInfo.STATE_VERIFIED
import android.app.supervision.flags.Flags
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.UserInfo
import android.os.UserManager
import android.os.UserManager.USER_TYPE_PROFILE_SUPERVISING
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.testutils.inflateViewHolder
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.BannerMessagePreference
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers.CALLS_REAL_METHODS
import org.mockito.kotlin.UseConstructor.Companion.withArguments
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class SupervisionRecoveryBannerPreferenceTest {
    private var preference = SupervisionRecoveryBannerPreference()
    private val appContext: Context = ApplicationProvider.getApplicationContext()
    private val mockSupervisionManager = mock<SupervisionManager>()
    private val mockKeyguardManager = mock<KeyguardManager>()
    private val mockUserManager = mock<UserManager>()
    private val mockActivityResultLauncher = mock<ActivityResultLauncher<Intent>>()
    private val context =
        object : ContextWrapper(appContext) {
            override fun getSystemService(name: String): Any =
                when (name) {
                    SUPERVISION_SERVICE -> mockSupervisionManager
                    KEYGUARD_SERVICE -> mockKeyguardManager
                    USER_SERVICE -> mockUserManager
                    else -> super.getSystemService(name)
                }
        }
    private val mockLifeCycleContext =
        mock<PreferenceLifecycleContext>(
            useConstructor = withArguments(context),
            defaultAnswer = CALLS_REAL_METHODS,
        )
    @get:Rule val setFlagsRule = SetFlagsRule()

    @Before
    fun setUp() {
        whenever(
                mockLifeCycleContext.registerForActivityResult(
                    any<ActivityResultContracts.StartActivityForResult>(),
                    any(),
                )
            )
            .thenReturn(mockActivityResultLauncher)
        preference.onCreate(mockLifeCycleContext)
        whenever(mockUserManager.users).thenReturn(listOf(SUPERVISING_USER_INFO))
        whenever(mockKeyguardManager.isDeviceSecure(any())).thenReturn(true)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun noRecoveryInfo_showsAddFlowText() {
        whenever(mockSupervisionManager.getSupervisionRecoveryInfo()).thenReturn(null)
        preference.createAndBindWidget<BannerMessagePreference>(context).also { banner ->
            assertThat(banner.getTitle())
                .isEqualTo(context.getString(R.string.supervision_recovery_banner_title_add))
            assertThat(banner.getSummary())
                .isEqualTo(context.getString(R.string.supervision_recovery_banner_summary_add))
            val positiveButton =
                (banner as BannerMessagePreference)
                    .inflateViewHolder()
                    .itemView
                    .findViewById<Button>(R.id.banner_positive_btn)
            assertThat(positiveButton.text.toString()).isEqualTo(context.getString(R.string.add))
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun recoveryPending_showsVerifyFlowText() {
        setSupervisionRecoveryInfo(STATE_PENDING)
        preference.createAndBindWidget<BannerMessagePreference>(context).also { banner ->
            assertThat(banner.getTitle())
                .isEqualTo(context.getString(R.string.supervision_recovery_banner_title_verify))
            assertThat(banner.getSummary())
                .isEqualTo(context.getString(R.string.supervision_recovery_banner_summary_verify))
            val positiveButton =
                banner.inflateViewHolder().itemView.findViewById<Button>(R.id.banner_positive_btn)
            assertThat(positiveButton.text.toString()).isEqualTo(context.getString(R.string.verify))
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun recoveryVerified_isNotAvailable() {
        setSupervisionRecoveryInfo(STATE_VERIFIED)

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun flagDisabled_recoveryMissing_isNotAvailable() {
        whenever(mockSupervisionManager.getSupervisionRecoveryInfo()).thenReturn(null)

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun flagDisabled_recoveryPending_isNotAvailable() {
        setSupervisionRecoveryInfo(STATE_PENDING)

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun flagDisabled_recoveryVerified_isNotAvailable() {
        setSupervisionRecoveryInfo(STATE_VERIFIED)

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun isAvailable_recoveryMissing_noAlternativeApprovalMethods_returnsTrue() {
        whenever(mockSupervisionManager.getSupervisionRecoveryInfo()).thenReturn(null)

        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun isAvailable_recoveryPending_noAlternativeApprovalMethods_returnsTrue() {
        setSupervisionRecoveryInfo(STATE_PENDING)

        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun isAvailable_supervisingCredentialNotSet_returnsFalse() {
        whenever(mockUserManager.users).thenReturn(emptyList())
        whenever(mockSupervisionManager.getSupervisionRecoveryInfo()).thenReturn(null)
        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun bind_noRecoveryInfo_setsAddFlowUIAndLaunchesAction() {
        whenever(mockSupervisionManager.getSupervisionRecoveryInfo()).thenReturn(null)
        val widget: Preference = preference.createAndBindWidget(context)
        val positiveButton =
            (widget as BannerMessagePreference)
                .inflateViewHolder()
                .itemView
                .findViewById<Button>(R.id.banner_positive_btn)
        positiveButton.performClick()
        verifyPinRecoveryActivityStarted(SupervisionPinRecoveryActivity.ACTION_SETUP_VERIFIED)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun bind_recoveryPending_setsVerifyFlowUIAndLaunchesAction() {
        setSupervisionRecoveryInfo(STATE_PENDING)
        val widget: Preference = preference.createAndBindWidget(context)
        val positiveButton =
            (widget as BannerMessagePreference)
                .inflateViewHolder()
                .itemView
                .findViewById<Button>(R.id.banner_positive_btn)
        positiveButton.performClick()
        verifyPinRecoveryActivityStarted(SupervisionPinRecoveryActivity.ACTION_POST_SETUP_VERIFY)
    }

    private fun setSupervisionRecoveryInfo(@SupervisionRecoveryInfo.State state: Int) {
        val recoveryInfo =
            SupervisionRecoveryInfo(
                /* accountName */ "email",
                /* accountType */ "default",
                /* state */ state,
                /* accountData */ null,
            )
        whenever(mockSupervisionManager.getSupervisionRecoveryInfo()).thenReturn(recoveryInfo)
    }

    private fun verifyPinRecoveryActivityStarted(expectedAction: String) {
        val intentCaptor = argumentCaptor<Intent>()
        verify(mockActivityResultLauncher).launch(intentCaptor.capture())
        assertThat(intentCaptor.allValues.size).isEqualTo(1)
        val intent = intentCaptor.firstValue
        assertThat(intent.component?.className)
            .isEqualTo(SupervisionPinRecoveryActivity::class.java.name)
        assertThat(intent.action).isEqualTo(expectedAction)
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
