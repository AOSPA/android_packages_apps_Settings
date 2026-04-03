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
package com.android.settings.accessibility.systemcontrols.ui

import android.app.settings.SettingsEnums.ACTION_POWER_BUTTON_ENDS_CALL
import android.content.Context
import android.view.KeyCharacterMap
import android.view.KeyEvent
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.accessibility.systemcontrols.data.PowerButtonEndsCallDataStore
import com.android.settings.metrics.PreferenceActionMetricsProvider
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SwitchPreference

// LINT.IfChange
// TODO("b/456322737") update sensitivity level once it goes with different classification.
class PowerButtonEndsCallPreference :
    SwitchPreference(
        PowerButtonEndsCallDataStore.KEY,
        R.string.incall_power_button_behavior_purpose,
        R.string.accessibility_power_button_ends_call_prerefence_title,
    ),
    PreferenceActionMetricsProvider,
    PreferenceAvailabilityProvider {
    override val preferenceActionMetrics: Int
        get() = ACTION_POWER_BUTTON_ENDS_CALL

    override val availabilityDescription =
        "The device must have a power button. The device must be voice capable."

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context) =
        KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_POWER) && Utils.isVoiceCapable(context)

    override fun storage(context: Context) = context.dataStore

    override fun getReadPermissions(context: Context) = SettingsSecureStore.getReadPermissions()

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermissions(context: Context) = SettingsSecureStore.getWritePermissions()

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    companion object {
        private val Context.dataStore: KeyValueStore
            get() = PowerButtonEndsCallDataStore(this)
    }
}
// LINT.ThenChange(../../PowerButtonEndsCallPreferenceController.java)
