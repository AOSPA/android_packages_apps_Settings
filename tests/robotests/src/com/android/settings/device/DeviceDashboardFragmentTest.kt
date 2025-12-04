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

import android.app.settings.SettingsEnums
import androidx.preference.Preference
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.androidx.fragment.FragmentController

@RunWith(RobolectricTestRunner::class)
class DeviceDashboardFragmentTest {

    private lateinit var fragment: DeviceDashboardFragment

    @Before
    fun setUp() {
        fragment = FragmentController.of(DeviceDashboardFragment()).create().visible().get()
    }

    @Test
    fun fragment_shouldBeCreated() {
        assertThat(fragment).isNotNull()
    }

    @Test
    fun preferenceScreen_shouldBeInflated() {
        assertThat(fragment.preferenceScreen).isNotNull()
        assertThat(fragment.preferenceScreen.title).isEqualTo(
            fragment.context?.getString(R.string.device_dashboard_title)
        )
    }

    @Test
    fun getMetricsCategory_shouldReturnCorrectEnum() {
        assertThat(fragment.getMetricsCategory()).isEqualTo(SettingsEnums.SETTINGS_DEVICE_CATEGORY)
    }

    @Test
    fun displayPreference_shouldBeConfiguredCorrectly() {
        val displayPreference = fragment.findPreference<Preference>("device_dashboard_display")
        assertThat(displayPreference).isNotNull()
        assertThat(displayPreference?.fragment).isEqualTo("com.android.settings.DisplaySettings")
        assertThat(displayPreference?.title).isEqualTo(fragment.context?.getString(R.string.display_settings))
        assertThat(displayPreference?.summary).isEqualTo(fragment.context?.getString(R.string.display_dashboard_summary))
        val icon = displayPreference?.icon
        assertThat(Shadows.shadowOf(icon).createdFromResId)
            .isEqualTo(R.drawable.ic_settings_display_filled)
    }

    @Test
    fun soundPreference_shouldBeConfiguredCorrectly() {
        val soundPreference = fragment.findPreference<Preference>("device_dashboard_sound")
        assertThat(soundPreference).isNotNull()
        assertThat(soundPreference?.fragment).isEqualTo("com.android.settings.notification.SoundSettings")
        assertThat(soundPreference?.title).isEqualTo(fragment.context?.getString(R.string.sound_settings))
        assertThat(soundPreference?.summary).isEqualTo(fragment.context?.getString(R.string.sound_dashboard_summary))
        val icon = soundPreference?.icon
        assertThat(Shadows.shadowOf(icon).createdFromResId).isEqualTo(R.drawable.ic_volume_up_filled)
    }
}
