/*
 * Copyright (C) 2020 Paranoid Android
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

import static android.provider.Settings.System.ADAPTIVE_PLAYBACK_ENABLED;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.widget.Switch;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.widget.LayoutPreference;

public class AdaptivePlaybackSwitchPreferenceController extends AbstractPreferenceController
        implements SwitchBar.OnSwitchChangeListener {

    private static final String KEY = "adaptive_playback_switch";

    private SwitchBar mSwitch;
    private LayoutPreference mPreference;

    public AdaptivePlaybackSwitchPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (isAvailable()) {
            mPreference = screen.findPreference(getPreferenceKey());
            if (mPreference != null) {
                mSwitch = mPreference.findViewById(R.id.switch_bar);
                if (mSwitch != null) {
                    mSwitch.addOnSwitchChangeListener(this);
                    mSwitch.show();
                }
            }
        }
    }

    public void setChecked(boolean isChecked) {
        if (mSwitch != null) {
            mSwitch.setChecked(isChecked);
            mPreference.notifyDependencyChange(!isChecked);
        }
    }

    @Override
    public void updateState(Preference preference) {
        boolean checked = Settings.System.getIntForUser(mContext.getContentResolver(),
                ADAPTIVE_PLAYBACK_ENABLED, 0, UserHandle.USER_CURRENT) == 1;
        setChecked(checked);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        Settings.System.putIntForUser(
                mContext.getContentResolver(),
                ADAPTIVE_PLAYBACK_ENABLED, isChecked ? 1 : 0, UserHandle.USER_CURRENT);
        mPreference.notifyDependencyChange(!isChecked);
    }
}
