/*
* Copyright (C) 2008 The Android Open Source Project
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

package com.android.settings;

import java.io.IOException;

import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.app.AlertDialog;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.os.Handler;
import android.util.Log;
import android.net.Uri;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.IWindowManager;
import android.os.ServiceManager;
import android.os.IBinder;
import android.os.IPowerManager;
import android.view.WindowManagerGlobal;

import android.provider.Settings;
import android.os.SystemProperties;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class CRSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "CR_Settings";
    private static final boolean DEBUG = true;

    private static final String DISABLE_BOOTANIMATION_PREF = "pref_disable_boot_animation";
    private static final String DISABLE_BOOTANIMATION_PERSIST_PROP = "persist.sys.nobootanimation";
    private static final String FORCE_HIGHEND_GFX_PREF = "pref_force_highend_gfx";
    private static final String FORCE_HIGHEND_GFX_PERSIST_PROP = "persist.sys.force_highendgfx";
    private static final String KONSTA_NAVBAR = "konsta_navbar";

    private final Configuration mCurrentConfig = new Configuration();

    private CheckBoxPreference mDisableBootanimPref;
    private CheckBoxPreference mForceHighEndGfx;

    private Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.crystal_rom);

        PreferenceScreen prefSet = getPreferenceScreen();

        mKonstaNavbar = (CheckBoxPreference) findPreference(KONSTA_NAVBAR);
        mKonstaNavbar.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.KONSTA_NAVBAR, 0) == 1);

        if (ActivityManager.isLowRamDeviceStatic()) {
            mForceHighEndGfx = (CheckBoxPreference) prefSet.findPreference(FORCE_HIGHEND_GFX_PREF);
            String forceHighendGfx = SystemProperties.get(FORCE_HIGHEND_GFX_PERSIST_PROP, "false");
            mForceHighEndGfx.setChecked("true".equals(forceHighendGfx));
        } else {
            prefSet.removePreference(findPreference(FORCE_HIGHEND_GFX_PREF));
        }

        mDisableBootanimPref = (CheckBoxPreference) prefSet.findPreference(DISABLE_BOOTANIMATION_PREF);
        mDisableBootanimPref.setChecked("1".equals(SystemProperties.get(DISABLE_BOOTANIMATION_PERSIST_PROP, "0")));
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        if (preference == mKonstaNavbar) {
            Settings.System.putInt(getContentResolver(), Settings.System.KONSTA_NAVBAR,
                    mKonstaNavbar.isChecked() ? 1 : 0);

            new AlertDialog.Builder(getActivity())
                    .setTitle(getResources().getString(R.string.konsta_navbar_dialog_title))
                    .setMessage(getResources().getString(R.string.konsta_navbar_dialog_msg))
                    .setNegativeButton(getResources().getString(R.string.konsta_navbar_dialog_negative), null)
                    .setCancelable(false)
                    .setPositiveButton(getResources().getString(R.string.konsta_navbar_dialog_positive), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
                            powerManager.reboot(null);
                        }
                    })
                    .create()
                    .show();
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mDisableBootanimPref) {
            SystemProperties.set(DISABLE_BOOTANIMATION_PERSIST_PROP, mDisableBootanimPref.isChecked() ? "1" : "0");
        } else if (preference == mForceHighEndGfx) {
            SystemProperties.set(FORCE_HIGHEND_GFX_PERSIST_PROP,
                    mForceHighEndGfx.isChecked() ? "true" : "false");
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }
}

