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
package com.android.settings.appfunctions.executors

import android.service.settings.preferences.SettingsPreferenceValue
import com.google.common.truth.Truth.assertThat
import java.lang.reflect.Method
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CatalystStateSetterExecutorTest {
    private lateinit var executor: CatalystStateSetterExecutor
    private lateinit var toSettingsPreferenceValueMethod: Method

    @Before
    fun setUp() {
        executor = CatalystStateSetterExecutor()
        toSettingsPreferenceValueMethod =
            executor::class
                .java
                .getDeclaredMethod(
                    "toSettingsPreferenceValue",
                    String::class.java,
                    Int::class.javaObjectType,
                )
                .apply { isAccessible = true }
    }

    @Test
    fun toSettingsPreferenceValue_booleanType_parsesCorrectly() {
        val result =
            toSettingsPreferenceValueMethod.invoke(
                executor,
                "true",
                SettingsPreferenceValue.TYPE_BOOLEAN,
            ) as SettingsPreferenceValue?
        assertThat(result?.type).isEqualTo(SettingsPreferenceValue.TYPE_BOOLEAN)
        assertThat(result?.booleanValue).isTrue()
    }

    @Test
    fun toSettingsPreferenceValue_intType_parsesCorrectly() {
        val result =
            toSettingsPreferenceValueMethod.invoke(
                executor,
                "123",
                SettingsPreferenceValue.TYPE_INT,
            ) as SettingsPreferenceValue?
        assertThat(result?.type).isEqualTo(SettingsPreferenceValue.TYPE_INT)
        assertThat(result?.intValue).isEqualTo(123)
    }

    @Test
    fun toSettingsPreferenceValue_stringType_parsesCorrectly() {
        val result =
            toSettingsPreferenceValueMethod.invoke(
                executor,
                "test_string",
                SettingsPreferenceValue.TYPE_STRING,
            ) as SettingsPreferenceValue?
        assertThat(result?.type).isEqualTo(SettingsPreferenceValue.TYPE_STRING)
        assertThat(result?.stringValue).isEqualTo("test_string")
    }

    @Test
    fun toSettingsPreferenceValue_longType_parsesCorrectly() {
        val result =
            toSettingsPreferenceValueMethod.invoke(
                executor,
                "1234567890",
                SettingsPreferenceValue.TYPE_LONG,
            ) as SettingsPreferenceValue?
        assertThat(result?.type).isEqualTo(SettingsPreferenceValue.TYPE_LONG)
        assertThat(result?.longValue).isEqualTo(1234567890L)
    }

    @Test
    fun toSettingsPreferenceValue_doubleType_parsesCorrectly() {
        val result =
            toSettingsPreferenceValueMethod.invoke(
                executor,
                "123.45",
                SettingsPreferenceValue.TYPE_DOUBLE,
            ) as SettingsPreferenceValue?
        assertThat(result?.type).isEqualTo(SettingsPreferenceValue.TYPE_DOUBLE)
        assertThat(result?.doubleValue).isEqualTo(123.45)
    }

    @Test
    fun toSettingsPreferenceValue_invalidValue_returnsNull() {
        val result =
            toSettingsPreferenceValueMethod.invoke(
                executor,
                "not_a_boolean",
                SettingsPreferenceValue.TYPE_BOOLEAN,
            ) as SettingsPreferenceValue?
        assertThat(result).isNull()
    }
}
