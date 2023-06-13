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

package com.android.settings.accessibility;

import android.content.Context;
import android.content.res.Resources;
import android.os.VibrationEffect;
import android.provider.Settings;

/**
 * This class allows choosing a vibration pattern for notifications
 */
public class NotificationVibrationPatternPreferenceController extends PhoneVibrationPatternPreferenceController {

    private static final String KEY_VIB_PATTERN = "notification_vibration_pattern";
    private static final long[] DEFAULT_VIBRATE_PATTERN = {0, 250, 250, 250};
    private static final int VIBRATE_PATTERN_MAXLEN = 8 * 2 + 1; // up to eight bumps

    private final long[] mDefaultPattern;

    private static final long[] DZZZ_VIBRATION_PATTERN = {
        0, // No delay before starting
        255, // How long to vibrate
    };

    private static final long[] DA_MM_VIBRATION_PATTERN = {
        0, // No delay before starting
        70, // How long to vibrate
        70, // Delay
        300, // How long to vibrate
    };

    private static final long[] DA_DA_VIBRATION_PATTERN = {
        0, // No delay before starting
        70, // How long to vibrate
        80, // Delay
        70, // How long to vibrate
    };

    public NotificationVibrationPatternPreferenceController(Context context) {
        super(context);
        mDefaultPattern = getLongArray(context.getResources(),
                com.android.internal.R.array.config_defaultNotificationVibePattern,
                VIBRATE_PATTERN_MAXLEN,
                DEFAULT_VIBRATE_PATTERN);
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
                        getPatternAmplitude(DZZZ_VIBRATION_PATTERN), -1);
            case 2:
                return vibrationEffectProxy.createWaveform(DA_MM_VIBRATION_PATTERN,
                        getPatternAmplitude(DA_MM_VIBRATION_PATTERN), -1);
            case 3:
                return vibrationEffectProxy.createWaveform(DA_DA_VIBRATION_PATTERN,
                        getPatternAmplitude(DA_DA_VIBRATION_PATTERN), -1);
            default:
                return vibrationEffectProxy.createWaveform(mDefaultPattern,
                        getPatternAmplitude(mDefaultPattern), -1);
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
