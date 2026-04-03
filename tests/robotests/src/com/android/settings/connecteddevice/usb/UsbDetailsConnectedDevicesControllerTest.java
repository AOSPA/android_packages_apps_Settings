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

import static android.hardware.usb.flags.Flags.FLAG_ENABLE_PERSISTENT_USB_DEVICE_PERMISSIONS;

import static com.android.settings.connecteddevice.usb.UsbDetailsConnectedDevicesController.createDevicePreferenceKey;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.core.graphics.drawable.IconCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class UsbDetailsConnectedDevicesControllerTest {
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock private UsbBackend mUsbBackend;
    @Mock private UsbDetailsFragment mFragment;

    private AutoCloseable mOpenMocks;
    private Context mContext;
    private PreferenceScreen mScreen;
    private UsbDetailsConnectedDevicesController mUsbDetailsConnectedDevicesController;

    @Before
    public void setUp() {
        mOpenMocks = MockitoAnnotations.openMocks(this);

        mContext = RuntimeEnvironment.getApplication();
        mScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);

        mUsbDetailsConnectedDevicesController =
                new UsbDetailsConnectedDevicesController(mContext, mFragment, mUsbBackend);

        PreferenceCategory preference = new PreferenceCategory(mContext);
        preference.setKey(mUsbDetailsConnectedDevicesController.getPreferenceKey());
        mScreen.addPreference(preference);
    }

    @After
    public void tearDown() throws Exception {
        mOpenMocks.close();
    }

    @Test
    @DisableFlags(FLAG_ENABLE_PERSISTENT_USB_DEVICE_PERMISSIONS)
    public void connectedUsbDevicesPreferences_featureDisabled() {
        final Map<String, UsbDevice> usbDevices = Map.of("usbDeviceName", mock(UsbDevice.class));
        when(mUsbBackend.getUsbDevices()).thenReturn(usbDevices);

        mUsbDetailsConnectedDevicesController.displayPreference(mScreen);
        final PreferenceCategory preferenceCategory =
                mScreen.findPreference(mUsbDetailsConnectedDevicesController.getPreferenceKey());

        assertThat(preferenceCategory).isNotNull();
        assertThat(preferenceCategory.isVisible()).isFalse();
        assertThat(preferenceCategory.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    @EnableFlags(FLAG_ENABLE_PERSISTENT_USB_DEVICE_PERMISSIONS)
    public void connectedUsbDevicesPreferences_NoDevices() {
        when(mUsbBackend.getUsbDevices()).thenReturn(Map.of());

        mUsbDetailsConnectedDevicesController.displayPreference(mScreen);
        final PreferenceCategory preferenceCategory =
                mScreen.findPreference(mUsbDetailsConnectedDevicesController.getPreferenceKey());

        assertThat(preferenceCategory).isNotNull();
        assertThat(preferenceCategory.isVisible()).isFalse();
        assertThat(preferenceCategory.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    @EnableFlags(FLAG_ENABLE_PERSISTENT_USB_DEVICE_PERMISSIONS)
    public void connectedUsbDevicesPreferences() {
        final UsbDevice firstUsbDevice = mock(UsbDevice.class);
        final UsbDevice secondUsbDevice = mock(UsbDevice.class);
        final UsbDevice thirdUsbDevice = mock(UsbDevice.class);

        final String firstUsbDeviceName = "firstDeviceName";
        final String secondUsbDeviceName = "secondDeviceName";
        final String thirdUsbDeviceName = "thirdDeviceName";

        final Map<String, UsbDevice> usbDevices =
                Map.of(
                        firstUsbDeviceName,
                        firstUsbDevice,
                        secondUsbDeviceName,
                        secondUsbDevice,
                        thirdUsbDeviceName,
                        thirdUsbDevice);
        final Map<String, String> expectedTitles =
                Map.of(
                        createDevicePreferenceKey(firstUsbDevice),
                        firstUsbDeviceName,
                        createDevicePreferenceKey(secondUsbDevice),
                        secondUsbDeviceName,
                        createDevicePreferenceKey(thirdUsbDevice),
                        thirdUsbDeviceName);

        when(mUsbBackend.getUsbDevices()).thenReturn(usbDevices);
        when(mUsbBackend.getDeviceName(firstUsbDevice)).thenReturn(firstUsbDeviceName);
        when(mUsbBackend.getDeviceName(secondUsbDevice)).thenReturn(secondUsbDeviceName);
        when(mUsbBackend.getDeviceName(thirdUsbDevice)).thenReturn(thirdUsbDeviceName);

        mUsbDetailsConnectedDevicesController.displayPreference(mScreen);
        final PreferenceCategory preferenceCategory =
                mScreen.findPreference(mUsbDetailsConnectedDevicesController.getPreferenceKey());

        assertThat(preferenceCategory).isNotNull();
        assertThat(preferenceCategory.getPreferenceCount()).isEqualTo(expectedTitles.size());

        final Bitmap expectedIcon =
                Utils.createIconWithDrawable(
                                IconCompat.createWithResource(mContext, R.drawable.ic_usb)
                                        .loadDrawable(mContext))
                        .getBitmap();

        for (int i = 0; i < preferenceCategory.getPreferenceCount(); ++i) {
            final Preference preference = preferenceCategory.getPreference(i);

            final Bitmap preferenceIcon =
                    Utils.createIconWithDrawable(preference.getIcon()).getBitmap();
            assertThat(preferenceIcon).isNotNull();
            assertThat(preferenceIcon.sameAs(expectedIcon)).isTrue();

            final String expectedTitle = expectedTitles.get(preference.getKey());
            final CharSequence preferenceTitle = preference.getTitle();
            assertThat(preferenceTitle).isNotNull();
            assertThat(preferenceTitle.toString()).isEqualTo(expectedTitle);
        }
    }
}
