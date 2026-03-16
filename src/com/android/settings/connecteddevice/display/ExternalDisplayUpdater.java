/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.connecteddevice.display;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settingslib.RestrictedPreference;

public class ExternalDisplayUpdater extends BaseExternalDisplayUpdater {

    @VisibleForTesting public static final String PREF_KEY = "external_display_settings";
    @Nullable private RestrictedPreference mPreference;

    public ExternalDisplayUpdater(@NonNull DevicePreferenceCallback callback, int metricsCategory) {
        super(callback, metricsCategory);
    }

    @Override
    @VisibleForTesting
    public void initPreference(@NonNull Context context, ConnectedDisplayInjector injector) {
        super.initPreference(context, injector);

        mPreference = new RestrictedPreference(context, null /* AttributeSet */);
        mPreference.setTitle(R.string.external_display_settings_title);
        mPreference.setIcon(getDrawable(context));
        mPreference.setKey(PREF_KEY);
        applyUsbDataSignalingPolicy(mPreference, context);
        mPreference.setOnPreferenceClickListener(
                (Preference p) -> {
                    metricsFeatureProvider.logClickedPreference(p, metricsCategory);
                    launchSettings(context, /* args= */ null);
                    return true;
                });
    }

    @VisibleForTesting
    @Nullable
    protected Drawable getDrawable(Context context) {
        return context.getDrawable(R.drawable.ic_external_display_32dp);
    }

    @Override
    protected void update() {
        if (mPreference == null) {
            return;
        }
        if (hasEnabledConnectedDisplay()) {
            devicePreferenceCallback.onDeviceAdded(mPreference);
        } else {
            devicePreferenceCallback.onDeviceRemoved(mPreference);
        }
    }

    private boolean hasEnabledConnectedDisplay() {
        if (injector == null) {
            return false;
        }
        return injector.getDisplays().stream()
                .anyMatch(
                        display ->
                                display.isConnectedDisplay()
                                        && display.isEnabled() == DisplayIsEnabled.YES);
    }
}
