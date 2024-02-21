/*
 * Copyright (C) 2024 Paranoid Android
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

package com.android.settings.fuelguage.powerprofile;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.widget.RadioButtonPreference;

public class PowerProfilePreferenceController extends BasePreferenceController
        implements RadioButtonPreference.OnClickListener, LifecycleObserver, OnStart, OnStop {

    private static final String KEY_POWER_PROFILE_ENABLED = "power_profile_enabled";
    private static final String KEY_POWER_PROFILE_SELECTED = "power_profile_selected";

    private static final String KEY_POWERSAVER = "powersaver";
    private static final String KEY_BALANCED = "balanced";
    private static final String KEY_PERFORMANCE = "performance";

    private static final int PROFILE_POWER_SAVER = 0;
    private static final int PROFILE_BALANCED = 1;
    private static final int PROFILE_PERFORMANCE = 2;

    private boolean mPowerProfileEnabled;
    private int mPowerProfileSelected;

    private PreferenceCategory mPreferenceCategory;
    private RadioButtonPreference mProfilePowerSaverPref;
    private RadioButtonPreference mProfileBalancedPref;
    private RadioButtonPreference mProfilePerformancePref;

    private final SettingObserver mSettingObserver;

    public PowerProfilePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);

        mSettingObserver = new SettingObserver(new Handler(Looper.getMainLooper()));
        mPowerProfileEnabled = Settings.System.getIntForUser(mContext.getContentResolver(),
                KEY_POWER_PROFILE_ENABLED, 0, UserHandle.USER_CURRENT) != 0;
        mPowerProfileSelected = Settings.System.getIntForUser(mContext.getContentResolver(),
                KEY_POWER_PROFILE_SELECTED, PROFILE_BALANCED, UserHandle.USER_CURRENT);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreferenceCategory = screen.findPreference(getPreferenceKey());
        mProfilePowerSaverPref = makeRadioPreference(KEY_POWERSAVER, R.string.power_profile_select_powersaver);
        mProfileBalancedPref = makeRadioPreference(KEY_BALANCED, R.string.power_profile_select_balanced);
        mProfilePerformancePref = makeRadioPreference(KEY_PERFORMANCE, R.string.power_profile_select_performance);
        updateState(null);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE_UNSEARCHABLE;
    }

    @Override
    public void onRadioButtonClicked(RadioButtonPreference preference) {
        int powerProfileSelected = keyToSetting(preference.getKey());
        if (powerProfileSelected != Settings.System.getIntForUser(mContext.getContentResolver(),
                KEY_POWER_PROFILE_SELECTED, PROFILE_BALANCED, UserHandle.USER_CURRENT)) {
            Settings.System.putIntForUser(mContext.getContentResolver(),
                    KEY_POWER_PROFILE_SELECTED, powerProfileSelected, UserHandle.USER_CURRENT);
        }
    }

    @Override
    public void updateState(Preference preference) {
        final boolean isProfilePowerSaver = mPowerProfileEnabled
                && mPowerProfileSelected == PROFILE_POWER_SAVER;
        final boolean isProfileBalanced = mPowerProfileEnabled
                && mPowerProfileSelected == PROFILE_BALANCED;
        final boolean isProfilePerformance = mPowerProfileEnabled
                && mPowerProfileSelected == PROFILE_PERFORMANCE;
        if (mProfilePowerSaverPref != null && mProfilePowerSaverPref.isChecked() != isProfilePowerSaver) {
            mProfilePowerSaverPref.setChecked(isProfilePowerSaver);
        }
        if (mProfileBalancedPref != null && mProfileBalancedPref.isChecked() != isProfileBalanced) {
            mProfileBalancedPref.setChecked(isProfileBalanced);
        }
        if (mProfilePerformancePref != null && mProfilePerformancePref.isChecked() != isProfilePerformance) {
            mProfilePerformancePref.setChecked(isProfilePerformance);
        }

        if (mPowerProfileEnabled) {
            mPreferenceCategory.setEnabled(true);
            mProfilePowerSaverPref.setEnabled(true);
            mProfileBalancedPref.setEnabled(true);
            mProfilePerformancePref.setEnabled(true);
        } else {
            mPreferenceCategory.setEnabled(false);
            mProfilePowerSaverPref.setEnabled(false);
            mProfileBalancedPref.setEnabled(false);
            mProfilePerformancePref.setEnabled(false);
        }
    }

    @Override
    public void onStart() {
        mSettingObserver.observe();
    }

    @Override
    public void onStop() {
        mContext.getContentResolver().unregisterContentObserver(mSettingObserver);
    }

    private static int keyToSetting(String key) {
        switch (key) {
            case KEY_POWER_SAVER:
                return PROFILE_POWER_SAVER;
            case KEY_BALANCED:
                return PROFILE_BALANCED;
            case KEY_PERFORMANCE:
                return PROFILE_PERFORMANCE;
            default:
                return PROFILE_BALANCED;
        }
    }

    private RadioButtonPreference makeRadioPreference(String key, int titleId) {
        RadioButtonPreference pref = new RadioButtonPreference(mPreferenceCategory.getContext());
        pref.setKey(key);
        pref.setTitle(titleId);
        pref.setOnClickListener(this);
        mPreferenceCategory.addPreference(pref);
        return pref;
    }

    private final class SettingObserver extends ContentObserver {
        private final Uri POWER_PROFILE_ENABLED = Settings.System.getUriFor(KEY_POWER_PROFILE_ENABLED);
        private final Uri POWER_PROFILE_SELECTED = Settings.System.getUriFor(KEY_POWER_PROFILE_SELECTED;

        public SettingObserver(Handler handler) {
            super(handler);
        }

        public void observe() {
            final ContentResolver cr = mContext.getContentResolver();
            cr.registerContentObserver(POWER_PROFILE_ENABLED, false, this, UserHandle.USER_ALL);
            cr.registerContentObserver(POWER_PROFILE_SELECTED, false, this, UserHandle.USER_ALL);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (POWER_PROFILE_ENABLED.equals(uri) || POWER_PROFILE_SELECTED.equals(uri)) {
                mPowerProfileEnabled = Settings.System.getIntForUser(mContext.getContentResolver(),
                        KEY_POWER_PROFILE_ENABLED, 0, UserHandle.USER_CURRENT) != 0;
                mPowerProfileSelected = Settings.System.getIntForUser(mContext.getContentResolver(),
                        KEY_POWER_PROFILE_SELECTED, PROFILE_BALANCED, UserHandle.USER_CURRENT);
                updateState(null);
            }
        }
    }
}
