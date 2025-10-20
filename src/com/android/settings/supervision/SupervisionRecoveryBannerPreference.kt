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
import android.app.supervision.flags.Flags
import android.content.Context
import androidx.preference.Preference
import com.android.settings.R
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.BannerMessagePreference

class SupervisionRecoveryBannerPreference :
    PreferenceMetadata, PreferenceBinding, PreferenceAvailabilityProvider {
    override val key: String
        get() = KEY

    override val indexable: Boolean
        get() = false

    override fun isAvailable(context: Context): Boolean {
        if (!Flags.enableSupervisionSettingsUiUpdates()) {
            return false
        }
        val missingRecovery = context.isMissingRecoveryMethod()
        return missingRecovery && !isDismissed()
    }

    // This method sets up the preference instance and its basic structure.
    override fun createWidget(context: Context): Preference {
        return BannerMessagePreference(context).apply {
            setAttentionLevel(BannerMessagePreference.AttentionLevel.MEDIUM)

            // Add the dismiss 'X' button
            setDismissButtonVisible(true)
            setDismissButtonOnClickListener {
                // TODO(b/446025922): Implement logic to store in shared prefs after user dismissed
                // it).
                isVisible = false
            }
            setPositiveButtonVisible(true)
        }
    }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        val banner = preference as BannerMessagePreference
        val context = banner.context

        val supervisionManager = context.getSystemService(SupervisionManager::class.java)
        val recoveryInfo = supervisionManager?.getSupervisionRecoveryInfo()
        val hasAccount = hasAccountNameSet(recoveryInfo)
        val state = recoveryInfo?.state
        val showVerifyFlow = hasAccount && state == SupervisionRecoveryInfo.STATE_PENDING
        val titleRes =
            if (showVerifyFlow) R.string.supervision_recovery_banner_title_verify
            else R.string.supervision_recovery_banner_title_add
        val summaryRes =
            if (showVerifyFlow) R.string.supervision_recovery_banner_summary_verify
            else R.string.supervision_recovery_banner_summary_add
        val buttonTextRes = if (showVerifyFlow) R.string.verify else R.string.add

        banner.setTitle(titleRes)
        banner.setSummary(summaryRes)
        banner.setPositiveButtonText(buttonTextRes)
        banner.setPositiveButtonOnClickListener {
            // TODO(b/446025922): Implement action to launch Add/Verify flow.
        }
    }

    private fun hasAccountNameSet(info: SupervisionRecoveryInfo?): Boolean {
        return !info?.accountName.isNullOrEmpty()
    }

    private fun isDismissed(): Boolean {
        // TODO(b/446025922): Implement real dismiss logic
        return false
    }

    companion object {
        const val KEY = "supervision_pin_recovery_banner"
    }
}
