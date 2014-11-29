/*
 * Copyright (C) 2014 AOSPA-legacy Project
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

package com.android.settings.sound;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class SoundSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "SoundSettings";

    private static final String KEY_VOLBTN_MUSIC_CTRL = "volbtn_music_controls";

    private SwitchPreference mVolBtnMusicCtrl;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.sound_settings);

        mVolBtnMusicCtrl = (SwitchPreference) findPreference(KEY_VOLBTN_MUSIC_CTRL);
        mVolBtnMusicCtrl.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.VOLUME_MUSIC_CONTROLS, 1) != 0);
        mVolBtnMusicCtrl.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        // If we didn't handle it, let preferences handle it.
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (KEY_VOLBTN_MUSIC_CTRL.equals(key)) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.VOLUME_MUSIC_CONTROLS,
                    (Boolean) objValue ? 1 : 0);
        }
        return true;
    }
}
