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

package com.android.settings.device

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.settings.core.BasePreferenceController
import com.android.settings.testutils.shadow.ShadowDesktopSettingsUtils
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowDesktopSettingsUtils::class])
class TopLevelDevicePreferenceControllerTest {

    private lateinit var context: Context
    private lateinit var controller: TopLevelDevicePreferenceController

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        controller = TopLevelDevicePreferenceController(context, "top_level_device")
    }

    @Test
    fun getAvailabilityStatus_shouldShow_isAvailable() {
        ShadowDesktopSettingsUtils.setShouldShow(true)

        assertThat(controller.availabilityStatus).isEqualTo(BasePreferenceController.AVAILABLE)
    }

    @Test
    fun getAvailabilityStatus_shouldNotShow_isUnsupported() {
        ShadowDesktopSettingsUtils.setShouldShow(false)

        assertThat(controller.availabilityStatus)
            .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE)
    }
}
