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
import com.android.settings.accessibility.CaptioningAppearanceFragment
import com.android.settings.accessibility.Flags
import com.android.settings.accessibility.captionpreferences.data.CaptionFontSizeDataStore
import com.android.settings.accessibility.captionpreferences.data.CaptionStyleDataStore
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.shadows.ShadowLooper

/** Test for [CaptioningAppearanceScreen] */
class CaptioningAppearanceScreenTest : SettingsCatalystTestCase() {
    @get:Rule val settingsStoreRule = SettingsStoreRule()

    override val flagName: String
        get() = Flags.FLAG_CATALYST_CAPTION_PREFERENCES_SCREEN

    override val preferenceScreenCreator = CaptioningAppearanceScreen(appContext)

    @Test
    fun key_returnsCorrectValue() {
        assertThat(preferenceScreenCreator.key).isEqualTo(CaptioningAppearanceScreen.KEY)
    }

    @Test
    fun title_returnsCorrectValue() {
        assertThat(preferenceScreenCreator.title).isEqualTo(R.string.captioning_appearance_title)
    }

    @Test
    fun indexable_returnsCorrectValue() {
        assertThat(preferenceScreenCreator.indexable).isTrue()
    }

    @Test
    fun purpose_returnsCorrectValue() {
        assertThat(preferenceScreenCreator.purpose)
            .isEqualTo(R.string.caption_preferences_appearance_purpose)
    }

    @Test
    fun highlightMenuKey_returnsCorrectValue() {
        assertThat(preferenceScreenCreator.highlightMenuKey)
            .isEqualTo(R.string.menu_key_accessibility)
    }

    @Test
    fun fragmentClass_returnsCorrectValue() {
        assertThat(preferenceScreenCreator.fragmentClass())
            .isEqualTo(CaptioningAppearanceFragment::class.java)
    }

    @Test
    fun getMetricsCategory_returnsCorrectValue() {
        assertThat(preferenceScreenCreator.getMetricsCategory())
            .isEqualTo(SettingsEnums.ACCESSIBILITY_CAPTION_APPEARANCE)
    }

    @Test
    fun getSummary_returnsCorrectValue() {
        val fontSizeValue =
            appContext.resources
                .getStringArray(R.array.captioning_font_size_selector_values)[0]
                .toFloat()
        val fontSizeLabel =
            appContext.resources.getStringArray(R.array.captioning_font_size_selector_titles)[0]
        val captionStyleValue =
            appContext.resources.getIntArray(R.array.captioning_preset_selector_values)[0]
        val captionStyleLabel =
            appContext.resources.getStringArray(R.array.captioning_preset_selector_titles)[0]

        SettingsSecureStore.get(appContext).apply {
            setInt(Settings.Secure.ACCESSIBILITY_CAPTIONING_PRESET, captionStyleValue)
            setFloat(Settings.Secure.ACCESSIBILITY_CAPTIONING_FONT_SCALE, fontSizeValue)
        }

        assertThat(preferenceScreenCreator.getSummary(appContext).toString())
            .isEqualTo("$fontSizeLabel / $captionStyleLabel")
    }

    @Test
    fun onCreate_asEntryPoint_settingChanges_notifiesPreferenceChange() {
        val mockLifecycleContext =
            mock<PreferenceLifecycleContext> {
                on { preferenceScreenKey } doReturn "some_other_key"
            }

        preferenceScreenCreator.onCreate(mockLifecycleContext)
        SettingsSecureStore.get(appContext).apply {
            setInt(
                Settings.Secure.ACCESSIBILITY_CAPTIONING_PRESET,
                CaptionStyleDataStore.DEFAULT_STYLE,
            )
            setFloat(
                Settings.Secure.ACCESSIBILITY_CAPTIONING_FONT_SCALE,
                CaptionFontSizeDataStore.DEFAULT_CAPTION_SIZE,
            )
        }
        ShadowLooper.idleMainLooper()

        verify(mockLifecycleContext, times(2))
            .notifyPreferenceChange(preferenceScreenCreator.bindingKey)
    }

    @Test
    fun onCreate_asContainer_settingChanges_doesNotNotifyPreferenceChange() {
        val mockLifecycleContext =
            mock<PreferenceLifecycleContext> {
                on { preferenceScreenKey } doReturn preferenceScreenCreator.key
            }

        preferenceScreenCreator.onCreate(mockLifecycleContext)
        SettingsSecureStore.get(appContext).apply {
            setInt(Settings.Secure.ACCESSIBILITY_CAPTIONING_PRESET, 4)
            setFloat(Settings.Secure.ACCESSIBILITY_CAPTIONING_FONT_SCALE, 1.5f)
        }
        ShadowLooper.idleMainLooper()

        verify(mockLifecycleContext, never()).notifyPreferenceChange(any())
    }

    @Test
    fun onDestroy_removesObserver() {
        val mockLifecycleContext =
            mock<PreferenceLifecycleContext> {
                on { preferenceScreenKey } doReturn "some_other_key"
            }
        preferenceScreenCreator.onCreate(mockLifecycleContext)

        // Change setting once to confirm observer is active.
        SettingsSecureStore.get(appContext).apply {
            setInt(Settings.Secure.ACCESSIBILITY_CAPTIONING_PRESET, 4)
            setFloat(Settings.Secure.ACCESSIBILITY_CAPTIONING_FONT_SCALE, 1.5f)
        }
        ShadowLooper.idleMainLooper()
        verify(mockLifecycleContext, times(2))
            .notifyPreferenceChange(preferenceScreenCreator.bindingKey)

        preferenceScreenCreator.onDestroy(mockLifecycleContext)
        clearInvocations(mockLifecycleContext)

        // Change setting again. Observer should be removed, so no notification.
        SettingsSecureStore.get(appContext).apply {
            setInt(
                Settings.Secure.ACCESSIBILITY_CAPTIONING_PRESET,
                CaptionStyleDataStore.DEFAULT_STYLE,
            )
            setFloat(
                Settings.Secure.ACCESSIBILITY_CAPTIONING_FONT_SCALE,
                CaptionFontSizeDataStore.DEFAULT_CAPTION_SIZE,
            )
        }
        ShadowLooper.idleMainLooper()

        verify(mockLifecycleContext, never())
            .notifyPreferenceChange(preferenceScreenCreator.bindingKey)
    }

    override fun migration() {
        // Because we're planning to do full migration (not a hybrid migration),
        // temporarily disable the migration test until the full migration is done
    }
}
