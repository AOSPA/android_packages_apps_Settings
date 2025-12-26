/*
 * Copyright 2025 The Android Open Source Project
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

package com.android.settings.applications.specialaccess.screenautomation

import android.content.Context
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import android.platform.test.flag.junit.SetFlagsRule
import androidx.preference.Preference
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.SettingsActivityUtil
import com.android.settings.core.BasePreferenceController
import com.android.settings.spa.SpaActivity.Companion.startSpaActivity
import com.android.settings.spa.app.specialaccess.ComputerControlAppInfoPageProvider
import com.android.settings.spa.app.specialaccess.ComputerControlAutomationAppListProvider
import com.android.settings.spa.app.specialaccess.ComputerControlAutomationPreferenceController
import com.google.common.truth.Truth.assertThat
import java.lang.reflect.Field
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class ComputerControlAutomationPreferenceControllerTest {

    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @get:Rule val setFlagsRule = SetFlagsRule()

    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @Mock private lateinit var context: Context
    private lateinit var controller: ComputerControlAutomationPreferenceController

    @Before
    fun setUp() {
        controller = ComputerControlAutomationPreferenceController(context, "test_key")
    }

    @Test
    @RequiresFlagsEnabled(android.companion.virtualdevice.flags.Flags.FLAG_COMPUTER_CONTROL_ACCESS)
    fun computerControlFlagEnabled_mapContainsComputerControl() {
        val field: Field =
            SettingsActivityUtil::class
                .java
                .getDeclaredField("FRAGMENT_TO_SPA_APP_DESTINATION_PREFIX_MAP")
        field.isAccessible = true
        val fragmentMap = field.get(null) as Map<*, *>
        assertThat(fragmentMap).containsKey(ComputerControlAppInfoPageProvider::class.qualifiedName)
    }

    @Test
    @EnableFlags(android.companion.virtualdevice.flags.Flags.FLAG_COMPUTER_CONTROL_ACCESS)
    fun getAvailabilityStatus_flagEnabled_isAvailable() {
        assertThat(controller.availabilityStatus).isEqualTo(BasePreferenceController.AVAILABLE)
    }

    @Test
    @DisableFlags(android.companion.virtualdevice.flags.Flags.FLAG_COMPUTER_CONTROL_ACCESS)
    fun getAvailabilityStatus_flagDisabled_isUnsupported() {
        assertThat(controller.availabilityStatus)
            .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE)
    }

    @Test
    fun handlePreferenceTreeClick_correctKey_startsSpaActivityAndReturnsTrue() {
        val preference = mock(Preference::class.java)
        whenever(preference.key).thenReturn("test_key")

        val handled = controller.handlePreferenceTreeClick(preference)

        assertThat(handled).isTrue()
        verify(context).startSpaActivity(ComputerControlAutomationAppListProvider.getAppListRoute())
    }

    @Test
    fun handlePreferenceTreeClick_incorrectKey_doesNothingAndReturnsFalse() {
        val preference = mock(Preference::class.java)
        whenever(preference.key).thenReturn("incorrect_key")

        val handled = controller.handlePreferenceTreeClick(preference)

        assertThat(handled).isFalse()
    }
}
