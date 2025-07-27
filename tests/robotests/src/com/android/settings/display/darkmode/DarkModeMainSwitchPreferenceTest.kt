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

package com.android.settings.display.darkmode

import android.app.UiModeManager
import android.app.UiModeManager.MODE_NIGHT_AUTO
import android.app.UiModeManager.MODE_NIGHT_CUSTOM
import android.app.UiModeManager.MODE_NIGHT_NO
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settingslib.notification.modes.ZenMode
import com.android.settingslib.notification.modes.ZenModesBackend
import com.google.common.truth.Truth.assertThat
import java.time.LocalTime
import java.util.Locale
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.eq
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DarkModeMainSwitchPreferenceTest {
    private val mockRes = mock<Resources>()
    private val mockUiModeManager = mock<UiModeManager>()
    private val mockZenModesBackend = mock<ZenModesBackend>()
    private val context =
        spy(ApplicationProvider.getApplicationContext<Context>()) {
            on { resources } doReturn mockRes
            on { getSystemService(UiModeManager::class.java) } doReturn mockUiModeManager
        }
    private val mainSwitchPreference = DarkModeMainSwitchPreference(DarkModeStorage(context))
    private val mConfigNightYes = Configuration()
    private val mConfigNightNo = Configuration()
    private val mLocal = Locale("ENG")

    @Before
    fun setUp() {
        mConfigNightNo.uiMode = Configuration.UI_MODE_NIGHT_NO
        mConfigNightYes.uiMode = Configuration.UI_MODE_NIGHT_YES
        mConfigNightNo.locale = mLocal
        mConfigNightYes.locale = mLocal

        context.stub {
            on { getString(R.string.dark_ui_activation_off_auto) } doReturn "off_auto"
            on { getString(R.string.dark_ui_activation_on_auto) } doReturn "on_auto"
            on { getString(R.string.dark_ui_activation_off_manual) } doReturn "off_manual"
            on { getString(R.string.dark_ui_activation_on_manual) } doReturn "on_manual"
            on { getString(R.string.dark_ui_summary_off_auto_mode_auto) } doReturn
                "summary_off_auto"
            on { getString(R.string.dark_ui_summary_on_auto_mode_auto) } doReturn "summary_on_auto"
            on { getString(R.string.dark_ui_summary_off_auto_mode_never) } doReturn
                "summary_off_manual"
            on { getString(R.string.dark_ui_summary_on_auto_mode_never) } doReturn
                "summary_on_manual"
            on { getString(eq(R.string.dark_ui_summary_on_auto_mode_custom), any()) } doReturn
                "summary_on_custom"
            on { getString(eq(R.string.dark_ui_summary_off_auto_mode_custom), any()) } doReturn
                "summary_off_custom"
            on { getString(R.string.dark_ui_summary_on_auto_mode_custom_bedtime) } doReturn
                "summary_on_custom_bedtime"
            on { getString(R.string.dark_ui_summary_off_auto_mode_custom_bedtime) } doReturn
                "summary_off_custom_bedtime"
        }

        ZenModesBackend.setInstance(mockZenModesBackend)
        mockZenModesBackend.stub { on { modes } doReturn mutableListOf<ZenMode?>() }
    }

    @Test
    fun key() {
        assertThat(mainSwitchPreference.key).isEqualTo(DarkModeMainSwitchPreference.KEY)
    }

    @Test
    fun isIndexable() {
        assertThat(mainSwitchPreference.isIndexable(context)).isFalse()
    }

    @Test
    fun getTitle() {
        assertThat(mainSwitchPreference.title).isEqualTo(R.string.dark_theme_main_switch_title)
    }

    @Test
    fun getSummary_darkModeOn() {
        mockUiModeManager.stub { on { nightMode } doReturn MODE_NIGHT_NO }
        mockRes.stub { on { configuration } doReturn mConfigNightYes }

        assertThat(mainSwitchPreference.getSummary(context)).isEqualTo("summary_on_manual")
    }

    @Test
    fun getSummary_darkModeOff() {
        mockUiModeManager.stub { on { nightMode } doReturn MODE_NIGHT_NO }
        mockRes.stub { on { configuration } doReturn mConfigNightNo }
        assertThat(mainSwitchPreference.getSummary(context)).isEqualTo("summary_off_manual")
    }

    @Test
    fun getSummary_nightModeCustom_darkModeOn() {
        mockUiModeManager.stub {
            on { nightMode } doReturn MODE_NIGHT_CUSTOM
            on { customNightModeStart } doReturn LocalTime.of(10, 0, 0, 0)
            on { customNightModeEnd } doReturn LocalTime.of(12, 0, 0, 0)
        }
        mockRes.stub { on { configuration } doReturn mConfigNightYes }

        assertThat(mainSwitchPreference.getSummary(context)).isEqualTo("summary_on_custom")
    }

    @Test
    fun getSummary_nightModeCustom_darkModeOff() {
        mockUiModeManager.stub {
            on { nightMode } doReturn MODE_NIGHT_CUSTOM
            on { customNightModeStart } doReturn LocalTime.of(10, 0, 0, 0)
            on { customNightModeEnd } doReturn LocalTime.of(12, 0, 0, 0)
        }
        mockRes.stub { on { configuration } doReturn mConfigNightNo }

        assertThat(mainSwitchPreference.getSummary(context)).isEqualTo("summary_off_custom")
    }

    @Test
    fun getSummary_nightModeAuto_darkModeOn() {
        mockUiModeManager.stub { on { nightMode } doReturn MODE_NIGHT_AUTO }
        mockRes.stub { on { configuration } doReturn mConfigNightYes }

        assertThat(mainSwitchPreference.getSummary(context)).isEqualTo("summary_on_auto")
    }

    @Test
    fun getSummary_nightModeAuto_darkModeOff() {
        mockUiModeManager.stub { on { nightMode } doReturn MODE_NIGHT_AUTO }
        mockRes.stub { on { configuration } doReturn mConfigNightNo }

        assertThat(mainSwitchPreference.getSummary(context)).isEqualTo("summary_off_auto")
    }
}
