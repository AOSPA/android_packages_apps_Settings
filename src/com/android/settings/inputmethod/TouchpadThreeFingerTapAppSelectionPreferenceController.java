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

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.hardware.input.InputSettings;
import android.os.UserHandle;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.Comparator;
import java.util.List;

/**
 * Preference controller that updates a list of installed app Preferences.
 * This screen is a sub-page of {@link TouchpadThreeFingerTapPreferenceController}) allowing three
 * finger tap gesture to open the selected app.
 */
public class TouchpadThreeFingerTapAppSelectionPreferenceController
        extends BasePreferenceController {

    private final LauncherApps mLauncherApps;
    @Nullable private PreferenceScreen mPreferenceScreen;

    public TouchpadThreeFingerTapAppSelectionPreferenceController(@NonNull Context context,
            @NonNull String key) {
        super(context, key);
        mLauncherApps = mContext.getSystemService(LauncherApps.class);
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
        mPreferenceScreen = screen;
        updateApps();
    }

    private void updateApps() {
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

    private SelectorWithWidgetPreference createPreference(LauncherActivityInfo appInfo) {
        SelectorWithWidgetPreference preference = new SelectorWithWidgetPreference(mContext);
        preference.setKey(appInfo.getComponentName().flattenToString());
        preference.setTitle(appInfo.getLabel());
        preference.setIcon(appInfo.getIcon(DisplayMetrics.DENSITY_DEVICE_STABLE));
        return preference;
    }
}
