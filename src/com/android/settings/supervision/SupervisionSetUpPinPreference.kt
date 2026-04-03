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
import android.app.settings.SettingsEnums.ACTION_SUPERVISION_SET_UP_PIN_ENTRY
import android.app.supervision.SupervisionManager
import android.app.supervision.flags.Flags
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceClickListener
import com.android.settings.R
import com.android.settings.overlay.FeatureFactory
import com.android.settings.supervision.credentialmanagement.SupervisionPinManagementScreen
import com.android.settings.supervision.shared.isSupervisingCredentialSet
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.preference.PreferenceBinding

/** Preference on the Supervision dashboard the invokes the flow to create a device PIN. */
class SupervisionSetUpPinPreference :
    PreferenceMetadata,
    PreferenceAvailabilityProvider,
    PreferenceBinding,
    OnPreferenceClickListener,
    PreferenceLifecycleProvider {

    private lateinit var lifeCycleContext: PreferenceLifecycleContext

    private lateinit var confirmCredentialsLauncher: ActivityResultLauncher<Intent>

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.supervision_set_up_pin_purpose

    override val title: Int
        get() = R.string.supervision_set_up_pin_preference_title

    override fun dependencies(context: Context) = arrayOf(SupervisionPinManagementScreen.KEY)

    override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)

    override val availabilityDescription = "The device must not have a supervising credential."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override fun isAvailable(context: Context) = !context.isSupervisingCredentialSet()

    override fun onCreate(context: PreferenceLifecycleContext) {
        lifeCycleContext = context
        confirmCredentialsLauncher =
            context.registerForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
                ::onConfirmCredentials,
            )
    }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.onPreferenceClickListener = this
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        FeatureFactory.featureFactory.metricsFeatureProvider.action(
            preference.context,
            ACTION_SUPERVISION_SET_UP_PIN_ENTRY,
        )

        if (Flags.enableParentApprovalForPinSetup()) {
            val supervisionManager =
                preference.context.getSystemService(SupervisionManager::class.java)

            val confirmCredentialsIntent =
                supervisionManager.createConfirmSupervisionCredentialsIntent()
            if (confirmCredentialsIntent != null) {
                confirmCredentialsLauncher.launch(confirmCredentialsIntent)
            } else {
                startPinSetupFlow(preference.context)
            }
        } else {
            startPinSetupFlow(preference.context)
        }
        return true
    }

    fun onConfirmCredentials(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            startPinSetupFlow(lifeCycleContext)
        }
    }

    private fun startPinSetupFlow(context: Context) {
        val intent = Intent(context, SetupSupervisionActivity::class.java)
        context.startActivity(intent)
    }

    companion object {
        const val KEY = "supervision_set_up_pin"
    }
}
