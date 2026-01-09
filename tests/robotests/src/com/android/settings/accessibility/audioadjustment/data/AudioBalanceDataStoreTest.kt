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

package com.android.settings.accessibility.audioadjustment.data

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.android.settings.accessibility.audioadjustment.data.AudioBalanceDataStore.Companion.BALANCE_CENTER_VALUE
import com.android.settings.accessibility.audioadjustment.data.AudioBalanceDataStore.Companion.BALANCE_MAX_VALUE
import com.android.settings.accessibility.audioadjustment.data.AudioBalanceDataStore.Companion.BALANCE_MIN_VALUE
import com.android.settings.testutils.SettingsStoreRule
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameters
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector

/** Tests for [AudioBalanceDataStore] */
@RunWith(RobolectricTestParameterInjector::class)
class AudioBalanceDataStoreTest {

    @get:Rule val settingsStoreRule = SettingsStoreRule()

    private lateinit var context: Context
    private lateinit var dataStore: AudioBalanceDataStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dataStore = AudioBalanceDataStore(context)
    }

    @Test
    fun getDefaultValue_forInt_returnsCenterValue() {
        val defaultValue =
            dataStore.getDefaultValue(AudioBalanceDataStore.SETTING_KEY, Int::class.javaObjectType)
        assertThat(defaultValue).isEqualTo(BALANCE_CENTER_VALUE)
    }

    @Test
    fun getDefaultValue_forOtherType_returnsNull() {
        val defaultValue =
            dataStore.getDefaultValue(AudioBalanceDataStore.SETTING_KEY, String::class.java)
        assertThat(defaultValue).isNull()
    }

    @Test
    fun getValue_noValueStored_returnsCenterValue() {
        Settings.System.putString(context.contentResolver, AudioBalanceDataStore.SETTING_KEY, null)

        val value = dataStore.getValue(AudioBalanceDataStore.SETTING_KEY, Int::class.javaObjectType)

        assertThat(value).isEqualTo(BALANCE_CENTER_VALUE)
    }

    @TestParameters(
        customName = "Left channel, balance = -1f",
        value = ["{settingValue: -1f, expectedConvertedValue: 0}"],
    )
    @TestParameters(
        customName = "Center, balance = 0f",
        value = ["{settingValue: 0f, expectedConvertedValue: 100}"],
    )
    @TestParameters(
        customName = "Right channel, balance = 1f",
        value = ["{settingValue: 1f, expectedConvertedValue: 200}"],
    )
    @TestParameters(
        customName = "In between, balance = 0.5f",
        value = ["{settingValue: 0.5f, expectedConvertedValue: 150}"],
    )
    @Test
    fun getValue_valueStored_returnsCorrectConvertedValue(
        settingValue: Float,
        expectedConvertedValue: Int,
    ) {
        Settings.System.putFloat(
            context.contentResolver,
            AudioBalanceDataStore.SETTING_KEY,
            settingValue,
        )

        val observed =
            dataStore.getValue(AudioBalanceDataStore.SETTING_KEY, Int::class.javaObjectType)

        assertThat(observed).isEqualTo(expectedConvertedValue)
    }

    @Test
    fun getValue_unsupportedType_returnsNull() {
        val value = dataStore.getValue(AudioBalanceDataStore.SETTING_KEY, String::class.java)
        assertThat(value).isNull()
    }

    @TestParameters(
        customName = "Min",
        value = ["{valueToSet: $BALANCE_MIN_VALUE, expectedStoredValue: -1f}"],
    )
    @TestParameters(
        customName = "Center",
        value = ["{valueToSet: $BALANCE_CENTER_VALUE, expectedStoredValue: 0f}"],
    )
    @TestParameters(
        customName = "Max",
        value = ["{valueToSet: $BALANCE_MAX_VALUE, expectedStoredValue: 1f}"],
    )
    @TestParameters(
        customName = "In between",
        value = ["{valueToSet: 150, expectedStoredValue: 0.5f}"],
    )
    @Test
    fun setValue_validIntValue_storesCorrectFloatValue(
        valueToSet: Int,
        expectedStoredValue: Float,
    ) {
        dataStore.setValue(AudioBalanceDataStore.SETTING_KEY, Int::class.javaObjectType, valueToSet)

        val floatValue =
            Settings.System.getFloat(context.contentResolver, AudioBalanceDataStore.SETTING_KEY, 0f)
        assertThat(floatValue).isWithin(TOLERANCE).of(expectedStoredValue)
    }

    @Test
    fun setValue_unsupportedType_doesNotStoreValue() {
        val initialValue = 0.5f
        Settings.System.putFloat(
            context.contentResolver,
            AudioBalanceDataStore.SETTING_KEY,
            initialValue,
        )
        dataStore.setValue(AudioBalanceDataStore.SETTING_KEY, String::class.java, "test")
        val finalValue =
            Settings.System.getFloat(context.contentResolver, AudioBalanceDataStore.SETTING_KEY, 0f)
        assertThat(finalValue).isWithin(TOLERANCE).of(initialValue)
    }

    companion object {
        private const val TOLERANCE = 1e-5f
    }
}
