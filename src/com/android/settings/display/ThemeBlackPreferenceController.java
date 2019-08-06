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
 * limitations under the License
 */

package com.android.settings.display;

import android.content.Context;
import android.os.Handler;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;

import com.android.settings.core.BasePreferenceController;
import com.pa.support.utils.PAUtils;

public class ThemeBlackPreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "ThemeBlackController";
    private static final boolean DEBUG = false;
    protected SwitchPreference mPreference;

    public ThemeBlackPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = (SwitchPreference) screen.findPreference(getPreferenceKey());
        int value = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.PREFER_BLACK_THEMES, 0);
        mPreference.setChecked(value == 1);
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateVisibility();
            }
        }, 100);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        boolean value = (Boolean) o;
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.PREFER_BLACK_THEMES, value ? 1 : 0);
        return true;
    }

    private void updateVisibility() {
        if (DEBUG) Log.v(TAG, "Inside updateVisibility");
        if (PAUtils.isUsingLightTheme(mContext.getContentResolver())) {
            mPreference.setVisible(false);
        } else {
            mPreference.setVisible(true);
        }
    }
}
