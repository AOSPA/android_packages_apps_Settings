/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.android.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.appfunctions.executors

import android.app.Application
import android.app.appsearch.GenericDocument
import android.os.Build
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.android.settings.appfunctions.DeviceStateAppFunctionType
import com.android.settingslib.graph.PreferenceGetterResponse
import com.android.settingslib.graph.proto.PreferenceErrorProto
import com.android.settingslib.graph.proto.PreferenceProto
import com.android.settingslib.graph.proto.PreferenceValueProto
import com.android.settingslib.metadata.FixedArrayMap
import com.android.settingslib.metadata.KeyParameters
import com.android.settingslib.metadata.KeyParametersSchema
import com.android.settingslib.metadata.PreferenceCoordinate
import com.android.settingslib.metadata.PreferenceScreenMetadataFactory
import com.android.settingslib.metadata.PreferenceScreenMetadataParameterizedFactory
import com.android.settingslib.metadata.PreferenceScreenRegistry
import com.android.settingslib.metadata.preferencesapi.types.AnyString
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowPreferenceServiceClient::class])
class CatalystStateGetterExecutorTest {

    private lateinit var executor: CatalystStateGetterExecutor
    private val context = ApplicationProvider.getApplicationContext<Application>()

    @Before
    fun setUp() {
        ShadowPreferenceServiceClient.reset()
        executor = CatalystStateGetterExecutor(context)

        // Default to debuggable build for most tests
        ReflectionHelpers.setStaticField(Build::class.java, "TYPE", "userdebug")

        // Enable the flag for testing by default
        Settings.Global.putInt(
            context.contentResolver,
            "com.android.settings.APP_FUNCTION_ITEM_GETTER_AVAILABLE",
            1,
        )

        val parameterizedSchema = KeyParametersSchema { parameter("pkg", 0, type = AnyString) }
        val mockParameterizedFactory =
            mock(PreferenceScreenMetadataParameterizedFactory::class.java)
        `when`(mockParameterizedFactory.parametersSchema).thenReturn(parameterizedSchema)

        val mockUnparameterizedFactory = mock(PreferenceScreenMetadataFactory::class.java)

        // FixedArrayMap keys must be provided in sorted order
        val testFactories =
            FixedArrayMap<String, PreferenceScreenMetadataFactory>(3) { initializer ->
                initializer.put("invalid_key", mockParameterizedFactory)
                initializer.put("parameterized_screen", mockParameterizedFactory)
                initializer.put("unparameterized_screen", mockUnparameterizedFactory)
            }
        PreferenceScreenRegistry.preferenceScreenMetadataFactories =
            PreferenceScreenRegistry.preferenceScreenMetadataFactories.merge(testFactories)
    }

    @Test
    fun execute_validItemizedParams_returnsSuccessResult() = runTest {
        val proto =
            PreferenceProto.newBuilder()
                .setValue(PreferenceValueProto.newBuilder().setBooleanValue(true).build())
                .build()

        val keyParameters = KeyParameters(mapOf("pkg" to "com.android.chrome"))
        val coord = PreferenceCoordinate("parameterized_screen", keyParameters, "key")
        val response =
            PreferenceGetterResponse(
                errors = emptyMap<PreferenceCoordinate, Int>(),
                preferences = mapOf<PreferenceCoordinate, PreferenceProto>(coord to proto),
            )
        ShadowPreferenceServiceClient.mockGetterResponse = response

        val innerDoc =
            GenericDocument.Builder<GenericDocument.Builder<*>>("namespace", "id", "schema")
                .setPropertyString("key", "parameterized_screen/key")
                .setPropertyString("itemizationKeys", "com.android.chrome")
                .build()
        val params =
            GenericDocument.Builder<GenericDocument.Builder<*>>("namespace", "id", "schema")
                .setPropertyDocument("getDeviceStateItemParams", innerDoc)
                .build()

        val result = executor.execute(DeviceStateAppFunctionType.GET_DEVICE_STATE, params)

        assertThat(result.result).isNotNull()
        assertThat(result.result?.deviceStateItem?.key).isEqualTo("parameterized_screen/key")
        assertThat(result.result?.deviceStateItem?.jsonValue).isEqualTo("true")
    }

