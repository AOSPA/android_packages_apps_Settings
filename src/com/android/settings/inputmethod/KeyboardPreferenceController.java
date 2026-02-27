/*
 * Copyright (C) 2022 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.inputmethod;

import android.content.Context;
import android.hardware.input.InputManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.inputmethod.PhysicalKeyboardFragment.HardKeyboardDeviceInfo;
import com.android.settings.utils.DesktopSettingsUtils;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.List;

public class KeyboardPreferenceController extends BasePreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnStart, OnStop,
        InputManager.InputDeviceListener {

    private final InputManager mIm;

    private Preference mPreference;

    public KeyboardPreferenceController(Context context, String key) {
        super(context, key);
        mIm = context.getSystemService(InputManager.class);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        updateState(mPreference);
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        updateState(mPreference);
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        updateState(mPreference);
    }

    @Override
    public void onStart() {
        mIm.registerInputDeviceListener(this, null);
    }

    @Override
    public void onStop() {
        mIm.unregisterInputDeviceListener(this);
    }

    @Override
    public int getAvailabilityStatus() {
        // TODO(b/483182050): Temporary mitigation by hiding keyboard settings.
        if (Utils.shouldDisableKeyboardSettingsInDemoMode(mContext)) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        if (DesktopSettingsUtils.shouldShowTopLevelDeviceCategory(mContext)) {
            return  mContext.getString(R.string.device_keyboard_settings_summary);
        }

        final List<HardKeyboardDeviceInfo> keyboards =
                PhysicalKeyboardFragment.getHardKeyboards(mContext);

        return mContext.getString(keyboards.isEmpty() ? R.string.keyboard_settings_summary :
                 R.string.keyboard_settings_with_physical_keyboard_summary);
    }
}