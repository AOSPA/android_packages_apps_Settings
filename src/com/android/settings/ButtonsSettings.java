/*
 * Copyright (C) 2014 ParanoidAndroid Project
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
import android.os.RemoteException;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.support.v14.preference.SwitchPreference;
import android.util.Log;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ButtonsSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener, Indexable {
    private static final String TAG = "SystemSettings";

    private static final int KEY_MASK_HOME = 0x01;
    private static final int KEY_MASK_BACK = 0x02;
    private static final int KEY_MASK_MENU = 0x04;
    private static final int KEY_MASK_ASSIST = 0x08;
    private static final int KEY_MASK_APP_SWITCH = 0x10;
    private static final int KEY_MASK_CAMERA = 0x20;

    private static final String KEY_NAVIGATION_BAR         = "navigation_bar";
    private static final String KEY_SWAP_NAVIGATION_KEYS   = "swap_navigation_keys";
    private static final String KEY_SWAP_SLIDER_ORDER      = "swap_slider_order";
    private static final String KEY_BUTTON_BRIGHTNESS      = "button_brightness";

    private static final String KEY_HOME_LONG_PRESS        = "home_key_long_press";
    private static final String KEY_HOME_DOUBLE_TAP        = "home_key_double_tap";
    private static final String KEY_BACK_LONG_PRESS        = "back_key_long_press";
    private static final String KEY_BACK_DOUBLE_TAP        = "back_key_double_tap";
    private static final String KEY_MENU_LONG_PRESS        = "menu_key_long_press";
    private static final String KEY_MENU_DOUBLE_TAP        = "menu_key_double_tap";
    private static final String KEY_ASSIST_LONG_PRESS      = "assist_key_long_press";
    private static final String KEY_ASSIST_DOUBLE_TAP      = "assist_key_double_tap";
    private static final String KEY_APP_SWITCH_LONG_PRESS  = "app_switch_key_long_press";
    private static final String KEY_APP_SWITCH_DOUBLE_TAP  = "app_switch_key_double_tap";
    private static final String KEY_CAMERA_LONG_PRESS      = "camera_key_long_press";
    private static final String KEY_CAMERA_DOUBLE_TAP      = "camera_key_double_tap";

    private static final String KEY_CATEGORY_HOME          = "home_key";
    private static final String KEY_CATEGORY_BACK          = "back_key";
    private static final String KEY_CATEGORY_MENU          = "menu_key";
    private static final String KEY_CATEGORY_ASSIST        = "assist_key";
    private static final String KEY_CATEGORY_APP_SWITCH    = "app_switch_key";
    private static final String KEY_CATEGORY_CAMERA        = "camera_key";

    private static final String EMPTY_STRING = "";

    private Handler mHandler;

    private int mDeviceHardwareKeys;

    private boolean mHasAlertSlider = false;

    private ListPreference mHomeLongPressAction;
    private ListPreference mHomeDoubleTapAction;
    private ListPreference mBackLongPressAction;
    private ListPreference mBackDoubleTapAction;
    private ListPreference mMenuLongPressAction;
    private ListPreference mMenuDoubleTapAction;
    private ListPreference mAssistLongPressAction;
    private ListPreference mAssistDoubleTapAction;
    private ListPreference mAppSwitchLongPressAction;
    private ListPreference mAppSwitchDoubleTapAction;
    private ListPreference mCameraLongPressAction;
    private ListPreference mCameraDoubleTapAction;

    private SwitchPreference mNavigationBar;
    private SwitchPreference mSwapNavigationkeys;
    private SwitchPreference mSwapSliderOrder;
    private SwitchPreference mButtonBrightness;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.buttons_settings);

        mHandler = new Handler();

        final Resources res = getActivity().getResources();
        final ContentResolver resolver = getActivity().getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();

        mDeviceHardwareKeys = res.getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys);

        mHasAlertSlider = res.getBoolean(com.android.internal.R.bool.config_hasAlertSlider)
                && !TextUtils.isEmpty(res.getString(com.android.internal.R.string.alert_slider_state_path))
                && !TextUtils.isEmpty(res.getString(com.android.internal.R.string.alert_slider_uevent_match_path));

        /* Navigation Bar */
        mNavigationBar = (SwitchPreference) findPreference(KEY_NAVIGATION_BAR);
        if (mNavigationBar != null) {
            if (mDeviceHardwareKeys != 0) {
                mNavigationBar.setOnPreferenceChangeListener(this);
            } else {
                mNavigationBar = null;
                removePreference(KEY_NAVIGATION_BAR);
            }
        }

        /* Swap Navigation Keys */
        mSwapNavigationkeys = (SwitchPreference) findPreference(KEY_SWAP_NAVIGATION_KEYS);
        if (mSwapNavigationkeys != null) {
            mSwapNavigationkeys.setOnPreferenceChangeListener(this);
        }

        /* Swap Slider order */
        mSwapSliderOrder = (SwitchPreference) findPreference(KEY_SWAP_SLIDER_ORDER);
        if (mSwapSliderOrder != null) {
            if (mHasAlertSlider) {
                mSwapSliderOrder.setOnPreferenceChangeListener(this);
            } else {
                mSwapSliderOrder = null;
                removePreference(KEY_SWAP_SLIDER_ORDER);
            }
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

        /* Home Key Long Press */
        int defaultLongPressOnHomeKeyBehavior = res.getInteger(
                com.android.internal.R.integer.config_longPressOnHomeKeyBehavior);
        int longPressOnHomeKeyBehavior = Settings.System.getIntForUser(resolver,
                    Settings.System.KEY_HOME_LONG_PRESS_ACTION,
                    defaultLongPressOnHomeKeyBehavior,
                    UserHandle.USER_CURRENT);
        mHomeLongPressAction = initActionList(KEY_HOME_LONG_PRESS, longPressOnHomeKeyBehavior);

        /* Home Key Double Tap */
        int defaultDoubleTapOnHomeKeyBehavior = res.getInteger(
                com.android.internal.R.integer.config_doubleTapOnHomeKeyBehavior);
        int doubleTapOnHomeKeyBehavior = Settings.System.getIntForUser(resolver,
                    Settings.System.KEY_HOME_DOUBLE_TAP_ACTION,
                    defaultDoubleTapOnHomeKeyBehavior,
                    UserHandle.USER_CURRENT);
        mHomeDoubleTapAction = initActionList(KEY_HOME_DOUBLE_TAP, doubleTapOnHomeKeyBehavior);

        /* Back Key Long Press */
        int defaultLongPressOnBackKeyBehavior = res.getInteger(
                com.android.internal.R.integer.config_longPressOnBackKeyBehavior);
        int longPressOnBackKeyBehavior = Settings.System.getIntForUser(resolver,
                Settings.System.KEY_BACK_LONG_PRESS_ACTION,
                defaultLongPressOnBackKeyBehavior,
                UserHandle.USER_CURRENT);
        mBackLongPressAction = initActionList(KEY_BACK_LONG_PRESS, longPressOnBackKeyBehavior);

        /* Back Key Double Tap */
        int defaultDoubleTapOnBackKeyBehavior = res.getInteger(
                com.android.internal.R.integer.config_doubleTapOnBackKeyBehavior);
        int doubleTapOnBackKeyBehavior = Settings.System.getIntForUser(resolver,
                Settings.System.KEY_BACK_DOUBLE_TAP_ACTION,
                defaultDoubleTapOnBackKeyBehavior,
                UserHandle.USER_CURRENT);
        mBackDoubleTapAction = initActionList(KEY_BACK_DOUBLE_TAP, doubleTapOnBackKeyBehavior);

        /* Menu Key Long Press */
        int defaultLongPressOnMenuKeyBehavior = res.getInteger(
                com.android.internal.R.integer.config_longPressOnMenuKeyBehavior);
        int longPressOnMenuKeyBehavior = Settings.System.getIntForUser(resolver,
                Settings.System.KEY_MENU_LONG_PRESS_ACTION,
                defaultLongPressOnMenuKeyBehavior,
                UserHandle.USER_CURRENT);
        mMenuLongPressAction = initActionList(KEY_MENU_LONG_PRESS, longPressOnMenuKeyBehavior);

        /* Menu Key Double Tap */
        int defaultDoubleTapOnMenuKeyBehavior = res.getInteger(
                com.android.internal.R.integer.config_doubleTapOnMenuKeyBehavior);
        int doubleTapOnMenuKeyBehavior = Settings.System.getIntForUser(resolver,
                Settings.System.KEY_MENU_DOUBLE_TAP_ACTION,
                defaultDoubleTapOnMenuKeyBehavior,
                UserHandle.USER_CURRENT);
        mMenuDoubleTapAction = initActionList(KEY_MENU_DOUBLE_TAP, doubleTapOnMenuKeyBehavior);

        /* Assist Key Long Press */
        int defaultLongPressOnAssistKeyBehavior = res.getInteger(
                com.android.internal.R.integer.config_longPressOnAssistKeyBehavior);
        int longPressOnAssistKeyBehavior = Settings.System.getIntForUser(resolver,
                Settings.System.KEY_ASSIST_LONG_PRESS_ACTION,
                defaultLongPressOnAssistKeyBehavior,
                UserHandle.USER_CURRENT);
        mAssistLongPressAction = initActionList(KEY_ASSIST_LONG_PRESS, longPressOnAssistKeyBehavior);

        /* Assist Key Double Tap */
        int defaultDoubleTapOnAssistKeyBehavior = res.getInteger(
                com.android.internal.R.integer.config_doubleTapOnAssistKeyBehavior);
        int doubleTapOnAssistKeyBehavior = Settings.System.getIntForUser(resolver,
                Settings.System.KEY_ASSIST_DOUBLE_TAP_ACTION,
                defaultDoubleTapOnAssistKeyBehavior,
                UserHandle.USER_CURRENT);
        mAssistDoubleTapAction = initActionList(KEY_ASSIST_DOUBLE_TAP, doubleTapOnAssistKeyBehavior);

        /* AppSwitch Key Long Press */
        int defaultLongPressOnAppSwitchKeyBehavior = res.getInteger(
                com.android.internal.R.integer.config_longPressOnAppSwitchKeyBehavior);
        int longPressOnAppSwitchKeyBehavior = Settings.System.getIntForUser(resolver,
                Settings.System.KEY_APP_SWITCH_LONG_PRESS_ACTION,
                defaultLongPressOnAppSwitchKeyBehavior,
                UserHandle.USER_CURRENT);
        mAppSwitchLongPressAction = initActionList(KEY_APP_SWITCH_LONG_PRESS, longPressOnAppSwitchKeyBehavior);

        /* AppSwitch Key Double Tap */
        int defaultDoubleTapOnAppSwitchKeyBehavior = res.getInteger(
                com.android.internal.R.integer.config_doubleTapOnAppSwitchKeyBehavior);
        int doubleTapOnAppSwitchKeyBehavior = Settings.System.getIntForUser(resolver,
                Settings.System.KEY_APP_SWITCH_DOUBLE_TAP_ACTION,
                defaultDoubleTapOnAppSwitchKeyBehavior,
                UserHandle.USER_CURRENT);
        mAppSwitchDoubleTapAction = initActionList(KEY_APP_SWITCH_DOUBLE_TAP, doubleTapOnAppSwitchKeyBehavior);

        /* Camera Key Long Press */
        int defaultLongPressOnCameraKeyBehavior = res.getInteger(
                com.android.internal.R.integer.config_longPressOnCameraKeyBehavior);
        int longPressOnCameraKeyBehavior = Settings.System.getIntForUser(resolver,
                Settings.System.KEY_CAMERA_LONG_PRESS_ACTION,
                defaultLongPressOnCameraKeyBehavior,
                UserHandle.USER_CURRENT);
        mCameraLongPressAction = initActionList(KEY_CAMERA_LONG_PRESS, longPressOnCameraKeyBehavior);

        /* Camera Key Double Tap */
        int defaultDoubleTapOnCameraKeyBehavior = res.getInteger(
                com.android.internal.R.integer.config_doubleTapOnCameraKeyBehavior);
        int doubleTapOnCameraKeyBehavior = Settings.System.getIntForUser(resolver,
                Settings.System.KEY_CAMERA_DOUBLE_TAP_ACTION,
                defaultDoubleTapOnCameraKeyBehavior,
                UserHandle.USER_CURRENT);
        mCameraDoubleTapAction = initActionList(KEY_CAMERA_DOUBLE_TAP, doubleTapOnCameraKeyBehavior);
    }

    @Override
    public void onResume() {
        super.onResume();
        reload();
    }

    @Override
    protected int getMetricsCategory() {
        return -1;
    }

    private ListPreference initActionList(String key, int value) {
        ListPreference list = (ListPreference) getPreferenceScreen().findPreference(key);
        if (list != null) {
            list.setValue(Integer.toString(value));
            list.setSummary(list.getEntry());
            list.setOnPreferenceChangeListener(this);
        }
        return list;
    }

    private boolean handleOnPreferenceTreeClick(Preference preference) {
        if (preference != null && preference == mNavigationBar) {
            mNavigationBar.setEnabled(false);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mNavigationBar.setEnabled(true);
                }
            }, 1000);
            return true;
        }
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
        } else if (preference == mNavigationBar) {
            return Settings.System.NAVIGATION_BAR_ENABLED;
        } else if (preference == mSwapNavigationkeys) {
            return Settings.System.SWAP_NAVIGATION_KEYS;
        } else if (preference == mSwapSliderOrder) {
            return Settings.System.ALERT_SLIDER_ORDER;
        } else if (preference == mButtonBrightness) {
            return Settings.System.BUTTON_BRIGHTNESS_ENABLED;
        } else if (preference == mHomeLongPressAction) {
            return Settings.System.KEY_HOME_LONG_PRESS_ACTION;
        } else if (preference == mHomeDoubleTapAction) {
            return Settings.System.KEY_HOME_DOUBLE_TAP_ACTION;
        } else if (preference == mBackLongPressAction) {
            return Settings.System.KEY_BACK_LONG_PRESS_ACTION;
        } else if (preference == mBackDoubleTapAction) {
            return Settings.System.KEY_BACK_DOUBLE_TAP_ACTION;
        } else if (preference == mMenuLongPressAction) {
            return Settings.System.KEY_MENU_LONG_PRESS_ACTION;
        } else if (preference == mMenuDoubleTapAction) {
            return Settings.System.KEY_MENU_DOUBLE_TAP_ACTION;
        } else if (preference == mAssistLongPressAction) {
            return Settings.System.KEY_ASSIST_LONG_PRESS_ACTION;
        } else if (preference == mAssistDoubleTapAction) {
            return Settings.System.KEY_ASSIST_DOUBLE_TAP_ACTION;
        } else if (preference == mAppSwitchLongPressAction) {
            return Settings.System.KEY_APP_SWITCH_LONG_PRESS_ACTION;
        } else if (preference == mAppSwitchDoubleTapAction) {
            return Settings.System.KEY_APP_SWITCH_DOUBLE_TAP_ACTION;
        } else if (preference == mCameraLongPressAction) {
            return Settings.System.KEY_CAMERA_LONG_PRESS_ACTION;
        } else if (preference == mCameraDoubleTapAction) {
            return Settings.System.KEY_CAMERA_DOUBLE_TAP_ACTION;
        }

        return EMPTY_STRING;
    }

    private void reload() {
        final ContentResolver resolver = getActivity().getContentResolver();
        final Resources res = getActivity().getResources();

        final boolean defaultToNavigationBar = res.getBoolean(com.android.internal.R.bool.config_defaultToNavigationBar);
        final boolean navigationBarEnabled = Settings.System.getIntForUser(resolver,
                Settings.System.NAVIGATION_BAR_ENABLED, defaultToNavigationBar ? 1 : 0, UserHandle.USER_CURRENT) != 0;

        final boolean hasHome = (mDeviceHardwareKeys & KEY_MASK_HOME) != 0 || navigationBarEnabled;
        final boolean hasMenu = (mDeviceHardwareKeys & KEY_MASK_MENU) != 0;
        final boolean hasBack = (mDeviceHardwareKeys & KEY_MASK_BACK) != 0 || navigationBarEnabled;
        final boolean hasAssist = (mDeviceHardwareKeys & KEY_MASK_ASSIST) != 0;
        final boolean hasAppSwitch = (mDeviceHardwareKeys & KEY_MASK_APP_SWITCH) != 0 || navigationBarEnabled;
        final boolean hasCamera = (mDeviceHardwareKeys & KEY_MASK_CAMERA) != 0;

        final boolean swapNavigationkeysEnabled = Settings.System.getIntForUser(resolver,
                Settings.System.SWAP_NAVIGATION_KEYS, 0, UserHandle.USER_CURRENT) != 0;

        final boolean swapSliderOrderEnabled = Settings.System.getIntForUser(resolver,
                Settings.System.ALERT_SLIDER_ORDER, 0, UserHandle.USER_CURRENT) != 0;

        final boolean buttonBrightnessEnabled = Settings.System.getIntForUser(resolver,
                Settings.System.BUTTON_BRIGHTNESS_ENABLED, 1, UserHandle.USER_CURRENT) != 0;

        if (mNavigationBar != null) {
            mNavigationBar.setChecked(navigationBarEnabled);
        }

        if (mSwapNavigationkeys != null) {
            mSwapNavigationkeys.setChecked(swapNavigationkeysEnabled);
            // Disable when navigation bar is disabled and no hw back and recents available.
            mSwapNavigationkeys.setEnabled(navigationBarEnabled
                    || hasBack && hasAppSwitch);
        }

        if (mSwapSliderOrder != null) {
            mSwapSliderOrder.setChecked(swapSliderOrderEnabled);
        }

        if (mButtonBrightness != null) {
            mButtonBrightness.setChecked(buttonBrightnessEnabled);
        }

        final PreferenceScreen prefScreen = getPreferenceScreen();

        final PreferenceCategory homeCategory =
                (PreferenceCategory) prefScreen.findPreference(KEY_CATEGORY_HOME);

        final PreferenceCategory backCategory =
                (PreferenceCategory) prefScreen.findPreference(KEY_CATEGORY_BACK);

        final PreferenceCategory menuCategory =
                (PreferenceCategory) prefScreen.findPreference(KEY_CATEGORY_MENU);

        final PreferenceCategory assistCategory =
                (PreferenceCategory) prefScreen.findPreference(KEY_CATEGORY_ASSIST);

        final PreferenceCategory appSwitchCategory =
                (PreferenceCategory) prefScreen.findPreference(KEY_CATEGORY_APP_SWITCH);

        final PreferenceCategory cameraCategory =
                (PreferenceCategory) prefScreen.findPreference(KEY_CATEGORY_CAMERA);

        if (mDeviceHardwareKeys != 0 && mButtonBrightness != null) {
            mButtonBrightness.setEnabled(!navigationBarEnabled);
        } else if (mDeviceHardwareKeys == 0 && mButtonBrightness != null) {
            prefScreen.removePreference(mButtonBrightness);
        }

        if (!hasHome && homeCategory != null) {
            prefScreen.removePreference(homeCategory);
        }

        if (!hasBack && backCategory != null) {
            prefScreen.removePreference(backCategory);
        }

        if (!hasMenu && menuCategory != null) {
            prefScreen.removePreference(menuCategory);
        }

        if (!hasAssist && assistCategory != null) {
            prefScreen.removePreference(assistCategory);
        }

        if (!hasAppSwitch && appSwitchCategory != null) {
            prefScreen.removePreference(appSwitchCategory);
        }

        if (!hasCamera && cameraCategory != null) {
            prefScreen.removePreference(cameraCategory);
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
        // return super.onPreferenceTreeClick(preferenceScreen, preference);
        return handled;
    }

    /**
     * For Search.
     */
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(
                    Context context, boolean enabled) {
                SearchIndexableResource sir = new SearchIndexableResource(context);
                sir.xmlResId = R.xml.buttons_settings;
                return Arrays.asList(sir);
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                final ArrayList<String> result = new ArrayList<String>();

                final UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
                final int myUserId = UserHandle.myUserId();
                final boolean isSecondaryUser = myUserId != UserHandle.USER_OWNER;

                // TODO: Implement search index provider.

                return result;
            }
        };
}
