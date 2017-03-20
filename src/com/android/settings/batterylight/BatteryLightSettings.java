/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.android.settings.batterylight;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.support.v14.preference.PreferenceFragment;
import android.support.v14.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Switch;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.widget.SwitchBar;

import java.util.List;
import java.util.ArrayList;

public class BatteryLightSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener, Indexable  {

    private static final String TAG = "BatteryLightSettings";

    private static final String LOW_COLOR_PREF = "low_color";
    private static final String MEDIUM_COLOR_PREF = "medium_color";
    private static final String FULL_COLOR_PREF = "full_color";
    private static final String REALLY_FULL_COLOR_PREF = "really_full_color";
    private static final String BATTERY_PULSE_PREF = "battery_light_pulse";

    private static final String[] DEPENDENT_PREFS = {
        BATTERY_PULSE_PREF,
        LOW_COLOR_PREF,
        MEDIUM_COLOR_PREF,
        FULL_COLOR_PREF,
        REALLY_FULL_COLOR_PREF
    };

    private static final int MENU_RESET = Menu.FIRST;

    private boolean mBatteryLightEnabled;
    private BatteryLightEnabler mBatteryLightEnabler;
    private SwitchPreference mPulsePref;
    private BatteryLightPreference mLowColorPref;
    private BatteryLightPreference mMediumColorPref;
    private BatteryLightPreference mFullColorPref;
    private BatteryLightPreference mReallyFullColorPref;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.CONFIGURE_NOTIFICATION;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.battery_light_settings);

        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getContentResolver();

        mBatteryLightEnabled = getResources().getBoolean(
                com.android.internal.R.bool.config_intrusiveBatteryLed);

        mPulsePref = (SwitchPreference)prefSet.findPreference(BATTERY_PULSE_PREF);
        mPulsePref.setOnPreferenceChangeListener(this);

        // Does the Device support changing battery LED colors?
        if (getResources().getBoolean(com.android.internal.R.bool.config_multiColorBatteryLed)) {
            setHasOptionsMenu(true);

            // Low, Medium and full color preferences
            mLowColorPref = (BatteryLightPreference) prefSet.findPreference(LOW_COLOR_PREF);
            mLowColorPref.setOnPreferenceChangeListener(this);

            mMediumColorPref = (BatteryLightPreference) prefSet.findPreference(MEDIUM_COLOR_PREF);
            mMediumColorPref.setOnPreferenceChangeListener(this);

            mFullColorPref = (BatteryLightPreference) prefSet.findPreference(FULL_COLOR_PREF);
            mFullColorPref.setOnPreferenceChangeListener(this);

            mReallyFullColorPref = (BatteryLightPreference) prefSet.findPreference(REALLY_FULL_COLOR_PREF);
            mReallyFullColorPref.setOnPreferenceChangeListener(this);
        } else {
            prefSet.removePreference(prefSet.findPreference("colors_list"));
            resetColors();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mBatteryLightEnabler != null) {
            mBatteryLightEnabler.teardownSwitchBar();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        SettingsActivity activity = (SettingsActivity) getActivity();
        mBatteryLightEnabler = new BatteryLightEnabler(activity.getSwitchBar());
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mBatteryLightEnabler != null) {
            mBatteryLightEnabler.resume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mBatteryLightEnabler != null) {
            mBatteryLightEnabler.pause();
        }
    }

    /**
     * Updates the default or application specific notification settings.
     *
     * @param key of the specific setting to update
     * @param color
     */
    protected void updateValues(String key, Integer color) {
        ContentResolver resolver = getContentResolver();

        if (key.equals(LOW_COLOR_PREF)) {
            Settings.System.putInt(resolver, Settings.System.BATTERY_LIGHT_LOW_COLOR, color);
        } else if (key.equals(MEDIUM_COLOR_PREF)) {
            Settings.System.putInt(resolver, Settings.System.BATTERY_LIGHT_MEDIUM_COLOR, color);
        } else if (key.equals(FULL_COLOR_PREF)) {
            Settings.System.putInt(resolver, Settings.System.BATTERY_LIGHT_FULL_COLOR, color);
        } else if (key.equals(REALLY_FULL_COLOR_PREF)) {
            Settings.System.putInt(resolver, Settings.System.BATTERY_LIGHT_REALLY_FULL_COLOR, color);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset)
                .setIcon(R.drawable.ic_settings_backup_restore)
                .setAlphabeticShortcut('r')
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                resetToDefaults();
                return true;
        }

        return false;
    }

    protected void resetToDefaults() {
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.BATTERY_LIGHT_PULSE, 0);

        if (mBatteryLightEnabler != null) {
            mBatteryLightEnabler.setState(mBatteryLightEnabled);
        }

        resetColors();
    }

    protected void resetColors() {
        ContentResolver resolver = getActivity().getContentResolver();
        Resources res = getResources();

        // Reset to the framework default colors
        Settings.System.putInt(resolver, Settings.System.BATTERY_LIGHT_LOW_COLOR,
                res.getInteger(com.android.internal.R.integer.config_notificationsBatteryLowARGB));
        Settings.System.putInt(resolver, Settings.System.BATTERY_LIGHT_MEDIUM_COLOR,
                res.getInteger(com.android.internal.R.integer.config_notificationsBatteryMediumARGB));
        Settings.System.putInt(resolver, Settings.System.BATTERY_LIGHT_FULL_COLOR,
                res.getInteger(com.android.internal.R.integer.config_notificationsBatteryFullARGB));
        Settings.System.putInt(resolver, Settings.System.BATTERY_LIGHT_REALLY_FULL_COLOR,
                res.getInteger(com.android.internal.R.integer.config_notificationsBatteryReallyFullARGB));
        refreshDefault();
    }

    private void refreshDefault() {
        ContentResolver resolver = getContentResolver();
        Resources res = getResources();

        mPulsePref.setChecked(Settings.System.getInt(resolver,
                        Settings.System.BATTERY_LIGHT_PULSE, 0) != 0);

        if (mLowColorPref != null) {
            int lowColor = Settings.System.getInt(resolver, Settings.System.BATTERY_LIGHT_LOW_COLOR,
                    res.getInteger(com.android.internal.R.integer.config_notificationsBatteryLowARGB));
            mLowColorPref.setColor(lowColor);
        }

        if (mMediumColorPref != null) {
            int mediumColor = Settings.System.getInt(resolver, Settings.System.BATTERY_LIGHT_MEDIUM_COLOR,
                    res.getInteger(com.android.internal.R.integer.config_notificationsBatteryMediumARGB));
            mMediumColorPref.setColor(mediumColor);
        }

        if (mFullColorPref != null) {
            int fullColor = Settings.System.getInt(resolver, Settings.System.BATTERY_LIGHT_FULL_COLOR,
                    res.getInteger(com.android.internal.R.integer.config_notificationsBatteryFullARGB));
            mFullColorPref.setColor(fullColor);
        }

        if (mReallyFullColorPref != null) {
            int reallyFullColor = Settings.System.getInt(resolver, Settings.System.BATTERY_LIGHT_REALLY_FULL_COLOR,
                    res.getInteger(com.android.internal.R.integer.config_notificationsBatteryReallyFullARGB));
            mReallyFullColorPref.setColor(reallyFullColor);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mPulsePref) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.BATTERY_LIGHT_PULSE, (Boolean) objValue ? 1:0);
        } else {
            BatteryLightPreference lightPref = (BatteryLightPreference) preference;
            updateValues(lightPref.getKey(), lightPref.getColor());
        }

        return true;
    }

    private void enableBatteryLight(boolean enabled, boolean start) {
        final PreferenceScreen prefSet = getPreferenceScreen();

        for (String prefKey : DEPENDENT_PREFS) {
            Preference pref = (Preference) prefSet.findPreference(prefKey);
            pref.setEnabled(enabled);
        }

        if (start) refreshDefault();
    }

    private class BatteryLightEnabler implements SwitchBar.OnSwitchChangeListener {

        private final Context mContext;
        private final SwitchBar mSwitchBar;
        private boolean mListeningToOnSwitchChange;

        public BatteryLightEnabler(SwitchBar switchBar) {
            mContext = switchBar.getContext();
            mSwitchBar = switchBar;
            mSwitchBar.show();

            boolean enabled = Settings.System.getInt(
                            mContext.getContentResolver(),
                            Settings.System.BATTERY_LIGHT_ENABLED, mBatteryLightEnabled ? 1 : 0) != 0;
            mSwitchBar.setChecked(enabled);
            BatteryLightSettings.this.enableBatteryLight(enabled, true);
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

        public void setState(boolean enabled) {
            mSwitchBar.setChecked(enabled);
            onSwitchChanged(null, enabled);
        }

        @Override
        public void onSwitchChanged(Switch switchView, boolean isChecked) {
            Settings.System.putInt(
                    mContext.getContentResolver(),
                    Settings.System.BATTERY_LIGHT_ENABLED, isChecked ? 1 : 0);
            BatteryLightSettings.this.enableBatteryLight(isChecked, false);
        }
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.battery_light_settings;
                    result.add(sir);
                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    ArrayList<String> result = new ArrayList<String>();
                    final Resources res = context.getResources();
                    if (!res.getBoolean(com.android.internal.R.bool.config_intrusiveBatteryLed)) {
                        result.add(BATTERY_PULSE_PREF);
                    }
                    if (!res.getBoolean(com.android.internal.R.bool.config_multiColorBatteryLed)) {
                        result.add(LOW_COLOR_PREF);
                        result.add(MEDIUM_COLOR_PREF);
                        result.add(FULL_COLOR_PREF);
                        result.add(REALLY_FULL_COLOR_PREF);
                    }
                    return result;
                }
            };
}
