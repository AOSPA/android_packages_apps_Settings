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

package com.android.settings.notification;

import android.content.Context;
import android.provider.Settings;

import com.android.settings.core.TogglePreferenceController;

import static android.provider.Settings.Global.BATTERY_LIGHT_ENABLED;

public class BatteryLightPreferenceController extends TogglePreferenceController {

    public BatteryLightPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_intrusiveNotificationLed) ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isChecked() {
        boolean enabledByDefault = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_intrusiveBatteryLed);
        return Settings.Global.getInt(mContext.getContentResolver(), BATTERY_LIGHT_ENABLED,
                enabledByDefault ? 1 : 0) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Global.putInt(mContext.getContentResolver(), BATTERY_LIGHT_ENABLED,
                isChecked ? 1 : 0);
    }
}
