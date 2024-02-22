/*
 * Copyright (C) 2024 Paranoid Android
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

package com.android.settings.deviceinfo.batteryinfo;

import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.fuelgauge.BatteryUtils;

/**
 * A controller that manages the information about battery maximum capacity.
 */
public class BatteryMaximumCapacityPreferenceController extends BasePreferenceController {

    public BatteryMaximumCapacityPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        Intent batteryIntent = BatteryUtils.getBatteryIntent(mContext);
        final int currentCapacityMicroAh =
                batteryIntent.getIntExtra(BatteryManager.EXTRA_CURRENT_CAPACITY, -1);
        final int designCapacityMicroAh =
                batteryIntent.getIntExtra(BatteryManager.EXTRA_DESIGN_CAPACITY, -1);

        if (currentCapacityMicroAh != -1 && designCapacityMicroAh != -1) {
            int currentCapacity = currentCapacityMicroAh / 1_000;
            int designCapacity = designCapacityMicroAh / 1_000;
            int percentage = (currentCapacity * 100) / designCapacity;

            return mContext.getString(
                    R.string.battery_maximum_capacity_summary, currentCapacity, percentage);
        }

        return mContext.getString(R.string.battery_maximum_capacity_not_available);
    }
}
