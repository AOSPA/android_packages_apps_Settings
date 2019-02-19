/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.buttons;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.UserManager;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.android.internal.R;

public class NavigationBarPreferenceController extends ButtonsSettingsPreferenceController {

    private final int ON = 1;
    private final int OFF = 0;

    private final UserManager mUserManager;

    private int mDeviceHardwareKeys;
    private boolean mOnlyHasCameraButton;

    public NavigationBarPreferenceController(Context context, String key) {
        super(context, key);
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
    }

    static boolean isNavigationBarAvailable(Context context) {

        mDeviceHardwareKeys = context.getResources().getInt(
            com.android.internal.R.integer.config_deviceHardwareKeys);

        mOnlyHasCameraButton = context.getResources().getBoolean(
            R.bool.config_has_only_camera_button);

        return mDeviceHardwareKeys != 0 && !mOnlyHasCameraButton;
    }

    @Override
    public int getAvailabilityStatus() {
        return isNavigationBarAvailable(mContext) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        setNavigationBarPreference(mContext, mUserManager, isChecked ? ON : OFF);
        return true;
    }

    public static void setNavigationBarPreference(Context context, UserManager userManager,
            int enabled) {
        Settings.System.putInt(context.getContentResolver(),
                Settings.System.NAVIGATION_BAR_ENABLED, enabled);
    }

    @Override
    public boolean isChecked() {
        final int defaultValue = mContext.getResources()
                .getBoolean(com.android.internal.R.bool.config_showNavigationBar) ? ON : OFF;
        final int swipeUpEnabled = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.NAVIGATION_BAR_ENABLED, defaultValue);
        return navigationBarEnabled != OFF;
    }
}