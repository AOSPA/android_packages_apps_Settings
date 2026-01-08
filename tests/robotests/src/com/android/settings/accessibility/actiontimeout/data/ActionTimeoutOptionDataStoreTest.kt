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

package com.android.settings.accessibility.actiontimeout.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.accessibility.actiontimeout.data.ActionTimeoutOptionDataStore.Companion.INTERACTIVE_UI_TIMEOUT_SETTING_KEY
import com.android.settings.accessibility.actiontimeout.data.ActionTimeoutOptionDataStore.Companion.NON_INTERACTIVE_UI_TIMEOUT_SETTING_KEY
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.datastore.SettingsSecureStore
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Test for [ActionTimeoutOptionDataStore] */
@RunWith(AndroidJUnit4::class)
class ActionTimeoutOptionDataStoreTest {

    @get:Rule val settingsStoreRule = SettingsStoreRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dataStore = ActionTimeoutOptionDataStore(context)
    private val settingsSecureStore = SettingsSecureStore.get(context)

    @Test
    fun getValue_noValueSet_returnsDefaultTimeout() {
        settingsSecureStore.setInt(INTERACTIVE_UI_TIMEOUT_SETTING_KEY, null)

        val timeout =
            dataStore.getValue(INTERACTIVE_UI_TIMEOUT_SETTING_KEY, Int::class.javaObjectType)
        assertThat(timeout).isEqualTo(0)
    }

    @Test
    fun getValue_intValue_returnsCorrectValue() {
        settingsSecureStore.setInt(INTERACTIVE_UI_TIMEOUT_SETTING_KEY, 10000)

        val timeout =
            dataStore.getValue(INTERACTIVE_UI_TIMEOUT_SETTING_KEY, Int::class.javaObjectType)
        assertThat(timeout).isEqualTo(10000)
    }

    @Test
    fun getValue_booleanValue_correctKeyAndValue_returnsTrue() {
        settingsSecureStore.setInt(INTERACTIVE_UI_TIMEOUT_SETTING_KEY, 10000)

        val isSelected =
            dataStore.getValue(
                "accessibility_control_timeout_10secs",
                Boolean::class.javaObjectType,
            )
        assertThat(isSelected).isTrue()
    }

    @Test
    fun getValue_booleanValue_incorrectKey_returnsFalse() {
        settingsSecureStore.setInt(INTERACTIVE_UI_TIMEOUT_SETTING_KEY, 10000)

        val isSelected =
            dataStore.getValue(
                "accessibility_control_timeout_30secs",
                Boolean::class.javaObjectType,
            )
        assertThat(isSelected).isFalse()
    }

    @Test
    fun getValue_unsupportedType_returnsNull() {
        val value =
            dataStore.getValue(INTERACTIVE_UI_TIMEOUT_SETTING_KEY, String::class.javaObjectType)
        assertThat(value).isNull()
    }

    @Test
    fun setValue_booleanValue_true_setsCorrectTimeout() {
        dataStore.setValue(
            "accessibility_control_timeout_30secs",
            Boolean::class.javaObjectType,
            true,
        )

        val timeout = settingsSecureStore.getInt(INTERACTIVE_UI_TIMEOUT_SETTING_KEY)
        assertThat(timeout).isEqualTo(30000)
    }

    @Test
    fun setValue_booleanValue_false_doesNotChangeTimeout() {
        settingsSecureStore.setInt(INTERACTIVE_UI_TIMEOUT_SETTING_KEY, 10000)
        dataStore.setValue(
            "accessibility_control_timeout_30secs",
            Boolean::class.javaObjectType,
            false,
        )

        val timeout = settingsSecureStore.getInt(INTERACTIVE_UI_TIMEOUT_SETTING_KEY)
        assertThat(timeout).isEqualTo(10000)
    }

    @Test
    fun setValue_intValue_setsCorrectTimeout() {
        dataStore.setValue(INTERACTIVE_UI_TIMEOUT_SETTING_KEY, Int::class.javaObjectType, 60000)

        val timeout = settingsSecureStore.getInt(INTERACTIVE_UI_TIMEOUT_SETTING_KEY)
        assertThat(timeout).isEqualTo(60000)
        val nonInteractiveTimeout =
            settingsSecureStore.getInt(NON_INTERACTIVE_UI_TIMEOUT_SETTING_KEY)
        assertThat(nonInteractiveTimeout).isEqualTo(60000)
    }

    @Test
    fun setValue_unsupportedType_doesNotChangeTimeout() {
        settingsSecureStore.setInt(INTERACTIVE_UI_TIMEOUT_SETTING_KEY, 10000)
        dataStore.setValue(INTERACTIVE_UI_TIMEOUT_SETTING_KEY, String::class.javaObjectType, "test")

        val timeout = settingsSecureStore.getInt(INTERACTIVE_UI_TIMEOUT_SETTING_KEY)
        assertThat(timeout).isEqualTo(10000)
    }

    @Test
    fun contains_keyExists_returnsTrue() {
        settingsSecureStore.setInt(INTERACTIVE_UI_TIMEOUT_SETTING_KEY, 10000)

        assertThat(dataStore.contains(INTERACTIVE_UI_TIMEOUT_SETTING_KEY)).isTrue()
    }
}
