/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static android.hardware.usb.UsbPortStatus.DATA_ROLE_DEVICE;
import static android.hardware.usb.UsbPortStatus.DATA_ROLE_HOST;
import static android.hardware.usb.UsbPortStatus.POWER_ROLE_SINK;
import static android.hardware.usb.UsbPortStatus.POWER_ROLE_SOURCE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.annotation.Nullable;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.usb.IUsbSerialReader;
import android.hardware.usb.UsbConfiguration;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbPort;
import android.hardware.usb.UsbPortStatus;
import android.net.TetheringManager;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class UsbBackendTest {

    @Mock(answer = RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private UsbManager mUsbManager;
    @Mock
    private UserManager mUserManager;
    @Mock
    private TetheringManager mTetheringManager;
    @Mock
    private UsbPort mUsbPort;
    @Mock
    private UsbPortStatus mUsbPortStatus;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_MIDI))
                .thenReturn(true);
        when((Object) mContext.getSystemService(UsbManager.class)).thenReturn(mUsbManager);
        when((Object) mContext.getSystemService(
                TetheringManager.class)).thenReturn(mTetheringManager);
        when(mUsbManager.getPorts()).thenReturn(Collections.singletonList(mUsbPort));
        when(mUsbPortStatus.isConnected()).thenReturn(true);
        when(mUsbPort.getStatus()).thenReturn(mUsbPortStatus);
    }

    @Test
    public void setDataRole_allRolesSupported_shouldSetDataRole() {
        final UsbBackend usbBackend = new UsbBackend(mContext, mUserManager);

        when(mUsbPortStatus
                .isRoleCombinationSupported(POWER_ROLE_SINK, DATA_ROLE_DEVICE))
                .thenReturn(true);
        when(mUsbPortStatus
                .isRoleCombinationSupported(POWER_ROLE_SINK, DATA_ROLE_HOST))
                .thenReturn(true);
        when(mUsbPortStatus
                .isRoleCombinationSupported(POWER_ROLE_SOURCE, DATA_ROLE_DEVICE))
                .thenReturn(true);
        when(mUsbPortStatus
                .isRoleCombinationSupported(POWER_ROLE_SOURCE, DATA_ROLE_HOST))
                .thenReturn(true);
        when(mUsbPortStatus.getCurrentPowerRole()).thenReturn(POWER_ROLE_SINK);

        usbBackend.setDataRole(DATA_ROLE_HOST);

        verify(mUsbPort).setRoles(POWER_ROLE_SINK, DATA_ROLE_HOST);
    }

    @Test
    public void setDataRole_notAllRolesSupported_shouldSetDataAndPowerRole() {
        final UsbBackend usbBackend = new UsbBackend(mContext, mUserManager);

        when(mUsbPortStatus
                .isRoleCombinationSupported(POWER_ROLE_SINK, DATA_ROLE_DEVICE))
                .thenReturn(true);
        when(mUsbPortStatus
                .isRoleCombinationSupported(POWER_ROLE_SOURCE, DATA_ROLE_HOST))
                .thenReturn(true);
        when(mUsbPortStatus.getCurrentPowerRole()).thenReturn(POWER_ROLE_SINK);

        usbBackend.setDataRole(DATA_ROLE_HOST);

        verify(mUsbPort).setRoles(POWER_ROLE_SOURCE, DATA_ROLE_HOST);
    }

    @Test
    public void setPowerRole_allRolesSupported_shouldSetPowerRole() {
        final UsbBackend usbBackend = new UsbBackend(mContext, mUserManager);

        when(mUsbPortStatus
                .isRoleCombinationSupported(POWER_ROLE_SINK, DATA_ROLE_DEVICE))
                .thenReturn(true);
        when(mUsbPortStatus
                .isRoleCombinationSupported(POWER_ROLE_SINK, DATA_ROLE_HOST))
                .thenReturn(true);
        when(mUsbPortStatus
                .isRoleCombinationSupported(POWER_ROLE_SOURCE, DATA_ROLE_DEVICE))
                .thenReturn(true);
        when(mUsbPortStatus
                .isRoleCombinationSupported(POWER_ROLE_SOURCE, DATA_ROLE_HOST))
                .thenReturn(true);
        when(mUsbPortStatus.getCurrentDataRole()).thenReturn(DATA_ROLE_DEVICE);

        usbBackend.setPowerRole(POWER_ROLE_SOURCE);

        verify(mUsbPort).setRoles(POWER_ROLE_SOURCE, DATA_ROLE_DEVICE);
    }

    @Test
    public void setPowerRole_notAllRolesSupported_shouldSetDataAndPowerRole() {
        final UsbBackend usbBackend = new UsbBackend(mContext, mUserManager);

        when(mUsbPortStatus
                .isRoleCombinationSupported(POWER_ROLE_SINK, DATA_ROLE_DEVICE))
                .thenReturn(true);
        when(mUsbPortStatus
                .isRoleCombinationSupported(POWER_ROLE_SOURCE, DATA_ROLE_HOST))
                .thenReturn(true);
        when(mUsbPortStatus.getCurrentDataRole()).thenReturn(DATA_ROLE_DEVICE);

        usbBackend.setPowerRole(POWER_ROLE_SOURCE);

        verify(mUsbPort).setRoles(POWER_ROLE_SOURCE, DATA_ROLE_HOST);
    }

    @Test
    public void areFunctionsSupported_fileTransferDisallowedByBaseRestriction_shouldReturnFalse() {
        when(mUserManager.hasUserRestriction(UserManager.DISALLOW_USB_FILE_TRANSFER))
                .thenReturn(true);
        when(mUserManager.hasBaseUserRestriction(
                eq(UserManager.DISALLOW_USB_FILE_TRANSFER), any(UserHandle.class)))
                .thenReturn(true);

        final UsbBackend usbBackend = new UsbBackend(mContext, mUserManager);

        assertThat(usbBackend.areFunctionsSupported(UsbManager.FUNCTION_MTP)).isFalse();
    }

    @Test
    public void areFunctionsSupported_fileTransferDisallowedByAdminRestriction_shouldReturnTrue() {
        when(mUserManager.hasUserRestriction(UserManager.DISALLOW_USB_FILE_TRANSFER))
                .thenReturn(true);
        when(mUserManager.hasBaseUserRestriction(
                eq(UserManager.DISALLOW_USB_FILE_TRANSFER), any(UserHandle.class)))
                .thenReturn(false);

        final UsbBackend usbBackend = new UsbBackend(mContext, mUserManager);

        assertThat(usbBackend.areFunctionsSupported(UsbManager.FUNCTION_MTP)).isTrue();
    }

    @Test
    public void areFunctionsSupported_fileTransferAllowed_shouldReturnTrue() {
        when(mUserManager.hasUserRestriction(UserManager.DISALLOW_USB_FILE_TRANSFER))
                .thenReturn(false);
        when(mUserManager.hasBaseUserRestriction(
                eq(UserManager.DISALLOW_USB_FILE_TRANSFER), any(UserHandle.class)))
                .thenReturn(false);

        final UsbBackend usbBackend = new UsbBackend(mContext, mUserManager);

        assertThat(usbBackend.areFunctionsSupported(UsbManager.FUNCTION_MTP)).isTrue();
    }

    @Test
    public void areFunctionsDisallowedByNonAdminUser_isAdminUser_returnFalse() {
        when(mUserManager.isAdminUser()).thenReturn(true);

        final UsbBackend usbBackend = new UsbBackend(mContext, mUserManager);

        assertThat(usbBackend.areFunctionsDisallowedByNonAdminUser(
                UsbManager.FUNCTION_RNDIS)).isFalse();
    }

    @Test
    public void areFunctionsDisallowedByNonAdminUser_isNotAdminUser_returnTrue() {
        when(mUserManager.isAdminUser()).thenReturn(false);

        final UsbBackend usbBackend = new UsbBackend(mContext, mUserManager);

        assertThat(usbBackend.areFunctionsDisallowedByNonAdminUser(
                UsbManager.FUNCTION_RNDIS)).isTrue();
    }

    @Test
    public void maybeGetUserRestriction_functionMtp_returnsFileTransferRestriction() {
        final UsbBackend usbBackend = new UsbBackend(mContext, mUserManager);

        assertThat(usbBackend.maybeGetUserRestriction(UsbManager.FUNCTION_MTP)).isEqualTo(
                UserManager.DISALLOW_USB_FILE_TRANSFER);
    }

    @Test
    public void maybeGetUserRestriction_functionPtp_returnsFileTransferRestriction() {
        final UsbBackend usbBackend = new UsbBackend(mContext, mUserManager);

        assertThat(usbBackend.maybeGetUserRestriction(UsbManager.FUNCTION_PTP)).isEqualTo(
                UserManager.DISALLOW_USB_FILE_TRANSFER);
    }

    @Test
    public void maybeGetUserRestriction_functionRndis_returnsTetheringRestriction() {
        final UsbBackend usbBackend = new UsbBackend(mContext, mUserManager);

        assertThat(usbBackend.maybeGetUserRestriction(UsbManager.FUNCTION_RNDIS)).isEqualTo(
                UserManager.DISALLOW_CONFIG_TETHERING);
    }

    @Test
    public void maybeGetUserRestriction_functionUvc_returnsNull() {
        final UsbBackend usbBackend = new UsbBackend(mContext, mUserManager);

        assertThat(usbBackend.maybeGetUserRestriction(UsbManager.FUNCTION_UVC)).isNull();
    }

    private UsbDevice createUsbDevice(
            int vendorId,
            int productId,
            @Nullable String manufactureName,
            @Nullable String productName) {
        return new UsbDevice.Builder(
                        /* name= */ "name",
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
    public void getDeviceName_productNameAvailable_returnsProductName() {
        final UsbBackend usbBackend = new UsbBackend(mContext, mUserManager);
        final UsbDevice usbDevice =
                createUsbDevice(
                        /* vendorId= */ 10,
                        /* productId= */ 11,
                        /* manufactureName= */ null,
                        /* productName= */ "Product Name");

        assertThat(usbBackend.getDeviceName(usbDevice)).isEqualTo(usbDevice.getProductName());
    }

    @Test
    public void getDeviceName_onlyManufacturerAvailable_returnsManufacturerString() {
        final UsbBackend usbBackend = new UsbBackend(mContext, mUserManager);
        final UsbDevice usbDevice =
                createUsbDevice(
                        /* vendorId= */ 10,
                        /* productId= */ 11,
                        /* manufactureName= */ "Manufacture Name",
                        /* productName= */ null);

        final String expected =
                mContext.getString(
                        R.string.usb_device_name_unknown_with_manufacturer_name,
                        usbDevice.getManufacturerName());

        assertThat(usbBackend.getDeviceName(usbDevice)).isEqualTo(expected);
    }

    @Test
    public void getDeviceName_noNamesAvailable_returnsVendorProductIds() {
        final UsbBackend usbBackend = new UsbBackend(mContext, mUserManager);
        final UsbDevice usbDevice =
                createUsbDevice(
                        /* vendorId= */ 30,
                        /* productId= */ 31,
                        /* manufactureName= */ null,
                        /* productName= */ null);

        final String expected =
                mContext.getString(
                        R.string.usb_device_name_unknown_with_vendor_id_and_product_id,
                        usbDevice.getVendorId(),
                        usbDevice.getProductId());

        assertThat(usbBackend.getDeviceName(usbDevice)).isEqualTo(expected);
    }
}
