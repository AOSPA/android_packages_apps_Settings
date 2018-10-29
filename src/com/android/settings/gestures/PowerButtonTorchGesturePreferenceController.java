/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.gestures;

import static android.provider.Settings.Secure.TORCH_POWER_BUTTON_GESTURE;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import androidx.preference.SwitchPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;

public class PowerButtonTorchGesturePreferenceController extends BasePreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private final String SECURE_KEY = TORCH_POWER_BUTTON_GESTURE;

    public PowerButtonTorchGesturePreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH) ? AVAILABLE :
                        UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference != null) {
            if (preference instanceof SwitchPreference) {
                SwitchPreference pref = (SwitchPreference) preference;
                boolean on = Settings.Secure.getInt(
                        mContext.getContentResolver(), SECURE_KEY, 0)
                        == 1;
                pref.setChecked(on);
            }
        }
    }

    @Override
    public CharSequence getSummary() {
        boolean on = Settings.Secure.getInt(
                mContext.getContentResolver(), SECURE_KEY, 0)
                == 1;
        int summary = on ? R.string.torch_power_button_gesture_lp :
                R.string.torch_power_button_gesture_none;
        return mContext.getString(summary);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Settings.Secure.putInt(mContext.getContentResolver(),
                SECURE_KEY, ((boolean) newValue) ? 1 : 0);
        preference.setSummary(getSummary());
        return true;
    }
}
