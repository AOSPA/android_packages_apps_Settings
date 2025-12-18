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

package com.android.settings.accessibility.shared.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.datastore.SettingsSecureStore
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [StringToIntDataStoreWrapper]. */
@RunWith(AndroidJUnit4::class)
class StringToIntDataStoreWrapperTest {

    @get:Rule val settingsStoreRule = SettingsStoreRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val underlyingStore = SettingsSecureStore.get(context)
    private val store = StringToIntDataStoreWrapper(underlyingStore)

    @Test
    fun setValue_stringType_validIntString_storesAsInt() {
        store.setValue(TEST_KEY, String::class.javaObjectType, "123")

        assertThat(underlyingStore.getInt(TEST_KEY)).isEqualTo(123)
    }

    @Test
    fun setValue_stringType_invalidIntString_storesNull() {
        underlyingStore.setInt(TEST_KEY, 123)

        store.setValue(TEST_KEY, String::class.javaObjectType, "Foo")

        assertThat(underlyingStore.getInt(TEST_KEY)).isNull()
    }

    @Test
    fun setValue_stringType_null_storesNull() {
        underlyingStore.setInt(TEST_KEY, 123)

        store.setValue(TEST_KEY, String::class.javaObjectType, null)

        assertThat(underlyingStore.getInt(TEST_KEY)).isNull()
    }

    @Test
    fun setValue_nonStringType_passesThrough() {
        store.setValue(TEST_KEY, Boolean::class.javaObjectType, true)

        assertThat(underlyingStore.getBoolean(TEST_KEY)).isTrue()
    }

    @Test
    fun getValue_stringType_underlyingIntExists_returnsAsString() {
        underlyingStore.setInt(TEST_KEY, 456)

        val result = store.getValue(TEST_KEY, String::class.javaObjectType)

        assertThat(result).isEqualTo("456")
    }

    @Test
    fun getValue_stringType_underlyingValueMissing_returnsNull() {
        underlyingStore.setString(TEST_KEY, null)

        val result = store.getValue(TEST_KEY, String::class.javaObjectType)

        assertThat(result).isNull()
    }

    @Test
    fun getValue_nonStringType_passesThrough() {
        underlyingStore.setBoolean(TEST_KEY, true)

        val result = store.getValue(TEST_KEY, Boolean::class.javaObjectType)

        assertThat(result).isTrue()
    }

    @Test
    fun getDefaultValue_stringType_underlyingDefaultIntExists_returnsAsString() {
        underlyingStore.setDefaultValue(TEST_KEY, 789)

        val result = store.getDefaultValue(TEST_KEY, String::class.javaObjectType)

        assertThat(result).isEqualTo("789")
    }

    @Test
    fun getDefaultValue_stringType_underlyingDefaultValueMissing_returnsNull() {
        val result = store.getDefaultValue(TEST_KEY, String::class.javaObjectType)

        assertThat(result).isNull()
    }

    @Test
    fun getDefaultValue_nonStringType_passesThrough() {
        underlyingStore.setDefaultValue(TEST_KEY, true)

        val result = store.getDefaultValue(TEST_KEY, Boolean::class.javaObjectType)

        assertThat(result).isTrue()
    }

    companion object {
        private const val TEST_KEY = "test_key"
    }
}
