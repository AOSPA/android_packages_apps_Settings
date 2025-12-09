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

import android.app.settings.SettingsEnums.ACTION_SUPERVISION_SET_UP_PIN_ENTRY
import android.content.Context
import android.content.Intent
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceClickListener
import com.android.settings.R
import com.android.settings.overlay.FeatureFactory
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.preference.PreferenceBinding

/** Preference on the Supervision dashboard the invokes the flow to create a device PIN. */
class SupervisionSetUpPinPreference :
    PreferenceMetadata,
    PreferenceAvailabilityProvider,
    PreferenceBinding,
    OnPreferenceClickListener {

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.supervision_set_up_pin_purpose

    override val title: Int
        get() = R.string.supervision_set_up_pin_preference_title

    override fun dependencies(context: Context) = arrayOf(SupervisionPinManagementScreen.KEY)

    override fun isAvailable(context: Context) = !context.isSupervisingCredentialSet()

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.onPreferenceClickListener = this
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        FeatureFactory.featureFactory.metricsFeatureProvider.action(
            preference.context,
            ACTION_SUPERVISION_SET_UP_PIN_ENTRY,
        )

        val intent = Intent(preference.context, SetupSupervisionActivity::class.java)
        preference.context.startActivity(intent)
        return true
    }

    companion object {
        const val KEY = "supervision_set_up_pin"
    }
}
