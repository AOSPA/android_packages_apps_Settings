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

import android.app.appsearch.GenericDocument
import android.os.OutcomeReceiver
import android.service.settings.preferences.GetValueResult
import android.service.settings.preferences.SettingsPreferenceServiceClient
import android.service.settings.preferences.SettingsPreferenceValue
import com.android.settings.appfunctions.DeviceStateAppFunctionType
import com.android.settings.appfunctions.SettingsPreferenceServiceClientManager
import com.google.common.truth.Truth.assertThat
import java.lang.reflect.Method
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CatalystStateSetterExecutorTest {
    private lateinit var executor: CatalystStateSetterExecutor
    private lateinit var toSettingsPreferenceValueMethod: Method
    private lateinit var settingsPreferenceValueToStringMethod: Method
    private lateinit var mockClient: SettingsPreferenceServiceClient

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
        settingsPreferenceValueToStringMethod =
            executor::class
                .java
                .getDeclaredMethod(
                    "settingsPreferenceValueToString",
                    SettingsPreferenceValue::class.java,
                )
                .apply { isAccessible = true }

        mockClient = mock(SettingsPreferenceServiceClient::class.java)
        val clientField =
            SettingsPreferenceServiceClientManager::class.java.getDeclaredField("client")
        clientField.isAccessible = true
        clientField.set(null, mockClient)
    }

    @Test
    fun execute_setPreferenceValueFails_returnsOldValue() = runTest {
        val oldValue =
            SettingsPreferenceValue.Builder(SettingsPreferenceValue.TYPE_BOOLEAN)
                .setBooleanValue(true)
                .build()
        doAnswer { invocation ->
                val receiver =
                    invocation.getArgument(2) as OutcomeReceiver<GetValueResult, Exception>
                val result = mock(GetValueResult::class.java)
                `when`(result.value).thenReturn(oldValue)
                receiver.onResult(result)
                null
            }
            .`when`(mockClient)
            .getPreferenceValue(any(), any(), any())
        //  Mock setPreferenceValue to fail (onError)
        doAnswer { invocation ->
                val receiver = invocation.getArgument(2) as OutcomeReceiver<*, Exception>
                receiver.onError(Exception("Set failed"))
                null
            }
            .`when`(mockClient)
            .setPreferenceValue(any(), any(), any())
        val innerDoc =
            GenericDocument.Builder<GenericDocument.Builder<*>>("namespace", "id", "schema")
                .setPropertyString("key", "screen/key")
                .setPropertyString("value", "false")
                .build()
        val params =
            GenericDocument.Builder<GenericDocument.Builder<*>>("namespace", "id", "schema")
                .setPropertyDocument("setDeviceStateItemParams", innerDoc)
                .build()

        val result = executor.execute(DeviceStateAppFunctionType.SET_DEVICE_STATE, params)

        assertThat(result.result?.isSuccessful).isFalse()
        assertThat(result.result?.currentValue).isEqualTo("true")
    }

    @Test
    fun settingsPreferenceValueToString_booleanValue_returnsCorrectString() {
        val value =
            SettingsPreferenceValue.Builder(SettingsPreferenceValue.TYPE_BOOLEAN)
                .setBooleanValue(true)
                .build()
        val result = settingsPreferenceValueToStringMethod.invoke(executor, value) as String
        assertThat(result).isEqualTo("true")
    }

    @Test
    fun settingsPreferenceValueToString_intValue_returnsCorrectString() {
        val value =
            SettingsPreferenceValue.Builder(SettingsPreferenceValue.TYPE_INT)
                .setIntValue(123)
                .build()
        val result = settingsPreferenceValueToStringMethod.invoke(executor, value) as String
        assertThat(result).isEqualTo("123")
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
