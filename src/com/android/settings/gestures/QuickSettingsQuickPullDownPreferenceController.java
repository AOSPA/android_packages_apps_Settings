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

package com.android.settings.gestures;

import static android.provider.Settings.Secure.STATUS_BAR_QUICK_QS_PULLDOWN;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.text.TextUtils;

public class QuickSettingsQuickPullDownPreferenceController extends GesturePreferenceController {

    private final int ON = 1;
    private final int OFF = 0;

    private final String PREF_KEY_VIDEO = "gesture_quick_settings_quick_pull_down_video";

    public QuickSettingsQuickPullDownPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "quick_settings_quick_pull_down");
    }

    @Override
    public String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(mContext.getContentResolver(), STATUS_BAR_QUICK_QS_PULLDOWN,
                isChecked ? ON : OFF);
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(mContext.getContentResolver(), STATUS_BAR_QUICK_QS_PULLDOWN, 1) != 0;
    }
}
