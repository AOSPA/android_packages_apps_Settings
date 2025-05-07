/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.inputmethod;
import android.content.ContentResolver;
import android.content.Context;
import android.hardware.input.InputSettings;
import android.provider.Settings;
import android.widget.ImageView;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceScreen;

import com.android.server.accessibility.Flags;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.LayoutPreference;

import java.util.Objects;

/** Controller class that controls mouse keys acceleration seekbar settings. */
public class MouseKeysAccelerationController extends BasePreferenceController  {
    private static final float ACCELERATION_STEP = 0.1f;

    private final ContentResolver mContentResolver;
    @SuppressWarnings("NullAway")
    private SeekBar mSeekBar;

    public MouseKeysAccelerationController(@NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
        mContentResolver = context.getContentResolver();
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        final LayoutPreference preference = screen.findPreference(getPreferenceKey());
        if (preference == null) {
            return;
        }

        final float accelerationFromSettings = getAccelerationFromSettings();
        mSeekBar = Objects.requireNonNull(
                preference.findViewById(R.id.mouse_keys_acceleration_seekbar));

        // Scale the float acceleration value to the SeekBar's integer range.
        mSeekBar.setProgress(convertAccelerationToProgress(accelerationFromSettings));

        mSeekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(@NonNull SeekBar seekBar, int progress,
                            boolean fromUser) {
                        // Convert the SeekBar's integer progress back to a float value
                        float acceleration = convertProgressToAcceleration(progress);
                        updateAccelerationValue(acceleration);
                    }

                    @Override
                    public void onStartTrackingTouch(@NonNull SeekBar seekBar) {
                        // Nothing to do.
                    }

                    @Override
                    public void onStopTrackingTouch(@NonNull SeekBar seekBar) {
                        // Nothing to do.
                    }
                });

        ImageView slow = preference.findViewById(R.id.fast_icon);
        ImageView fast = preference.findViewById(R.id.slow_icon);
        if (slow == null || fast == null) {
            return;
        }
        slow.setOnClickListener(v -> increaseAccelerationByImageView());
        fast.setOnClickListener(v -> decreaseAccelerationByImageView());
    }

    private void updateAccelerationValue(float acceleration) {
        Settings.Secure.putFloat(mContentResolver,
                Settings.Secure.ACCESSIBILITY_MOUSE_KEYS_ACCELERATION, acceleration);
    }

    private void decreaseAccelerationByImageView() {
        float currentAcceleration = convertProgressToAcceleration(mSeekBar.getProgress());
        if (currentAcceleration > mSeekBar.getMin()) {
            float newAcceleration = Math.max(mSeekBar.getMin(),
                    currentAcceleration - ACCELERATION_STEP);
            mSeekBar.setProgress(convertAccelerationToProgress(newAcceleration));
            updateAccelerationValue(newAcceleration);
        }
    }

    private void increaseAccelerationByImageView() {
        float currentAcceleration = convertProgressToAcceleration(mSeekBar.getProgress());
        if (currentAcceleration < mSeekBar.getMax()) {
            float newAcceleration = Math.min(mSeekBar.getMax(),
                    currentAcceleration + ACCELERATION_STEP);
            mSeekBar.setProgress(convertAccelerationToProgress(newAcceleration));
            updateAccelerationValue(newAcceleration);
        }
    }

    private float getAccelerationFromSettings() {
        return Settings.Secure.getFloat(mContentResolver,
                Settings.Secure.ACCESSIBILITY_MOUSE_KEYS_ACCELERATION,
                InputSettings.DEFAULT_MOUSE_KEYS_ACCELERATION);
    }

    @Override
    public int getAvailabilityStatus() {
        return Flags.enableMouseKeyEnhancement() ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    /** Helper function to convert float acceleration to SeekBar integer progress */
    private int convertAccelerationToProgress(float acceleration) {
        return Math.round((acceleration - mSeekBar.getMin()) / ACCELERATION_STEP);
    }

    /** Helper function to convert SeekBar integer progress back to float acceleration */
    private float convertProgressToAcceleration(int progress) {
        return mSeekBar.getMin() + (progress * ACCELERATION_STEP);
    }
}
