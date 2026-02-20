/*
 * Copyright 2026 The Android Open Source Project
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

package com.android.settings.appfunctions

import android.app.appsearch.GenericDocument
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.appfunctions.executors.DeviceStateExecutor
import com.google.android.appfunctions.schema.common.v1.devicestate.DeviceStateItem
import com.google.android.appfunctions.schema.common.v1.devicestate.DeviceStateItemResponse
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnit

@RunWith(AndroidJUnit4::class)
class DeviceStateItemProviderAggregatorTest {

    @get:Rule val mockitoRule = MockitoJUnit.rule()

    @Mock private lateinit var executor1: DeviceStateExecutor
    @Mock private lateinit var executor2: DeviceStateExecutor

    private val itemResponse =
        DeviceStateItemResponse(
            deviceStateItem = DeviceStateItem(key = "key", purpose = "purpose", jsonValue = "value")
        )
    private val params = GenericDocument.Builder<GenericDocument.Builder<*>>("", "", "").build()

    @Test
    fun aggregate_singleValidResult_returnsResult() = runTest {
        val result1 = DeviceStateItemProviderExecutorResult(null)
        val result2 = DeviceStateItemProviderExecutorResult(itemResponse)
        `when`(executor1.execute(DeviceStateAppFunctionType.GET_DEVICE_STATE, params))
            .thenReturn(result1)
        `when`(executor2.execute(DeviceStateAppFunctionType.GET_DEVICE_STATE, params))
            .thenReturn(result2)
        val aggregator = DeviceStateItemProviderAggregator(listOf(executor1, executor2))

        val response =
            aggregator.aggregate(DeviceStateAppFunctionType.GET_DEVICE_STATE, params, "en-US")

        assertThat(response).isEqualTo(itemResponse)
    }

    @Test
    fun aggregate_multipleValidResults_throwsException() = runTest {
        val result1 = DeviceStateItemProviderExecutorResult(itemResponse)
        val result2 = DeviceStateItemProviderExecutorResult(itemResponse)
        `when`(executor1.execute(DeviceStateAppFunctionType.GET_DEVICE_STATE, params))
            .thenReturn(result1)
        `when`(executor2.execute(DeviceStateAppFunctionType.GET_DEVICE_STATE, params))
            .thenReturn(result2)
        val aggregator = DeviceStateItemProviderAggregator(listOf(executor1, executor2))

        assertThrows(IllegalStateException::class.java) {
            runTest {
                aggregator.aggregate(DeviceStateAppFunctionType.GET_DEVICE_STATE, params, "en-US")
            }
        }
    }

    @Test
    fun aggregate_noValidResults_throwsException() = runTest {
        val result1 = DeviceStateItemProviderExecutorResult(null)
        val result2 = DeviceStateItemProviderExecutorResult(null)
        `when`(executor1.execute(DeviceStateAppFunctionType.GET_DEVICE_STATE, params))
            .thenReturn(result1)
        `when`(executor2.execute(DeviceStateAppFunctionType.GET_DEVICE_STATE, params))
            .thenReturn(result2)
        val aggregator = DeviceStateItemProviderAggregator(listOf(executor1, executor2))

        assertThrows(IllegalStateException::class.java) {
            runTest {
                aggregator.aggregate(DeviceStateAppFunctionType.GET_DEVICE_STATE, params, "en-US")
            }
        }
    }

    @Test
    fun aggregate_emptyExecutors_throwsException() = runTest {
        val aggregator = DeviceStateItemProviderAggregator(emptyList())

        assertThrows(IllegalStateException::class.java) {
            runTest {
                aggregator.aggregate(DeviceStateAppFunctionType.GET_DEVICE_STATE, params, "en-US")
            }
        }
    }
}
