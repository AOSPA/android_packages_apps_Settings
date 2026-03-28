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

package com.android.settings.deviceinfo.hardwareinfo

import android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE
import android.content.Context
import android.os.Build
import android.os.SystemProperties
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.types.AnyString
import com.android.settingslib.metadata.preferencesapi.types.Year
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// LINT.IfChange
@ProvidePreferenceScreen(HardwareInfoApiScreen.KEY)
class HardwareInfoApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.ABOUT_DEVICE,
        fragment = HardwareInfoFragment::class,
        alreadyPartiallyMigrated = HardwareInfoScreen::class,
        purpose = R.string.device_model_purpose_api,
    ) {
    init {
        flag { Flags.catalystMigration26q2() }

        preference(
            key = SERIAL_NUMBER_KEY,
            purpose = R.string.hardware_info_serial_purpose,
            type = AnyString,
        ) {
            sensitivityLevel(SensitivityLevel.DO_NOT_EXPOSE)

            get {
                permissions(READ_PRIVILEGED_PHONE_STATE)
                execute { Build.getSerial() }
            }
        }

        preference(
            key = MANUFACTURED_YEAR_KEY,
            purpose = R.string.hardware_info_mfg_year_purpose,
            type = Year,
        ) {
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)

            get { execute { getManufacturedYear(context) } }
        }
    }

    private fun getManufacturedYear(context: Context): String {
        val dateUtc = SystemProperties.getLong("ro.build.date.utc", 0)
        if (dateUtc > 0) {
            // Format to get just the year
            val sdf = SimpleDateFormat("yyyy", Locale.US)
            return sdf.format(Date(dateUtc * 1000))
        }
        return context.getString(R.string.device_info_default)
    }

    companion object {
        private const val TAG = "HardwareInfoApiScreen"
        const val KEY = "api_device_model"
        const val SERIAL_NUMBER_KEY = "serial_number"
        const val MANUFACTURED_YEAR_KEY = "manufactured_year"
    }
}
// LINT.ThenChange(HardwareInfoFragment.java)
