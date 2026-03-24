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

import android.app.appfunctions.AppFunctionException
import android.app.appfunctions.ExecuteAppFunctionRequest
import android.app.appfunctions.ExecuteAppFunctionResponse
import android.content.Context
import android.content.pm.SigningInfo
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.appfunctions.executors.DeviceStateExecutor
import com.android.settings.metrics.toMetricsId
import com.android.settingslib.metadata.AppFunctionMetricsLoggerInterface
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnit
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class AbstractDeviceStateAppFunctionServiceTest {

    @get:Rule val mockitoRule = MockitoJUnit.rule()

    @Mock private lateinit var mockProvider: DeviceStateExecutor
    @Mock
    private lateinit var mockCallback:
        OutcomeReceiver<ExecuteAppFunctionResponse, AppFunctionException>
    @Mock private lateinit var mockMetricsLogger: AppFunctionMetricsLoggerInterface

    private lateinit var service: TestDeviceStateAppFunctionService

    // Test implementation of the abstract class that allows injecting mocks
    private class TestDeviceStateAppFunctionService(
        override val deviceStateProviderExecutors: List<DeviceStateExecutor>,
        private val logger: AppFunctionMetricsLoggerInterface,
    ) : AbstractDeviceStateAppFunctionService() {
        override val metricsLogger: AppFunctionMetricsLoggerInterface
            get() = logger

        init {
            // Attach a base context to allow applicationContext to be used
            attachBaseContext(ApplicationProvider.getApplicationContext<Context>())
        }
    }

    @Before
    fun setUp() {
        service = TestDeviceStateAppFunctionService(listOf(mockProvider), mockMetricsLogger)
        service.onCreate()
    }

    @Test
    fun onExecuteFunction_invalidFunctionId_logsError() {
        val request = ExecuteAppFunctionRequest.Builder("test.package", "invalidFunction").build()
        val latch = CountDownLatch(1)
        doAnswer {
                latch.countDown()
                null
            }
            .whenever(mockCallback)
            .onError(any())

        service.onExecuteFunction(
            request,
            "test.package",
            SigningInfo(),
            CancellationSignal(),
            mockCallback
        )
        latch.await(5, TimeUnit.SECONDS)

        verify(mockMetricsLogger)
            .logAppFunctionError(
                "test.package",
                AppFunctionException.ERROR_FUNCTION_NOT_FOUND,
                service.applicationContext,
                null,
            )
    }

    @Test
    fun onExecuteFunction_validFunctionId_logsSuccess() {
        val request =
            ExecuteAppFunctionRequest.Builder("test.package", "getUncategorizedDeviceState").build()
        runBlocking {
            whenever(mockProvider.execute(any<DeviceStateAppFunctionType>(), anyOrNull()))
                .thenReturn(DeviceStateProviderExecutorResult(emptyList()))
        }
        val latch = CountDownLatch(1)
        doAnswer {
                latch.countDown()
                null
            }
            .whenever(mockCallback)
            .onResult(any())

        service.onExecuteFunction(
            request,
            "test.package",
            SigningInfo(),
            CancellationSignal(),
            mockCallback
        )
        latch.await(5, TimeUnit.SECONDS)

        verify(mockMetricsLogger)
            .logAppFunction(
                eq(DeviceStateAppFunctionType.GET_UNCATEGORIZED.toMetricsId()),
                eq("test.package"),
                any(),
                eq(service.applicationContext),
            )
    }
}
