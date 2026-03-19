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

package com.android.settings.accessibility.hearingdevices.ui

import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import android.content.Context
import com.android.settings.R
import com.android.settings.accessibility.SavedHearingDeviceUpdater
import com.android.settings.bluetooth.BluetoothDeviceUpdater
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE

class SavedHearingDevicePreferenceCategory(
    val metricsCategory: Int,
    key: String = "previously_connected_hearing_devices",
    purpose: Int = R.string.previously_connected_hearing_devices_purpose,
    title: Int = R.string.accessibility_hearing_device_saved_title,
) : HearingDevicePreferenceCategory(key, purpose, title) {
    override val availabilityDescription = UI_ONLY_PREFERENCE

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE
    override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)
    override fun createDeviceUpdater(context: Context): BluetoothDeviceUpdater? =
        SavedHearingDeviceUpdater(context, this, metricsCategory)
}
