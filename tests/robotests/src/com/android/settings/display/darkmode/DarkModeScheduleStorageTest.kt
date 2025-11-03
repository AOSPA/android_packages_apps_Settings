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
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.location.Location
import android.location.LocationManager
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.testutils.BedtimeSettingsUtils
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Test for [DarkModeScheduleStorage] */
@Config(shadows = [SettingsShadowResources::class])
@RunWith(RobolectricTestRunner::class)
class DarkModeScheduleStorageTest {
    private val mockUiModeManager = mock<UiModeManager>()
    private val mockLocationManager = mock<LocationManager>()
    private val context =
        spy(ApplicationProvider.getApplicationContext<Context>()) {
            on { getSystemService(UiModeManager::class.java) } doReturn mockUiModeManager
            on { getSystemService(LocationManager::class.java) } doReturn mockLocationManager
        }
    private val configNightNo = Configuration()
    private val configNightYes = Configuration()
    private val bedtimeActivityInfo = ActivityInfo()
    private val bedtimeSettingsUtils: BedtimeSettingsUtils = BedtimeSettingsUtils(context)
    private val dataStore = DarkModeScheduleStorage(context)
    private val KEY = "test_key"

    @Before
    fun setUp() {
        configNightNo.uiMode = Configuration.UI_MODE_NIGHT_NO
        configNightYes.uiMode = Configuration.UI_MODE_NIGHT_YES

        SettingsShadowResources.overrideResource(
            com.android.internal.R.string.config_systemWellbeing,
            "wellbeing",
        )
    }

    @Test
    fun contains() {
        assertThat(dataStore.contains(KEY)).isEqualTo(true)
    }

    @Test
    fun getValue_nightMode_valueIsNone() {
        mockUiModeManager.stub { on { nightMode } doReturn UiModeManager.MODE_NIGHT_YES }

        assertThat(dataStore.getString(KEY))
            .isEqualTo(context.getString(R.string.dark_ui_auto_mode_never))
    }

    @Test
    fun getValue_nightModeAuto_valueIsAuto() {
        mockUiModeManager.stub { on { nightMode } doReturn UiModeManager.MODE_NIGHT_AUTO }

        assertThat(dataStore.getString(KEY))
            .isEqualTo(context.getString(R.string.dark_ui_auto_mode_auto))
    }

    @Test
    fun getValue_nightModeCustom_valueIsCustom() {
        mockUiModeManager.stub { on { nightMode } doReturn UiModeManager.MODE_NIGHT_CUSTOM }

        assertThat(dataStore.getString(KEY))
            .isEqualTo(context.getString(R.string.dark_ui_auto_mode_custom))
    }

    @Test
    fun getValue_nightModeCustom_bedtimeNotInstalled_valueIsCustom() {
        mockUiModeManager.stub {
            on { nightMode } doReturn UiModeManager.MODE_NIGHT_CUSTOM
            on { nightModeCustomType } doReturn UiModeManager.MODE_NIGHT_CUSTOM_TYPE_BEDTIME
        }

        assertThat(dataStore.getString(KEY))
            .isEqualTo(context.getString(R.string.dark_ui_auto_mode_custom))
    }

    @Test
    fun getValue_nightModeCustom_bedtimeDisabled_valueIsCustom() {
        bedtimeSettingsUtils.installBedtimeSettings(
            "wellbeing", /* wellbeingPackage */
            false, /* enabled */
        )
        mockUiModeManager.stub {
            on { nightMode } doReturn UiModeManager.MODE_NIGHT_CUSTOM
            on { nightModeCustomType } doReturn UiModeManager.MODE_NIGHT_CUSTOM_TYPE_BEDTIME
        }

        assertThat(dataStore.getString(KEY))
            .isEqualTo(context.getString(R.string.dark_ui_auto_mode_custom))
    }

    @Test
    fun getValue_nightModeCustom_bedtimeEnabled_valueIsCustomBedtime() {
        bedtimeSettingsUtils.installBedtimeSettings(
            "wellbeing", /* wellbeingPackage */
            true, /* enabled */
        )
        mockUiModeManager.stub {
            on { nightMode } doReturn UiModeManager.MODE_NIGHT_CUSTOM
            on { nightModeCustomType } doReturn UiModeManager.MODE_NIGHT_CUSTOM_TYPE_BEDTIME
        }
        bedtimeActivityInfo.enabled = true

        assertThat(dataStore.getString(KEY))
            .isEqualTo(
                context.getString(com.android.settings.R.string.dark_ui_auto_mode_custom_bedtime)
            )
    }

    @Test
    fun setValue_isNone_configurationIsNightMode_nightModeChangeToYes() {
        context.resources.configuration.updateFrom(configNightYes)

        dataStore.setString(KEY, context.getString(R.string.dark_ui_auto_mode_never))

        verify(mockUiModeManager).nightMode = UiModeManager.MODE_NIGHT_YES
    }

    @Test
    fun setValue_isNone_configurationIsNonNightMode_nightModeChangeToNo() {
        context.resources.configuration.updateFrom(configNightNo)

        dataStore.setString(KEY, context.getString(R.string.dark_ui_auto_mode_never))

        verify(mockUiModeManager).nightMode = UiModeManager.MODE_NIGHT_NO
    }

    @Test
    fun setValue_isAuto_isLocationEnabled_nightModeChangeToAuto() {
        mockLocationManager.stub {
            on { isLocationEnabled } doReturn true
            on { lastLocation } doReturn Location("mock")
        }

        dataStore.setString(KEY, context.getString(R.string.dark_ui_auto_mode_auto))

        verify(mockUiModeManager).nightMode = UiModeManager.MODE_NIGHT_AUTO
    }

    @Test
    fun setValue_isAuto_isLocationDisabled_nightModeNonChangeToAuto() {
        mockLocationManager.stub {
            on { isLocationEnabled } doReturn false
            on { lastLocation } doReturn Location("mock")
        }

        dataStore.setString(KEY, context.getString(R.string.dark_ui_auto_mode_auto))

        verify(mockUiModeManager, never()).nightMode = UiModeManager.MODE_NIGHT_AUTO
    }

    @Test
    fun setValue_dropDownValueIsCustom_nightModeChangeToCustom() {
        dataStore.setString(KEY, context.getString(R.string.dark_ui_auto_mode_custom))

        verify(mockUiModeManager).nightMode = UiModeManager.MODE_NIGHT_CUSTOM
    }

    @Test
    fun setValue_isBedtime_nightModeCustomTypeChangeToBedtime() {
        dataStore.setString(KEY, context.getString(R.string.dark_ui_auto_mode_custom_bedtime))

        verify(mockUiModeManager).nightModeCustomType = UiModeManager.MODE_NIGHT_CUSTOM_TYPE_BEDTIME
    }
}
