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

import android.app.settings.SettingsEnums
import android.provider.Settings
import com.android.settings.R
import com.android.settings.accessibility.CaptioningMoreOptionsFragment
import com.android.settings.accessibility.Flags
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.android.settingslib.datastore.SettingsSecureStore
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

/** Test for [CaptioningMoreOptionsScreen] */
class CaptioningMoreOptionsScreenTest : SettingsCatalystTestCase() {
    override val flagName: String
        get() = Flags.FLAG_CATALYST_CAPTION_PREFERENCES_SCREEN

    override val preferenceScreenCreator = CaptioningMoreOptionsScreen()

    @get:Rule val settingsStoreRule = SettingsStoreRule()

    @Test
    fun key_returnsCorrectValue() {
        assertThat(preferenceScreenCreator.key).isEqualTo(CaptioningMoreOptionsScreen.KEY)
    }

    @Test
    fun title_returnsCorrectValue() {
        assertThat(preferenceScreenCreator.title).isEqualTo(R.string.captioning_more_options_title)
    }

    @Test
    fun purpose_returnsCorrectValue() {
        assertThat(preferenceScreenCreator.purpose)
            .isEqualTo(R.string.caption_preferences_more_options_screen_purpose)
    }

    @Test
    fun highlightMenuKey_returnsCorrectValue() {
        assertThat(preferenceScreenCreator.highlightMenuKey)
            .isEqualTo(R.string.menu_key_accessibility)
    }

    @Test
    fun isFlagEnabled_returnsCorrectValue() {
        assertThat(preferenceScreenCreator.isFlagEnabled(appContext))
            .isEqualTo(Flags.catalystCaptionPreferencesScreen())
    }

    @Test
    fun fragmentClass_returnsCorrectValue() {
        assertThat(preferenceScreenCreator.fragmentClass())
            .isEqualTo(CaptioningMoreOptionsFragment::class.java)
    }

    @Test
    fun getMetricsCategory_returnsCorrectValue() {
        assertThat(preferenceScreenCreator.getMetricsCategory())
            .isEqualTo(SettingsEnums.ACCESSIBILITY_CAPTION_MORE_OPTIONS)
    }

    @Test
    fun isIndexable_whenCaptioningEnabled_returnsTrue() {
        SettingsSecureStore.get(appContext)
            .setBoolean(Settings.Secure.ACCESSIBILITY_CAPTIONING_ENABLED, true)

        assertThat(preferenceScreenCreator.isIndexable(appContext)).isTrue()
    }

    @Test
    fun isIndexable_whenCaptioningDisabled_returnsFalse() {
        SettingsSecureStore.get(appContext)
            .setBoolean(Settings.Secure.ACCESSIBILITY_CAPTIONING_ENABLED, false)

        assertThat(preferenceScreenCreator.isIndexable(appContext)).isFalse()
    }

    override fun migration() {
        // Because we're planning to do full migration (not a hybrid migration),
        // temporarily disable the migration test until the full migration is done
    }
}