    @Test
    fun execute_validUnitemizedParams_returnsSuccessResult() = runTest {
        val proto =
            PreferenceProto.newBuilder()
                .setValue(PreferenceValueProto.newBuilder().setBooleanValue(false).build())
                .build()

        val coord = PreferenceCoordinate("unparameterized_screen", "key")
        val response =
            PreferenceGetterResponse(
                errors = emptyMap<PreferenceCoordinate, Int>(),
                preferences = mapOf<PreferenceCoordinate, PreferenceProto>(coord to proto),
            )
        ShadowPreferenceServiceClient.mockGetterResponse = response

        val innerDoc =
            GenericDocument.Builder<GenericDocument.Builder<*>>("namespace", "id", "schema")
                .setPropertyString("key", "unparameterized_screen/key")
                .build()
        val params =
            GenericDocument.Builder<GenericDocument.Builder<*>>("namespace", "id", "schema")
                .setPropertyDocument("getDeviceStateItemParams", innerDoc)
                .build()

        val result = executor.execute(DeviceStateAppFunctionType.GET_DEVICE_STATE, params)

        assertThat(result.result).isNotNull()
        assertThat(result.result?.deviceStateItem?.key).isEqualTo("unparameterized_screen/key")
        assertThat(result.result?.deviceStateItem?.jsonValue).isEqualTo("false")
    }

    @Test
    fun execute_missingParams_throwsException() = runTest {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { executor.execute(DeviceStateAppFunctionType.GET_DEVICE_STATE, null) }
            }
        assertThat(exception.message).isEqualTo("Provided params are null.")
    }

    @Test
    fun execute_invalidKey_throwsException() = runTest {
        val coord = PreferenceCoordinate("invalid_key", "invalid_key")
        val response =
            PreferenceGetterResponse(
                errors = mapOf(coord to 1), // 1 is NOT_FOUND
                preferences = emptyMap(),
            )
        ShadowPreferenceServiceClient.mockGetterResponse = response

        val innerDoc =
            GenericDocument.Builder<GenericDocument.Builder<*>>("namespace", "id", "schema")
                .setPropertyString("key", "invalid_key")
                .build()
        val params =
            GenericDocument.Builder<GenericDocument.Builder<*>>("namespace", "id", "schema")
                .setPropertyDocument("getDeviceStateItemParams", innerDoc)
                .build()

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    executor.execute(DeviceStateAppFunctionType.GET_DEVICE_STATE, params)
                }
            }
        assertThat(exception.message).isEqualTo("Key [invalid_key] not found")
    }

    @Test
    fun execute_flagDisabled_returnsNullResult() = runTest {
        Settings.Global.putInt(
            context.contentResolver,
            "com.android.settings.APP_FUNCTION_ITEM_GETTER_AVAILABLE",
            0,
        )
        val innerDoc =
            GenericDocument.Builder<GenericDocument.Builder<*>>("namespace", "id", "schema")
                .setPropertyString("key", "unparameterized_screen/key")
                .build()
        val params =
            GenericDocument.Builder<GenericDocument.Builder<*>>("namespace", "id", "schema")
                .setPropertyDocument("getDeviceStateItemParams", innerDoc)
                .build()

        val result = executor.execute(DeviceStateAppFunctionType.GET_DEVICE_STATE, params)

        assertThat(result.result).isNull()
    }

    @Test
    fun execute_notDebuggable_returnsNullResult() = runTest {
        ReflectionHelpers.setStaticField(Build::class.java, "TYPE", "user")
        val innerDoc =
            GenericDocument.Builder<GenericDocument.Builder<*>>("namespace", "id", "schema")
                .setPropertyString("key", "unparameterized_screen/key")
                .build()
        val params =
            GenericDocument.Builder<GenericDocument.Builder<*>>("namespace", "id", "schema")
                .setPropertyDocument("getDeviceStateItemParams", innerDoc)
                .build()

        val result = executor.execute(DeviceStateAppFunctionType.GET_DEVICE_STATE, params)

        assertThat(result.result).isNull()
    }

    @Test
    fun execute_protoContainsError_returnsErrorInResult() = runTest {
        val proto = PreferenceProto.newBuilder().setValue(
            PreferenceValueProto.newBuilder().setError(
                PreferenceErrorProto.newBuilder().setError("Preconditions failed").build()
            )
        ).build()

        val coord = PreferenceCoordinate("screen_key", "pref_key")
        val response =
            PreferenceGetterResponse(
                errors = emptyMap<PreferenceCoordinate, Int>(),
                preferences = mapOf<PreferenceCoordinate, PreferenceProto>(coord to proto),
            )
        ShadowPreferenceServiceClient.mockGetterResponse = response

        val innerDoc =
            GenericDocument.Builder<GenericDocument.Builder<*>>("namespace", "id", "schema")
                .setPropertyString("key", "screen_key/pref_key")
                .build()
        val params =
            GenericDocument.Builder<GenericDocument.Builder<*>>("namespace", "id", "schema")
                .setPropertyDocument("getDeviceStateItemParams", innerDoc)
                .build()

        val result = executor.execute(DeviceStateAppFunctionType.GET_DEVICE_STATE, params)

        assertThat(result.result).isNotNull()
        assertThat(result.result?.deviceStateItem?.key).isEqualTo("screen_key/pref_key")
        assertThat(result.result?.deviceStateItem?.jsonValue).isEqualTo("Error: Preconditions failed")
    }
}
