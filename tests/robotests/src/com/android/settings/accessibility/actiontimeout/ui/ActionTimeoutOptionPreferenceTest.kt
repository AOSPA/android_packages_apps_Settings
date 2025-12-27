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

package com.android.settings.accessibility.actiontimeout.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.accessibility.actiontimeout.data.ActionTimeoutOptionDataStore
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.SelectorWithWidgetPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Test for [ActionTimeoutOptionPreference] */
@RunWith(AndroidJUnit4::class)
class ActionTimeoutOptionPreferenceTest {

    @get:Rule val settingsStoreRule = SettingsStoreRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val timeoutOptionDataStore = ActionTimeoutOptionDataStore(context)
    private val settingsSecureStore = SettingsSecureStore.get(context)

    private fun createPreference(
        key: String = "accessibility_control_timeout_10secs",
        titleRes: Int = R.string.accessibility_timeout_default,
    ): ActionTimeoutOptionPreference {
        return ActionTimeoutOptionPreference(
            key = key,
            title = titleRes,
            dataStoreProvider = { timeoutOptionDataStore },
        )
    }

    @Test
    fun purpose() {
        val preference = createPreference()
        assertThat(preference.purpose).isEqualTo(R.string.a11y_action_timeout_option_purpose)
    }

    @Test
    fun storage_returnsProvidedDataStore() {
        val preference = createPreference()
        assertThat(preference.storage(context)).isSameInstanceAs(timeoutOptionDataStore)
    }

    @Test
    fun createWidget_returnsSelectorWithWidgetPreference() {
        val preference = createPreference()
        val widget = preference.createWidget(context)
        assertThat(widget).isInstanceOf(SelectorWithWidgetPreference::class.java)
    }

    @Test
    fun bindWidget_optionSelected_isCheckedIsTrue() {
        val preferenceKey = "accessibility_control_timeout_10secs"
        val preference = createPreference(preferenceKey, R.string.accessibility_timeout_10secs)
        settingsSecureStore.setInt(
            ActionTimeoutOptionDataStore.INTERACTIVE_UI_TIMEOUT_SETTING_KEY,
            10000,
        )

        val widget = preference.createAndBindWidget(context) as SelectorWithWidgetPreference
        assertThat(widget.isChecked).isTrue()
    }

    @Test
    fun bindWidget_optionNotSelected_isCheckedIsFalse() {
        val preferenceKey = "accessibility_control_timeout_10secs"
        val preference = createPreference(preferenceKey, R.string.accessibility_timeout_10secs)
        settingsSecureStore.setInt(
            ActionTimeoutOptionDataStore.INTERACTIVE_UI_TIMEOUT_SETTING_KEY,
            30000,
        )

        val widget = preference.createAndBindWidget(context) as SelectorWithWidgetPreference
        assertThat(widget.isChecked).isFalse()
    }

    @Test
    fun onClick_setsPreferenceCheckedAndStoresValue() {
        val preferenceKey = "accessibility_control_timeout_30secs"
        val preference = createPreference(preferenceKey, R.string.accessibility_timeout_30secs)

        val widget = preference.createAndBindWidget(context) as SelectorWithWidgetPreference
        widget.performClick()

        assertThat(widget.isChecked).isTrue()
        val storedTimeout =
            settingsSecureStore.getInt(
                ActionTimeoutOptionDataStore.INTERACTIVE_UI_TIMEOUT_SETTING_KEY
            )
        assertThat(storedTimeout).isEqualTo(30000)
    }
}
