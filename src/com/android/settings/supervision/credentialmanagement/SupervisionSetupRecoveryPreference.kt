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
import android.app.settings.SettingsEnums.ACTION_SUPERVISION_ADD_RECOVERY
import android.app.settings.SettingsEnums.ACTION_SUPERVISION_VERIFY_RECOVERY
import android.app.supervision.SupervisionManager
import android.app.supervision.SupervisionRecoveryInfo.STATE_PENDING
import android.app.supervision.flags.Flags
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.overlay.FeatureFactory
import com.android.settings.supervision.credentialmanagement.SupervisionUpdateRecoveryEmailPreference.Companion.asMaskedEmail
import com.android.settings.supervision.shared.shouldDisplayPinRecoveryReminders
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceIconProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.preference.PreferenceBinding

/**
 * Setting on PIN Management screen (Settings > Supervision > Manage Pin) that invokes the flow to
 * add the device PIN recovery method or verify an unverified PIN recovery method.
 */
class SupervisionSetupRecoveryPreference :
    PersistentPreference<String>,
    PreferenceMetadata,
    PreferenceAvailabilityProvider,
    PreferenceLifecycleProvider,
    PreferenceBinding,
    PreferenceSummaryProvider,
    PreferenceTitleProvider,
    PreferenceIconProvider,
    Preference.OnPreferenceClickListener {

    private lateinit var lifeCycleContext: PreferenceLifecycleContext
    private lateinit var setUpRecoveryLauncher: ActivityResultLauncher<Intent>
    private var isVerificationFlow: Boolean = false

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.supervision_setup_recovery_purpose

    override fun getTitle(context: Context): CharSequence {
        return if (hasAccountNameToVerify(context)) {
            context.getString(R.string.supervision_verify_pin_recovery_title)
        } else {
            context.getString(R.string.supervision_add_pin_recovery_title)
        }
    }

    override val supportsWrite = false

    override val valueType = String::class.javaObjectType

    override fun storage(context: Context): KeyValueStore = createSummaryStorage(context, key)

    override fun getSummary(context: Context): CharSequence? {
        return accountNameToVerify(context)?.asMaskedEmail()
    }

    @DrawableRes
    override fun getIcon(context: Context): Int {
        if (
            Flags.enableSupervisionSettingsUiUpdates() &&
                !context.shouldDisplayPinRecoveryReminders()
        ) {
            return 0
        }
        return R.drawable.exclamation_icon
    }

    override val availabilityDescription =
        "The device must support the PIN recovery screen and the recovery email must be pending."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override fun isAvailable(context: Context): Boolean {
        if (!Flags.enableSupervisionPinRecoveryScreen()) {
            return false
        }
        return context
            .getSystemService(SupervisionManager::class.java)
            ?.getSupervisionRecoveryInfo()
            ?.let { it.state == STATE_PENDING } ?: true
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        lifeCycleContext = context
        setUpRecoveryLauncher =
            context.registerForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
                ::updateRecoveryInfo,
            )
    }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.onPreferenceClickListener = this
    }

    fun updateRecoveryInfo(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            lifeCycleContext.notifyPreferenceChange(KEY)
            val messageResId =
                if (isVerificationFlow) {
                    R.string.supervision_recovery_email_verified
                } else {
                    R.string.supervision_recovery_email_added
                }
            Toast.makeText(
                    lifeCycleContext,
                    lifeCycleContext.getString(messageResId),
                    Toast.LENGTH_SHORT,
                )
                .show()
        }
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        val intent = Intent(lifeCycleContext, SupervisionPinRecoveryActivity::class.java)
        val metricsFeatureProvider = FeatureFactory.featureFactory.metricsFeatureProvider
        isVerificationFlow = hasAccountNameToVerify(lifeCycleContext)
        if (isVerificationFlow) {
            // It's a "verify" flow
            intent.action = SupervisionPinRecoveryActivity.ACTION_POST_SETUP_VERIFY
            metricsFeatureProvider.action(preference.context, ACTION_SUPERVISION_VERIFY_RECOVERY)
        } else {
            // It's an "add" flow
            intent.action = SupervisionPinRecoveryActivity.ACTION_SETUP_VERIFIED
            metricsFeatureProvider.action(preference.context, ACTION_SUPERVISION_ADD_RECOVERY)
        }

        setUpRecoveryLauncher.launch(intent)
        return true
    }

    private fun accountNameToVerify(context: Context): String? {
        return context
            .getSystemService(SupervisionManager::class.java)
            ?.getSupervisionRecoveryInfo()
            ?.accountName
    }

    private fun hasAccountNameToVerify(context: Context): Boolean {
        return !accountNameToVerify(context).isNullOrEmpty()
    }

    companion object {
        const val KEY = "supervision_setup_recovery"
    }
}
