/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.android.settings.appfunctions.DeviceStateCategory
import com.android.settings.appfunctions.providers.DeviceStateProvider
import com.android.settings.appfunctions.providers.DeviceStateProviderResult
import com.google.android.appfunctions.schema.common.v1.devicestate.PerScreenDeviceStates
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnit
import org.mockito.Mock
import org.mockito.Mockito.`when`

@RunWith(AndroidJUnit4::class)
class DeviceStateAggregatorTest {

    @get:Rule
    val mockitoRule = MockitoJUnit.rule()

    @Mock
    private lateinit var provider1: DeviceStateProvider

    @Mock
    private lateinit var provider2: DeviceStateProvider

    @Test
    fun aggregate_combinesResultsFromMultipleProviders() = runTest {
        // Arrange
        val states1 = listOf(PerScreenDeviceStates(description = "State from Provider 1"))
        val result1 = DeviceStateProviderResult(states = states1, hintText = "Hint 1")
        `when`(provider1.provide(DeviceStateCategory.UNCATEGORIZED)).thenReturn(result1)

        val states2 = listOf(PerScreenDeviceStates(description = "State from Provider 2"))
        val result2 = DeviceStateProviderResult(states = states2, hintText = "Hint 2")
        `when`(provider2.provide(DeviceStateCategory.UNCATEGORIZED)).thenReturn(result2)

        val aggregator = DeviceStateAggregator(
            providers = listOf(provider1, provider2)
        )

        // Act
        val response = aggregator.aggregate(DeviceStateCategory.UNCATEGORIZED, "en-US")

        // Assert
        assertThat(response.perScreenDeviceStates).containsExactlyElementsIn(states1 + states2)
        assertThat(response.deviceLocale).isEqualTo("en-US")
    }

    @Test
    fun aggregate_withNoProviders_returnsEmptyResponse() = runTest {
        // Arrange
        val aggregator = DeviceStateAggregator(providers = emptyList())

        // Act
        val response = aggregator.aggregate(DeviceStateCategory.UNCATEGORIZED, "en-US")

        // Assert
        assertThat(response.perScreenDeviceStates).isEmpty()
        assertThat(response.deviceLocale).isEqualTo("en-US")
    }
}