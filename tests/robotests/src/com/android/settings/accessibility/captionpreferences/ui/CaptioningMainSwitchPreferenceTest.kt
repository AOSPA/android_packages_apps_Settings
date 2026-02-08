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

package com.android.settings.accessibility.captionpreferences.ui

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.MainSwitchPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowLooper

/** Tests for [CaptioningMainSwitchPreference]. */
@RunWith(AndroidJUnit4::class)
class CaptioningMainSwitchPreferenceTest {

    @get:Rule val settingsStoreRule = SettingsStoreRule()

    private lateinit var context: Context
    private lateinit var preference: CaptioningMainSwitchPreference
    private lateinit var settingsSecureStore: SettingsSecureStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        settingsSecureStore = SettingsSecureStore.get(context)
        preference = CaptioningMainSwitchPreference()
    }

    @Test
    fun key_returnsCorrectValue() {
        assertThat(preference.key).isEqualTo(Settings.Secure.ACCESSIBILITY_CAPTIONING_ENABLED)
    }

    @Test
    fun purpose_returnsCorrectValue() {
        assertThat(preference.purpose).isEqualTo(R.string.caption_preferences_main_switch_purpose)
    }

    @Test
    fun title_returnsCorrectValue() {
        assertThat(preference.title)
            .isEqualTo(R.string.accessibility_captioning_primary_switch_title)
    }

    @Test
    fun storage_returnsSettingsSecureStore() {
        assertThat(preference.storage(context)).isInstanceOf(SettingsSecureStore::class.java)
    }

    @Test
    fun bindWidget_featureOn_toggleIsChecked() {
        setCaptioningEnabled(true)
        val widget = preference.createAndBindWidget<MainSwitchPreference>(context)
        assertThat(widget.isChecked).isTrue()
    }

    @Test
    fun bindWidget_featureOff_toggleIsNotChecked() {
        setCaptioningEnabled(false)
        val widget = preference.createAndBindWidget<MainSwitchPreference>(context)
        assertThat(widget.isChecked).isFalse()
    }

    @Test
    fun performClick_featureOff_turnsFeatureOn() {
        setCaptioningEnabled(false)
        val widget = preference.createAndBindWidget<MainSwitchPreference>(context)

        widget.performClick()

        assertThat(isCaptioningEnabled()).isTrue()
    }

    @Test
    fun performClick_featureOn_turnsFeatureOff() {
        setCaptioningEnabled(true)
        val widget = preference.createAndBindWidget<MainSwitchPreference>(context)

        widget.performClick()

        assertThat(isCaptioningEnabled()).isFalse()
    }

    private fun setCaptioningEnabled(enabled: Boolean) {
        settingsSecureStore.setBoolean(Settings.Secure.ACCESSIBILITY_CAPTIONING_ENABLED, enabled)

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    }

    private fun isCaptioningEnabled(): Boolean {
        return settingsSecureStore.getBoolean(Settings.Secure.ACCESSIBILITY_CAPTIONING_ENABLED)
            ?: false
    }
}
