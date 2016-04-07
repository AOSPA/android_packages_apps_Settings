/*
 * Copyright (C) 2021 Paranoid Android
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

import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;

import androidx.preference.ListPreference;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.MainSwitchBarController;
import com.android.settings.widget.SettingsMainSwitchBar;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@SearchIndexable
public class OffscreenGestureSettings extends SettingsPreferenceFragment implements
        OffscreenGestureEnabler.OnGestureSwitchChangeListener {
    private static final String KEY_DOUBLE_TAP = "double_tap";
    private static final String KEY_SINGLE_TAP = "single_tap";
    private static final String KEY_DRAW_V = "draw_v";
    private static final String KEY_DRAW_INVERSE_V = "draw_inverse_v";
    private static final String KEY_DRAW_O = "draw_o";
    private static final String KEY_DRAW_M = "draw_m";
    private static final String KEY_DRAW_W = "draw_w";
    private static final String KEY_DRAW_S = "draw_s";
    private static final String KEY_DRAW_ARROW_LEFT = "draw_arrow_left";
    private static final String KEY_DRAW_ARROW_RIGHT = "draw_arrow_right";
    private static final String KEY_ONE_FINGER_SWIPE_UP = "one_finger_swipe_up";
    private static final String KEY_ONE_FINGER_SWIPE_RIGHT = "one_finger_swipe_right";
    private static final String KEY_ONE_FINGER_SWIPE_DOWN = "one_finger_swipe_down";
    private static final String KEY_ONE_FINGER_SWIPE_LEFT = "one_finger_swipe_left";
    private static final String KEY_TWO_FINGER_SWIPE = "two_finger_swipe";

    private static final HashMap<String, Integer> sGesturesKeyCodes = new HashMap<>();
    private static final HashMap<String, Integer> sGesturesDefaults = new HashMap<>();
    private static final HashMap<String, String> sGesturesSettings = new HashMap<>();

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

    private OffscreenGestureEnabler mGestureEnabler;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);

        final boolean enabled = Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.GESTURES_ENABLED, 1, UserHandle.USER_CURRENT) != 0;
        for (String gestureKey : sGesturesKeyCodes.keySet()) {
            if (getResources().getInteger(sGesturesKeyCodes.get(gestureKey)) > 0) {
                ListPreference pref = findPreference(gestureKey);
                int gestureDefault = getResources().getInteger(
                        sGesturesDefaults.get(gestureKey));
                int gestureBehaviour = Settings.System.getInt(getContentResolver(),
                        sGesturesSettings.get(gestureKey), gestureDefault);

                pref.setValue(String.valueOf(gestureBehaviour));
                pref.setEnabled(enabled);
                pref.setOnPreferenceChangeListener(
                        (preference, o) -> Settings.System.putIntForUser(getContentResolver(),
                                sGesturesSettings.get(preference.getKey()),
                                Integer.parseInt((String) o), UserHandle.USER_CURRENT)
                );
            } else {
                removePreference(gestureKey);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mGestureEnabler.teardownSwitchController();
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.offscreen_gesture_settings;
    }

    @Override
    public int getMetricsCategory() {
        return -1;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final SettingsActivity activity = (SettingsActivity) getActivity();
        final SettingsMainSwitchBar switchBar = activity.getSwitchBar();
        mGestureEnabler = new OffscreenGestureEnabler(getContext(),
                new MainSwitchBarController(switchBar), this, getSettingsLifecycle());
    }

    @Override
    public void onChanged(boolean enabled) {
        for (String gestureKey : sGesturesKeyCodes.keySet()) {
            if (getResources().getInteger(sGesturesKeyCodes.get(gestureKey)) > 0) {
                ListPreference gesturePref = findPreference(gestureKey);
                gesturePref.setEnabled(enabled);
            }
        }
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(
                Context context, boolean enabled) {
            final SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = R.xml.offscreen_gesture_settings;
            return Collections.singletonList(sir);
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            final List<String> keys = new ArrayList<>();
            for (String gestureKey : sGesturesKeyCodes.keySet()) {
                if (context.getResources().getInteger(sGesturesKeyCodes.get(gestureKey)) == 0) {
                    keys.add(gestureKey);
                }
            }
            return keys;
        }
    };
}
