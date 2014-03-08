package com.android.settings.AOSPAL;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
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

import com.android.internal.util.paranoid.DeviceUtils;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class SystemSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String SYSTEM_SETTINGS = "system_settings";

    private static final String KEY_DUAL_PANEL = "force_dualpanel";
    private static final String KEY_REVERSE_DEFAULT_APP_PICKER = "reverse_default_app_picker";
    private static final String TELO_RADIO_SETTINGS = "telo_radio_settings";

    private CheckBoxPreference mDualPanel;
    private CheckBoxPreference mReverseDefaultAppPicker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.system_settings);

        final ContentResolver resolver = getActivity().getContentResolver();
        PreferenceScreen prefs = getPreferenceScreen();

        PreferenceScreen systemSettings = (PreferenceScreen) findPreference(SYSTEM_SETTINGS);

        if (!DeviceUtils.isPhone(getActivity())) {
            systemSettings.removePreference(findPreference(TELO_RADIO_SETTINGS));
        }

        mDualPanel = (CheckBoxPreference) findPreference(KEY_DUAL_PANEL);
        mDualPanel.setChecked(Settings.System.getBoolean(getContentResolver(), Settings.System.FORCE_DUAL_PANEL, false));

        mReverseDefaultAppPicker = (CheckBoxPreference) findPreference(KEY_REVERSE_DEFAULT_APP_PICKER);
        mReverseDefaultAppPicker.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.REVERSE_DEFAULT_APP_PICKER, 0) != 0);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mDualPanel) {
            Settings.System.putBoolean(getContentResolver(), Settings.System.FORCE_DUAL_PANEL, ((CheckBoxPreference) preference).isChecked());
        } else if (preference == mReverseDefaultAppPicker) {
            Settings.System.putInt(getContentResolver(), Settings.System.REVERSE_DEFAULT_APP_PICKER,
                    mReverseDefaultAppPicker.isChecked() ? 1 : 0);
        }else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        return true;
    }
}
