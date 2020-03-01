/*
 * Copyright (C) 2020 Paranoid Android
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.gesture;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Resources;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import androidx.preference.*;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class AudioZeroMagicSettings extends SettingsPreferenceFragment implements
    Preference.OnPreferenceChangeListener {

    private static final String AUDIO_AUTO_PAUSE = "audio_auto_pause";
    private static final String AUDIO_AUTO_RESUME = "audio_auto_resume";

    private SwitchPreference mAudioAutoPause;
    private SwitchPreference mAudioAutoResume;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.audio_auto_pause_resume);
        ContentResolver resolver = getActivity().getContentResolver();

        mAudioAutoPause = (SwitchPreference) findPreference(AUDIO_AUTO_PAUSE);
        mAudioAutoPause.setChecked((Settings.System.getInt(resolver,
                Settings.System.AUDIO_AUTO_PAUSE, 0) == 1));
        mAudioAutoPause.setOnPreferenceChangeListener(this);

        mAudioAutoResume = (SwitchPreference) findPreference(AUDIO_AUTO_RESUME);
        mAudioAutoResume.setChecked((Settings.System.getInt(resolver,
                Settings.System.AUDIO_AUTO_RESUME, 0) == 1));
        mAudioAutoResume.setOnPreferenceChangeListener(this);

        int zeroMagicStatus = Settings.System.getInt(resolver,
                Settings.System.AUDIO_AUTO_PAUSE, 0);
        updateAudioZeroMagicPrefs(zeroMagicStatus);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mAudioAutoPause) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.AUDIO_AUTO_PAUSE, value ? 1 : 0);
            return true;
	} else if (preference == mAudioAutoResume) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.AUDIO_AUTO_RESUME, value ? 1 : 0);
            return true;
        }
        return false;
    }

    private void updateAudioZeroMagicPrefs(int zeroMagicStatus) {
        if (mAudioAutoPause != null) {
            if (zeroMagicStatus == 0) {
                mAudioAutoResume.setEnabled(false);
            } else {
                mAudioAutoResume.setEnabled(true);
            }
        }
    }

    @Override
    public int getMetricsCategory() {
        return -1;
    }
}
