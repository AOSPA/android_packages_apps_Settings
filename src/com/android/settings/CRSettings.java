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
import android.widget.Toast;

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
    private static final String ENABLE_NAVBAR = "enable_navbar";
    private static final String KEY_NAVIGATION_HEIGHT = "nav_buttons_height";
    private static final String KEY_LISTVIEW_ANIMATION = "listview_animation";
    private static final String KEY_LISTVIEW_INTERPOLATOR = "listview_interpolator";
    private static final String KEY_TOAST_ANIMATION = "toast_animation";
    private static final String KEY_FLOAT_RECENT = "pref_float_recent";
    private static final String KEY_FLOAT_NOTIFICATION = "pref_float_notification";

    private final Configuration mCurrentConfig = new Configuration();

    private CheckBoxPreference mDisableBootanimPref;
    private CheckBoxPreference mForceHighEndGfx;
    private CheckBoxPreference mEnableNavbar;
    private ListPreference mNavButtonsHeight;
    private ListPreference mListViewAnimation;
    private ListPreference mListViewInterpolator;
    private ListPreference mToastAnimation;
    private CheckBoxPreference mFloatRecent;
    private CheckBoxPreference mFloatNotification;

    private Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.crystal_rom);

        PreferenceScreen prefSet = getPreferenceScreen();

        mListViewAnimation = (ListPreference) prefSet.findPreference(KEY_LISTVIEW_ANIMATION);
        int listviewanimation = Settings.System.getInt(getContentResolver(),
                Settings.System.LISTVIEW_ANIMATION, 0);
        mListViewAnimation.setValue(String.valueOf(listviewanimation));
        mListViewAnimation.setSummary(mListViewAnimation.getEntry());
        mListViewAnimation.setOnPreferenceChangeListener(this);

        mListViewInterpolator = (ListPreference) prefSet.findPreference(KEY_LISTVIEW_INTERPOLATOR);
        int listviewinterpolator = Settings.System.getInt(getContentResolver(),
                Settings.System.LISTVIEW_INTERPOLATOR, 0);
        mListViewInterpolator.setValue(String.valueOf(listviewinterpolator));
        mListViewInterpolator.setSummary(mListViewInterpolator.getEntry());
        mListViewInterpolator.setOnPreferenceChangeListener(this);
        mListViewInterpolator.setEnabled(listviewanimation > 0);

        mToastAnimation = (ListPreference) findPreference(KEY_TOAST_ANIMATION);
        if (mToastAnimation != null) {
           int toastAnimation = Settings.System.getInt(getContentResolver(),
                    Settings.System.TOAST_ANIMATION, 1);
           mToastAnimation.setSummary(mToastAnimation.getEntry());
           mToastAnimation.setSummary(mToastAnimation.getEntries()[toastAnimation]);
           mToastAnimation.setValue(String.valueOf(toastAnimation));
        }
        mToastAnimation.setOnPreferenceChangeListener(this);

        boolean hasNavBarByDefault = getResources().getBoolean(
                com.android.internal.R.bool.config_showNavigationBar);
        boolean enableNavigationBar = Settings.System.getInt(getContentResolver(),
                Settings.System.ENABLE_NAVBAR, hasNavBarByDefault ? 1 : 0) == 1;
        mEnableNavbar = (CheckBoxPreference) findPreference(ENABLE_NAVBAR);
        mEnableNavbar.setChecked(enableNavigationBar);
        mEnableNavbar.setOnPreferenceChangeListener(this);

        mNavButtonsHeight = (ListPreference) findPreference(KEY_NAVIGATION_HEIGHT);
        if (mNavButtonsHeight != null) {
            int statusNavButtonsHeight = Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_HEIGHT, 48);

            mNavButtonsHeight.setValue(String.valueOf(statusNavButtonsHeight));
            mNavButtonsHeight.setSummary(mNavButtonsHeight.getEntry());
            mNavButtonsHeight.setEnabled(enableNavigationBar);
            mNavButtonsHeight.setOnPreferenceChangeListener(this);
        }

        if (ActivityManager.isLowRamDeviceStatic()) {
            mForceHighEndGfx = (CheckBoxPreference) prefSet.findPreference(FORCE_HIGHEND_GFX_PREF);
            String forceHighendGfx = SystemProperties.get(FORCE_HIGHEND_GFX_PERSIST_PROP, "false");
            mForceHighEndGfx.setChecked("true".equals(forceHighendGfx));
        } else {
            prefSet.removePreference(findPreference(FORCE_HIGHEND_GFX_PREF));
        }

        mDisableBootanimPref = (CheckBoxPreference) prefSet.findPreference(DISABLE_BOOTANIMATION_PREF);
        mDisableBootanimPref.setChecked("1".equals(SystemProperties.get(DISABLE_BOOTANIMATION_PERSIST_PROP, "0")));

        mFloatRecent = (CheckBoxPreference) prefSet.findPreference(KEY_FLOAT_RECENT);
        mFloatRecent.setChecked(Settings.System.getInt(getContentResolver(), Settings.System.RECENTS_SWIPE_FLOATING, 0) == 1);

        mFloatNotification = (CheckBoxPreference) prefSet.findPreference(KEY_FLOAT_NOTIFICATION);
        mFloatNotification.setChecked(Settings.System.getInt(getContentResolver(), Settings.System.STATUS_BAR_NOTIFICATION_SWIPE_FLOATING, 0) == 1);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;

        if (preference == mDisableBootanimPref) {
            SystemProperties.set(DISABLE_BOOTANIMATION_PERSIST_PROP, mDisableBootanimPref.isChecked() ? "1" : "0");
        } else if (preference == mForceHighEndGfx) {
            SystemProperties.set(FORCE_HIGHEND_GFX_PERSIST_PROP,
                    mForceHighEndGfx.isChecked() ? "true" : "false");
        } else if (preference == mEnableNavbar) {
            Settings.System.putInt(getContentResolver(), Settings.System.ENABLE_NAVBAR,
                    mEnableNavbar.isChecked() ? 1 : 0);

            new AlertDialog.Builder(getActivity())
                    .setTitle(getResources().getString(R.string.enable_navbar_dialog_title))
                    .setMessage(getResources().getString(R.string.enable_navbar_dialog_msg))
                    .setNegativeButton(getResources().getString(R.string.enable_navbar_dialog_negative), null)
                    .setCancelable(false)
                    .setPositiveButton(getResources().getString(R.string.enable_navbar_dialog_positive), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
                            powerManager.reboot(null);
                        }
                    })
                    .create()
                    .show();
        } else if (preference == mFloatRecent) {
            value = mFloatRecent.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.RECENTS_SWIPE_FLOATING, value ? 1 : 2);
            return true;
        } else if (preference == mFloatNotification) {
            value = mFloatNotification.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUS_BAR_NOTIFICATION_SWIPE_FLOATING, value ? 1 : 2);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (KEY_LISTVIEW_ANIMATION.equals(key)) {
            int value = Integer.parseInt((String) objValue);
            int index = mListViewAnimation.findIndexOfValue((String) objValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LISTVIEW_ANIMATION,
                    value);
            mListViewAnimation.setSummary(mListViewAnimation.getEntries()[index]);
            mListViewInterpolator.setEnabled(value > 0);
        }
        if (KEY_LISTVIEW_INTERPOLATOR.equals(key)) {
            int value = Integer.parseInt((String) objValue);
            int index = mListViewInterpolator.findIndexOfValue((String) objValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LISTVIEW_INTERPOLATOR,
                    value);
            mListViewInterpolator.setSummary(mListViewInterpolator.getEntries()[index]);
        }
        if (preference == mToastAnimation) {
            int value = Integer.parseInt((String) objValue);
            int index = mToastAnimation.findIndexOfValue((String) objValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.TOAST_ANIMATION,
                    value);
            mToastAnimation.setSummary(mToastAnimation.getEntries()[index]);
            Context context = this.getActivity().getApplicationContext();
            if (context != null) {
                Toast.makeText(context, "Toast Test", Toast.LENGTH_SHORT).show();
            }
        } else if (preference == mNavButtonsHeight) {
            int statusNavButtonsHeight = Integer.valueOf((String) objValue);
            int index = mNavButtonsHeight.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_HEIGHT, statusNavButtonsHeight);
            mNavButtonsHeight.setSummary(mNavButtonsHeight.getEntries()[index]);
        }
        return true;
    }
}


