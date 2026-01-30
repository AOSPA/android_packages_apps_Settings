/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.connecteddevice.usb

import android.hardware.usb.UsbManager.FUNCTION_MIDI
import android.hardware.usb.UsbManager.FUNCTION_MTP
import android.hardware.usb.UsbManager.FUNCTION_PTP
import android.hardware.usb.UsbPortStatus.DATA_ROLE_DEVICE
import android.hardware.usb.UsbPortStatus.POWER_ROLE_NONE
import android.hardware.usb.UsbPortStatus.POWER_ROLE_SINK
import android.hardware.usb.UsbPortStatus.POWER_ROLE_SOURCE
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.connecteddevice.usb.UsbDetailsApiScreen.Companion.PREFERENCE_KEY_CONVERT_VIDEOS_TO_AVC
import com.android.settings.connecteddevice.usb.UsbDetailsApiScreen.Companion.PREFERENCE_USB_DETAILS_POWER_ROLE
import com.android.settings.flags.Flags
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.FailedPreconditionException
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class UsbDetailsApiScreenTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private lateinit var tester: ApiTester
    private val mockUsbDetailsRepository = mock<UsbDetailsRepository>()
    private lateinit var usbFeatureProvider: UsbFeatureProvider

    @Before
    fun setUp() {
        usbFeatureProvider = FakeFeatureFactory.setupForTest().usbFeatureProvider
        usbFeatureProvider.stub { on { usbDetailsRepository } doReturn mockUsbDetailsRepository }
        tester = ApiTester(UsbDetailsApiScreen())
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagEnabled_isNotNull() {
        assertThat(tester.getScreen()).isNotNull()
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagDisabled_isNull() {
        assertThat(tester.getScreen()).isNull()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getBoolean_mtpEnabled_returnTrue() {
        mockUsbDetailsRepository.stub {
            on { isMtpTranscodeEnabled() } doReturn true
            on { getCurrentFunction() } doReturn FUNCTION_MTP
            on { getDataRole() } doReturn DATA_ROLE_DEVICE
        }

        assertThat(tester.get<Boolean>(PREFERENCE_KEY_CONVERT_VIDEOS_TO_AVC)).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getBoolean_mtpDisabled_returnFalse() {
        mockUsbDetailsRepository.stub {
            on { isMtpTranscodeEnabled() } doReturn false
            on { getCurrentFunction() } doReturn FUNCTION_MTP
            on { getDataRole() } doReturn DATA_ROLE_DEVICE
        }

        assertThat(tester.get<Boolean>(PREFERENCE_KEY_CONVERT_VIDEOS_TO_AVC)).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getBoolean_ptpFunctionAndMtpDisabled_returnFalse() {
        mockUsbDetailsRepository.stub {
            on { isMtpTranscodeEnabled() } doReturn false
            on { getCurrentFunction() } doReturn FUNCTION_PTP
            on { getDataRole() } doReturn DATA_ROLE_DEVICE
        }

        assertThat(tester.get<Boolean>(PREFERENCE_KEY_CONVERT_VIDEOS_TO_AVC)).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getBoolean_midiFunctionAndMtpDisabled_failedException() {
        mockUsbDetailsRepository.stub {
            on { isMtpTranscodeEnabled() } doReturn false
            on { getCurrentFunction() } doReturn FUNCTION_MIDI
            on { getDataRole() } doReturn DATA_ROLE_DEVICE
        }

        try {
            tester.get<Boolean>(PREFERENCE_KEY_CONVERT_VIDEOS_TO_AVC)
        } catch (e: FailedPreconditionException) {
            // no Crash
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun setBoolean_turnOnAvc_success() {
        mockUsbDetailsRepository.stub {
            on { getCurrentFunction() } doReturn FUNCTION_MTP
            on { getDataRole() } doReturn DATA_ROLE_DEVICE
        }

        tester.set(PREFERENCE_KEY_CONVERT_VIDEOS_TO_AVC, true)

        verify(mockUsbDetailsRepository).setMtpTranscodeState(true)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun setBoolean_turnOffAvc_success() {
        mockUsbDetailsRepository.stub {
            on { getCurrentFunction() } doReturn FUNCTION_MTP
            on { getDataRole() } doReturn DATA_ROLE_DEVICE
        }

        tester.set(PREFERENCE_KEY_CONVERT_VIDEOS_TO_AVC, false)

        verify(mockUsbDetailsRepository).setMtpTranscodeState(false)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun setBoolean_powerRoleNone_failedPreconditionExceptionAndNoCrash() {
        mockUsbDetailsRepository.stub { on { powerRole } doReturn POWER_ROLE_NONE }

        try {
            tester.get<Boolean>(PREFERENCE_USB_DETAILS_POWER_ROLE)
        } catch (e: FailedPreconditionException) {
            // no Crash
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun setBoolean_notSupportAllRoles_failedPreconditionExceptionAndNoCrash() {
        mockUsbDetailsRepository.stub { on { isSupportAllRoles } doReturn false }

        try {
            tester.get<Boolean>(PREFERENCE_USB_DETAILS_POWER_ROLE)
        } catch (e: FailedPreconditionException) {
            // no Crash
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getBoolean_allowedAndRoleIsSink_returnFalse() {
        mockUsbDetailsRepository.stub {
            on { powerRole } doReturn POWER_ROLE_SINK
            on { isSupportAllRoles } doReturn true
        }

        assertThat(tester.get<Boolean>(PREFERENCE_USB_DETAILS_POWER_ROLE)).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getBoolean_allowedAndRoleIsSource_returnTrue() {
        mockUsbDetailsRepository.stub {
            on { powerRole } doReturn POWER_ROLE_SOURCE
            on { isSupportAllRoles } doReturn true
        }

        assertThat(tester.get<Boolean>(PREFERENCE_USB_DETAILS_POWER_ROLE)).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun setBoolean_turnOffChargeConnectedDevice_success() {
        mockUsbDetailsRepository.stub {
            on { powerRole } doReturn POWER_ROLE_SOURCE
            on { isSupportAllRoles } doReturn true
        }

        tester.set(PREFERENCE_USB_DETAILS_POWER_ROLE, false)

        verify(mockUsbDetailsRepository).powerRole = POWER_ROLE_SINK
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun setBoolean_turnOnChargeConnectedDevice_success() {
        mockUsbDetailsRepository.stub {
            on { powerRole } doReturn POWER_ROLE_SINK
            on { isSupportAllRoles } doReturn true
        }

        tester.set(PREFERENCE_USB_DETAILS_POWER_ROLE, true)

        verify(mockUsbDetailsRepository).powerRole = POWER_ROLE_SOURCE
    }
}
