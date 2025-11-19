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
package com.android.settings.appfunctions

import android.content.Context
import android.os.OutcomeReceiver
import android.service.settings.preferences.SettingsPreferenceServiceClient
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

// Shadow for SettingsPreferenceServiceClient to intercept its constructor and methods
@Implements(SettingsPreferenceServiceClient::class)
class ShadowSettingsPreferenceServiceClient {
    companion object {
        var capturedOutcomeReceiver: OutcomeReceiver<SettingsPreferenceServiceClient, Exception>? =
            null

        fun reset() {
            capturedOutcomeReceiver = null
        }
    }

    @Implementation
    fun __constructor__(
        context: Context,
        packageName: String,
        executor: Executor,
        outcomeReceiver: OutcomeReceiver<SettingsPreferenceServiceClient, Exception>,
    ) {
        // Capture the callback to be used in tests
        capturedOutcomeReceiver = outcomeReceiver
    }

    @Implementation
    fun connect() {
        // Do nothing to prevent the original connect() method from being called.
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowSettingsPreferenceServiceClient::class])
class SettingsPreferenceServiceClientManagerTest {

    private lateinit var context: Context
    private val directExecutor: ExecutorService = MoreExecutors.newDirectExecutorService()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Reset the client before each test to ensure test isolation
        val clientField =
            SettingsPreferenceServiceClientManager::class.java.getDeclaredField("client")
        clientField.isAccessible = true
        clientField.set(null, null)
        // Reset the shadow state
        ShadowSettingsPreferenceServiceClient.reset()
    }

    @Test
    fun initialize_success_clientIsSet() {
        // Arrange
        val mockClient = mock(SettingsPreferenceServiceClient::class.java)

        // Act
        SettingsPreferenceServiceClientManager.initialize(context, directExecutor)

        // Assert that the constructor was called and the receiver was captured
        val outcomeReceiver = ShadowSettingsPreferenceServiceClient.capturedOutcomeReceiver
        assertThat(outcomeReceiver).isNotNull()

        // Simulate the onResult callback
        outcomeReceiver!!.onResult(mockClient)

        // Assert
        assertThat(SettingsPreferenceServiceClientManager.client).isEqualTo(mockClient)
    }

    @Test
    fun initialize_error_clientIsNull() {
        // Act
        SettingsPreferenceServiceClientManager.initialize(context, directExecutor)

        // Assert that the constructor was called and the receiver was captured
        val outcomeReceiver = ShadowSettingsPreferenceServiceClient.capturedOutcomeReceiver
        assertThat(outcomeReceiver).isNotNull()

        // Simulate the onError callback
        outcomeReceiver!!.onError(RuntimeException("Connection failed"))

        // Assert
        assertThat(SettingsPreferenceServiceClientManager.client).isNull()
    }
}
