package com.android.settings.AOSPAL;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.PreferenceCategory;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SeekBarPreference;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class AdditionalSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String QUICK_PULLDOWN = "quick_pulldown";
    private static final String CATEGORY_HEADSETHOOK = "button_headsethook";
    private static final String BUTTON_HEADSETHOOK_LAUNCH_VOICE = "button_headsethook_launch_voice";
    private static final String KEY_LISTVIEW_ANIMATION = "listview_animation";
    private static final String KEY_LISTVIEW_INTERPOLATOR = "listview_interpolator";
    private static final String KEY_DUAL_PANEL = "force_dualpanel"; 
    private static final String KEY_REVERSE_DEFAULT_APP_PICKER = "reverse_default_app_picker";
    private static final String LOCKSCREEN_POWER_MENU = "lockscreen_power_menu";
    private static final String KEY_NAVIGATION_BAR_HEIGHT = "navigation_bar_height";

    ListPreference mQuickPulldown;
    private CheckBoxPreference mHeadsetHookLaunchVoice;
    private CheckBoxPreference mLockScreenPowerMenu;
    private CheckBoxPreference mDualPanel;
    private CheckBoxPreference mReverseDefaultAppPicker;
    private ListPreference mListViewAnimation;
    private ListPreference mListViewInterpolator;
    private SeekBarPreference mNavigationBarHeight;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.additional_settings);
        PreferenceScreen prefs = getPreferenceScreen();
        final ContentResolver resolver = getActivity().getContentResolver();

        mQuickPulldown = (ListPreference) findPreference(QUICK_PULLDOWN);
        if (Utils.isTablet(getActivity())) {
            prefs.removePreference(mQuickPulldown);
        } else {
            mQuickPulldown.setOnPreferenceChangeListener(this);
            int statusQuickPulldown = Settings.System.getIntForUser(getContentResolver(),
                    Settings.System.QS_QUICK_PULLDOWN, 1, UserHandle.USER_CURRENT);
            mQuickPulldown.setValue(String.valueOf(statusQuickPulldown));
            updatePulldownSummary();
        }

        final PreferenceCategory headsethookCategory =
                (PreferenceCategory) prefs.findPreference(CATEGORY_HEADSETHOOK);

        mHeadsetHookLaunchVoice = (CheckBoxPreference) findPreference(BUTTON_HEADSETHOOK_LAUNCH_VOICE);
        mHeadsetHookLaunchVoice.setChecked(Settings.System.getInt(resolver,
                Settings.System.HEADSETHOOK_LAUNCH_VOICE, 1) == 1);

        //ListView Animations
        mListViewAnimation = (ListPreference) prefs.findPreference(KEY_LISTVIEW_ANIMATION);
        if (mListViewAnimation != null) {
           int listViewAnimation = Settings.System.getInt(getContentResolver(),
                    Settings.System.LISTVIEW_ANIMATION, 1);
           mListViewAnimation.setValue(String.valueOf(listViewAnimation));
           mListViewAnimation.setSummary(mListViewAnimation.getEntry());
        }
        mListViewAnimation.setOnPreferenceChangeListener(this);

        mListViewInterpolator = (ListPreference) prefs.findPreference(KEY_LISTVIEW_INTERPOLATOR);
        if (mListViewInterpolator != null) {
           int listViewInterpolator = Settings.System.getInt(getContentResolver(),
                    Settings.System.LISTVIEW_INTERPOLATOR, 1);
           mListViewInterpolator.setValue(String.valueOf(listViewInterpolator));
           mListViewInterpolator.setSummary(mListViewInterpolator.getEntry());
        }
        mListViewInterpolator.setOnPreferenceChangeListener(this);

        mDualPanel = (CheckBoxPreference) findPreference(KEY_DUAL_PANEL);
        mDualPanel.setChecked(Settings.System.getBoolean(getContentResolver(), Settings.System.FORCE_DUAL_PANEL, false));

        mReverseDefaultAppPicker = (CheckBoxPreference) findPreference(KEY_REVERSE_DEFAULT_APP_PICKER);
        mReverseDefaultAppPicker.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.REVERSE_DEFAULT_APP_PICKER, 0) != 0);

        mLockScreenPowerMenu = (CheckBoxPreference) prefs.findPreference(LOCKSCREEN_POWER_MENU);
        if (mLockScreenPowerMenu != null) {
            mLockScreenPowerMenu.setChecked(Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.LOCK_SCREEN_POWER_MENU, 1) == 1);
        }
        mNavigationBarHeight = (SeekBarPreference) findPreference(KEY_NAVIGATION_BAR_HEIGHT);
        mNavigationBarHeight.setProgress((int)(Settings.System.getFloat(getContentResolver(),
                    Settings.System.NAVIGATION_BAR_HEIGHT, 1f) * 100));
        mNavigationBarHeight.setTitle(getResources().getText(R.string.navigation_bar_height) + " " + mNavigationBarHeight.getProgress() + "%");
        mNavigationBarHeight.setOnPreferenceChangeListener(this);
    }

    private boolean isToggled(Preference pref) {
        return ((CheckBoxPreference) pref).isChecked();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String key = preference.getKey();

        if (preference == mQuickPulldown) {
            int statusQuickPulldown = Integer.valueOf((String) newValue);
            Settings.System.putIntForUser(getContentResolver(), Settings.System.QS_QUICK_PULLDOWN,
                    statusQuickPulldown, UserHandle.USER_CURRENT);
            updatePulldownSummary();
        } else if (KEY_LISTVIEW_ANIMATION.equals(key)) {
            int value = Integer.parseInt((String) newValue);
            int index = mListViewAnimation.findIndexOfValue((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LISTVIEW_ANIMATION,
                    value);
            mListViewAnimation.setValue(String.valueOf(value));
            mListViewAnimation.setSummary(mListViewAnimation.getEntry());
        } else if (KEY_LISTVIEW_INTERPOLATOR.equals(key)) {
            int value = Integer.parseInt((String) newValue);
            int index = mListViewInterpolator.findIndexOfValue((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LISTVIEW_INTERPOLATOR,
                    value);
            mListViewInterpolator.setValue(String.valueOf(value));
            mListViewInterpolator.setSummary(mListViewInterpolator.getEntry());
        } else if (preference == mNavigationBarHeight) {
            Settings.System.putFloat(getActivity().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_HEIGHT, (Integer)newValue / 100f);
            mNavigationBarHeight.setTitle(getResources().getText(R.string.navigation_bar_height) + " " + (Integer)newValue + "%");
        } else {
            return false;
        }
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mHeadsetHookLaunchVoice) {
            boolean checked = ((CheckBoxPreference)preference).isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.HEADSETHOOK_LAUNCH_VOICE, checked ? 1:0);
        } else if (preference == mDualPanel) {
            Settings.System.putBoolean(getContentResolver(), Settings.System.FORCE_DUAL_PANEL, ((CheckBoxPreference) preference).isChecked());
        } else if (preference == mReverseDefaultAppPicker) {
            Settings.System.putInt(getContentResolver(), Settings.System.REVERSE_DEFAULT_APP_PICKER,
                    mReverseDefaultAppPicker.isChecked() ? 1 : 0);
        } else if (preference == mLockScreenPowerMenu) {
            Settings.Secure.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.Secure.LOCK_SCREEN_POWER_MENU, isToggled(preference) ? 1 : 0);
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        return true;
    }

    private void updatePulldownSummary() {
        int summaryId;
        int directionId;
        summaryId = R.string.summary_quick_pulldown;
        String value = String.valueOf(Settings.System.getIntForUser(getContentResolver(),
                Settings.System.QS_QUICK_PULLDOWN, 1, UserHandle.USER_CURRENT));
        String[] pulldownArray = getResources().getStringArray(R.array.quick_pulldown_values);
        if (pulldownArray[0].equals(value)) {
            directionId = R.string.quick_pulldown_off;
            mQuickPulldown.setValueIndex(0);
            mQuickPulldown.setSummary(getResources().getString(directionId));
        } else if (pulldownArray[1].equals(value)) {
            directionId = R.string.quick_pulldown_right;
            mQuickPulldown.setValueIndex(1);
            mQuickPulldown.setSummary(getResources().getString(directionId)
                    + " " + getResources().getString(summaryId));
        } else {
            directionId = R.string.quick_pulldown_left;
            mQuickPulldown.setValueIndex(2);
            mQuickPulldown.setSummary(getResources().getString(directionId)
                    + " " + getResources().getString(summaryId));
        }
    }
}

