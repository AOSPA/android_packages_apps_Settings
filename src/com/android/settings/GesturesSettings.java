/*
 * Copyright (C) 2016 The ParanoidAndroid Project
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

package com.android.settings;

import android.content.Context;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.widget.Switch;

import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.widget.SwitchBar;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GesturesSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {

    private static final String TAG = "GesturesSettings";

    private static final String KEY_PROXIMITY_WAKE = "proximity_on_wake";
    private static final String KEY_DOUBLE_TAP = "double_tap";
    private static final String KEY_DRAW_V = "draw_v";
    private static final String KEY_DRAW_INVERSE_V = "draw_inverse_v";
    private static final String KEY_DRAW_O = "draw_o";
    private static final String KEY_DRAW_M = "draw_m";
    private static final String KEY_DRAW_W = "draw_w";
    private static final String KEY_DRAW_ARROW_LEFT = "draw_arrow_left";
    private static final String KEY_DRAW_ARROW_RIGHT = "draw_arrow_right";
    private static final String KEY_ONE_FINGER_SWIPE_UP = "one_finger_swipe_up";
    private static final String KEY_ONE_FINGER_SWIPE_RIGHT = "one_finger_swipe_right";
    private static final String KEY_ONE_FINGER_SWIPE_DOWN = "one_finger_swipe_down";
    private static final String KEY_ONE_FINGER_SWIPE_LEFT = "one_finger_swipe_left";
    private static final String KEY_TWO_FINGER_SWIPE = "two_finger_swipe";

    private static final HashMap<String, Integer> mGesturesKeyCodes = new HashMap<>();
    private static final HashMap<String, Integer> mGesturesDefaults = new HashMap();
    private static final HashMap<String, String> mGesturesSettings = new HashMap();

    static {
        mGesturesKeyCodes.put(KEY_DOUBLE_TAP, com.android.internal.R.integer.config_doubleTapKeyCode);
        mGesturesKeyCodes.put(KEY_DRAW_V, com.android.internal.R.integer.config_drawVKeyCode);
        mGesturesKeyCodes.put(KEY_DRAW_INVERSE_V, com.android.internal.R.integer.config_drawInverseVKeyCode);
        mGesturesKeyCodes.put(KEY_DRAW_O, com.android.internal.R.integer.config_drawOKeyCode);
        mGesturesKeyCodes.put(KEY_DRAW_M, com.android.internal.R.integer.config_drawMKeyCode);
        mGesturesKeyCodes.put(KEY_DRAW_W, com.android.internal.R.integer.config_drawWKeyCode);
        mGesturesKeyCodes.put(KEY_DRAW_ARROW_LEFT, com.android.internal.R.integer.config_drawArrowLeftKeyCode);
        mGesturesKeyCodes.put(KEY_DRAW_ARROW_RIGHT, com.android.internal.R.integer.config_drawArrowRightKeyCode);
        mGesturesKeyCodes.put(KEY_ONE_FINGER_SWIPE_UP, com.android.internal.R.integer.config_oneFingerSwipeUpKeyCode);
        mGesturesKeyCodes.put(KEY_ONE_FINGER_SWIPE_RIGHT, com.android.internal.R.integer.config_oneFingerSwipeRightKeyCode);
        mGesturesKeyCodes.put(KEY_ONE_FINGER_SWIPE_DOWN, com.android.internal.R.integer.config_oneFingerSwipeDownKeyCode);
        mGesturesKeyCodes.put(KEY_ONE_FINGER_SWIPE_LEFT, com.android.internal.R.integer.config_oneFingerSwipeLeftKeyCode);
        mGesturesKeyCodes.put(KEY_TWO_FINGER_SWIPE, com.android.internal.R.integer.config_twoFingerSwipeKeyCode);

        mGesturesDefaults.put(KEY_DOUBLE_TAP, com.android.internal.R.integer.config_doubleTapDefault);
        mGesturesDefaults.put(KEY_DRAW_V, com.android.internal.R.integer.config_drawVDefault);
        mGesturesDefaults.put(KEY_DRAW_INVERSE_V, com.android.internal.R.integer.config_drawInverseVDefault);
        mGesturesDefaults.put(KEY_DRAW_O, com.android.internal.R.integer.config_drawODefault);
        mGesturesDefaults.put(KEY_DRAW_M, com.android.internal.R.integer.config_drawMDefault);
        mGesturesDefaults.put(KEY_DRAW_W, com.android.internal.R.integer.config_drawWDefault);
        mGesturesDefaults.put(KEY_DRAW_ARROW_LEFT, com.android.internal.R.integer.config_drawArrowLeftDefault);
        mGesturesDefaults.put(KEY_DRAW_ARROW_RIGHT, com.android.internal.R.integer.config_drawArrowRightDefault);
        mGesturesDefaults.put(KEY_ONE_FINGER_SWIPE_UP, com.android.internal.R.integer.config_oneFingerSwipeUpDefault);
        mGesturesDefaults.put(KEY_ONE_FINGER_SWIPE_RIGHT, com.android.internal.R.integer.config_oneFingerSwipeRightDefault);
        mGesturesDefaults.put(KEY_ONE_FINGER_SWIPE_DOWN, com.android.internal.R.integer.config_oneFingerSwipeDownDefault);
        mGesturesDefaults.put(KEY_ONE_FINGER_SWIPE_LEFT, com.android.internal.R.integer.config_oneFingerSwipeLeftDefault);
        mGesturesDefaults.put(KEY_TWO_FINGER_SWIPE, com.android.internal.R.integer.config_twoFingerSwipeDefault);

        mGesturesSettings.put(KEY_DOUBLE_TAP, Settings.System.GESTURE_DOUBLE_TAP);
        mGesturesSettings.put(KEY_DRAW_V, Settings.System.GESTURE_DRAW_V);
        mGesturesSettings.put(KEY_DRAW_INVERSE_V, Settings.System.GESTURE_DRAW_INVERSE_V);
        mGesturesSettings.put(KEY_DRAW_O, Settings.System.GESTURE_DRAW_O);
        mGesturesSettings.put(KEY_DRAW_M, Settings.System.GESTURE_DRAW_M);
        mGesturesSettings.put(KEY_DRAW_W, Settings.System.GESTURE_DRAW_W);
        mGesturesSettings.put(KEY_DRAW_ARROW_LEFT, Settings.System.GESTURE_DRAW_ARROW_LEFT);
        mGesturesSettings.put(KEY_DRAW_ARROW_RIGHT, Settings.System.GESTURE_DRAW_ARROW_RIGHT);
        mGesturesSettings.put(KEY_ONE_FINGER_SWIPE_UP, Settings.System.GESTURE_ONE_FINGER_SWIPE_UP);
        mGesturesSettings.put(KEY_ONE_FINGER_SWIPE_RIGHT, Settings.System.GESTURE_ONE_FINGER_SWIPE_RIGHT);
        mGesturesSettings.put(KEY_ONE_FINGER_SWIPE_DOWN, Settings.System.GESTURE_ONE_FINGER_SWIPE_DOWN);
        mGesturesSettings.put(KEY_ONE_FINGER_SWIPE_LEFT, Settings.System.GESTURE_ONE_FINGER_SWIPE_LEFT);
        mGesturesSettings.put(KEY_TWO_FINGER_SWIPE, Settings.System.GESTURE_TWO_FINGER_SWIPE);
    }

    private GesturesEnabler mGesturesEnabler;

    private SwitchPreference mProximityWake;

    @Override
    protected int getMetricsCategory() {
        return -1;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.gestures_settings);

        for (String gestureKey : mGesturesKeyCodes.keySet()) {
            if (getResources().getInteger(mGesturesKeyCodes.get(gestureKey)) > 0) {
                findPreference(gestureKey).setOnPreferenceChangeListener(this);
            } else {
                removePreference(gestureKey);
            }
        }

        boolean proximityCheckOnWait = getResources().getBoolean(
                com.android.internal.R.bool.config_proximityCheckOnWake);
        if (proximityCheckOnWait) {
            boolean defaultValue = getResources().getBoolean(
                    com.android.internal.R.bool.config_proximityCheckOnWakeEnabledByDefault);
            int value = Settings.System.getInt(getContentResolver(), KEY_PROXIMITY_WAKE,
                    defaultValue ?  1 : 0);
            mProximityWake = (SwitchPreference) findPreference(KEY_PROXIMITY_WAKE);
            mProximityWake.setChecked(value != 0);
            mProximityWake.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_PROXIMITY_WAKE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mGesturesEnabler != null) {
            mGesturesEnabler.teardownSwitchBar();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        SettingsActivity activity = (SettingsActivity) getActivity();
        mGesturesEnabler = new GesturesEnabler(activity.getSwitchBar());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGesturesEnabler != null) {
            mGesturesEnabler.resume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mGesturesEnabler != null) {
            mGesturesEnabler.pause();
        }
    }

    private void enableGestures(boolean enable, boolean start) {
        for (String gestureKey : mGesturesKeyCodes.keySet()) {
            if (getResources().getInteger(mGesturesKeyCodes.get(gestureKey)) == 0) {
                continue;
            }
            ListPreference gesturePref = (ListPreference) findPreference(gestureKey);
            gesturePref.setEnabled(enable);
            if (mProximityWake != null) {
                mProximityWake.setEnabled(enable);
            }
            if (start) {
                int gestureDefault = getResources().getInteger(
                        mGesturesDefaults.get(gestureKey));
                int gestureBehaviour = Settings.System.getInt(getContentResolver(),
                        mGesturesSettings.get(gestureKey), gestureDefault);
                gesturePref.setValue(String.valueOf(gestureBehaviour));
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        String key = preference.getKey();
        if (KEY_PROXIMITY_WAKE.equals(key)) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(getContentResolver(), KEY_PROXIMITY_WAKE, value ? 1 : 0);
        } else {
            Settings.System.putInt(getContentResolver(),
                    mGesturesSettings.get(preference.getKey()),
                    Integer.parseInt((String) objValue));
        }
        return true;
    }

    public static boolean supportsGestures(Context context) {
        for (String gestureKey : mGesturesKeyCodes.keySet()) {
            if (context.getResources().getInteger(mGesturesKeyCodes
                    .get(gestureKey)) > 0) {
                return true;
            }
        }
        return false;
    }

    private class GesturesEnabler implements SwitchBar.OnSwitchChangeListener {

        private final Context mContext;
        private final SwitchBar mSwitchBar;
        private boolean mListeningToOnSwitchChange;

        public GesturesEnabler(SwitchBar switchBar) {
            mContext = switchBar.getContext();
            mSwitchBar = switchBar;

            mSwitchBar.show();

            boolean gesturesEnabled = Settings.System.getInt(
                    mContext.getContentResolver(),
                    Settings.System.GESTURES_ENABLED, 0) != 0;
            mSwitchBar.setChecked(gesturesEnabled);
            GesturesSettings.this.enableGestures(gesturesEnabled, true);
        }

        public void teardownSwitchBar() {
            pause();
            mSwitchBar.hide();
        }

        public void resume() {
            if (!mListeningToOnSwitchChange) {
                mSwitchBar.addOnSwitchChangeListener(this);
                mListeningToOnSwitchChange = true;
            }
        }

        public void pause() {
            if (mListeningToOnSwitchChange) {
                mSwitchBar.removeOnSwitchChangeListener(this);
                mListeningToOnSwitchChange = false;
            }
        }

        @Override
        public void onSwitchChanged(Switch switchView, boolean isChecked) {
            Settings.System.putInt(
                    mContext.getContentResolver(),
                    Settings.System.GESTURES_ENABLED, isChecked ? 1 : 0);
            GesturesSettings.this.enableGestures(isChecked, false);
        }

    }

    /**
     * For Search.
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {

            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(
                    Context context, boolean enabled) {
                final SearchIndexableResource sir =
                        new SearchIndexableResource(context);
                sir.xmlResId = R.xml.gestures_settings;
                return Arrays.asList(sir);
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                final List<String> keys = new ArrayList<String>();
                for (String gestureKey : mGesturesKeyCodes.keySet()) {
                    if (context.getResources().getInteger(mGesturesKeyCodes
                            .get(gestureKey)) == 0) {
                        keys.add(gestureKey);
                    }
                }
                return keys;
            }

        };
}
