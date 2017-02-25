/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.display;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings.Secure;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.TwoStatePreference;
import android.widget.TimePicker;

import com.android.internal.app.NightDisplayController;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Settings screen for Night display.
 */
public class NightDisplaySettings extends SettingsPreferenceFragment
        implements NightDisplayController.Callback, Preference.OnPreferenceChangeListener {

    private static final String KEY_NIGHT_DISPLAY_AUTO_MODE = "night_display_auto_mode";
    private static final String KEY_NIGHT_DISPLAY_START_TIME = "night_display_start_time";
    private static final String KEY_NIGHT_DISPLAY_END_TIME = "night_display_end_time";
    private static final String KEY_NIGHT_DISPLAY_ACTIVATED = "night_display_activated";

    private static final String SETTING_WARNING_HIDDEN = "night_display_warning_hidden";
    private static final int WARNING_SHOW = 0;
    private static final int WARNING_HIDE = 1;

    private static final int DIALOG_START_TIME = 0;
    private static final int DIALOG_END_TIME = 1;

    private NightDisplayController mController;
    private DateFormat mTimeFormatter;

    private DropDownPreference mAutoModePreference;
    private Preference mStartTimePreference;
    private Preference mEndTimePreference;
    private TwoStatePreference mActivatedPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Context context = getContext();
        mController = new NightDisplayController(context);

        mTimeFormatter = android.text.format.DateFormat.getTimeFormat(context);
        mTimeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);

        // Load the preferences from xml.
        addPreferencesFromResource(R.xml.night_display_settings);

        mAutoModePreference = (DropDownPreference) findPreference(KEY_NIGHT_DISPLAY_AUTO_MODE);
        mStartTimePreference = findPreference(KEY_NIGHT_DISPLAY_START_TIME);
        mEndTimePreference = findPreference(KEY_NIGHT_DISPLAY_END_TIME);
        mActivatedPreference = (TwoStatePreference) findPreference(KEY_NIGHT_DISPLAY_ACTIVATED);

        mAutoModePreference.setEntries(new CharSequence[] {
                getString(R.string.night_display_auto_mode_never),
                getString(R.string.night_display_auto_mode_custom),
                getString(R.string.night_display_auto_mode_twilight)
        });
        mAutoModePreference.setEntryValues(new CharSequence[] {
                String.valueOf(NightDisplayController.AUTO_MODE_DISABLED),
                String.valueOf(NightDisplayController.AUTO_MODE_CUSTOM),
                String.valueOf(NightDisplayController.AUTO_MODE_TWILIGHT)
        });
        mAutoModePreference.setOnPreferenceChangeListener(this);
        mActivatedPreference.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();

        // Listen for changes only while visible.
        mController.setListener(this);

        // Update the current state since it have changed while not visible.
        onActivated(mController.isActivated());
        onAutoModeChanged(mController.getAutoMode());
        onCustomStartTimeChanged(mController.getCustomStartTime());
        onCustomEndTimeChanged(mController.getCustomEndTime());
    }

    @Override
    public void onStop() {
        super.onStop();

        // Stop listening for state changes.
        mController.setListener(null);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mStartTimePreference) {
            showDialog(DIALOG_START_TIME);
            return true;
        } else if (preference == mEndTimePreference) {
            showDialog(DIALOG_END_TIME);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public Dialog onCreateDialog(final int dialogId) {
        if (dialogId == DIALOG_START_TIME || dialogId == DIALOG_END_TIME) {
            final NightDisplayController.LocalTime initialTime;
            if (dialogId == DIALOG_START_TIME) {
                initialTime = mController.getCustomStartTime();
            } else {
                initialTime = mController.getCustomEndTime();
            }

            final Context context = getContext();
            final boolean use24HourFormat = android.text.format.DateFormat.is24HourFormat(context);
            return new TimePickerDialog(context, new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    final NightDisplayController.LocalTime time =
                            new NightDisplayController.LocalTime(hourOfDay, minute);
                    if (dialogId == DIALOG_START_TIME) {
                        mController.setCustomStartTime(time);
                    } else {
                        mController.setCustomEndTime(time);
                    }
                }
            }, initialTime.hourOfDay, initialTime.minute, use24HourFormat);
        }
        return super.onCreateDialog(dialogId);
    }

    @Override
    public void onActivated(boolean activated) {
        mActivatedPreference.setChecked(activated);
    }

    @Override
    public void onAutoModeChanged(int autoMode) {
        mAutoModePreference.setValue(String.valueOf(autoMode));

        final boolean showCustomSchedule = autoMode == NightDisplayController.AUTO_MODE_CUSTOM;
        mStartTimePreference.setVisible(showCustomSchedule);
        mEndTimePreference.setVisible(showCustomSchedule);
    }

    private String getFormattedTimeString(NightDisplayController.LocalTime localTime) {
        final Calendar c = Calendar.getInstance();
        c.setTimeZone(mTimeFormatter.getTimeZone());
        c.set(Calendar.HOUR_OF_DAY, localTime.hourOfDay);
        c.set(Calendar.MINUTE, localTime.minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return mTimeFormatter.format(c.getTime());
    }

    @Override
    public void onCustomStartTimeChanged(NightDisplayController.LocalTime startTime) {
        mStartTimePreference.setSummary(getFormattedTimeString(startTime));
    }

    @Override
    public void onCustomEndTimeChanged(NightDisplayController.LocalTime endTime) {
        mEndTimePreference.setSummary(getFormattedTimeString(endTime));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean result = false;
        boolean displayWarning = false;

        if (preference == mAutoModePreference) {
            int selectedAutoMode = Integer.parseInt((String) newValue);
            displayWarning = selectedAutoMode != NightDisplayController.AUTO_MODE_DISABLED;
            result = mController.setAutoMode(selectedAutoMode);
        } else if (preference == mActivatedPreference) {
            boolean activated = (Boolean) newValue;
            displayWarning = activated;
            result = mController.setActivated(activated);
        }

        if (displayWarning) {
            displayWarning();
        }

        return result;
    }

    private void displayWarning() {
        final Context context = getContext();
        final ContentResolver contentResolver = getContentResolver();
        final int warningHidden = Secure.getIntForUser(contentResolver,
              SETTING_WARNING_HIDDEN, WARNING_SHOW, UserHandle.myUserId());

        if (warningHidden == WARNING_HIDE) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(getString(R.string.night_display_warning))
            .setCancelable(true)
            .setPositiveButton(getString(R.string.night_display_ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }})
            .setNegativeButton(getString(R.string.night_display_hide),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Secure.putIntForUser(contentResolver,
                              SETTING_WARNING_HIDDEN,
                              WARNING_HIDE,
                              UserHandle.myUserId());
                    }});
        AlertDialog alert = builder.create();
        alert.setCanceledOnTouchOutside(true);
        alert.show();
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.NIGHT_DISPLAY_SETTINGS;
    }
}
