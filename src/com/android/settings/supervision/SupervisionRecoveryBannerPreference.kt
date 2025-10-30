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
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import com.android.settings.R
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.BannerMessagePreference

class SupervisionRecoveryBannerPreference :
    PreferenceMetadata,
    PreferenceBinding,
    PreferenceAvailabilityProvider,
    PreferenceLifecycleProvider {

    private lateinit var lifeCycleContext: PreferenceLifecycleContext
    private lateinit var setUpRecoveryLauncher: ActivityResultLauncher<Intent>

    override val key: String
        get() = KEY

    override val indexable: Boolean
        get() = false

    override fun isAvailable(context: Context): Boolean {
        if (!Flags.enableSupervisionSettingsUiUpdates()) {
            return false
        }
        // If PIN is deleted, banner should also match the behavior and disappear
        if (!context.isSupervisingCredentialSet) {
            return false
        }
        val missingRecovery = context.isMissingRecoveryMethod()
        return missingRecovery && !isDismissed()
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        lifeCycleContext = context
        setUpRecoveryLauncher =
            context.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                lifeCycleContext.notifyPreferenceChange(KEY)
                lifeCycleContext.notifyPreferenceChange(SupervisionPinManagementScreen.KEY)
            }
    }

    override fun onResume(context: PreferenceLifecycleContext) {
        lifeCycleContext?.notifyPreferenceChange(KEY)
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

        val showVerifyFlow = shouldShowVerifyFlow(context)
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
        banner.setPositiveButtonOnClickListener { onPositiveButtonClick(banner, showVerifyFlow) }
    }

    private fun hasAccountNameSet(info: SupervisionRecoveryInfo?): Boolean {
        return !info?.accountName.isNullOrEmpty()
    }

    private fun isDismissed(): Boolean {
        // TODO(b/446025922): Implement real dismiss logic
        return false
    }

    private fun onPositiveButtonClick(preference: Preference, isVerificationFlow: Boolean) {
        val context = preference.context
        val intent = Intent(context, SupervisionPinRecoveryActivity::class.java)

        if (isVerificationFlow) {
            // It's a "verify" flow
            intent.action = SupervisionPinRecoveryActivity.ACTION_POST_SETUP_VERIFY
        } else {
            // It's an "add" flow
            intent.action = SupervisionPinRecoveryActivity.ACTION_SETUP_VERIFIED
        }
        setUpRecoveryLauncher.launch(intent)
    }

    // Checks the SupervisionManager state to determine if the Verify flow should be shown.
    private fun shouldShowVerifyFlow(context: Context): Boolean {
        val supervisionManager = context.getSystemService(SupervisionManager::class.java)
        val recoveryInfo = supervisionManager?.getSupervisionRecoveryInfo()
        val hasAccount = hasAccountNameSet(recoveryInfo)
        val state = recoveryInfo?.state
        return hasAccount && state == SupervisionRecoveryInfo.STATE_PENDING
    }

    companion object {
        const val KEY = "supervision_pin_recovery_banner"
    }
}
