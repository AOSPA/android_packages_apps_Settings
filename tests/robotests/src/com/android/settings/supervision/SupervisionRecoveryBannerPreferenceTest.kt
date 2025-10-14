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

import android.app.supervision.SupervisionManager
import android.app.supervision.SupervisionRecoveryInfo
import android.app.supervision.SupervisionRecoveryInfo.STATE_PENDING
import android.app.supervision.SupervisionRecoveryInfo.STATE_VERIFIED
import android.app.supervision.flags.Flags
import android.content.Context
import android.content.ContextWrapper
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.widget.Button
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.testutils.inflateViewHolder
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.BannerMessagePreference
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class SupervisionRecoveryBannerPreferenceTest {
    private var preference = SupervisionRecoveryBannerPreference()
    private val appContext: Context = ApplicationProvider.getApplicationContext()
    private val mockSupervisionManager = mock<SupervisionManager>()
    private val context =
        object : ContextWrapper(appContext) {
            override fun getSystemService(name: String): Any =
                when (name) {
                    SUPERVISION_SERVICE -> mockSupervisionManager
                    else -> super.getSystemService(name)
                }
        }
    @get:Rule val setFlagsRule = SetFlagsRule()

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun noRecoveryInfo_showsAddFlowText() {
        // Corresponds to the "else" branch: !hasAccount && !showVerifyFlow
        whenever(mockSupervisionManager.getSupervisionRecoveryInfo()).thenReturn(null)

        preference.createAndBindWidget<BannerMessagePreference>(context).also { banner ->
            assertThat(banner.getTitle())
                .isEqualTo(context.getString(R.string.supervision_recovery_banner_title_add))
            assertThat(banner.getSummary())
                .isEqualTo(context.getString(R.string.supervision_recovery_banner_summary_add))
            val holder = banner.inflateViewHolder()
            val positiveButton = holder.itemView.findViewById<Button>(R.id.banner_positive_btn)
            assertThat(positiveButton.text.toString()).isEqualTo(context.getString(R.string.add))
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun recoveryPending_showsVerifyFlowText() {
        // Corresponds to the "if (showVerifyFlow)" branch: hasAccount && state == STATE_PENDING
        val recoveryInfo =
            SupervisionRecoveryInfo(
                /* accountName */ "test@email.com",
                /* accountType */ "default",
                /* state */ STATE_PENDING,
                /* accountData */ null,
            )
        whenever(mockSupervisionManager.getSupervisionRecoveryInfo()).thenReturn(recoveryInfo)

        preference.createAndBindWidget<BannerMessagePreference>(context).also { banner ->
            assertThat(banner.getTitle())
                .isEqualTo(context.getString(R.string.supervision_recovery_banner_title_verify))
            assertThat(banner.getSummary())
                .isEqualTo(context.getString(R.string.supervision_recovery_banner_summary_verify))
            val holder = banner.inflateViewHolder()
            val positiveButton = holder.itemView.findViewById<Button>(R.id.banner_positive_btn)
            assertThat(positiveButton.text.toString()).isEqualTo(context.getString(R.string.verify))
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun recoveryVerified_isNotAvailable() {
        val recoveryInfo =
            SupervisionRecoveryInfo(
                /* accountName */ "email",
                /* accountType */ "default",
                /* state */ STATE_VERIFIED,
                /* accountData */ null,
            )
        whenever(mockSupervisionManager.getSupervisionRecoveryInfo()).thenReturn(recoveryInfo)
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
        val recoveryInfo =
            SupervisionRecoveryInfo(
                /* accountName */ "test@email.com",
                /* accountType */ "default",
                /* state */ STATE_PENDING,
                /* accountData */ null,
            )
        whenever(mockSupervisionManager.getSupervisionRecoveryInfo()).thenReturn(recoveryInfo)
        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun flagDisabled_recoveryVerified_isNotAvailable() {
        val recoveryInfo =
            SupervisionRecoveryInfo(
                /* accountName */ "email",
                /* accountType */ "default",
                /* state */ STATE_VERIFIED,
                /* accountData */ null,
            )
        whenever(mockSupervisionManager.getSupervisionRecoveryInfo()).thenReturn(recoveryInfo)
        assertThat(preference.isAvailable(context)).isFalse()
    }
}
