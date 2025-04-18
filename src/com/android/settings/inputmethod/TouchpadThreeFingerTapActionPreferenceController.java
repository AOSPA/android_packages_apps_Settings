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

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.hardware.input.InputGestureData;
import android.hardware.input.InputManager;
import android.hardware.input.InputSettings;
import android.hardware.input.KeyGestureEvent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

/**
 * Preference controller that updates different the three finger tap action.
 * When clicking on the top level Three Tinger Tap Preference (handled by
 * {@link TouchpadThreeFingerTapPreferenceController}) on the Touchpad page, it loads a
 * page of action Preferences.
 */
public class TouchpadThreeFingerTapActionPreferenceController extends BasePreferenceController
        implements LifecycleEventObserver, SelectorWithWidgetPreference.OnClickListener {

    private final InputManager mInputManager;
    private final ContentResolver mContentResolver;

    @Nullable
    private SelectorWithWidgetPreference mPreference;


    private ContentObserver mObserver =
            new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange, @Nullable Uri uri) {
                    if (mPreference == null || uri == null) {
                        return;
                    }
                    if (uri.equals(TouchpadThreeFingerTapUtils.TARGET_ACTION_URI)) {
                        updateState(mPreference);
                    }
                }
            };

    public TouchpadThreeFingerTapActionPreferenceController(@NonNull Context context,
            @NonNull String key) {
        super(context, key);
        mInputManager = context.getSystemService(InputManager.class);
        mContentResolver = context.getContentResolver();
    }

    @VisibleForTesting
    TouchpadThreeFingerTapActionPreferenceController(@NonNull Context context,
            @NonNull String key,
            ContentObserver contentObserver) {
        this(context, key);
        mObserver = contentObserver;
    }

    @Override
    public int getAvailabilityStatus() {
        boolean isTouchpad = InputPeripheralsSettingsUtils.isTouchpad();
        return (InputSettings.isTouchpadThreeFingerTapShortcutFeatureFlagEnabled() && isTouchpad)
                ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(mPreferenceKey);
        if (mPreference != null) {
            mPreference.setOnClickListener(this);
        }
    }

    @Override
    public void onRadioButtonClicked(@NonNull SelectorWithWidgetPreference preference) {
        final int gestureType = TouchpadThreeFingerTapUtils.getGestureTypeByPrefKey(mPreferenceKey);
        setGesture(gestureType);
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner lifecycleOwner,
            @NonNull Lifecycle.Event event) {
        if (event == Lifecycle.Event.ON_START) {
            mContentResolver.registerContentObserver(
                    TouchpadThreeFingerTapUtils.TARGET_ACTION_URI,
                    /* notifyForDescendants = */ true, mObserver);
        } else if (event == Lifecycle.Event.ON_STOP) {
            mContentResolver.unregisterContentObserver(mObserver);
        }

    }

    @Override
    public void updateState(@NonNull Preference preference) {
        super.updateState(preference);

        int prefValue = TouchpadThreeFingerTapUtils.getGestureTypeByPrefKey(mPreferenceKey);
        int currentValue =
                TouchpadThreeFingerTapUtils.getCurrentGestureType(mContentResolver);
        if (mPreference != null) {
            mPreference.setChecked(prefValue == currentValue);
        }
    }

    private void setGesture(int customGestureType) {
        mInputManager.removeAllCustomInputGestures(InputGestureData.Filter.TOUCHPAD);
        if (customGestureType != KeyGestureEvent.KEY_GESTURE_TYPE_UNSPECIFIED) {
            InputGestureData gestureData = new InputGestureData.Builder()
                    .setTrigger(TouchpadThreeFingerTapUtils.TRIGGER)
                    .setKeyGestureType(customGestureType)
                    .build();
            mInputManager.addCustomInputGesture(gestureData);
        }
        TouchpadThreeFingerTapUtils.setGestureType(mContentResolver, customGestureType);
    }
}
