/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.display.darkmode;

import android.app.settings.SettingsEnums;
import android.content.Context;

import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;

/**
 * Settings screen for Dark UI Mode
 */
public class DarkModeSettingsFragment extends DashboardFragment {

    private static final String TAG = "DarkModeSettingsFrag";

    @Override
    protected int getPreferenceScreenResId() {
        return 0;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_dark_theme;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DARK_UI_SETTINGS;
    }

    @Override
    public @Nullable String getPreferenceScreenBindingKey(@Nullable Context context) {
        return DarkModeScreen.KEY;
    }
}
