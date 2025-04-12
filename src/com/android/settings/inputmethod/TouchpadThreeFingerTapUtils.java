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

import static android.hardware.input.InputGestureData.TOUCHPAD_GESTURE_TYPE_THREE_FINGER_TAP;
import static android.hardware.input.InputGestureData.createTouchpadTrigger;

import android.content.ContentResolver;
import android.hardware.input.InputGestureData;
import android.hardware.input.KeyGestureEvent;
import android.net.Uri;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.annotation.NonNull;

import java.util.Map;

/**
 * Utility class for retrieving 3 Finger Tap related values in touchpad settings.
 */
public final class TouchpadThreeFingerTapUtils {
    static final String TARGET_ACTION =
            Settings.System.TOUCHPAD_THREE_FINGER_TAP_CUSTOMIZATION;
    static final Uri TARGET_ACTION_URI =
            Settings.System.getUriFor(TARGET_ACTION);

    static final InputGestureData.Trigger TRIGGER =
            createTouchpadTrigger(TOUCHPAD_GESTURE_TYPE_THREE_FINGER_TAP);

    private static final Map<String, Integer> PREF_KEY_TO_GESTURE_TYPE = Map.ofEntries(
            Map.entry("launch_gemini", KeyGestureEvent.KEY_GESTURE_TYPE_LAUNCH_ASSISTANT),
            Map.entry("go_home", KeyGestureEvent.KEY_GESTURE_TYPE_HOME),
            Map.entry("go_back", KeyGestureEvent.KEY_GESTURE_TYPE_BACK),
            Map.entry("recent_apps", KeyGestureEvent.KEY_GESTURE_TYPE_RECENT_APPS),
            Map.entry("middle_click", KeyGestureEvent.KEY_GESTURE_TYPE_UNSPECIFIED));

    /**
     * @param resolver ContentResolver
     * @return the current KeyGestureEvent set for three finger tap
     */
    public static int getCurrentGestureType(@NonNull ContentResolver resolver) {
        return Settings.System.getIntForUser(
                resolver,
                TARGET_ACTION,
                KeyGestureEvent.KEY_GESTURE_TYPE_UNSPECIFIED,
                UserHandle.USER_CURRENT);
    }

    /**
     * @param prefKey Preference key for input_touchpad_three_finger_tap_action
     * @return the corresponding KeyGestureEvent of the given key, or KEY_GESTURE_TYPE_UNSPECIFIED
     * if the key does not exist
     */
    public static int getGestureTypeByPrefKey(@NonNull String prefKey) {
        return PREF_KEY_TO_GESTURE_TYPE.getOrDefault(
                prefKey, KeyGestureEvent.KEY_GESTURE_TYPE_UNSPECIFIED);
    }

    /**
     * @param resolver ContentResolver
     * @param gestureType the three finger tap KeyGestureEvent
     */
    public static void setGestureType(@NonNull ContentResolver resolver, int gestureType) {
        Settings.System.putIntForUser(
                resolver,
                TouchpadThreeFingerTapUtils.TARGET_ACTION,
                gestureType,
                UserHandle.USER_CURRENT);
    }
}
