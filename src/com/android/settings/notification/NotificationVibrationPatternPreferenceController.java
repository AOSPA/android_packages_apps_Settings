/*
 * Copyright (C) 2022 Yet Another AOSP Project
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

import android.content.Context;
import android.content.res.Resources;
import android.os.VibrationEffect;
import android.provider.Settings;

/**
 * This class allows choosing a vibration pattern for notifications
 */
public class NotificationVibrationPatternPreferenceController extends VibrationPatternPreferenceController {

    private static final String KEY_VIB_PATTERN = "notification_vibration_pattern";
    private static final long[] DEFAULT_VIBRATE_PATTERN = {0, 250, 250, 250};
    private static final int VIBRATE_PATTERN_MAXLEN = 8 * 2 + 1; // up to eight bumps

    private final long[] mDefaultPattern;
    private final int[] mDefaultPatternAmp;

    private static final long[] DZZZ_VIBRATION_PATTERN = {
        0, // No delay before starting
        500, // How long to vibrate
        720, // How long to wait before vibrating again
    };

    private static final long[] MM_MM_VIBRATION_PATTERN = {
        0, // No delay before starting
        300, // How long to vibrate
        400, // Delay
        300, // How long to vibrate
        1400, // How long to wait before vibrating again
    };

    private static final long[] DA_DA_VIBRATION_PATTERN = {
        0, // No delay before starting
        70, // How long to vibrate
        80, // Delay
        70, // How long to vibrate
        1050, // How long to wait before vibrating again
    };

    private static final int[] THREE_ELEMENTS_VIBRATION_AMPLITUDE = {
        0, // No delay before starting
        255, // Vibrate full amplitude
        0, // No amplitude while waiting
    };

    private static final int[] FIVE_ELEMENTS_VIBRATION_AMPLITUDE = {
        0, // No delay before starting
        255, // Vibrate full amplitude
        0, // No amplitude while waiting
        255, // No amplitude while waiting
        0, // No amplitude while waiting
    };

    public NotificationVibrationPatternPreferenceController(Context context) {
        super(context);
        mDefaultPattern = getLongArray(context.getResources(),
                com.android.internal.R.array.config_defaultNotificationVibePattern,
                VIBRATE_PATTERN_MAXLEN,
                DEFAULT_VIBRATE_PATTERN);

        // making a full amp array according to what we have in config
        int[] ampArr = new int[mDefaultPattern.length];
        for (int i = 0; i < mDefaultPattern.length; i++) {
            if (i % 2 == 0) ampArr[i] = 0;
            else ampArr[i] = 255;
        }
        mDefaultPatternAmp = ampArr;
    }

    @Override
    public boolean isAvailable() {
        return mVibrator.hasVibrator();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_VIB_PATTERN;
    }

    @Override
    protected String getSettingsKey() {
        return Settings.System.NOTIFICATION_VIBRATION_PATTERN;
    }

    @Override
    protected VibrationEffect getPattern(int vibPattern) {
        VibrationEffectProxy vibrationEffectProxy = new VibrationEffectProxy();
        switch (vibPattern) {
            case 1:
                return vibrationEffectProxy.createWaveform(DZZZ_VIBRATION_PATTERN,
                        THREE_ELEMENTS_VIBRATION_AMPLITUDE, -1);
            case 2:
                return vibrationEffectProxy.createWaveform(MM_MM_VIBRATION_PATTERN,
                        FIVE_ELEMENTS_VIBRATION_AMPLITUDE, -1);
            case 3:
                return vibrationEffectProxy.createWaveform(DA_DA_VIBRATION_PATTERN,
                        FIVE_ELEMENTS_VIBRATION_AMPLITUDE, -1);
            default:
                return vibrationEffectProxy.createWaveform(mDefaultPattern,
                        mDefaultPatternAmp, -1);
        }
    }

    private static long[] getLongArray(Resources resources, int resId, int maxLength, long[] def) {
        int[] ar = resources.getIntArray(resId);
        if (ar == null) return def;
        final int len = ar.length > maxLength ? maxLength : ar.length;
        long[] out = new long[len];
        for (int i = 0; i < len; i++) out[i] = ar[i];
        return out;
    }
}
