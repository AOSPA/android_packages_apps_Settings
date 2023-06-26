/*
 * Copyright (C) 2023 Yet Another AOSP Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.sound;

import android.content.Context;
import android.os.Binder;
import android.provider.DeviceConfig;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.internal.config.sysui.SystemUiDeviceConfigFlags;

import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;

/**
 * A simple preference controller to allow splitting notification and ringtone streams
 */
public class SeparateNotificationPreferenceController extends AbstractPreferenceController
        implements Preference.OnPreferenceChangeListener {

    protected boolean mSeparateNotification;

    private static final boolean CONFIG_SEPARATE_NOTIFICATION_DEFAULT_VAL = false;

    private static final String KEY = "volume_separate_notification";

    private SwitchPreference mPreference;

    public SeparateNotificationPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = (SwitchPreference) screen.findPreference(KEY);
        mPreference.setChecked(readSeparateNotificationVolumeConfig());
        mPreference.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mPreference) {
            final boolean value = (Boolean) newValue;
            updateDeviceConfig(value);
            return true;
        }
        return false;
    }

    private void updateDeviceConfig(boolean enabled) {
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_SYSTEMUI,
                SystemUiDeviceConfigFlags.VOLUME_SEPARATE_NOTIFICATION,
                Boolean.toString(enabled), false /* makeDefault */);
    }

    protected boolean isSeparateNotificationConfigEnabled() {
        return Binder.withCleanCallingIdentity(()
                -> DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_SYSTEMUI,
                SystemUiDeviceConfigFlags.VOLUME_SEPARATE_NOTIFICATION,
                CONFIG_SEPARATE_NOTIFICATION_DEFAULT_VAL));
    }

    /**
     * side effect: updates the cached value of the config
     * @return has the config changed?
     */
    protected boolean readSeparateNotificationVolumeConfig() {
        boolean newVal = isSeparateNotificationConfigEnabled();

        boolean valueUpdated = newVal != mSeparateNotification;
        if (valueUpdated) {
            mSeparateNotification = newVal;
        }

        return valueUpdated;
    }
}
