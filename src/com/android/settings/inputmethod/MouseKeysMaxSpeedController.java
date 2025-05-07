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

/** Controller class that controls mouse keys max speed seekbar settings. */
public class MouseKeysMaxSpeedController extends BasePreferenceController {

    private static final int SEEK_BAR_STEP = 1;

    private final @NonNull ContentResolver mContentResolver;

    public MouseKeysMaxSpeedController(@NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
        mContentResolver = context.getContentResolver();
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        final LayoutPreference preference = screen.findPreference(getPreferenceKey());

        final int maxSpeedFromSettings = getMaxSpeedFromSettings();
        // Initialize seek bar preference. Sets seek bar size to the number of possible delay
        // values.
        @NonNull SeekBar seekBar = preference.findViewById(R.id.max_speed_seekbar);
        seekBar.setProgress(maxSpeedFromSettings);
        seekBar.setOnSeekBarChangeListener(
            new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onProgressChanged(@NonNull SeekBar seekBar, int progress,
                        boolean fromUser) {
                    updateMaxSpeedValue(seekBar, progress);
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

        @NonNull ImageView mShorter = preference.findViewById(R.id.shorter);
        mShorter.setOnClickListener(v -> minusDelayByImageView(seekBar));

        @NonNull ImageView mLonger = preference.findViewById(R.id.longer);
        mLonger.setOnClickListener(v -> plusDelayByImageView(seekBar));
    }

    private void updateMaxSpeedValue(@NonNull SeekBar seekBar, int position) {
        Settings.Secure.putInt(mContentResolver,
                Settings.Secure.ACCESSIBILITY_MOUSE_KEYS_MAX_SPEED, position);
        seekBar.setProgress(position);
    }

    private void minusDelayByImageView(@NonNull SeekBar seekBar) {
        final int maxSpeed = getMaxSpeedFromSettings();
        if (maxSpeed > seekBar.getMin()) {
            updateMaxSpeedValue(seekBar, maxSpeed - SEEK_BAR_STEP);
        }
    }

    private void plusDelayByImageView(@NonNull SeekBar seekBar) {
        final int maxSpeed = getMaxSpeedFromSettings();
        if (maxSpeed < seekBar.getMax()) {
            updateMaxSpeedValue(seekBar, maxSpeed + SEEK_BAR_STEP);
        }
    }

    private int getMaxSpeedFromSettings() {
        return Settings.Secure.getInt(mContentResolver,
                Settings.Secure.ACCESSIBILITY_MOUSE_KEYS_MAX_SPEED,
                InputSettings.DEFAULT_MOUSE_KEYS_MAX_SPEED);
    }

    @Override
    public int getAvailabilityStatus() {
        return Flags.enableMouseKeyEnhancement() ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }
}
