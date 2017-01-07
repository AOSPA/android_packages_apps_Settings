/*
 * Copyright (C) 2017-2020 The Android Open Source Project
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
package com.android.settings.display;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.SwitchPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

public class SRGBModePreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String KEY_SRGB = "srgb";

    public SRGBModePreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_SRGB;
    }

    @Override
    public boolean isAvailable() {
        return !TextUtils.isEmpty(mContext.getString(com.android.internal.R.string.config_srgb_path));
    }

    @Override
    public void updateState(Preference preference) {
        int sRGBValue = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.SRGB_ENABLED, 0);
        ((SwitchPreference) preference).setChecked(sRGBValue != 0);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean sRGBValue = (Boolean) newValue;
        Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.SRGB_ENABLED, sRGBValue ? 1 : 0);
        return true;
    }
}
