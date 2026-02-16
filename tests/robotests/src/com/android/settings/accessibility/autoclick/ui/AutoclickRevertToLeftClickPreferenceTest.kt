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

package com.android.settings.accessibility.autoclick.ui

import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.preference.PreferenceManager
import androidx.preference.TwoStatePreference
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils.inflateViewHolder
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

/** Tests for [AutoclickRevertToLeftClickPreference]. */
@RunWith(RobolectricTestRunner::class)
class AutoclickRevertToLeftClickPreferenceTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val secureStore by lazy { SettingsSecureStore.get(context) }
    private lateinit var preference: AutoclickRevertToLeftClickPreference

    @Before
    fun setUp() {
        preference = AutoclickRevertToLeftClickPreference()
    }

    @Test
    fun key_returnsCorrectValue() {
        assertThat(preference.key)
            .isEqualTo(Settings.Secure.ACCESSIBILITY_AUTOCLICK_REVERT_TO_LEFT_CLICK)
    }

    @Test
    fun purpose_returnsAutoclickRevertToLeftClickPurpose() {
        assertThat(context.getString(preference.purpose))
            .isEqualTo(context.getString(R.string.a11y_autoclick_revert_to_left_click_purpose))
    }

    @Test
    fun title_returnsAutoclickRevertToLeftClickTitle() {
        assertThat(context.getString(preference.title))
            .isEqualTo(context.getString(R.string.autoclick_revert_to_left_click_title))
    }

    @Test
    fun summary_returnsAutoclickRevertToLeftClickSummary() {
        assertThat(context.getString(preference.summary))
            .isEqualTo(context.getString(R.string.autoclick_revert_to_left_click_summary))
    }

    @Test
    fun storage_returnsSettingsSecureStoreWithDefault() {
        val store = preference.storage(context)
        assertThat(store).isInstanceOf(SettingsSecureStore::class.java)
        assertThat(store.getDefaultValue(preference.key, Boolean::class.javaObjectType))
            .isEqualTo(AccessibilityManager.AUTOCLICK_REVERT_TO_LEFT_CLICK_DEFAULT)
    }

    @Test
    fun performClick_whenTrue_setsFalse() {
        secureStore.setBoolean(Settings.Secure.ACCESSIBILITY_AUTOCLICK_REVERT_TO_LEFT_CLICK, true)
        val preferenceScreen = PreferenceManager(context).createPreferenceScreen(context)
        val preferenceWidget =
            preference.createAndBindWidget<TwoStatePreference>(context, preferenceScreen)
        preferenceWidget.inflateViewHolder()

        preferenceWidget.performClick()
        ShadowLooper.idleMainLooper()

        assertThat(
                secureStore.getBoolean(Settings.Secure.ACCESSIBILITY_AUTOCLICK_REVERT_TO_LEFT_CLICK)
            )
            .isFalse()
    }

    @Test
    fun performClick_whenFalse_setsTrue() {
        secureStore.setBoolean(Settings.Secure.ACCESSIBILITY_AUTOCLICK_REVERT_TO_LEFT_CLICK, false)
        val preferenceScreen = PreferenceManager(context).createPreferenceScreen(context)
        val preferenceWidget =
            preference.createAndBindWidget<TwoStatePreference>(context, preferenceScreen)
        preferenceWidget.inflateViewHolder()

        preferenceWidget.performClick()
        ShadowLooper.idleMainLooper()

        assertThat(
                secureStore.getBoolean(Settings.Secure.ACCESSIBILITY_AUTOCLICK_REVERT_TO_LEFT_CLICK)
            )
            .isTrue()
    }
}
