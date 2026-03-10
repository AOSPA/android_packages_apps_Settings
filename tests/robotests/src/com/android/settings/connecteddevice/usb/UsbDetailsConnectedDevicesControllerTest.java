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

import static org.mockito.Mockito.when;

import android.annotation.Nullable;
import android.content.Context;
import android.hardware.usb.IUsbSerialReader;
import android.hardware.usb.UsbConfiguration;
import android.hardware.usb.UsbDevice;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.core.graphics.drawable.IconCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
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
    private IconCompat mUsbIcon;

    @Before
    public void setUp() {
        mOpenMocks = MockitoAnnotations.openMocks(this);

        mContext = RuntimeEnvironment.getApplication();
        mScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mUsbIcon =
                Utils.createIconWithDrawable(
                        IconCompat.createWithResource(mContext, R.drawable.ic_usb)
                                .loadDrawable(mContext));

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

    private UsbDevice createUsbDevice(
            String name,
            int vendorId,
            int productId,
            @Nullable String manufactureName,
            @Nullable String productName) {
        return new UsbDevice.Builder(
                        name,
                        vendorId,
                        productId,
                        /* Class= */ 0,
                        /* subClass= */ 1,
                        /* protocol= */ 2,
                        manufactureName,
                        productName,
                        /* version= */ "version",
                        /* configurations= */ new UsbConfiguration[] {},
                        /* serialNumber= */ "serialNumber",
                        /* hasAudioPlayback= */ false,
                        /* hasAudioCapture= */ false,
                        /* hasMidi= */ false,
                        /* hasVideoPlayback= */ false,
                        /* hasVideoCapture= */ false)
                .build(new IUsbSerialReader.Default());
    }

    @Test
    @DisableFlags(FLAG_ENABLE_PERSISTENT_USB_DEVICE_PERMISSIONS)
    public void connectedUsbDevicesPreferences_featureDisabled() {
        final UsbDevice usbDeviceWithProductName =
                createUsbDevice(
                        /* name= */ "name",
                        /* vendorId= */ 1,
                        /* productId= */ 2,
                        /* manufactureName= */ null,
                        /* productName= */ null);
        final Map<String, UsbDevice> usbDevices =
                Map.of(usbDeviceWithProductName.getDeviceName(), usbDeviceWithProductName);
        when(mUsbBackend.getUsbDevices()).thenReturn(usbDevices);

        mUsbDetailsConnectedDevicesController.displayPreference(mScreen);
        final PreferenceGroup preferenceGroup =
                mScreen.findPreference(mUsbDetailsConnectedDevicesController.getPreferenceKey());

        assertThat(preferenceGroup.isVisible()).isFalse();
        assertThat(preferenceGroup.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    @EnableFlags(FLAG_ENABLE_PERSISTENT_USB_DEVICE_PERMISSIONS)
    public void connectedUsbDevicesPreferences_NoDevices() {
        when(mUsbBackend.getUsbDevices()).thenReturn(Map.of());

        mUsbDetailsConnectedDevicesController.displayPreference(mScreen);
        final PreferenceGroup preferenceGroup =
                mScreen.findPreference(mUsbDetailsConnectedDevicesController.getPreferenceKey());

        assertThat(preferenceGroup.isVisible()).isFalse();
        assertThat(preferenceGroup.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    @EnableFlags(FLAG_ENABLE_PERSISTENT_USB_DEVICE_PERMISSIONS)
    public void connectedUsbDevicesPreferences() {
        final UsbDevice usbDeviceWithProductName =
                createUsbDevice(
                        /* name= */ "usbDeviceWithProductName",
                        /* vendorId= */ 10,
                        /* productId= */ 11,
                        /* manufactureName= */ null,
                        /* productName= */ "Product Name");
        final UsbDevice usbDeviceWithManufactureName =
                createUsbDevice(
                        /* name= */ "usbDeviceWithManufactureName",
                        /* vendorId= */ 20,
                        /* productId= */ 21,
                        /* manufactureName= */ "Manufacture Name",
                        /* productName= */ null);
        final UsbDevice usbDeviceOnlyWithIds =
                createUsbDevice(
                        /* name= */ "usbDeviceOnlyWithIds",
                        /* vendorId= */ 30,
                        /* productId= */ 31,
                        /* manufactureName= */ null,
                        /* productName= */ null);

        final Map<String, UsbDevice> usbDevices =
                Map.of(
                        usbDeviceWithProductName.getDeviceName(), usbDeviceWithProductName,
                        usbDeviceWithManufactureName.getDeviceName(), usbDeviceWithManufactureName,
                        usbDeviceOnlyWithIds.getDeviceName(), usbDeviceOnlyWithIds);
        when(mUsbBackend.getUsbDevices()).thenReturn(usbDevices);

        mUsbDetailsConnectedDevicesController.displayPreference(mScreen);

        final String productNameTitle = usbDeviceWithProductName.getProductName();
        final String manufacturerNameTitle =
                mContext.getString(
                        R.string.usb_device_name_unknown_with_manufacturer_name,
                        usbDeviceWithManufactureName.getManufacturerName());
        final String onlyWithIdsTitle =
                mContext.getString(
                        R.string.usb_device_name_unknown_with_vendor_id_and_product_id,
                        usbDeviceOnlyWithIds.getVendorId(),
                        usbDeviceOnlyWithIds.getProductId());
        final Map<String, String> expectedTitles =
                Map.of(
                        createDevicePreferenceKey(usbDeviceWithProductName), productNameTitle,
                        createDevicePreferenceKey(usbDeviceWithManufactureName),
                                manufacturerNameTitle,
                        createDevicePreferenceKey(usbDeviceOnlyWithIds), onlyWithIdsTitle);

        final PreferenceGroup preferenceGroup =
                mScreen.findPreference(mUsbDetailsConnectedDevicesController.getPreferenceKey());

        assertThat(preferenceGroup.getPreferenceCount()).isEqualTo(expectedTitles.size());
        for (int i = 0; i < preferenceGroup.getPreferenceCount(); ++i) {
            final Preference preference = preferenceGroup.getPreference(i);

            final IconCompat preferenceIcon = Utils.createIconWithDrawable(preference.getIcon());
            assertThat(preferenceIcon.getBitmap().sameAs(mUsbIcon.getBitmap())).isTrue();

            final String expectedTitle = expectedTitles.get(preference.getKey());
            assertThat(preference.getTitle().toString()).isEqualTo(expectedTitle);
        }
    }
}
