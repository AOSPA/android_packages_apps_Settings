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

package com.android.settings.display

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.hardware.display.ColorDisplayManager
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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

// LINT.IfChange
@RunWith(AndroidJUnit4::class)
class NightDisplayScreenTest {
    private val mockResources = mock<Resources>()
    private val displayInfo = DisplayInfo()
    private val daj: DisplayAdjustments? = null
    private val context =
        object :
            ContextWrapper(
                ApplicationProvider.getApplicationContext<Context>()
                    .createDisplayContext(
                        Display(
                            mock<DisplayManagerGlobal>(),
                            Display.DEFAULT_DISPLAY,
                            displayInfo,
                            daj,
                        )
                    )
            ) {
            override fun getResources(): Resources = mockResources
        }

    val colorDM: ColorDisplayManager = context.getSystemService(ColorDisplayManager::class.java)

    val preferenceScreenCreator = NightDisplayScreen(context)

    @Before
    fun setup() {
        displayInfo.type = Display.TYPE_INTERNAL
    }

    @Test
    fun key() {
        assertThat(preferenceScreenCreator.key).isEqualTo(NightDisplayScreen.KEY)
    }

    @Test
    fun configuredAvailableAndNotBlocked() {
        mockResources.stub {
            on { getBoolean(NightDisplayConstants.NIGHT_DISPLAY_AVAILABLE_RES_ID) } doReturn true
            on {
                getBoolean(NightDisplayConstants.NIGHT_DISPLAY_SETTINGS_PAGE_BLOCKER_RES_ID)
            } doReturn false
        }
        assertThat(preferenceScreenCreator.isAvailable(context)).isTrue()
    }

    @Test
    fun configuredUnavailableAndNotBlocked() {
        mockResources.stub {
            on { getBoolean(NightDisplayConstants.NIGHT_DISPLAY_AVAILABLE_RES_ID) } doReturn false
            on {
                getBoolean(NightDisplayConstants.NIGHT_DISPLAY_SETTINGS_PAGE_BLOCKER_RES_ID)
            } doReturn false
        }
        assertThat(preferenceScreenCreator.isAvailable(context)).isFalse()
    }

    @Test
    fun configuredAvailableAndBlocked() {
        mockResources.stub {
            on { getBoolean(NightDisplayConstants.NIGHT_DISPLAY_AVAILABLE_RES_ID) } doReturn true
            on {
                getBoolean(NightDisplayConstants.NIGHT_DISPLAY_SETTINGS_PAGE_BLOCKER_RES_ID)
            } doReturn true
        }
        assertThat(preferenceScreenCreator.isAvailable(context)).isFalse()
    }

    @Test
    fun configuredUnavailableAndBlocked() {
        mockResources.stub {
            on { getBoolean(NightDisplayConstants.NIGHT_DISPLAY_AVAILABLE_RES_ID) } doReturn false
            on {
                getBoolean(NightDisplayConstants.NIGHT_DISPLAY_SETTINGS_PAGE_BLOCKER_RES_ID)
            } doReturn true
        }
        assertThat(preferenceScreenCreator.isAvailable(context)).isFalse()
    }

    @Test
    fun configuredAvailableAndNotBlocked_externalDisplay() {
        displayInfo.type = Display.TYPE_EXTERNAL
        mockResources.stub {
            on { getBoolean(NightDisplayConstants.NIGHT_DISPLAY_AVAILABLE_RES_ID) } doReturn true
            on {
                getBoolean(NightDisplayConstants.NIGHT_DISPLAY_SETTINGS_PAGE_BLOCKER_RES_ID)
            } doReturn false
        }
        assertThat(preferenceScreenCreator.isAvailable(context)).isFalse()
    }

    @Test
    fun toggleOn() {
        mockResources.stub {
            on { getBoolean(NightDisplayConstants.NIGHT_DISPLAY_AVAILABLE_RES_ID) } doReturn true
            on {
                getBoolean(NightDisplayConstants.NIGHT_DISPLAY_SETTINGS_PAGE_BLOCKER_RES_ID)
            } doReturn false
        }

        colorDM.isNightDisplayActivated = false
        assertThat(colorDM.isNightDisplayActivated).isFalse()

        preferenceScreenCreator.storage(context).setBoolean(NightDisplayScreen.KEY, true)
        assertThat(colorDM.isNightDisplayActivated).isTrue()
    }

    @Test
    fun toggleOff() {
        mockResources.stub {
            on { getBoolean(NightDisplayConstants.NIGHT_DISPLAY_AVAILABLE_RES_ID) } doReturn true
            on {
                getBoolean(NightDisplayConstants.NIGHT_DISPLAY_SETTINGS_PAGE_BLOCKER_RES_ID)
            } doReturn false
        }

        colorDM.isNightDisplayActivated = true
        assertThat(colorDM.isNightDisplayActivated).isTrue()

        preferenceScreenCreator.storage(context).setBoolean(NightDisplayScreen.KEY, false)
        assertThat(colorDM.isNightDisplayActivated).isFalse()
    }
}
// LINT.ThenChange(NightDisplaySettingsTest.java, NightDisplayApiScreenTest.kt)
