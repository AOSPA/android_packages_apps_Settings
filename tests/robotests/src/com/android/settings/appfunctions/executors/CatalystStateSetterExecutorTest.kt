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

import android.app.Application
import android.app.appsearch.GenericDocument
import androidx.test.core.app.ApplicationProvider
import com.android.settings.appfunctions.DeviceStateAppFunctionType
import com.android.settingslib.graph.PreferenceGetterResponse
import com.android.settingslib.graph.PreferenceSetterResult
import com.android.settingslib.graph.proto.PreferenceErrorProto
import com.android.settingslib.graph.proto.PreferenceProto
import com.android.settingslib.graph.proto.PreferenceValueProto
import com.android.settingslib.metadata.PreferenceCoordinate
import com.android.settingslib.metadata.SensitivityLevel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowPreferenceServiceClient::class])
class CatalystStateSetterExecutorTest {
    private lateinit var executor: CatalystStateSetterExecutor
    private val context = ApplicationProvider.getApplicationContext<Application>()

    @Before
    fun setUp() {
        ShadowPreferenceServiceClient.reset()
        executor = CatalystStateSetterExecutor(context)
    }

    @Test
    fun execute_setPreferenceValueFails_returnsOldValue() = runTest {
        // Setup initial value for "getPreference" call
        val proto =
            PreferenceProto.newBuilder()
                .setValue(PreferenceValueProto.newBuilder().setBooleanValue(true).build())
                .setAvailable(true)
                .setEnabled(true)
                .setWritable(true)
                .setSensitivityLevel(SensitivityLevel.NO_SENSITIVITY)
                .build()

        // The executor will request "screen" / "key"
        val coord = PreferenceCoordinate("screen", "key")
        val response =
            PreferenceGetterResponse(
                errors = emptyMap<PreferenceCoordinate, Int>(),
                preferences = mapOf<PreferenceCoordinate, PreferenceProto>(coord to proto),
            )
        ShadowPreferenceServiceClient.mockGetterResponse = response

        // Mock setPreferenceValue to fail by throwing an exception in the shadow
        ShadowPreferenceServiceClient.setterException = Exception("Set failed")

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
        assertThat(result.result?.failureReason).contains("Error with code ${PreferenceSetterResult.INTERNAL_ERROR} due to Set failed")
    }

    @Test
    fun execute_setPreferenceValueFailsWhenGet_returnsNAValue() = runTest {
        // Setup initial value for "getPreference" call that contains an error
        val proto =
            PreferenceProto.newBuilder()
                .setValue(PreferenceValueProto.newBuilder().setError(
                    PreferenceErrorProto.newBuilder().setError("Preconditions failed").build()
                ))
                .setAvailable(true)
                .setEnabled(true)
                .setWritable(true)
                .setSensitivityLevel(SensitivityLevel.NO_SENSITIVITY)
                .build()

        // The executor will request "screen" / "key"
        val coord = PreferenceCoordinate("screen", "key")
        val response =
            PreferenceGetterResponse(
                errors = emptyMap<PreferenceCoordinate, Int>(),
                preferences = mapOf<PreferenceCoordinate, PreferenceProto>(coord to proto),
            )
        ShadowPreferenceServiceClient.mockGetterResponse = response

        // Mock setPreferenceValue to fail by throwing an exception in the shadow
        ShadowPreferenceServiceClient.setterException = Exception("Set failed")

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
        assertThat(result.result?.currentValue).isEqualTo("N/A")
        assertThat(result.result?.failureReason).isEqualTo("Preconditions failed")
    }
}
