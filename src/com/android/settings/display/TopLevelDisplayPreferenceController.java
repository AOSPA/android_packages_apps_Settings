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

package com.android.settings.display;

import android.content.Context;
import android.content.pm.PackageManager;

import com.android.settings.R;
import com.android.settings.connecteddevice.display.ConnectedDisplayInjector;
import com.android.settings.connecteddevice.display.DisplayDevice;
import com.android.settings.connecteddevice.display.DisplayIsEnabled;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.utils.DesktopSettingsUtils;

import java.util.List;

public class TopLevelDisplayPreferenceController extends BasePreferenceController {

    private final ConnectedDisplayInjector mConnectedDisplayInjector;
    private final boolean mShouldShowTopLevelDeviceCategory;

    public TopLevelDisplayPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mConnectedDisplayInjector = new ConnectedDisplayInjector(mContext);
        mShouldShowTopLevelDeviceCategory =
                DesktopSettingsUtils.shouldShowTopLevelDeviceCategory(mContext);
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getResources().getBoolean(R.bool.config_show_top_level_display)
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public CharSequence getSummary() {
        if (!mShouldShowTopLevelDeviceCategory) {
            return mContext.getText(R.string.display_dashboard_summary);
        }

        PackageManager packageManager = mContext.getPackageManager();
        boolean hasTouch = packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
                || packageManager.hasSystemFeature(PackageManager.FEATURE_FAKETOUCH);
        boolean hasExternal = hasConnectedDisplay();

        if (hasTouch && hasExternal) {
            return mContext.getText(R.string.device_display_dashboard_summary_with_touch_external);
        } else if (hasTouch) {
            return mContext.getText(R.string.device_display_dashboard_summary_with_touch);
        } else if (hasExternal) {
            return mContext.getText(R.string.device_display_dashboard_summary_with_external);
        } else {
            return mContext.getText(R.string.device_display_dashboard_summary);
        }
    }

    private boolean hasConnectedDisplay() {
        List<DisplayDevice> displays = mConnectedDisplayInjector.getDisplays();
        for (DisplayDevice display : displays) {
            if (display.isConnectedDisplay() && display.isEnabled() == DisplayIsEnabled.YES) {
                return true;
            }
        }
        return false;
    }
}
