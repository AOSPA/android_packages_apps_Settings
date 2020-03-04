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

import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.widget.Switch;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.RadioButtonPreference;
import com.android.settings.widget.SwitchBar;

public class PulseAmbientLightSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceClickListener {

    private static final String TAG = "PulseAmbientLightSettings";

    private static final String COLOR_AUTOMATIC = "ambient_notification_light_automatic";
    private static final String COLOR_ACCENT = "ambient_notification_light_accent";

    private PulseAmbientLightEnabler mPulseAmbientLightEnabler;
    private RadioButtonPreference mColorAutomatic;
    private RadioButtonPreference mColorAccent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pulse_ambient_light_settings);
        mColorAutomatic = (RadioButtonPreference) findPreference(COLOR_AUTOMATIC);
        mColorAccent = (RadioButtonPreference) findPreference(COLOR_ACCENT);

        mColorAutomatic.setOnPreferenceClickListener(this);
        mColorAccent.setOnPreferenceClickListener(this);

        boolean colorAuto = Settings.System.getIntForUser(getContentResolver(), COLOR_AUTOMATIC, 1, UserHandle.USER_CURRENT) != 0;
        updateState(colorAuto ? COLOR_AUTOMATIC : COLOR_ACCENT);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        final String key = preference.getKey();
        if (preference instanceof RadioButtonPreference) {
            updateState(key);
        }
        return true;
    }

    @Override
    public int getMetricsCategory() {
        return -1;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mPulseAmbientLightEnabler != null) {
            mPulseAmbientLightEnabler.teardownSwitchBar();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        SettingsActivity activity = (SettingsActivity) getActivity();
        mPulseAmbientLightEnabler = new PulseAmbientLightEnabler(activity.getSwitchBar());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mPulseAmbientLightEnabler != null) {
            mPulseAmbientLightEnabler.resume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mPulseAmbientLightEnabler != null) {
            mPulseAmbientLightEnabler.pause();
        }
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

    private void updateDependencies(boolean enabled) {
        mColorAutomatic.setEnabled(enabled);
        mColorAccent.setEnabled(enabled);
    }

    private class PulseAmbientLightEnabler implements SwitchBar.OnSwitchChangeListener {

        private final Context mContext;
        private final SwitchBar mSwitchBar;
        private boolean mListeningToOnSwitchChange;

        public PulseAmbientLightEnabler(SwitchBar switchBar) {
            mContext = switchBar.getContext();
            mSwitchBar = switchBar;

            mSwitchBar.show();

            boolean enabled = Settings.System.getIntForUser(
                    mContext.getContentResolver(),
                    Settings.System.AMBIENT_NOTIFICATION_LIGHT, 1, UserHandle.USER_CURRENT) != 0;
            mSwitchBar.setChecked(enabled);
            PulseAmbientLightSettings.this.updateDependencies(enabled);
        }

        public void teardownSwitchBar() {
            pause();
            mSwitchBar.hide();
        }

        public void resume() {
            if (!mListeningToOnSwitchChange) {
                mSwitchBar.addOnSwitchChangeListener(this);
                mListeningToOnSwitchChange = true;
            }
        }

        public void pause() {
            if (mListeningToOnSwitchChange) {
                mSwitchBar.removeOnSwitchChangeListener(this);
                mListeningToOnSwitchChange = false;
            }
        }

        @Override
        public void onSwitchChanged(Switch switchView, boolean isChecked) {
            Settings.System.putIntForUser(
                    mContext.getContentResolver(),
                    Settings.System.AMBIENT_NOTIFICATION_LIGHT, isChecked ? 1 : 0, UserHandle.USER_CURRENT);
            PulseAmbientLightSettings.this.updateDependencies(isChecked);
        }

    }
}
