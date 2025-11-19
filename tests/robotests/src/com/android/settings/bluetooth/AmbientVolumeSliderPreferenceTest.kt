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

package com.android.settings.bluetooth

import android.bluetooth.AudioInputControl
import android.content.Context
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.preference.PreferenceViewHolder
import androidx.test.core.app.ApplicationProvider
import com.android.settings.testutils.inflateViewHolder
import com.android.settingslib.widget.preference.slider.R
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AmbientVolumeSliderPreferenceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var preference: AmbientVolumeSliderPreference

    @Before
    fun setUp() {
        preference = AmbientVolumeSliderPreference(context)
        preference.min = TEST_SLIDER_VALUE_MIN
        preference.max = TEST_SLIDER_VALUE_MAX
        preference.value = TEST_SLIDER_VALUE
    }

    @Test
    fun setEnabled_disabled_valueIsMin() {
        preference.setEnabled(false)

        assertThat(preference.value).isEqualTo(TEST_SLIDER_VALUE_MIN)
    }

    @Test
    fun onBindViewHolder_disabled_muteIconNotVisible() {
        preference.setMuteState(AudioInputControl.MUTE_DISABLED)

        val holder = preference.inflateViewHolder()
        assertThat(getMuteIconFrame(holder)?.isVisible).isFalse()
    }

    @Test
    fun onBindViewHolder_muted_muteIconVisibleAndValueIsMin() {
        preference.setMuteState(AudioInputControl.MUTE_MUTED)

        val holder = preference.inflateViewHolder()
        assertThat(getMuteIconFrame(holder)?.isVisible).isTrue()
        assertThat(preference.value).isEqualTo(TEST_SLIDER_VALUE_MIN)
    }

    @Test
    fun onBindViewHolder_notMuted_muteIconVisible() {
        preference.setMuteState(AudioInputControl.MUTE_NOT_MUTED)

        val holder = preference.inflateViewHolder()
        assertThat(getMuteIconFrame(holder)?.isVisible).isTrue()
    }

    private fun getMuteIconFrame(holder: PreferenceViewHolder): ViewGroup? {
        return holder.findViewById(R.id.icon_end)?.parent as? ViewGroup
    }

    companion object {
        const val TEST_SLIDER_VALUE_MIN = 0
        const val TEST_SLIDER_VALUE_MAX = 100
        const val TEST_SLIDER_VALUE = 10
    }
}
