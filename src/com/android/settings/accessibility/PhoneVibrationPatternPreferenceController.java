/*
 * Copyright (C) 2020-2022 Yet Another AOSP Project
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

package com.android.settings.accessibility;

import android.content.Context;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.core.AbstractPreferenceController;

/**
 * This class allows choosing a vibration pattern while ringing
 */
public class PhoneVibrationPatternPreferenceController extends AbstractPreferenceController
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_VIB_PATTERN = "ring_vibration_pattern";

    private ListPreference mVibPattern;

    protected final Vibrator mVibrator;

    protected static class VibrationEffectProxy {
        public VibrationEffect createWaveform(long[] timings, int[] amplitudes, int repeat) {
            return VibrationEffect.createWaveform(timings, amplitudes, repeat);
        }
    }

    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .build();

    private static final long[] SIMPLE_VIBRATION_PATTERN = {
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
        20,  // How long to vibrate
        1050, // How long to wait before vibrating again
    };

    public PhoneVibrationPatternPreferenceController(Context context) {
        super(context);
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    public boolean isAvailable() {
        return Utils.isVoiceCapable(mContext) && mVibrator.hasVibrator();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_VIB_PATTERN;
    }

    protected String getSettingsKey() {
        return Settings.System.RINGTONE_VIBRATION_PATTERN;
    }

    public int[] getPatternAmplitude(long[] pattern) {
        // making a full amp array according to what we have as param
        int[] ampArr = new int[pattern.length];
        for (int i = 0; i < pattern.length; i++) {
            if (i % 2 == 0) ampArr[i] = 0;
            else ampArr[i] = 255;
        }
        return ampArr;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mVibPattern = screen.findPreference(getPreferenceKey());
        int vibPattern = Settings.System.getInt(
                mContext.getContentResolver(), getSettingsKey(), 0);
        mVibPattern.setValueIndex(vibPattern);
        mVibPattern.setSummary(mVibPattern.getEntries()[vibPattern]);
        mVibPattern.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int vibPattern = Integer.valueOf((String) newValue);
        Settings.System.putInt(mContext.getContentResolver(),
                getSettingsKey(), vibPattern);
        mVibPattern.setSummary(mVibPattern.getEntries()[vibPattern]);
        previewPattern();
        return true;
    }

    private void previewPattern() {
        VibrationEffect effect = getPattern(Settings.System.getInt(
                mContext.getContentResolver(), getSettingsKey(), 0));
        mVibrator.vibrate(effect, VIBRATION_ATTRIBUTES);
    }

    protected VibrationEffect getPattern(int vibPattern) {
        VibrationEffectProxy vibrationEffectProxy = new VibrationEffectProxy();
        switch (vibPattern) {
            case 1:
                return vibrationEffectProxy.createWaveform(DZZZ_DA_VIBRATION_PATTERN,
                        getPatternAmplitude(DZZZ_DA_VIBRATION_PATTERN), -1);
            case 2:
                return vibrationEffectProxy.createWaveform(MM_MM_MM_VIBRATION_PATTERN,
                        getPatternAmplitude(MM_MM_MM_VIBRATION_PATTERN), -1);
            case 3:
                return vibrationEffectProxy.createWaveform(DA_DA_DZZZ_VIBRATION_PATTERN,
                        getPatternAmplitude(DA_DA_DZZZ_VIBRATION_PATTERN), -1);
            case 4:
                return vibrationEffectProxy.createWaveform(DA_DZZZ_DA_VIBRATION_PATTERN,
                        getPatternAmplitude(DA_DZZZ_DA_VIBRATION_PATTERN), -1);
            default:
                return vibrationEffectProxy.createWaveform(SIMPLE_VIBRATION_PATTERN,
                        getPatternAmplitude(SIMPLE_VIBRATION_PATTERN), -1);
        }
    }
}
