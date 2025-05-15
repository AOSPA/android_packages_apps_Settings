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

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.server.accessibility.Flags;
import com.android.settings.core.SliderPreferenceController;
import com.android.settingslib.widget.SliderPreference;

/** Controller class that controls mouse keys max speed seekbar settings. */
public class MouseKeysMaxSpeedController extends SliderPreferenceController implements
        Preference.OnPreferenceChangeListener {

    private static final int SEEK_BAR_STEP = 1;
    private static final int MAX_SLIDER_POSITION = 10;
    private static final int MIN_SLIDER_POSITION = 1;

    private final @NonNull ContentResolver mContentResolver;

    public MouseKeysMaxSpeedController(@NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
        mContentResolver = context.getContentResolver();
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        final SliderPreference preference = screen.findPreference(getPreferenceKey());
        if (preference == null) {
            return;
        }
        preference.setTickVisible(true);
        updateState(preference);
    }

    @Override
    public boolean setSliderPosition(int position) {
        if (position < getMin() || position > getMax()) {
            return false;
        }

        updateMaxSpeedValue(position);
        return true;
    }

    @Override
    public int getSliderPosition() {
        return getMaxSpeedFromSettings();
    }

    private void updateMaxSpeedValue(int position) {
        Settings.Secure.putInt(mContentResolver,
                Settings.Secure.ACCESSIBILITY_MOUSE_KEYS_MAX_SPEED, position);
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

    @Override
    public int getMin() {
        return MIN_SLIDER_POSITION;
    }

    @Override
    public int getMax() {
        return MAX_SLIDER_POSITION;
    }
}
