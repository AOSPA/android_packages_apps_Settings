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

package com.android.settings.connecteddevice.display

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.DisplayTopology
import android.view.Display.DEFAULT_DISPLAY
import androidx.core.content.getSystemService
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.wm.shell.shared.desktopmode.FakeDesktopState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.Spy
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for [ConnectedDisplayInjector].
 *
 * This class focuses on testing the logic within the injector, particularly its interaction with
 * the Android DisplayManager service.
 */
@RunWith(AndroidJUnit4::class)
class ConnectedDisplayInjectorTest {

    @Spy private val context: Context = ApplicationProvider.getApplicationContext()
    @Mock private lateinit var mockDisplayManager: DisplayManager

    private lateinit var fakeDesktopState: FakeDesktopState

    private lateinit var injector: ConnectedDisplayInjector

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        fakeDesktopState = FakeDesktopState()
        injector =
            object : ConnectedDisplayInjector(context) {
                override val desktopState = fakeDesktopState
            }
        doReturn(mockDisplayManager).whenever(context).getSystemService(DisplayManager::class.java)
    }

    @Test
    fun getDisplayConnectionPreference_whenIdExists_returnsPreferenceFromManager() {
        val uniqueId = "test_display_id_1"
        val expectedPreference = 1

        whenever(mockDisplayManager.getExternalDisplayConnectionPreference(uniqueId))
            .thenReturn(expectedPreference)

        val result = injector.getDisplayConnectionPreference(uniqueId)

        assertEquals(
            "Should return the preference value from DisplayManager",
            expectedPreference,
            result,
        )
    }

    @Test
    fun updateDisplayConnectionPreference_withValidId_callsDisplayManager() {
        val uniqueId = "test_display_id_2"
        val newPreference = 2

        injector.updateDisplayConnectionPreference(uniqueId, newPreference)

        verify(mockDisplayManager).setExternalDisplayConnectionPreference(uniqueId, newPreference)
    }

    @Test
    fun isProjectedModeEnabled_canEnterDesktopModeAndNotSupportedOnDefaultDisplay_returnsTrue() {
        fakeDesktopState.canEnterDesktopMode = true
        fakeDesktopState.overrideDesktopModeSupportPerDisplay[DEFAULT_DISPLAY] = false

        assertTrue(injector.isProjectedModeEnabled())
    }

    @Test
    fun isProjectedModeEnabled_canEnterDesktopModeAndSupportedOnDefaultDisplay_returnsFalse() {
        fakeDesktopState.canEnterDesktopMode = true
        fakeDesktopState.overrideDesktopModeSupportPerDisplay[DEFAULT_DISPLAY] = true

        assertFalse(injector.isProjectedModeEnabled())
    }

    @Test
    fun isProjectedModeEnabled_cannotEnterDesktopMode_returnsFalse() {
        fakeDesktopState.canEnterDesktopMode = false

        assertFalse(injector.isProjectedModeEnabled())
    }

    @Test
    fun isProjectedModeEnabled_desktopStateIsNull_returnsFalse() {
        val injectorWithNullDesktopState =
            object : ConnectedDisplayInjector(context) {
                override val desktopState: FakeDesktopState?
                    get() = null
            }

        assertFalse(injectorWithNullDesktopState.isProjectedModeEnabled())
    }

    @Test
    fun displayTopology_set_catchesIllegalArgumentException() {
        // Arrange: Create a mock DisplayTopology and configure the DisplayManager to throw an
        // IllegalArgumentException when the setter is called.
        val mockTopology = Mockito.mock(DisplayTopology::class.java)
        doThrow(IllegalArgumentException("Test Exception"))
            .whenever(mockDisplayManager)
            .displayTopology = any()

        // Act: Set the displayTopology on the injector.
        // The test will fail if an exception is thrown.
        injector.displayTopology = mockTopology

        // Assert: No crash occurred, which means the exception was caught.
        // Verification of the setter call is implicit in the setup.
    }
}
