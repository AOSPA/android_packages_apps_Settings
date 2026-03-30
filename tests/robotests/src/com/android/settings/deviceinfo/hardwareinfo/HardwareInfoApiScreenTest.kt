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

package com.android.settings.deviceinfo.hardwareinfo

import android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE
import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.MissingPermissionException
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBuild
import org.robolectric.shadows.ShadowSystemProperties

@RunWith(AndroidJUnit4::class)
@Config
class HardwareInfoApiScreenTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val tester = ApiTester(HardwareInfoApiScreen())
    private val shadowApplication = shadowOf(context as Application)

    @Before
    fun setUp() {
        ShadowSystemProperties.reset()
        ShadowBuild.reset()
        shadowApplication.grantPermissions(READ_PRIVILEGED_PHONE_STATE)
    }

    @Test
    fun getScreen_isNotNull() {
        assertThat(tester.getScreen()).isNotNull()
    }

    @Test
    fun getLaunchIntent_hasIntent() {
        assertThat(tester.getLaunchIntent()).isNotNull()
    }

    @Test
    fun getSerialNumber_returnsValue() {
        ShadowBuild.setSerial("TESTSERIAL123")
        assertThat(tester.get<String>(HardwareInfoApiScreen.SERIAL_NUMBER_KEY))
            .isEqualTo("TESTSERIAL123")
    }

    @Test
    fun getManufacturedYear_validDate_returnsYear() {
        // Unix timestamp for 2025-10-26 00:00:00 GMT
        ShadowSystemProperties.override("ro.build.date.utc", "1761465600")
        assertThat(tester.get<String>(HardwareInfoApiScreen.MANUFACTURED_YEAR_KEY))
            .isEqualTo("2025")
    }

    @Test
    fun getManufacturedYear_anotherValidDate_returnsYear() {
        // Unix timestamp for 2023-03-15 00:00:00 GMT
        ShadowSystemProperties.override("ro.build.date.utc", "1678838400")
        assertThat(tester.get<String>(HardwareInfoApiScreen.MANUFACTURED_YEAR_KEY))
            .isEqualTo("2023")
    }

    @Test
    fun getSerialNumber_permissionDenied_throwsMissingPermissionException() {
        shadowApplication.denyPermissions(READ_PRIVILEGED_PHONE_STATE)
        ShadowBuild.setSerial("TESTSERIAL123")

        assertFailsWith<MissingPermissionException> {
            tester.get<String>(HardwareInfoApiScreen.SERIAL_NUMBER_KEY)
        }
    }
}
