/*
 * Copyright (C) 2021 Paranoid Android
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
package com.android.settings.notification

import android.content.Context
import android.provider.Settings
import com.android.settings.core.TogglePreferenceController

class BatteryLightPreferenceController(context: Context?, key: String?) :
    TogglePreferenceController(context, key) {

    override fun getAvailabilityStatus(): Int {
        return if (mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_intrusiveNotificationLed
            )
        ) AVAILABLE else UNSUPPORTED_ON_DEVICE
    }

    override fun isChecked(): Boolean {
        val enabledByDefault: Boolean = mContext.getResources().getBoolean(
            com.android.internal.R.bool.config_intrusiveBatteryLed
        )
        return Settings.Global.getInt(
            mContext.getContentResolver(), Settings.Global.BATTERY_LIGHT_ENABLED,
            if (enabledByDefault) 1 else 0
        ) == 1
    }

    override fun setChecked(isChecked: Boolean): Boolean {
        return Settings.Global.putInt(
            mContext.getContentResolver(), Settings.Global.BATTERY_LIGHT_ENABLED,
            if (isChecked) 1 else 0
        )
    }
}
