package com.android.settings.AOSPAL;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.PreferenceCategory;
import android.preference.Preference.OnPreferenceChangeListener;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class AdditionalSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String QUICK_PULLDOWN = "quick_pulldown";


    ListPreference mQuickPulldown

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.additional_settings);
        PreferenceScreen prefs = getPreferenceScreen();

        mQuickPulldown = (ListPreference) findPreference(QUICK_PULLDOWN);
        if (!Utils.isPhone(getActivity())) {
            prefs.removePreference(mQuickPulldown);
        } else {
            mQuickPulldown.setOnPreferenceChangeListener(this);
            int statusQuickPulldown = Settings.System.getInt(getContentResolver(),
                    Settings.System.QS_QUICK_PULLDOWN, 0);
            mQuickPulldown.setValue(String.valueOf(statusQuickPulldown));
            updatePulldownSummary();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mQuickPulldown) {
            int statusQuickPulldown = Integer.valueOf((String) newValue);
            Settings.System.putInt(getContentResolver(), Settings.System.QS_QUICK_PULLDOWN,
                    statusQuickPulldown);
            updatePulldownSummary();
            return true;
        }
        return false;
    }

    private void updatePulldownSummary() {
        int summaryId;
        int directionId;
        summaryId = R.string.summary_quick_pulldown;
        String value = Settings.System.getString(getContentResolver(),
                Settings.System.QS_QUICK_PULLDOWN);
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

