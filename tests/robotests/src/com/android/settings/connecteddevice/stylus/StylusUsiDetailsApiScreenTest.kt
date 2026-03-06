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

package com.android.settings.connecteddevice.stylus

import android.app.Application
import android.content.Context
import android.hardware.input.InputManager
import android.os.Bundle
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.view.InputDevice
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.flags.Flags.FLAG_CATALYST_MIGRATION_26Q2
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.Parameters
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.kotlin.mock
import org.robolectric.shadow.api.Shadow.extract
import org.robolectric.shadows.ShadowContextImpl

@RunWith(AndroidJUnit4::class)
class StylusUsiDetailsApiScreenTest {

    @get:Rule
    val setFlagsRule = SetFlagsRule()

    private val tester = ApiTester(StylusUsiDetailsApiScreen())
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val mockInputManager = mock<InputManager>()
    private val mockInputDevice = mock<InputDevice>()

    @Before
    fun setUp() {
        val shadowContext = extract<ShadowContextImpl>((context as Application).baseContext)
        shadowContext.setSystemService(Context.INPUT_SERVICE, mockInputManager)
    }

    private fun setupMockStylus(id: Int, isStylus: Boolean) {
        `when`(mockInputManager.inputDeviceIds).thenReturn(intArrayOf(id))
        `when`(mockInputManager.getInputDevice(id)).thenReturn(mockInputDevice)
        `when`(mockInputDevice.supportsSource(InputDevice.SOURCE_STYLUS)).thenReturn(isStylus)
        if (isStylus) {
            `when`(mockInputDevice.name).thenReturn("Fake USI Stylus")
        }
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_isNotNull() {
        assertThat(tester.getScreen()).isNotNull()
    }

    @Test
    @DisableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagDisabled_isNull() {
        assertThat(tester.getScreen()).isNull()
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun prepareScreenExtras_convertsStringIdToInt() {
        setupMockStylus(id = 42, isStylus = true)
        tester.initializeScreenParameters(Parameters(StylusUsiDetailsApiScreen.PARAM_KEY to "42"))

        val extras: Bundle = tester.getLaunchScreenExtras()
        assertThat(extras.getInt(StylusUsiDetailsApiScreen.PARAM_KEY)).isEqualTo(42)
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun parameterGeneration_includesConnectedStylus() {
        setupMockStylus(id = 42, isStylus = true)

        val options = tester.getParameterOptions(StylusUsiDetailsApiScreen.PARAM_KEY)

        // The API tester returns the values as strings, not the Pair
        assertThat(options).containsExactly("42")
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun parameterGeneration_ignoresNonStylusDevices() {
        setupMockStylus(id = 99, isStylus = false)

        val options = tester.getParameterOptions(StylusUsiDetailsApiScreen.PARAM_KEY)

        assertThat(options).isEmpty()
    }
}