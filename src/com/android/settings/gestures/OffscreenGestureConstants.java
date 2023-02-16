/*
 * Copyright (C) 2069 Paranoid Android
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

import android.provider.Settings;

import java.util.HashMap;

public class OffscreenGestureConstants {

    public static final String KEY_DOUBLE_TAP = "double_tap";
    public static final String KEY_SINGLE_TAP = "single_tap";
    public static final String KEY_DRAW_V = "draw_v";
    public static final String KEY_DRAW_INVERSE_V = "draw_inverse_v";
    public static final String KEY_DRAW_O = "draw_o";
    public static final String KEY_DRAW_M = "draw_m";
    public static final String KEY_DRAW_W = "draw_w";
    public static final String KEY_DRAW_S = "draw_s";
    public static final String KEY_DRAW_ARROW_LEFT = "draw_arrow_left";
    public static final String KEY_DRAW_ARROW_RIGHT = "draw_arrow_right";
    public static final String KEY_ONE_FINGER_SWIPE_UP = "one_finger_swipe_up";
    public static final String KEY_ONE_FINGER_SWIPE_RIGHT = "one_finger_swipe_right";
    public static final String KEY_ONE_FINGER_SWIPE_DOWN = "one_finger_swipe_down";
    public static final String KEY_ONE_FINGER_SWIPE_LEFT = "one_finger_swipe_left";
    public static final String KEY_TWO_FINGER_SWIPE = "two_finger_swipe";

    public static final HashMap<String, Integer> sGesturesKeyCodes = new HashMap<>();
    public static final HashMap<String, Integer> sGesturesDefaults = new HashMap<>();
    public static final HashMap<String, String> sGesturesSettings = new HashMap<>();


    static {
        sGesturesKeyCodes.put(KEY_DOUBLE_TAP, com.android.internal.R.integer.config_doubleTapKeyCode);
        sGesturesKeyCodes.put(KEY_SINGLE_TAP, com.android.internal.R.integer.config_singleTapKeyCode);
        sGesturesKeyCodes.put(KEY_DRAW_V, com.android.internal.R.integer.config_drawVKeyCode);
        sGesturesKeyCodes.put(KEY_DRAW_INVERSE_V, com.android.internal.R.integer.config_drawInverseVKeyCode);
        sGesturesKeyCodes.put(KEY_DRAW_O, com.android.internal.R.integer.config_drawOKeyCode);
        sGesturesKeyCodes.put(KEY_DRAW_M, com.android.internal.R.integer.config_drawMKeyCode);
        sGesturesKeyCodes.put(KEY_DRAW_W, com.android.internal.R.integer.config_drawWKeyCode);
        sGesturesKeyCodes.put(KEY_DRAW_S, com.android.internal.R.integer.config_drawSKeyCode);
        sGesturesKeyCodes.put(KEY_DRAW_ARROW_LEFT, com.android.internal.R.integer.config_drawArrowLeftKeyCode);
        sGesturesKeyCodes.put(KEY_DRAW_ARROW_RIGHT, com.android.internal.R.integer.config_drawArrowRightKeyCode);
        sGesturesKeyCodes.put(KEY_ONE_FINGER_SWIPE_UP, com.android.internal.R.integer.config_oneFingerSwipeUpKeyCode);
        sGesturesKeyCodes.put(KEY_ONE_FINGER_SWIPE_RIGHT, com.android.internal.R.integer.config_oneFingerSwipeRightKeyCode);
        sGesturesKeyCodes.put(KEY_ONE_FINGER_SWIPE_DOWN, com.android.internal.R.integer.config_oneFingerSwipeDownKeyCode);
        sGesturesKeyCodes.put(KEY_ONE_FINGER_SWIPE_LEFT, com.android.internal.R.integer.config_oneFingerSwipeLeftKeyCode);
        sGesturesKeyCodes.put(KEY_TWO_FINGER_SWIPE, com.android.internal.R.integer.config_twoFingerSwipeKeyCode);
    }

    static {
        sGesturesDefaults.put(KEY_DOUBLE_TAP, com.android.internal.R.integer.config_doubleTapDefault);
        sGesturesDefaults.put(KEY_SINGLE_TAP, com.android.internal.R.integer.config_singleTapDefault);
        sGesturesDefaults.put(KEY_DRAW_V, com.android.internal.R.integer.config_drawVDefault);
        sGesturesDefaults.put(KEY_DRAW_INVERSE_V, com.android.internal.R.integer.config_drawInverseVDefault);
        sGesturesDefaults.put(KEY_DRAW_O, com.android.internal.R.integer.config_drawODefault);
        sGesturesDefaults.put(KEY_DRAW_M, com.android.internal.R.integer.config_drawMDefault);
        sGesturesDefaults.put(KEY_DRAW_W, com.android.internal.R.integer.config_drawWDefault);
        sGesturesDefaults.put(KEY_DRAW_S, com.android.internal.R.integer.config_drawSDefault);
        sGesturesDefaults.put(KEY_DRAW_ARROW_LEFT, com.android.internal.R.integer.config_drawArrowLeftDefault);
        sGesturesDefaults.put(KEY_DRAW_ARROW_RIGHT, com.android.internal.R.integer.config_drawArrowRightDefault);
        sGesturesDefaults.put(KEY_ONE_FINGER_SWIPE_UP, com.android.internal.R.integer.config_oneFingerSwipeUpDefault);
        sGesturesDefaults.put(KEY_ONE_FINGER_SWIPE_RIGHT, com.android.internal.R.integer.config_oneFingerSwipeRightDefault);
        sGesturesDefaults.put(KEY_ONE_FINGER_SWIPE_DOWN, com.android.internal.R.integer.config_oneFingerSwipeDownDefault);
        sGesturesDefaults.put(KEY_ONE_FINGER_SWIPE_LEFT, com.android.internal.R.integer.config_oneFingerSwipeLeftDefault);
        sGesturesDefaults.put(KEY_TWO_FINGER_SWIPE, com.android.internal.R.integer.config_twoFingerSwipeDefault);
    }

    static {
        sGesturesSettings.put(KEY_DOUBLE_TAP, Settings.System.GESTURE_DOUBLE_TAP);
        sGesturesSettings.put(KEY_SINGLE_TAP, Settings.System.GESTURE_SINGLE_TAP);
        sGesturesSettings.put(KEY_DRAW_V, Settings.System.GESTURE_DRAW_V);
        sGesturesSettings.put(KEY_DRAW_INVERSE_V, Settings.System.GESTURE_DRAW_INVERSE_V);
        sGesturesSettings.put(KEY_DRAW_O, Settings.System.GESTURE_DRAW_O);
        sGesturesSettings.put(KEY_DRAW_M, Settings.System.GESTURE_DRAW_M);
        sGesturesSettings.put(KEY_DRAW_W, Settings.System.GESTURE_DRAW_W);
        sGesturesSettings.put(KEY_DRAW_S, Settings.System.GESTURE_DRAW_S);
        sGesturesSettings.put(KEY_DRAW_ARROW_LEFT, Settings.System.GESTURE_DRAW_ARROW_LEFT);
        sGesturesSettings.put(KEY_DRAW_ARROW_RIGHT, Settings.System.GESTURE_DRAW_ARROW_RIGHT);
        sGesturesSettings.put(KEY_ONE_FINGER_SWIPE_UP, Settings.System.GESTURE_ONE_FINGER_SWIPE_UP);
        sGesturesSettings.put(KEY_ONE_FINGER_SWIPE_RIGHT, Settings.System.GESTURE_ONE_FINGER_SWIPE_RIGHT);
        sGesturesSettings.put(KEY_ONE_FINGER_SWIPE_DOWN, Settings.System.GESTURE_ONE_FINGER_SWIPE_DOWN);
        sGesturesSettings.put(KEY_ONE_FINGER_SWIPE_LEFT, Settings.System.GESTURE_ONE_FINGER_SWIPE_LEFT);
        sGesturesSettings.put(KEY_TWO_FINGER_SWIPE, Settings.System.GESTURE_TWO_FINGER_SWIPE);
    }
}
