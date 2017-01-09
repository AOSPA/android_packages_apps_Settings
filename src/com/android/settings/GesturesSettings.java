/*
 * Copyright (C) 2016 Paranoid Android
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
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.widget.RecyclerView;
import android.widget.Switch;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.internal.hardware.AmbientDisplayConfiguration;

import com.android.settings.gestures.GesturePreference;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.widget.SwitchBar;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GesturesSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener, Indexable {

    private static final String TAG = "GesturesSettings";

    private static final String KEY_OFFSCREEN_GESTURES = "offscreen_gestures";
    private static final String KEY_SYSTEM_GESTURES = "system_gestures";
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

    private static final String KEY_DOUBLE_TAP_POWER = "gesture_double_tap_power";
    private static final String KEY_DOUBLE_TWIST = "gesture_double_twist";
    private static final String KEY_PICK_UP = "gesture_pick_up";
    private static final String KEY_SWIPE_DOWN_FINGERPRINT = "gesture_swipe_down_fingerprint";
    private static final String KEY_DOUBLE_TAP_SLEEP = "gesture_double_tap_sleep";
    private static final String KEY_DOUBLE_TAP_SCREEN = "gesture_double_tap_screen";
    private static final String DEBUG_DOZE_COMPONENT = "debug.doze.component";

    private List<GesturePreference> mPreferences;

    private AmbientDisplayConfiguration mAmbientConfig;

    private PreferenceCategory mOnScreenGestures;
    private PreferenceCategory mSystemGestures;

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
    }

    static {
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
    }

    static {
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

    @Override
    protected int getMetricsCategory() {
        return -1;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.gestures_settings);

        Context context = getActivity();

        mOnScreenGestures = (PreferenceCategory) findPreference(KEY_OFFSCREEN_GESTURES);
        mSystemGestures = (PreferenceCategory) findPreference(KEY_SYSTEM_GESTURES);

        mPreferences = new ArrayList();

        // Double tap power for camera
        if (isCameraDoubleTapPowerGestureAvailable(getResources())) {
            int cameraDisabled = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED, 0);
            addPreference(KEY_DOUBLE_TAP_POWER, cameraDisabled == 0);
        } else {
            mSystemGestures.removePreference(findPreference(KEY_DOUBLE_TAP_POWER));
        }

        // Double tap to sleep
        int tapToSleep = Settings.System.getInt(getContentResolver(),
                Settings.System.GESTURE_DOUBLE_TAP_SLEEP, 0);
        addPreference(KEY_DOUBLE_TAP_SLEEP, tapToSleep == 0);

        // Ambient Display
        mAmbientConfig = new AmbientDisplayConfiguration(context);
        if (mAmbientConfig.pulseOnPickupAvailable()) {
            boolean pickup = mAmbientConfig.pulseOnPickupEnabled(UserHandle.myUserId());
            addPreference(KEY_PICK_UP, pickup);
        } else {
            mSystemGestures.removePreference(findPreference(KEY_PICK_UP));
        }
        if (mAmbientConfig.pulseOnDoubleTapAvailable()) {
            boolean doubleTap = mAmbientConfig.pulseOnDoubleTapEnabled(UserHandle.myUserId());
            addPreference(KEY_DOUBLE_TAP_SCREEN, doubleTap);
        } else {
            mSystemGestures.removePreference(findPreference(KEY_DOUBLE_TAP_SCREEN));
        }

        // Fingerprint slide for notifications
        if (isSystemUINavigationAvailable(context)) {
            addPreference(KEY_SWIPE_DOWN_FINGERPRINT, isSystemUINavigationEnabled(context));
        } else {
            mSystemGestures.removePreference(findPreference(KEY_SWIPE_DOWN_FINGERPRINT));
        }

        // Double twist for camera mode
        if (isDoubleTwistAvailable(context)) {
            int doubleTwistEnabled = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.CAMERA_DOUBLE_TWIST_TO_FLIP_ENABLED, 1);
            addPreference(KEY_DOUBLE_TWIST, doubleTwistEnabled != 0);
        } else {
            mSystemGestures.removePreference(findPreference(KEY_DOUBLE_TWIST));
        }

        for (String gestureKey : mGesturesKeyCodes.keySet()) {
            if (getResources().getInteger(mGesturesKeyCodes.get(gestureKey)) > 0) {
                findPreference(gestureKey).setOnPreferenceChangeListener(this);
            } else {
                mOnScreenGestures.removePreference(findPreference(gestureKey));
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        RecyclerView listview = getListView();
        listview.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    for (GesturePreference pref : mPreferences) {
                        pref.setScrolling(true);
                    }
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    for (GesturePreference pref : mPreferences) {
                        pref.setScrolling(false);
                    }
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            }
        });
        return view;
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

        for (GesturePreference preference : mPreferences) {
            preference.onViewVisible();
        }

        if (mGesturesKeyCodes.keySet().stream().allMatch(keyCode -> getResources().getInteger(
                mGesturesKeyCodes.get(keyCode)) == 0)) {
            getPreferenceScreen().removePreference(mOnScreenGestures);
        } else {
            SettingsActivity activity = (SettingsActivity) getActivity();
            mGesturesEnabler = new GesturesEnabler(activity.getSwitchBar());
        }

        if (mPreferences.isEmpty()) {
            getPreferenceScreen().removePreference(mSystemGestures);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        for (GesturePreference preference : mPreferences) {
            preference.onViewInvisible();
        }
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
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (KEY_DOUBLE_TAP_POWER.equals(key)) {
            boolean enabled = (boolean) newValue;
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED, enabled ? 0 : 1);
        } else if (KEY_DOUBLE_TAP_SLEEP.equals(key)) {
            boolean enabled = (boolean) newValue;
            Settings.System.putInt(getContentResolver(), Settings.System.GESTURE_DOUBLE_TAP_SLEEP,
                    enabled ? 0 : 1);
        } else if (KEY_PICK_UP.equals(key)) {
            boolean enabled = (boolean) newValue;
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.DOZE_PULSE_ON_PICK_UP,
                    enabled ? 1 : 0);
        } else if (KEY_DOUBLE_TAP_SCREEN.equals(key)) {
            boolean enabled = (boolean) newValue;
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.DOZE_PULSE_ON_DOUBLE_TAP,
                    enabled ? 1 : 0);
        } else if (KEY_SWIPE_DOWN_FINGERPRINT.equals(key)) {
            boolean enabled = (boolean) newValue;
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.SYSTEM_NAVIGATION_KEYS_ENABLED, enabled ? 1 : 0);
        } else if (KEY_DOUBLE_TWIST.equals(key)) {
            boolean enabled = (boolean) newValue;
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.CAMERA_DOUBLE_TWIST_TO_FLIP_ENABLED, enabled ? 1 : 0);
        } else if (mGesturesSettings.containsKey(key)) {
            Settings.System.putInt(getContentResolver(), mGesturesSettings.get(key),
                    Integer.parseInt((String) newValue));
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

    private static boolean isCameraDoubleTapPowerGestureAvailable(Resources res) {
        return res.getBoolean(
                com.android.internal.R.bool.config_cameraDoubleTapPowerGestureEnabled);
    }

    private static boolean isSystemUINavigationAvailable(Context context) {
        return context.getResources().getBoolean(
                com.android.internal.R.bool.config_supportSystemNavigationKeys);
    }

    private static boolean isSystemUINavigationEnabled(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.SYSTEM_NAVIGATION_KEYS_ENABLED, 0) == 1;
    }

    private static boolean isDoubleTwistAvailable(Context context) {
        return hasSensor(context, R.string.gesture_double_twist_sensor_name,
                R.string.gesture_double_twist_sensor_vendor);
    }

    private static boolean hasSensor(Context context, int nameResId, int vendorResId) {
        Resources resources = context.getResources();
        String name = resources.getString(nameResId);
        String vendor = resources.getString(vendorResId);
        if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(vendor)) {
            SensorManager sensorManager =
                    (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            for (Sensor s : sensorManager.getSensorList(Sensor.TYPE_ALL)) {
                if (name.equals(s.getName()) && vendor.equals(s.getVendor())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void addPreference(String key, boolean enabled) {
        GesturePreference preference = (GesturePreference) findPreference(key);
        preference.setChecked(enabled);
        preference.setOnPreferenceChangeListener(this);
        mPreferences.add(preference);
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
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER
            = new BaseSearchIndexProvider() {
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
