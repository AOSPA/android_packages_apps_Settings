/*
 * Copyright (C) 2019 Paranoid Android
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

package com.android.settings.display;

import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.RadioButtonPreference;

public class HorizonLightSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceClickListener {

    private static final String TAG = "HorizonLightSettings";

    private static final String HORIZON_LIGHT = "ambient_notification_light";
    private static final String COLOR_AUTOMATIC = "ambient_notification_light_automatic";
    private static final String COLOR_ACCENT = "ambient_notification_light_accent";

    private SwitchPreference mHorizonLight;
    private RadioButtonPreference mColorAutomatic;
    private RadioButtonPreference mColorAccent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.horizon_light_settings);
        mHorizonLight = (SwitchPreference) findPreference(HORIZON_LIGHT);
        mColorAutomatic = (RadioButtonPreference) findPreference(COLOR_AUTOMATIC);
        mColorAccent = (RadioButtonPreference) findPreference(COLOR_ACCENT);

        mHorizonLight.setOnPreferenceClickListener(this);
        mColorAutomatic.setOnPreferenceClickListener(this);
        mColorAccent.setOnPreferenceClickListener(this);

        mHorizonLight.setChecked(Settings.System.getIntForUser(getContentResolver(), HORIZON_LIGHT, 1, UserHandle.USER_CURRENT) != 0);

        boolean colorAuto = Settings.System.getIntForUser(getContentResolver(), COLOR_AUTOMATIC, 1, UserHandle.USER_CURRENT) != 0;
        updateState(colorAuto ? COLOR_AUTOMATIC : COLOR_ACCENT);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        final String key = preference.getKey();
        if (preference instanceof RadioButtonPreference) {
            updateState(key);
        } else if (preference instanceof SwitchPreference) {
            Settings.System.putIntForUser(getContentResolver(), HORIZON_LIGHT,
                ((SwitchPreference) preference).isChecked() ? 1 : 0, UserHandle.USER_CURRENT);
        }
        return true;
    }

    @Override
    public int getMetricsCategory() {
        return -1;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void updateState(String key) {
        if (key.equals(COLOR_AUTOMATIC)) {
            Settings.System.putIntForUser(getContentResolver(),
                COLOR_AUTOMATIC, 1, UserHandle.USER_CURRENT);
            mColorAutomatic.setChecked(true);
            mColorAccent.setChecked(false);
        } else {
            Settings.System.putIntForUser(getContentResolver(),
                COLOR_AUTOMATIC, 0, UserHandle.USER_CURRENT);
            mColorAutomatic.setChecked(false);
            mColorAccent.setChecked(true);
        }
    }
}