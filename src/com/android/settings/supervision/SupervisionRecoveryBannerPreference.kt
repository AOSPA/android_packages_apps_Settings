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
import android.content.SharedPreferences
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.recyclerview.widget.RecyclerView
import com.android.settings.R
import com.android.settings.supervision.credentialmanagement.SupervisionPinManagementScreen
import com.android.settings.supervision.credentialmanagement.SupervisionPinRecoveryActivity
import com.android.settings.supervision.shared.isSupervisingCredentialSet
import com.android.settings.supervision.shared.shouldDisplayPinRecoveryReminders
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.BannerMessagePreference

class SupervisionRecoveryBannerPreference :
    PreferenceMetadata,
    PreferenceBinding,
    PreferenceAvailabilityProvider,
    PreferenceLifecycleProvider {

    private lateinit var lifeCycleContext: PreferenceLifecycleContext
    private lateinit var setUpRecoveryLauncher: ActivityResultLauncher<Intent>
    private lateinit var prefs: SharedPreferences
    private var isVisible: Boolean? = null

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.supervision_pin_recovery_banner_purpose

    override val indexable: Boolean
        get() = false

    override val availabilityDescription = UI_ONLY_PREFERENCE

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context): Boolean {
        if (!Flags.enableSupervisionSettingsUiUpdates()) {
            return false
        }
        ensurePrefsInitialized(context)
        if (!context.isSupervisingCredentialSet()) {
            return false
        }
        val missingRecovery = context.shouldDisplayPinRecoveryReminders()
        return missingRecovery && !isDismissed()
    }

    override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)

    override fun onCreate(context: PreferenceLifecycleContext) {
        lifeCycleContext = context
        ensurePrefsInitialized(context)
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

        banner.setDismissButtonOnClickListener {
            persistDismissalState(true)
            banner.isVisible = false
            lifeCycleContext.notifyPreferenceChange(KEY)
        }

        if (isVisible == false && banner.isVisible) {
            val fragment = lifeCycleContext.lifecycleOwner
            if (fragment is SupervisionDashboardFragment) {
                val recyclerView: RecyclerView? = fragment.listView
                recyclerView?.post { recyclerView.scrollToPosition(0) }
            }
        }
        isVisible = banner.isVisible
    }

    private fun hasAccountNameSet(info: SupervisionRecoveryInfo?): Boolean {
        return !info?.accountName.isNullOrEmpty()
    }

    private fun isDismissed(): Boolean {
        return prefs.getBoolean(KEY_BANNER_DISMISSED, false)
    }

    private fun persistDismissalState(dismissed: Boolean) {
        prefs.edit().putBoolean(KEY_BANNER_DISMISSED, dismissed).commit()
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

    private fun ensurePrefsInitialized(context: Context) {
        if (!::prefs.isInitialized) {
            prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    companion object {
        const val KEY = "supervision_pin_recovery_banner"
        private const val SHARED_PREFS_NAME = "supervision_settings_prefs"
        private const val KEY_BANNER_DISMISSED = "supervision_recovery_banner_dismissed"
    }
}
