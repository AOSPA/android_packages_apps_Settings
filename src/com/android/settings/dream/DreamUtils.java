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
package com.android.settings.dream;

import android.content.res.Resources;

import com.android.settings.R;

/**
 * A collection of shared utility methods to work with dream settings.
 */
public class DreamUtils {
    protected static String[] getWhenToDreamEntries(Resources resources) {
        return resources.getStringArray(
                resources.getBoolean(com.android.internal.R.bool.config_dreamsEnabledOnBattery)
                        ? R.array.when_to_start_screensaver_entries
                        : R.array.when_to_start_screensaver_entries_no_battery);
    }


    protected static String[] getWhenToDreamKeys(Resources resources) {
        return resources.getStringArray(
                resources.getBoolean(com.android.internal.R.bool.config_dreamsEnabledOnBattery)
                        ? R.array.when_to_start_screensaver_values
                        : R.array.when_to_start_screensaver_values_no_battery);
    }

    protected static int[] getWhenToDreamOptions(Resources resources) {
        String[] keys = getWhenToDreamKeys(resources);
        int[] options = new int[keys.length];

        for (int i = options.length - 1; i >= 0; --i) {
            options[i] = DreamSettings.getSettingFromPrefKey(keys[i]);
        }

        return options;
    }
}
