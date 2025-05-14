/*
 * Copyright 2025 The Android Open Source Project
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

import static com.android.settings.flags.Flags.threeFingerTapAppLaunch;

import android.content.Context;
import android.hardware.input.InputSettings;

import androidx.annotation.NonNull;

import com.android.settings.core.BasePreferenceController;

/**
 * Controller that handles three finger tap app launching Preference's visibility based on the flag
 */
public class TouchpadThreeFingerTapAppLaunchPreferenceController extends BasePreferenceController {

    public TouchpadThreeFingerTapAppLaunchPreferenceController(@NonNull Context context,
            @NonNull String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        boolean isTouchpad = InputPeripheralsSettingsUtils.isTouchpad();
        return (InputSettings.isTouchpadThreeFingerTapShortcutFeatureFlagEnabled()
                && threeFingerTapAppLaunch() && isTouchpad)
                ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }
}
