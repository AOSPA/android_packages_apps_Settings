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
import android.provider.Settings
import android.view.InputDevice
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.flags.Flags.FLAG_CATALYST_MIGRATION_26Q2
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.FailedPreconditionException
import com.android.settings.testutils2.Parameters
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
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

    @get:Rule val setFlagsRule = SetFlagsRule()

    private val tester = ApiTester(StylusUsiDetailsApiScreen())
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val mockInputManager = mock<InputManager>()
    private val mockInputDevice = mock<InputDevice>()

    private val mockInputMethodManager = mock<InputMethodManager>()
    private val mockInputMethodInfo = mock<InputMethodInfo>()

    @Before
    fun setUp() {
        val shadowContext = extract<ShadowContextImpl>((context as Application).baseContext)
        shadowContext.setSystemService(Context.INPUT_SERVICE, mockInputManager)
        shadowContext.setSystemService(Context.INPUT_METHOD_SERVICE, mockInputMethodManager)
    }

    private fun setupMockStylus(id: Int, isStylus: Boolean) {
        `when`(mockInputManager.inputDeviceIds).thenReturn(intArrayOf(id))
        `when`(mockInputManager.getInputDevice(id)).thenReturn(mockInputDevice)
        `when`(mockInputDevice.supportsSource(InputDevice.SOURCE_STYLUS)).thenReturn(isStylus)
        if (isStylus) {
            `when`(mockInputDevice.name).thenReturn("Fake USI Stylus")
        }
    }

    private fun setupMockHandwritingSupport(supported: Boolean) {
        `when`(mockInputMethodManager.currentInputMethodInfo).thenReturn(mockInputMethodInfo)
        `when`(mockInputMethodInfo.supportsStylusHandwriting()).thenReturn(supported)

        // We also need to initialize the screen parameters for these tests,
        // because preferences on parameterized screens require the parameters to be set!
        setupMockStylus(id = 42, isStylus = true)
        tester.initializeScreenParameters(Parameters(StylusUsiDetailsApiScreen.PARAM_KEY to "42"))
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

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun handwriting_preconditionNotMet_throwsException() {
        setupMockHandwritingSupport(supported = false)

        assertFailsWith<FailedPreconditionException> {
            tester.get<Boolean>(StylusUsiDetailsApiScreen.HANDWRITING_SWITCH_KEY)
        }
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun handwriting_get_whenEnabled_returnsTrue() {
        // Arrange
        setupMockHandwritingSupport(supported = true)
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.STYLUS_HANDWRITING_ENABLED,
            StylusUsiDetailsApiScreen.STYLUS_HANDWRITING_ENABLED,
        )

        // Act & Assert
        assertThat(tester.get<Boolean>(StylusUsiDetailsApiScreen.HANDWRITING_SWITCH_KEY)).isTrue()
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun handwriting_get_whenDisabled_returnsFalse() {
        // Arrange
        setupMockHandwritingSupport(supported = true)
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.STYLUS_HANDWRITING_ENABLED,
            StylusUsiDetailsApiScreen.STYLUS_HANDWRITING_DISABLED,
        )

        // Act & Assert
        assertThat(tester.get<Boolean>(StylusUsiDetailsApiScreen.HANDWRITING_SWITCH_KEY)).isFalse()
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun handwriting_set_true_savesEnabledState() {
        // Arrange
        setupMockHandwritingSupport(supported = true)

        // Act
        tester.set(StylusUsiDetailsApiScreen.HANDWRITING_SWITCH_KEY, true)

        // Assert
        val savedValue =
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.STYLUS_HANDWRITING_ENABLED,
                -1,
            )
        assertThat(savedValue).isEqualTo(StylusUsiDetailsApiScreen.STYLUS_HANDWRITING_ENABLED)
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun handwriting_set_false_savesDisabledState() {
        // Arrange
        setupMockHandwritingSupport(supported = true)

        // Act
        tester.set(StylusUsiDetailsApiScreen.HANDWRITING_SWITCH_KEY, false)

        // Assert
        val savedValue =
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.STYLUS_HANDWRITING_ENABLED,
                -1,
            )
        assertThat(savedValue).isEqualTo(StylusUsiDetailsApiScreen.STYLUS_HANDWRITING_DISABLED)
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun ignoreButton_get_whenButtonsDisabled_returnsTrue() {
        // Arrange
        setupMockStylus(id = 42, isStylus = true)
        tester.initializeScreenParameters(Parameters(StylusUsiDetailsApiScreen.PARAM_KEY to "42"))
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.STYLUS_BUTTONS_ENABLED,
            StylusUsiDetailsApiScreen.STYLUS_BUTTONS_DISABLED,
        )

        // Act & Assert
        assertThat(tester.get<Boolean>(StylusUsiDetailsApiScreen.IGNORE_BUTTON_KEY)).isTrue()
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun ignoreButton_get_whenButtonsEnabled_returnsFalse() {
        // Arrange
        setupMockStylus(id = 42, isStylus = true)
        tester.initializeScreenParameters(Parameters(StylusUsiDetailsApiScreen.PARAM_KEY to "42"))
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.STYLUS_BUTTONS_ENABLED,
            StylusUsiDetailsApiScreen.STYLUS_BUTTONS_ENABLED,
        )

        // Act & Assert
        assertThat(tester.get<Boolean>(StylusUsiDetailsApiScreen.IGNORE_BUTTON_KEY)).isFalse()
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun ignoreButton_set_true_savesDisabledState() {
        // Arrange
        setupMockStylus(id = 42, isStylus = true)
        tester.initializeScreenParameters(Parameters(StylusUsiDetailsApiScreen.PARAM_KEY to "42"))

        // Act
        tester.set(StylusUsiDetailsApiScreen.IGNORE_BUTTON_KEY, true)

        // Assert
        val savedValue =
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.STYLUS_BUTTONS_ENABLED,
                -1,
            )
        assertThat(savedValue).isEqualTo(StylusUsiDetailsApiScreen.STYLUS_BUTTONS_DISABLED)
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun ignoreButton_set_false_savesEnabledState() {
        // Arrange
        setupMockStylus(id = 42, isStylus = true)
        tester.initializeScreenParameters(Parameters(StylusUsiDetailsApiScreen.PARAM_KEY to "42"))

        // Act
        tester.set(StylusUsiDetailsApiScreen.IGNORE_BUTTON_KEY, false)

        // Assert
        val savedValue =
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.STYLUS_BUTTONS_ENABLED,
                -1,
            )
        assertThat(savedValue).isEqualTo(StylusUsiDetailsApiScreen.STYLUS_BUTTONS_ENABLED)
    }
}
