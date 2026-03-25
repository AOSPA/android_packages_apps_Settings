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

package com.android.settings.gestures

import android.app.settings.SettingsEnums
import android.content.Context
import android.provider.Settings
import com.android.settings.R
import com.android.settings.metrics.PreferenceActionMetricsProvider
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.widget.MainSwitchPreferenceBinding

// LINT.IfChange
class DoubleTapPowerMainSwitchPreference :
    BooleanValuePreference,
    MainSwitchPreferenceBinding,
    PreferenceActionMetricsProvider,
    PreferenceAvailabilityProvider {
    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.double_tap_power_button_gesture_enabled_purpose

    override val title: Int
        get() = R.string.double_tap_power_enabled

    override val keywords: Int
        get() = R.string.keywords_double_tap_power

    override val preferenceActionMetrics: Int
        get() = SettingsEnums.ACTION_DOUBLE_TAP_POWER_ENABLED

    override fun storage(context: Context): KeyValueStore = createDataStore(context)

    override val availabilityDescription =
        "The device must support the double tap power button gesture."

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context): Boolean =
        DoubleTapPowerSettingsUtils.isMultiTargetDoubleTapPowerButtonGestureAvailable(context)

    override fun getReadPermissions(context: Context) = SettingsSecureStore.getReadPermissions()

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermissions(context: Context) = SettingsSecureStore.getWritePermissions()

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val sensitivityLevel: Int
        get() = SensitivityLevel.NO_SENSITIVITY

    override val supportsWrite = true
    companion object {
        const val KEY = Settings.Secure.DOUBLE_TAP_POWER_BUTTON_GESTURE_ENABLED

        // The default value is true.
        fun createDataStore(context: Context) =
            SettingsSecureStore.get(context).apply { setDefaultValue(KEY, true) }
    }
}
// LINT.ThenChange(DoubleTapPowerMainSwitchPreferenceController.java)
