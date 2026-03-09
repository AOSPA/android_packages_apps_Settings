/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings.accessibility.hearingdevices.ui

import android.content.Context
import androidx.preference.Preference
import androidx.preference.TwoStatePreference
import com.android.settings.accessibility.hearingdevices.data.HearingDeviceAudioRoutingDataStore
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.SwitchPreference
import com.android.settingslib.preference.SwitchPreferenceBinding

class HearingDeviceAudioRoutingSwitchPreference(
    private val context: Context,
    override val key: String,
    private val settingsProviderKey: String,
    titleRes: Int,
    private val summaryOnRes: Int,
    private val summaryOffRes: Int,
    purpose: Int,
    private val audioAttributeUsages: IntArray,
) :
    SwitchPreference(key, title = titleRes, purpose = purpose),
    SwitchPreferenceBinding,
    PreferenceLifecycleProvider {

    private val dataStore by lazy {
        HearingDeviceAudioRoutingDataStore(
            context,
            attributeSdkUsageList = audioAttributeUsages,
            preferenceKey = key,
            settingsProviderKey,
        )
    }

    override fun storage(context: Context): KeyValueStore = dataStore

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(
        context: Context,
        value: Boolean?,
        callingPid: Int,
        callingUid: Int,
    ) = ReadWritePermit.ALLOW

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        if (preference is TwoStatePreference) {
            updateSummary(preference, preference.isChecked)
        }
        preference.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { pref, newValue ->
                updateSummary(pref, newValue as Boolean)
                true
            }
    }

    private fun updateSummary(preference: Preference, isRouteToHearing: Boolean) {
        preference.setSummary(
            if (isRouteToHearing) {
                summaryOnRes
            } else {
                summaryOffRes
            }
        )
    }
}
