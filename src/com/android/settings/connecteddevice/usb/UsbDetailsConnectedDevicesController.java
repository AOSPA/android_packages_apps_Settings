/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings.connecteddevice.usb;

import static android.hardware.usb.flags.Flags.enablePersistentUsbDevicePermissions;

import android.content.Context;
import android.hardware.usb.UsbDevice;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.Initializer;
import com.android.settings.R;

/** This class displays and controls connected {@link UsbDevice}s. */
public class UsbDetailsConnectedDevicesController extends UsbDetailsController {
    private static final String KEY_USB_DETAILS_CONNECTED_DEVICES = "usb_details_connected_devices";
    private static final String PREFERENCE_KEY_PREFIX = "usb_device_";

    @VisibleForTesting
    static String createDevicePreferenceKey(UsbDevice usbDevice) {
        return PREFERENCE_KEY_PREFIX + usbDevice.hashCode();
    }

    private PreferenceGroup mPreferenceGroup;

    public UsbDetailsConnectedDevicesController(
            Context context, UsbDetailsFragment fragment, UsbBackend backend) {
        super(context, fragment, backend);
    }

    @Override
    @Initializer
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreferenceGroup = screen.findPreference(getPreferenceKey());
        mPreferenceGroup.setOrderingAsAdded(false);

        updateDevices();
    }

    @Override
    protected void refresh(boolean connected, long functions, int powerRole, int dataRole) {
        updateDevices();
    }

    @Override
    public boolean isAvailable() {
        return enablePersistentUsbDevicePermissions();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_USB_DETAILS_CONNECTED_DEVICES;
    }

    private void updateDevices() {
        if (!isAvailable()) {
            mPreferenceGroup.setVisible(false);
            return;
        }

        mPreferenceGroup.removeAll();
        for (final UsbDevice usbDevice : mUsbBackend.getUsbDevices().values()) {
            mPreferenceGroup.addPreference(createPreference(usbDevice));
        }

        mPreferenceGroup.setVisible(mPreferenceGroup.getPreferenceCount() != 0);
    }

    private Preference createPreference(UsbDevice usbDevice) {
        final Preference preference = new Preference(mContext);
        preference.setTitle(mUsbBackend.getDeviceName(usbDevice));
        preference.setIcon(R.drawable.ic_usb);
        preference.setKey(createDevicePreferenceKey(usbDevice));

        return preference;
    }
}
