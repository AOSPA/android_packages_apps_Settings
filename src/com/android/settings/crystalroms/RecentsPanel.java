/*
 * Copyright (C) 2013 SlimRoms Project
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

package com.android.settings.crystalroms;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.DialogInterface;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.SwitchPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;


import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.util.Helpers;
import com.android.internal.util.omni.OmniSwitchConstants;

public class RecentsPanel extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String TAG = "RecentsPanelSettings";

    private static final String RECENTS_USE_OMNISWITCH = "recents_use_omniswitch";
    private static final String OMNISWITCH_START_SETTINGS = "omniswitch_start_settings";
    private static final String SENSE4_STYLE_RECENTS_PROP = "pref_sense4_style_recents";

    private static final String RECENTS_USE_SLIM = "recents_use_slim";

    // Package name of the omnniswitch app
    public static final String OMNISWITCH_PACKAGE_NAME = "org.omnirom.omniswitch";

    // Intent for launching the omniswitch settings actvity
    public static Intent INTENT_OMNISWITCH_SETTINGS = new Intent(Intent.ACTION_MAIN)
         .setClassName(OMNISWITCH_PACKAGE_NAME, OMNISWITCH_PACKAGE_NAME + ".SettingsActivity");

    private CheckBoxPreference mRecentsUseOmniSwitch;
    private Preference mOmniSwitchSettings;
    private boolean mOmniSwitchStarted;
    private CheckBoxPreference mRecentsUseSlim;
    private SwitchPreference mSense4RecentsPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.recents_apps_panel);

        PreferenceScreen prefSet = getPreferenceScreen();

        mSense4RecentsPref = (SwitchPreference) prefSet.findPreference(SENSE4_STYLE_RECENTS_PROP);

        boolean useOmniSwitch = false;
        boolean useSlimRecents = false;

        try {
            useOmniSwitch = Settings.System.getInt(getContentResolver(), Settings.System.RECENTS_USE_OMNISWITCH) == 1
                                && isOmniSwitchServiceRunning();
            useSlimRecents = Settings.System.getInt(getContentResolver(), Settings.System.RECENTS_USE_SLIM) == 1;
        } catch(SettingNotFoundException e) {
               e.printStackTrace();
        }

        // OmniSwitch
        mRecentsUseOmniSwitch = (CheckBoxPreference) prefSet.findPreference(RECENTS_USE_OMNISWITCH);
        mRecentsUseOmniSwitch.setChecked(useOmniSwitch);
        mRecentsUseOmniSwitch.setOnPreferenceChangeListener(this);
        mRecentsUseOmniSwitch.setEnabled(!useSlimRecents);

        mOmniSwitchSettings = (Preference) prefSet.findPreference(OMNISWITCH_START_SETTINGS);
        mOmniSwitchSettings.setEnabled(useOmniSwitch);

        // Slim recents
        mRecentsUseSlim = (CheckBoxPreference) prefSet.findPreference(RECENTS_USE_SLIM);
        mRecentsUseSlim.setChecked(useSlimRecents);
        mRecentsUseSlim.setOnPreferenceChangeListener(this);
        mRecentsUseSlim.setEnabled(!useOmniSwitch);

        updateSense4Recents();
    }

    /* Update functions */
    private void updateSense4Recents() {
        mSense4RecentsPref.setChecked(Settings.System.getInt(getActivity().getContentResolver(), Settings.System.SENSE4_RECENT_APPS, 0) == 1);
        mSense4RecentsPref.setOnPreferenceChangeListener(this);
    }

    /* Write functions */
    private void writeSense4Recents(Object NewVal) {
        Settings.System.putInt(getActivity().getContentResolver(), Settings.System.SENSE4_RECENT_APPS, (Boolean) NewVal ? 1 : 0);
        Helpers.restartSystemUI();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mOmniSwitchSettings) {
            startActivity(INTENT_OMNISWITCH_SETTINGS);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
     }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mRecentsUseOmniSwitch) {
            boolean omniSwitchEnabled = (Boolean) newValue;

            // Give user information that OmniSwitch service is not running
            if (omniSwitchEnabled && !isOmniSwitchServiceRunning()) {
                openOmniSwitchFirstTimeWarning();
            }

            Settings.System.putInt(getContentResolver(), Settings.System.RECENTS_USE_OMNISWITCH, omniSwitchEnabled ? 1 : 0);

            // Update OmniSwitch UI components
            mRecentsUseOmniSwitch.setChecked(omniSwitchEnabled);
            mOmniSwitchSettings.setEnabled(omniSwitchEnabled);

            // Update Slim recents UI components
            mRecentsUseSlim.setEnabled(!omniSwitchEnabled);
            return true;
        } else if (preference == mRecentsUseSlim) {
            boolean useSlimRecents = (Boolean) newValue;

            Settings.System.putInt(getContentResolver(), Settings.System.RECENTS_USE_SLIM,
                    useSlimRecents ? 1 : 0);

            // Give user information that Slim Recents needs restart SystemUI
            openSlimRecentsWarning();

            // Update OmniSwitch UI components
            mRecentsUseOmniSwitch.setEnabled(!useSlimRecents);
            mRecentsUseSlim.setChecked(useSlimRecents);
            return true;
        } else if (preference == mSense4RecentsPref) {
            writeSense4Recents(newValue);
            return true;
        }
        return false;
    }

    private boolean isOmniSwitchServiceRunning() {
        String serviceName = "org.omnirom.omniswitch.SwitchService";
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceName.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void openOmniSwitchFirstTimeWarning() {
        new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.omniswitch_first_time_title))
            .setMessage(getResources().getString(R.string.omniswitch_first_time_message))
            .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            }).show();
    }

    private void openSlimRecentsWarning() {
        new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.slim_recents_warning_title))
            .setMessage(getResources().getString(R.string.slim_recents_warning_message))
            .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    Helpers.restartSystemUI();
                }
            }).show();
    }
}

