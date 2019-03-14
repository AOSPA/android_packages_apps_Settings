/*
 * Copyright (C) 2019-2020 Paranoid Android
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

package com.android.settings;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.search.SearchIndexable;


import java.util.Arrays;
import java.util.List;

@SearchIndexable
public class ButtonSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener, Indexable {
    private static final String TAG = "ButtonSettings";

    private static final int KEY_MASK_BACK = 0x02;
    private static final int KEY_MASK_APP_SWITCH = 0x10;

    private static final String KEY_SWAP_NAVIGATION_KEYS = "swap_navigation_keys";
    private static final String KEY_BUTTON_BRIGHTNESS = "button_brightness";
    private static final String EMPTY_STRING = "";

    private Handler mHandler;

    private SwitchPreference mSwapNavigationkeys;
    private SwitchPreference mButtonBrightness;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.button_settings);

        mHandler = new Handler();

        final Resources res = getActivity().getResources();
        final PreferenceScreen prefScreen = getPreferenceScreen();

        /* Swap Navigation Keys */
        mSwapNavigationkeys = (SwitchPreference) findPreference(KEY_SWAP_NAVIGATION_KEYS);
        if (mSwapNavigationkeys != null) {
            mSwapNavigationkeys.setOnPreferenceChangeListener(this);
        }

        /* Button Brightness */
        mButtonBrightness = (SwitchPreference) findPreference(KEY_BUTTON_BRIGHTNESS);
        if (mButtonBrightness != null) {
            int defaultButtonBrightness = res.getInteger(
                    com.android.internal.R.integer.config_buttonBrightnessSettingDefault);
            if (defaultButtonBrightness > 0) {
                mButtonBrightness.setOnPreferenceChangeListener(this);
            } else {
                prefScreen.removePreference(mButtonBrightness);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        reload();
    }

    @Override
    public int getMetricsCategory() {
        return -1;
    }

    private boolean handleOnPreferenceTreeClick(Preference preference) {
        return false;
    }

    private boolean handleOnPreferenceChange(Preference preference, Object newValue) {
        final String setting = getSystemPreferenceString(preference);

        if (TextUtils.isEmpty(setting)) {
            // No system setting.
            return false;
        }

        if (preference != null && preference instanceof ListPreference) {
            ListPreference listPref = (ListPreference) preference;
            String value = (String) newValue;
            int index = listPref.findIndexOfValue(value);
            listPref.setSummary(listPref.getEntries()[index]);
            Settings.System.putIntForUser(getContentResolver(), setting, Integer.valueOf(value),
                    UserHandle.USER_CURRENT);
        } else if (preference != null && preference instanceof SwitchPreference) {
            boolean state = false;
            if (newValue instanceof Boolean) {
                state = (Boolean) newValue;
            } else if (newValue instanceof String) {
                state = Integer.valueOf((String) newValue) != 0;
            }
            Settings.System.putIntForUser(getContentResolver(), setting, state ? 1 : 0,
                    UserHandle.USER_CURRENT);
        }

        return true;
    }

    private String getSystemPreferenceString(Preference preference) {
        if (preference == null) {
            return EMPTY_STRING;
        } else if (preference == mSwapNavigationkeys) {
            return Settings.System.SWAP_NAVIGATION_KEYS;
        } else if (preference == mButtonBrightness) {
            return Settings.System.BUTTON_BRIGHTNESS_ENABLED;
        }

        return EMPTY_STRING;
    }

    private void reload() {
        final ContentResolver resolver = getActivity().getContentResolver();
        final Resources res = getActivity().getResources();

        final PreferenceScreen prefScreen = getPreferenceScreen();

        final boolean hasBack = (KEY_MASK_BACK) != 0;
        final boolean hasAppSwitch = (KEY_MASK_APP_SWITCH) != 0;

        final boolean swapNavigationkeysEnabled = Settings.System.getIntForUser(resolver,
                Settings.System.SWAP_NAVIGATION_KEYS, 0, UserHandle.USER_CURRENT) != 0;

        final boolean buttonBrightnessEnabled = Settings.System.getIntForUser(resolver,
                Settings.System.BUTTON_BRIGHTNESS_ENABLED, 1, UserHandle.USER_CURRENT) != 0;

        if (mSwapNavigationkeys != null) {
            mSwapNavigationkeys.setChecked(swapNavigationkeysEnabled);
            // Disable when no HW back and recents available.
            mSwapNavigationkeys.setEnabled(hasBack && hasAppSwitch);
        }

        if (mButtonBrightness != null) {
            mButtonBrightness.setChecked(buttonBrightnessEnabled);
        } else {
            prefScreen.removePreference(mButtonBrightness);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean handled = handleOnPreferenceChange(preference, newValue);
        if (handled) {
            reload();
        }
        return handled;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        final boolean handled = handleOnPreferenceTreeClick(preference);
        return handled;
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {

                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.button_settings;
                    return Arrays.asList(sir);
                }


                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);

                    int defaultButtonBrightness = Resources.getSystem().getInteger(
                            com.android.internal.R.integer.config_buttonBrightnessSettingDefault);

                    // Add whatever we won´t want to show.
                    if (defaultButtonBrightness == 0)
                        keys.add(KEY_BUTTON_BRIGHTNESS);

                    return keys;
                }
            };
}
