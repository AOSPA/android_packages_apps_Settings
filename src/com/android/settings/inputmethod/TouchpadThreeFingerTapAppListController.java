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

import static android.content.ComponentName.unflattenFromString;
import static android.hardware.input.AppLaunchData.createLaunchDataForComponent;

import static com.android.settings.inputmethod.TouchpadThreeFingerTapActionPreferenceController.SET_GESTURE;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.database.ContentObserver;
import android.hardware.input.AppLaunchData;
import android.hardware.input.AppLaunchData.ComponentData;
import android.hardware.input.InputGestureData;
import android.hardware.input.InputManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.util.DisplayMetrics;

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

import java.util.Comparator;
import java.util.List;

/**
 * Controller that updates a list of installed app Preferences.
 * This screen is a sub-page of {@link TouchpadThreeFingerTapPreferenceController}) allowing three
 * finger tap gesture to open the selected app.
 */
public class TouchpadThreeFingerTapAppListController extends BasePreferenceController
        implements LifecycleEventObserver, SelectorWithWidgetPreference.OnClickListener {

    private final ContentResolver mContentResolver;
    private LauncherApps mLauncherApps;
    private InputManager mInputManager;

    private ContentObserver mObserver =
            new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange, @Nullable Uri uri) {
                    if (uri == null || mPreferenceScreen == null) {
                        return;
                    }
                    if (uri.equals(TouchpadThreeFingerTapUtils.TARGET_ACTION_URI)) {
                        updateState(mPreferenceScreen);
                    }
                }
            };

    @Nullable private PreferenceScreen mPreferenceScreen;

    private final LauncherApps.Callback mLauncherAppsCallback = new LauncherApps.Callback() {
        @Override
        public void onPackageRemoved(@Nullable String packageName, @Nullable UserHandle user) {
            if (packageName != null && isAppSelected(packageName)) {
                setGestureToDefault();
                repopulateApps();
            }
        }

        @Override
        public void onPackageAdded(@Nullable String packageName, @Nullable UserHandle user) {}

        @Override
        public void onPackageChanged(@Nullable String packageName, @Nullable UserHandle user) {}

        @Override
        public void onPackagesAvailable(
                @Nullable String[] packageNames, @Nullable UserHandle user, boolean replacing) {}

        @Override
        public void onPackagesUnavailable(
                @Nullable String[] packageNames, @Nullable UserHandle user, boolean replacing) {
            if (packageNames == null) {
                return;
            }
            for (String packageName : packageNames) {
                if (isAppSelected(packageName)) {
                    setGestureToDefault();
                    repopulateApps();
                }
            }
        }
    };

    public TouchpadThreeFingerTapAppListController(@NonNull Context context,
            @NonNull String key) {
        super(context, key);
        mLauncherApps = context.getSystemService(LauncherApps.class);
        mInputManager = context.getSystemService(InputManager.class);
        mContentResolver = context.getContentResolver();
        mLauncherApps.registerCallback(mLauncherAppsCallback);
    }

    @VisibleForTesting
    TouchpadThreeFingerTapAppListController(@NonNull Context context,
            @NonNull String key,
            @NonNull LauncherApps launcherApps,
            @NonNull InputManager inputManager,
            @NonNull ContentObserver contentObserver) {
        this(context, key);
        mLauncherApps = launcherApps;
        mInputManager = inputManager;
        mObserver = contentObserver;
        mLauncherApps.registerCallback(mLauncherAppsCallback);
    }

    private void setGestureToDefault() {
        mInputManager.removeAllCustomInputGestures(InputGestureData.Filter.TOUCHPAD);
        TouchpadThreeFingerTapUtils.setDefaultGestureType(mContentResolver);
    }

    private boolean isAppSelected(@NonNull String packageName) {
        if (TouchpadThreeFingerTapUtils.isGestureTypeLaunchApp(mContentResolver)) {
            ComponentName componentName = getSelectedAppComponent();
            return componentName != null && packageName.equals(componentName.getPackageName());
        }
        return false;
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
        updateAppListSelection();
    }

    private void updateAppListSelection() {
        // When the current gesture state is not app launching, the key is set to null so that no
        // app Preference will be selected
        String matchingKey = null;
        if (TouchpadThreeFingerTapUtils.isGestureTypeLaunchApp(mContentResolver)) {
            ComponentName componentName = getSelectedAppComponent();
            if (componentName != null) {
                matchingKey = parsePreferenceKeyFromComponent(componentName);
            }
        }
        updateCheckStatus(matchingKey);
    }

    private void updateCheckStatus(@Nullable String matchingKey) {
        if (mPreferenceScreen == null) {
            return;
        }
        int count = mPreferenceScreen.getPreferenceCount();
        for (int i = 0; i < count; i++) {
            Preference pref = mPreferenceScreen.getPreference(i);
            if (pref instanceof SelectorWithWidgetPreference appPreference) {
                boolean isMatched = matchingKey != null
                        && matchingKey.equals(appPreference.getKey());
                appPreference.setChecked(isMatched);
            }
        }
    }

    @Nullable
    private ComponentName getSelectedAppComponent() {
        InputGestureData gestureData =
                mInputManager.getInputGesture(TouchpadThreeFingerTapUtils.TRIGGER);
        if (gestureData != null) {
            ComponentData componentData = (ComponentData) gestureData.getAction().appLaunchData();
            if (componentData != null) {
                return new ComponentName(
                        componentData.getPackageName(), componentData.getClassName());
            }
        }
        return null;
    }

    @NonNull
    private String parsePreferenceKeyFromComponent(@NonNull ComponentName componentName) {
        // flattenToString contains the component's package name and its class name. This way, when
        // given a Preference, we can restore its corresponding component using its key.
        // unflattenFromString is called in onRadioButtonClicked
        return componentName.flattenToString();
    }

    @Override
    public int getAvailabilityStatus() {
        return InputPeripheralsSettingsUtils.isTouchpad() ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceScreen = screen;
        repopulateApps();
    }

    private void repopulateApps() {
        if (mPreferenceScreen == null) {
            return;
        }
        mPreferenceScreen.removeAll();

        int userId = ActivityManager.getCurrentUser();
        List<LauncherActivityInfo> appInfos =
                mLauncherApps.getActivityList(/* packageName = */ null, UserHandle.of(userId));
        appInfos.sort(Comparator.comparing(appInfo -> appInfo.getLabel().toString()));

        for (LauncherActivityInfo appInfo : appInfos) {
            mPreferenceScreen.addPreference(createPreference(appInfo));
        }
    }

    @NonNull
    private SelectorWithWidgetPreference createPreference(@NonNull LauncherActivityInfo appInfo) {
        SelectorWithWidgetPreference preference = new SelectorWithWidgetPreference(mContext);
        ComponentName component = appInfo.getComponentName();
        preference.setKey(parsePreferenceKeyFromComponent(component));
        preference.setTitle(appInfo.getLabel());
        preference.setIcon(appInfo.getIcon(DisplayMetrics.DENSITY_DEVICE_STABLE));
        preference.setOnClickListener(this);
        return preference;
    }

    @Override
    public void onRadioButtonClicked(@NonNull SelectorWithWidgetPreference preference) {
        String key = preference.getKey();

        // The key stores the component's information for each Preference
        // See comments in parsePreferenceKeyFromComponent()
        ComponentName component = unflattenFromString(key);
        if (component != null) {
            AppLaunchData appLaunchData = createLaunchDataForComponent(
                    component.getPackageName(), component.getClassName());
            setLaunchingApp(appLaunchData);
        }
        updateAppListSelection();
    }

    private void setLaunchingApp(@NonNull AppLaunchData appLaunchData) {
        if (mPreferenceScreen == null || !mPreferenceScreen.getExtras().getBoolean(SET_GESTURE)) {
            return;
        }
        InputGestureData gestureData =
                new InputGestureData.Builder()
                .setTrigger(TouchpadThreeFingerTapUtils.TRIGGER)
                .setAppLaunchData(appLaunchData)
                .build();
        mInputManager.removeAllCustomInputGestures(InputGestureData.Filter.TOUCHPAD);
        mInputManager.addCustomInputGesture(gestureData);
        TouchpadThreeFingerTapUtils.setLaunchAppAsGestureType(mContentResolver);
    }
}
