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

package com.android.settings.deviceinfo.firmwareversion

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.deviceinfo.firmwareversion.BasebandVersionPreference.Companion.BASEBAND_PROPERTY
import com.android.settings.testutils2.ApiTester
import com.android.settingslib.DeviceInfoUtils
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemProperties

@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowSystemProperties::class])
class FirmwareVersionScreenApiTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val firmwareVersionScreen = ApiTester(FirmwareVersionScreenApi())

    @Test
    fun getScreen_isNotNull() {
        assertThat(firmwareVersionScreen.getScreen()).isNotNull()
    }

    @Test
    fun getLaunchIntent_hasIntent() {
        assertThat(firmwareVersionScreen.getLaunchIntent()).isNotNull()
    }

    @Test
    fun getOsFirmwareVersionPreference_returnsReleaseOrPreviewDisplay() {
        assertThat(
                firmwareVersionScreen.get<String>(FirmwareVersionScreenApi.OS_FIRMWARE_VERSION_KEY)
            )
            .isEqualTo(Build.VERSION.RELEASE_OR_PREVIEW_DISPLAY)
    }

    @Test
    fun getOsBuildVersionPreference_returnsBuildDisplay() {
        assertThat(firmwareVersionScreen.get<String>(FirmwareVersionScreenApi.OS_BUILD_NUMBER_KEY))
            .isEqualTo(Build.DISPLAY)
    }

    @Test
    fun getBasebandVersionPreference_returnsCorrectValue() {
        val testBaseband = "test_baseband_version"
        ShadowSystemProperties.override(BASEBAND_PROPERTY, testBaseband)

        assertThat(firmwareVersionScreen.get<String>(FirmwareVersionScreenApi.BASEBAND_VERSION_KEY))
            .isEqualTo(testBaseband)
    }

    @Test
    fun getKernelVersionPreference_returnsCorrectValue() {
        val expectedKernel = DeviceInfoUtils.getFormattedKernelVersion(context)
        assertThat(firmwareVersionScreen.get<String>(FirmwareVersionScreenApi.KERNEL_VERSION_KEY))
            .isEqualTo(expectedKernel)
    }
}
