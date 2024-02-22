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
import android.icu.text.MeasureFormat;
import android.icu.util.Measure;
import android.icu.util.MeasureUnit;
import android.os.BatteryManager;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.fuelgauge.BatteryUtils;

import java.util.Locale;

/**
 * A controller that manages the information about battery design capacity.
 */
public class BatteryDesignCapacityPreferenceController extends BasePreferenceController {

    public BatteryDesignCapacityPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        Intent batteryIntent = BatteryUtils.getBatteryIntent(mContext);
        final int designCapacityMicroAh =
                batteryIntent.getIntExtra(BatteryManager.EXTRA_DESIGN_CAPACITY, -1);

        if (designCapacityMicroAh != -1) {
            float designCapacityMah = designCapacityMicroAh / 1_000f;

            return MeasureFormat.getInstance(Locale.getDefault(), MeasureFormat.FormatWidth.SHORT)
                    .format(new Measure(designCapacityMah, MeasureUnit.MILLIAMPERE_HOUR));
        }

        return mContext.getString(R.string.battery_design_capacity_info_not_available);
    }
}
