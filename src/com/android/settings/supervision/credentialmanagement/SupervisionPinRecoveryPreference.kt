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
import android.app.settings.SettingsEnums.ACTION_SUPERVISION_FORGOT_PIN
import android.app.supervision.SupervisionManager
import android.app.supervision.flags.Flags
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.overlay.FeatureFactory
import com.android.settings.spa.network.getActivity
import com.android.settings.supervision.shared.canLaunchPinRecovery
import com.android.settings.supervision.shared.isSupervisingCredentialSet
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.preference.PreferenceBinding

class SupervisionPinRecoveryPreference :
    PreferenceMetadata,
    PreferenceAvailabilityProvider,
    PreferenceLifecycleProvider,
    PreferenceBinding,
    Preference.OnPreferenceClickListener {

    private lateinit var lifeCycleContext: PreferenceLifecycleContext
    private lateinit var pinRecoveryLauncher: ActivityResultLauncher<Intent>

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.supervision_pin_recovery_purpose

    override val title: Int
        get() = R.string.supervision_add_forgot_pin_preference_title

    override fun dependencies(context: Context) = arrayOf(SupervisionSetupRecoveryPreference.KEY)

    override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)

    override val availabilityDescription =
        "The device must support the PIN recovery screen and be able to launch it."

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context): Boolean {
        if (!Flags.enableSupervisionPinRecoveryScreen()) {
            return false
        }

        if (Flags.enableSupervisionSettingsUiUpdates()) {
            return context.canLaunchPinRecovery()
        }

        return context
            .getSystemService(SupervisionManager::class.java)
            ?.getSupervisionRecoveryInfo() != null
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        lifeCycleContext = context
        pinRecoveryLauncher =
            context.registerForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
                ::onRecoveryFlowComplete,
            )
    }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.onPreferenceClickListener = this
    }

    @VisibleForTesting
    fun onRecoveryFlowComplete(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(
                    lifeCycleContext,
                    lifeCycleContext.getString(R.string.supervision_pin_updated),
                    Toast.LENGTH_SHORT,
                )
                .show()
        }
        if (Flags.enableSupervisionSettingsUiUpdates()) {
            if (
                result.resultCode == Activity.RESULT_CANCELED &&
                    !lifeCycleContext.isSupervisingCredentialSet()
            ) {
                // Navigate back to the dashboard.
                val activity =
                    (lifeCycleContext.baseContext.getActivity()
                        as? androidx.activity.ComponentActivity)
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
            lifeCycleContext.notifyPreferenceChange(KEY)
        }
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        val intent =
            Intent(preference.context, SupervisionPinRecoveryActivity::class.java).apply {
                action = SupervisionPinRecoveryActivity.ACTION_RECOVERY
            }
        val metricsFeatureProvider = FeatureFactory.featureFactory.metricsFeatureProvider
        metricsFeatureProvider.action(preference.context, ACTION_SUPERVISION_FORGOT_PIN)
        pinRecoveryLauncher.launch(intent)
        return true
    }

    companion object {
        const val KEY = "supervision_pin_recovery"
    }
}
