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

package com.android.settings.applications.specialaccess

import android.content.Context
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.core.BasePreferenceController
import com.android.settings.spa.app.specialaccess.HidAccessPreferenceController
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule

@RunWith(AndroidJUnit4::class)
class HidAccessPreferenceControllerTest {

    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @get:Rule val setFlagsRule = SetFlagsRule()

    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @Mock private lateinit var context: Context
    private lateinit var controller: HidAccessPreferenceController

    @Before
    fun setUp() {
        controller = HidAccessPreferenceController(context, "test_key")
    }

    @Test
    @EnableFlags(com.android.hardware.input.Flags.FLAG_HID_API)
    fun getAvailabilityStatus_flagEnabled_isAvailable() {
        assertThat(controller.availabilityStatus).isEqualTo(BasePreferenceController.AVAILABLE)
    }

    @Test
    @DisableFlags(com.android.hardware.input.Flags.FLAG_HID_API)
    fun getAvailabilityStatus_flagDisabled_isUnsupported() {
        assertThat(controller.availabilityStatus)
            .isEqualTo(BasePreferenceController.CONDITIONALLY_UNAVAILABLE)
    }
}
