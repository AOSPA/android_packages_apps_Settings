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

package com.android.settings.datetime

import android.content.Context
import android.provider.Settings
import androidx.preference.PreferenceScreen
import com.android.settingslib.widget.SelectorWithWidgetPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class TimeFormatOptionsControllerTest {

    @Mock private lateinit var mScreen: PreferenceScreen
    @Mock private lateinit var mAutoPref: SelectorWithWidgetPreference
    @Mock private lateinit var m12HourPref: SelectorWithWidgetPreference
    @Mock private lateinit var m24HourPref: SelectorWithWidgetPreference

    private lateinit var mContext: Context
    private lateinit var mController: TimeFormatOptionsController

    @Mock private lateinit var mPreferenceManager: androidx.preference.PreferenceManager

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        mContext = RuntimeEnvironment.application
        mController = TimeFormatOptionsController(mContext, "time_format_automatic")

        `when`(mScreen.findPreference<SelectorWithWidgetPreference>("time_format_automatic"))
            .thenReturn(mAutoPref)
        `when`(mScreen.findPreference<SelectorWithWidgetPreference>("time_format_12_hour"))
            .thenReturn(m12HourPref)
        `when`(mScreen.findPreference<SelectorWithWidgetPreference>("time_format_24_hour"))
            .thenReturn(m24HourPref)

        `when`(mAutoPref.key).thenReturn("time_format_automatic")
        `when`(m12HourPref.key).thenReturn("time_format_12_hour")
        `when`(m24HourPref.key).thenReturn("time_format_24_hour")

        `when`(mAutoPref.preferenceManager).thenReturn(mPreferenceManager)
        `when`(m12HourPref.preferenceManager).thenReturn(mPreferenceManager)
        `when`(m24HourPref.preferenceManager).thenReturn(mPreferenceManager)
        `when`(mPreferenceManager.preferenceScreen).thenReturn(mScreen)
    }

    @Test
    fun isAvailable_shouldReturnTrue() {
        assertThat(mController.isAvailable).isTrue()
    }

    @Test
    fun updateState_autoSet_shouldCheckAuto() {
        Settings.System.putString(mContext.contentResolver, Settings.System.TIME_12_24, null)
        mController.updateState(mAutoPref)
        verify(mAutoPref).isChecked = true

        mController.updateState(m12HourPref)
        verify(m12HourPref).isChecked = false

        mController.updateState(m24HourPref)
        verify(m24HourPref).isChecked = false
    }

    @Test
    fun updateState_12HourSet_shouldCheck12Hour() {
        Settings.System.putString(mContext.contentResolver, Settings.System.TIME_12_24, "12")
        mController.updateState(mAutoPref)
        verify(mAutoPref).isChecked = false

        mController.updateState(m12HourPref)
        verify(m12HourPref).isChecked = true

        mController.updateState(m24HourPref)
        verify(m24HourPref).isChecked = false
    }

    @Test
    fun updateState_24HourSet_shouldCheck24Hour() {
        Settings.System.putString(mContext.contentResolver, Settings.System.TIME_12_24, "24")
        mController.updateState(mAutoPref)
        verify(mAutoPref).isChecked = false

        mController.updateState(m12HourPref)
        verify(m12HourPref).isChecked = false

        mController.updateState(m24HourPref)
        verify(m24HourPref).isChecked = true
    }

    @Test
    fun handlePreferenceTreeClick_autoClicked_shouldUpdateSettings() {
        mController = TimeFormatOptionsController(mContext, "time_format_automatic")
        mController.handlePreferenceTreeClick(mAutoPref)

        val value = Settings.System.getString(mContext.contentResolver, Settings.System.TIME_12_24)
        assertThat(value).isNull()
        verify(mAutoPref).isChecked = true
        verify(m12HourPref).isChecked = false
        verify(m24HourPref).isChecked = false
    }

    @Test
    fun handlePreferenceTreeClick_12HourClicked_shouldUpdateSettings() {
        mController = TimeFormatOptionsController(mContext, "time_format_12_hour")
        mController.handlePreferenceTreeClick(m12HourPref)

        val value = Settings.System.getString(mContext.contentResolver, Settings.System.TIME_12_24)
        assertThat(value).isEqualTo("12")
        verify(mAutoPref).isChecked = false
        verify(m12HourPref).isChecked = true
        verify(m24HourPref).isChecked = false
    }

    @Test
    fun handlePreferenceTreeClick_24HourClicked_shouldUpdateSettings() {
        mController = TimeFormatOptionsController(mContext, "time_format_24_hour")
        mController.handlePreferenceTreeClick(m24HourPref)

        val value = Settings.System.getString(mContext.contentResolver, Settings.System.TIME_12_24)
        assertThat(value).isEqualTo("24")
        verify(mAutoPref).isChecked = false
        verify(m12HourPref).isChecked = false
        verify(m24HourPref).isChecked = true
    }
}
