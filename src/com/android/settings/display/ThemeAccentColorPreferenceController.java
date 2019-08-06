/*
 * Copyright (C) 2019 The Android Open Source Project
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
import android.content.Intent;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.content.res.Resources;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.DisplaySettings;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.wrapper.OverlayManagerWrapper;
import com.pa.support.colorpicker.ColorPickerPreference;

public class ThemeAccentColorPreferenceController extends BasePreferenceController
        implements ColorPickerPreference.CustomColorClickListener,
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "ThemeAccentController";
    private static final boolean DEBUG = true;
    private static final String KEY_CUSTOM_ACCENT_COLOR = "persist.sys.theme.accentcolor";
    private static final String KEY_LIGHT_INDEX = "persist.sys.theme.light.index";
    private static final String KEY_DARK_INDEX = "persist.sys.theme.dark.index";
    private static final int SYSTEM_LIGHT_THEME = 0;
    private static final int SYSTEM_DARK_THEME = 1;
    private static final int SYSTEM_BLACK_THEME = 2;
    protected static final int THEME_CUSTOM_ACCENT_REQUEST_CODE = 100;


    private final DisplaySettings mFragment;

    private int[] mColorStringIds;
    private String[] mColors;
    private ColorPickerPreference mAccentColorPreference;
    private IOverlayManager mOverlayManager;
    private OverlayManagerWrapper mOverlayManagerWrapper;
    private boolean mLightModeOn;
    private boolean mDarkModeOn;
    private String mCurrentTempColor;

    public ThemeAccentColorPreferenceController(Context context, String preferenceKey, DisplaySettings fragment) {
        super(context, preferenceKey);
        if (DEBUG) Log.v(TAG, "Inside constructor");
        mOverlayManager = IOverlayManager.Stub.asInterface(
                ServiceManager.getService(Context.OVERLAY_SERVICE));
        mOverlayManagerWrapper = ServiceManager.getService(Context.OVERLAY_SERVICE) != null
                ? new OverlayManagerWrapper() : null;
        mFragment = fragment;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (DEBUG) Log.v(TAG, "Inside displayPreference");
        initAccentColors();
        mAccentColorPreference = (ColorPickerPreference) screen.findPreference(getPreferenceKey());
        mAccentColorPreference.setCustomColorClickListener(this);
        mAccentColorPreference.setOnPreferenceChangeListener(this);
        mAccentColorPreference.setCurrentThemeMode(getCurrentSystemTheme());
        mAccentColorPreference.setColorPalette(mColors, mColorStringIds);
        mAccentColorPreference.setDefaultColor(toRGBString(R.color.primary_default_light));
        mAccentColorPreference.setMessageText(R.string.color_picker_accent_color_message);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (DEBUG) Log.v(TAG, "Inside updateState");

    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (DEBUG) Log.v(TAG, "Inside handlePreferenceTreeClick");
        return super.handlePreferenceTreeClick(preference);
    }

    private void initAccentColors() {
        if (DEBUG) Log.v(TAG, "Inside initAccentColors");
        mColors = new String[]{
                toRGBString(R.color.primary_default_light),
                toRGBString(R.color.primary_golden_light),
                toRGBString(R.color.primary_lemon_yellow_light),
                toRGBString(R.color.primary_grass_green_light),
                toRGBString(R.color.primary_charm_purple_light),
                toRGBString(R.color.primary_sky_blue_light),
                toRGBString(R.color.primary_vigour_red_light),
                toRGBString(R.color.primary_fashion_pink_light),
                toRGBString(R.color.primary_red_light),
                toRGBString(R.color.primary_blue_light),
                toRGBString(R.color.primary_green_light),
                toRGBString(R.color.primary_green_custom)
        };

        mColorStringIds = new int[]{
                R.string.primary_default_label,
                R.string.primary_golden_label,
                R.string.primary_lemon_yellow_label,
                R.string.primary_grass_green_label,
                R.string.primary_charm_purple_label,
                R.string.primary_sky_blue_label,
                R.string.primary_vigour_red_label,
                R.string.primary_fashion_pink_label,
                R.string.primary_red_label,
                R.string.primary_royal_blue_label,
                R.string.primary_dark_green_label,
                R.string.customization_settings_title
        };
    }

    private String toRGBString(int color) {
        Resources res = mContext.getResources();
        return "#" + res.getString(color).substring(3);
    }

    private int getCurrentSystemTheme() {
        OverlayInfo themeDarkInfo = null;
        OverlayInfo themeBlackInfo = null;
        try {
            themeDarkInfo = mOverlayManager.getOverlayInfo("com.android.system.theme.dark",
                    UserHandle.USER_CURRENT);
            themeBlackInfo = mOverlayManager.getOverlayInfo("com.android.system.theme.black",
                    UserHandle.USER_CURRENT);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        if (themeDarkInfo != null && themeDarkInfo.isEnabled()) {
            mDarkModeOn = true;
            return SYSTEM_DARK_THEME;
        } else if (themeBlackInfo != null && themeBlackInfo.isEnabled()) {
            mDarkModeOn = true;
            return SYSTEM_BLACK_THEME;
        } else {
            mLightModeOn = true;
            return SYSTEM_LIGHT_THEME;
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        if (DEBUG) Log.v(TAG, "Inside onPreferenceChange");
        String theme = (String) o;
        if (TextUtils.isEmpty(theme)) {
            Log.v(TAG, "Empty");
        } else {
            int i = 0;
            while (true) {
                if (i >= mColors.length)
                    break;
                if (theme.equals(mColors[i])) {
                    sendTheme(i);
                    break;
                }
                i++;
            }
        }
        return true;
    }

    private void sendTheme(int index) {
        if (DEBUG) Log.v(TAG, "Inside sendTheme");
        String accentColor = mColors[index];
        if (index == 11) {
            accentColor = mCurrentTempColor;
            mColors[11] = accentColor;
            if (DEBUG) Log.v(TAG, "Index is 11, color: " + accentColor);
        }
        if (TextUtils.isEmpty(accentColor)) {
            accentColor = mColors[0];
        }
        SystemProperties.set(KEY_CUSTOM_ACCENT_COLOR, accentColor.replace("#", ""));
        mOverlayManagerWrapper.reloadAndroidAssets(UserHandle.USER_CURRENT);
        mOverlayManagerWrapper.reloadAssets("com.android.settings", UserHandle.USER_CURRENT);
        mOverlayManagerWrapper.reloadAssets("com.android.systemui", UserHandle.USER_CURRENT);
    }

    @Override
    public void onCustomColorClick() {
        if (DEBUG) Log.v(TAG, "Inside onCustomColorClick");
        Intent mCustomColor = new Intent("pa.intent.action.PA_COLOR_PICKER");
        mCurrentTempColor = "#" + SystemProperties.get(KEY_CUSTOM_ACCENT_COLOR, "FF0000");
        mCustomColor.putExtra("current_color", mCurrentTempColor);
        mFragment.startActivityForResult(mCustomColor, THEME_CUSTOM_ACCENT_REQUEST_CODE);
    }

    public void onActivityResult(int requestCode, Intent data) {
        if (DEBUG) Log.v(TAG, "Inside onActivityResult");
        if (THEME_CUSTOM_ACCENT_REQUEST_CODE == requestCode && data != null) {
            mCurrentTempColor = data.getStringExtra("current_temp_color");
            mAccentColorPreference.setCustomBgColor(mCurrentTempColor);
        }
    }
}
