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

package com.android.settings.display

import android.content.Context
import android.hardware.display.DisplayManagerGlobal
import android.view.Display
import android.view.DisplayAdjustments
import android.view.DisplayInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import android.hardware.display.DisplayManager
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

// LINT.IfChange
@RunWith(AndroidJUnit4::class)
class BrightnessLevelPreferenceTest {
    private var context = ApplicationProvider.getApplicationContext<Context>()
    private val preference = BrightnessLevelPreference()
    private val displayInfo = DisplayInfo()
    private val displayManager = mock<DisplayManager>()

    @Before
    fun setUp() {
        displayInfo.type = Display.TYPE_INTERNAL
        val displayManagerGlobal = mock<DisplayManagerGlobal>()
        val daj: DisplayAdjustments? = null
        context =
            spy(context.createDisplayContext(
                Display(displayManagerGlobal, Display.DEFAULT_DISPLAY, displayInfo, daj)
            ))
        whenever(displayManagerGlobal.getDisplayInfo(Display.DEFAULT_DISPLAY))
            .thenReturn(displayInfo)
        whenever(context.getSystemService(DisplayManager::class.java))
            .thenReturn(displayManager)
    }

    @Test
    fun isAvailable_internalDisplay() {
        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_externalDisplay() {
        displayInfo.type = Display.TYPE_EXTERNAL
        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun setValue_updatesDisplayManagerBrightness() {
        preference.storage(context).setValue(
            BrightnessLevelPreference.KEY, Int::class.javaObjectType, 75)

        verify(displayManager).setBrightness(
            eq(Display.DEFAULT_DISPLAY),
            eq(75f),
            eq(DisplayManager.BRIGHTNESS_UNIT_PERCENTAGE)
        )
    }

    @Test
    fun setValue_nonInt_doesNothing() {
        preference
            .storage(context)
            .setValue(BrightnessLevelPreference.KEY, Float::class.javaObjectType, 75f)

        verify(displayManager, never()).setBrightness(any(), any(), any())
    }
}
// LINT.ThenChange(BrightnessLevelPreferenceControllerTest.java, DisplayApiScreenTest.kt)
