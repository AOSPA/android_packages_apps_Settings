/*
 * Copyright (C) 2021 Paranoid Android
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

package com.android.settings.notification;

import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.RadioButtonPreference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.provider.Settings.System.HAPTIC_FEEDBACK_INTENSITY;
import static android.provider.Settings.System.NOTIFICATION_VIBRATION_INTENSITY;
import static android.provider.Settings.System.RINGTONE_VIBRATION_PATTERN;
import static android.provider.Settings.System.RING_VIBRATION_INTENSITY;

public class VibrationSettingsPreferenceFragment extends DashboardFragment
        implements Preference.OnPreferenceClickListener {

    private static final String TAG = "VibrationSettingsPreferenceFragment";

    private static final String[] mKeys = {"pattern_dzzz_dzzz", "pattern_dzzz_da", "pattern_mm_mm_mm",
            "pattern_da_da_dzzz", "pattern_da_dzzz_da"};

    private static final long[] DZZZ_DZZZ_VIBRATION_PATTERN = {
            0, // No delay before starting
            1000, // How long to vibrate
            1000, // How long to wait before vibrating again
            1000, // How long to vibrate
            1000, // How long to wait before vibrating again
    };

    private static final long[] DZZZ_DA_VIBRATION_PATTERN = {
            0, // No delay before starting
            500, // How long to vibrate
            200, // Delay
            70, // How long to vibrate
            720, // How long to wait before vibrating again
    };

    private static final long[] MM_MM_MM_VIBRATION_PATTERN = {
            0, // No delay before starting
            300, // How long to vibrate
            400, // Delay
            300, // How long to vibrate
            400, // Delay
            300, // How long to vibrate
            1400, // How long to wait before vibrating again
    };

    private static final long[] DA_DA_DZZZ_VIBRATION_PATTERN = {
            0, // No delay before starting
            70, // How long to vibrate
            80, // Delay
            70, // How long to vibrate
            180, // Delay
            600,  // How long to vibrate
            1050, // How long to wait before vibrating again
    };

    private static final long[] DA_DZZZ_DA_VIBRATION_PATTERN = {
            0, // No delay before starting
            80, // How long to vibrate
            200, // Delay
            600, // How long to vibrate
            150, // Delay
            60,  // How long to vibrate
            1050, // How long to wait before vibrating again
    };

    private static final int[] SEVEN_ELEMENTS_VIBRATION_AMPLITUDE = {
            0, // No delay before starting
            255, // Vibrate full amplitude
            0, // No amplitude while waiting
            255,
            0,
            255,
            0,
    };

    private static final int[] FIVE_ELEMENTS_VIBRATION_AMPLITUDE = {
            0, // No delay before starting
            255, // Vibrate full amplitude
            0, // No amplitude while waiting
            255,
            0,
    };
    private final AudioAttributes mAudioAttributesRingtone = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .build();
    private final AudioAttributes mAudioAttributesNotification = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build();
    private final Map<String, RadioButtonPreference> mStringToPreferenceMap = new HashMap<String, RadioButtonPreference>();
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final RadioButtonPreference[] mRadioPreferences = new RadioButtonPreference[5];
    private Context mContext;
    private ContentResolver mContentResolver;
    private Vibrator mVibrator;
    private AudioManager mAudioManager;
    private Preference mRingerVibrationIntensity;
    private Preference mRingPatternPref;
    private Preference mNotificationVibrationIntensity;
    private Preference mHapticIntensity;
    private SettingsObserver mSettingObserver;
    private boolean mSupportsMultipleIntensities;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getContext();
        mContentResolver = mContext.getContentResolver();
        mSettingObserver = new SettingsObserver(mHandler);
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        mSupportsMultipleIntensities = mContext.getResources().getBoolean(
                R.bool.config_vibration_supports_multiple_intensities);

        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        if (mVibrator != null && !mVibrator.hasVibrator()) {
            mVibrator = null;
        }

        mRingerVibrationIntensity = (Preference) findPreference(RING_VIBRATION_INTENSITY);
        mNotificationVibrationIntensity = (Preference) findPreference(NOTIFICATION_VIBRATION_INTENSITY);
        mHapticIntensity = (Preference) findPreference(HAPTIC_FEEDBACK_INTENSITY);
        mRingPatternPref = (Preference) findPreference(RINGTONE_VIBRATION_PATTERN);

        for (int i = 0; i < 5; i++) {
            mRadioPreferences[i] = (RadioButtonPreference) findPreference(mKeys[i]);
            mStringToPreferenceMap.put(mKeys[i], mRadioPreferences[i]);
            mRadioPreferences[i].setOnPreferenceClickListener(this);
        }

        if (mSupportsMultipleIntensities) {
            mRingerVibrationIntensity.setOnPreferenceClickListener(this);
            mNotificationVibrationIntensity.setOnPreferenceClickListener(this);
            mHapticIntensity.setOnPreferenceClickListener(this);
            updateIntensityText();
        } else {
            mRingerVibrationIntensity.setEnabled(false);
            mRingerVibrationIntensity.setVisible(false);
            mNotificationVibrationIntensity.setEnabled(false);
            mNotificationVibrationIntensity.setVisible(false);
            mHapticIntensity.setEnabled(false);
            mHapticIntensity.setVisible(false);
        }

        final int currentPattern = Settings.System.getIntForUser(mContentResolver,
                RINGTONE_VIBRATION_PATTERN,
                0,
                UserHandle.USER_CURRENT);

        updateVibrationPattern(currentPattern);
        updateRingerPrefs();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mSupportsMultipleIntensities) {
            mContentResolver.unregisterContentObserver(mSettingObserver);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateRingerPrefs();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mSupportsMultipleIntensities) {
            mContentResolver.registerContentObserver(Settings.System.getUriFor(RING_VIBRATION_INTENSITY),
                    false,
                    mSettingObserver);

            mContentResolver.registerContentObserver(Settings.System.getUriFor(NOTIFICATION_VIBRATION_INTENSITY),
                    false,
                    mSettingObserver);

            mContentResolver.registerContentObserver(Settings.System.getUriFor(HAPTIC_FEEDBACK_INTENSITY),
                    false,
                    mSettingObserver);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        final String key = preference.getKey();
        if (preference instanceof RadioButtonPreference) {
            int val = Arrays.asList(mKeys).indexOf(key);
            updateVibrationPattern(val);
            performVibrationDemo(val);
        } else {
            final VibrationIntensityDialog dialog = new VibrationIntensityDialog();
            dialog.setParameters(mContext, key, preference);
            dialog.show(getFragmentManager(), TAG);
        }
        return true;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_VIBRATION;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.vibration_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, this, getSettingsLifecycle());
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            VibrationSettingsPreferenceFragment fragment, Lifecycle lifecycle) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();

        final VibrateOnTouchPreferenceController vibrateOnTouchPreferenceController =
                new VibrateOnTouchPreferenceController(context, fragment, lifecycle);

        controllers.add(vibrateOnTouchPreferenceController);

        return controllers;
    }

    private void updateVibrationPattern(int val) {
        for (int i = 0; i < 5; i++) {
            ((RadioButtonPreference) mStringToPreferenceMap.get(mKeys[i])).setChecked(val == i);
        }
        Settings.System.putIntForUser(mContentResolver,
                RINGTONE_VIBRATION_PATTERN,
                val,
                UserHandle.USER_CURRENT);
    }

    private void updateIntensityText() {
        setText(mRingerVibrationIntensity, Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.RING_VIBRATION_INTENSITY,
                2));
        setText(mNotificationVibrationIntensity, Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_VIBRATION_INTENSITY,
                2));
        setText(mHapticIntensity, Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.HAPTIC_FEEDBACK_INTENSITY,
                2));
    }

    private void setText(Preference pref, int intensity) {
        switch (intensity) {
            case 0:
                pref.setSummary(R.string.vibration_intensity_disabled);
                break;
            case 1:
                pref.setSummary(R.string.vibration_intensity_light);
                break;
            case 2:
                pref.setSummary(R.string.vibration_intensity_medium);
                break;
            case 3:
                pref.setSummary(R.string.vibration_intensity_strong);
                break;
            case 4:
                pref.setSummary(R.string.vibration_intensity_custom);
                break;
        }
    }

    private void performVibrationDemo(int val) {
        VibrationEffect mDefaultVibrationEffect;
        switch (val) {
            case 1:
                mDefaultVibrationEffect = VibrationEffect.createWaveform(DZZZ_DA_VIBRATION_PATTERN,
                        FIVE_ELEMENTS_VIBRATION_AMPLITUDE,
                        -1);
                break;
            case 2:
                mDefaultVibrationEffect = VibrationEffect.createWaveform(MM_MM_MM_VIBRATION_PATTERN,
                        SEVEN_ELEMENTS_VIBRATION_AMPLITUDE,
                        -1);
                break;
            case 3:
                mDefaultVibrationEffect = VibrationEffect.createWaveform(DA_DA_DZZZ_VIBRATION_PATTERN,
                        SEVEN_ELEMENTS_VIBRATION_AMPLITUDE,
                        -1);
                break;
            case 4:
                mDefaultVibrationEffect = VibrationEffect.createWaveform(DA_DZZZ_DA_VIBRATION_PATTERN,
                        SEVEN_ELEMENTS_VIBRATION_AMPLITUDE,
                        -1);
                break;
            default:
                mDefaultVibrationEffect = VibrationEffect.createWaveform(DZZZ_DZZZ_VIBRATION_PATTERN,
                        FIVE_ELEMENTS_VIBRATION_AMPLITUDE,
                        -1);
                break;
        }
        if (mVibrator != null && mVibrator.hasVibrator()) {
            mVibrator.vibrate(mDefaultVibrationEffect, mAudioAttributesRingtone);
        }
    }

    private void updateRingerPrefs() {
        boolean ringVibEnable = Settings.System.getInt(mContentResolver,
                Settings.System.VIBRATE_WHEN_RINGING,
                0) == 1;
        ringVibEnable |= Settings.Global.getInt(mContentResolver,
                Settings.Global.APPLY_RAMPING_RINGER,
                0) == 1;
        boolean ringerModeSilent = mAudioManager.getRingerModeInternal() == AudioManager.RINGER_MODE_SILENT;
        mRingerVibrationIntensity.setEnabled(ringVibEnable && !ringerModeSilent);
        mRingPatternPref.setEnabled(ringVibEnable && !ringerModeSilent);
        mNotificationVibrationIntensity.setEnabled(!ringerModeSilent);
    }

    private final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            mHandler.postDelayed(() -> {
                if (uri.equals(Settings.System.getUriFor(RING_VIBRATION_INTENSITY))) {
                    performVibrationDemo(Settings.System.getIntForUser(mContentResolver,
                            RINGTONE_VIBRATION_PATTERN,
                            0,
                            UserHandle.USER_CURRENT));
                } else if (uri.equals(Settings.System.getUriFor(NOTIFICATION_VIBRATION_INTENSITY))) {
                    if (mVibrator != null) {
                        mVibrator.vibrate(250, mAudioAttributesNotification);
                    }
                } else if (uri.equals(Settings.System.getUriFor(HAPTIC_FEEDBACK_INTENSITY))) {
                    if (mVibrator != null) {
                        mVibrator.vibrate(VibrationEffect.get(VibrationEffect.EFFECT_CLICK));
                    }
                }
            }, 15);
        }
    }
}
