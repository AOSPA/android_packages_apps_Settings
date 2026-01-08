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

package com.android.settings.accessibility.audioadjustment.ui

import android.content.Context
import android.provider.Settings
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.datastore.SettingsSystemStore
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Test for [MonoAudioPreference]. */
@RunWith(AndroidJUnit4::class)
class MonoAudioPreferenceTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preference = MonoAudioPreference()
    private val settingsSystemStore = SettingsSystemStore.get(context)

    @Test
    fun key_matchesSettingKey() {
        assertThat(preference.key).isEqualTo(MONO_AUDIO_SETTING_KEY)
    }

    @Test
    fun purpose() {
        assertThat(preference.purpose).isEqualTo(R.string.a11y_mono_audio_setting_purpose)
    }

    @Test
    fun title() {
        assertThat(preference.title).isEqualTo(R.string.accessibility_toggle_primary_mono_title)
    }

    @Test
    fun summary() {
        assertThat(preference.summary).isEqualTo(R.string.accessibility_toggle_primary_mono_summary)
    }

    @Test
    fun createWidget_returnsSwitchPreferenceCompatWidget() {
        val widget = preference.createAndBindWidget<Preference>(context)
        assertThat(widget).isInstanceOf(SwitchPreferenceCompat::class.java)
    }

    @Test
    fun storage_returnsSettingsSystemStore() {
        assertThat(preference.storage(context)).isInstanceOf(SettingsSystemStore::class.java)
    }

    @Test
    fun bindWidget_monoAudioEnabled_isChecked() {
        settingsSystemStore.setBoolean(MONO_AUDIO_SETTING_KEY, true)

        val widget = preference.createAndBindWidget(context) as SwitchPreferenceCompat

        assertThat(widget.isChecked).isTrue()
    }

    @Test
    fun bindWidget_monoAudioDisabled_isNotChecked() {
        settingsSystemStore.setBoolean(MONO_AUDIO_SETTING_KEY, false)

        val widget = preference.createAndBindWidget(context) as SwitchPreferenceCompat

        assertThat(widget.isChecked).isFalse()
    }

    @Test
    fun widgetClicked_wasChecked_unchecksAndDisablesMonoAudio() {
        settingsSystemStore.setBoolean(MONO_AUDIO_SETTING_KEY, true)
        val widget = preference.createAndBindWidget(context) as SwitchPreferenceCompat
        assertThat(widget.isChecked).isTrue()

        widget.performClick()

        assertThat(widget.isChecked).isFalse()
        assertThat(settingsSystemStore.getBoolean(MONO_AUDIO_SETTING_KEY)).isFalse()
    }

    @Test
    fun widgetClicked_wasUnchecked_checksAndEnablesMonoAudio() {
        settingsSystemStore.setBoolean(MONO_AUDIO_SETTING_KEY, false)
        val widget = preference.createAndBindWidget(context) as SwitchPreferenceCompat
        assertThat(widget.isChecked).isFalse()

        widget.performClick()

        assertThat(widget.isChecked).isTrue()
        assertThat(settingsSystemStore.getBoolean(MONO_AUDIO_SETTING_KEY)).isTrue()
    }

    companion object {
        private const val MONO_AUDIO_SETTING_KEY = Settings.System.MASTER_MONO
    }
}
