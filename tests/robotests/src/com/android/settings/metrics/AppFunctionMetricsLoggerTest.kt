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

package com.android.settings.metrics

import android.app.settings.SettingsEnums
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.settings.appfunctions.DeviceStateAppFunctionType
import com.android.settings.appfunctions.executors.DeviceStateExecutor
import com.android.settings.core.instrumentation.SettingsStatsLog
import com.google.common.truth.Truth.assertThat
import java.util.ArrayList
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [AppFunctionMetricsLoggerTest.ShadowSettingsStatsLog::class])
class AppFunctionMetricsLoggerTest {

    @Rule @JvmField val mockitoRule: MockitoRule = MockitoJUnit.rule()

    private lateinit var appFunctionMetricsLogger: AppFunctionMetricsLogger
    private lateinit var context: Context

    @Mock private lateinit var mockDeviceStateExecutor: DeviceStateExecutor

    @Before
    fun setUp() {
        ShadowSettingsStatsLog.reset()
        appFunctionMetricsLogger = AppFunctionMetricsLogger()
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun logAppFunction_getStorage_logsCorrectly() {
        // Given
        val functionType = DeviceStateAppFunctionType.GET_STORAGE
        val callingPackage = "com.android.settings.test"
        val latencyMs = 100L

        // When
        appFunctionMetricsLogger.logAppFunction(
            functionType.toMetricsId(),
            callingPackage,
            latencyMs,
            context,
        )

        // Then
        val writtenEvents = ShadowSettingsStatsLog.getWrittenEvents()
        assertThat(writtenEvents).hasSize(1)
        val event = writtenEvents[0]
        assertThat(event.atomId).isEqualTo(SettingsStatsLog.SETTINGS_APP_FUNCTION_EVENT)
        assertThat(event.functionId).isEqualTo(SettingsEnums.APP_FUNCTION_GET_STORAGE)
        assertThat(event.latencyMillis).isEqualTo(latencyMs)
        assertThat(event.errorCode).isEqualTo(0)
    }

    @Test
    fun logAppFunction_setDeviceState_logsCorrectly() {
        // Given
        val functionType = DeviceStateAppFunctionType.SET_DEVICE_STATE
        val callingPackage = "com.android.systemui"
        val latencyMs = 250L

        // When
        appFunctionMetricsLogger.logAppFunction(
            functionType.toMetricsId(),
            callingPackage,
            latencyMs,
            context,
        )

        // Then
        val writtenEvents = ShadowSettingsStatsLog.getWrittenEvents()
        assertThat(writtenEvents).hasSize(1)
        val event = writtenEvents[0]
        assertThat(event.atomId).isEqualTo(SettingsStatsLog.SETTINGS_APP_FUNCTION_EVENT)
        assertThat(event.functionId).isEqualTo(SettingsEnums.APP_FUNCTION_SET_DEVICE_STATE)
        assertThat(event.latencyMillis).isEqualTo(latencyMs)
        assertThat(event.errorCode).isEqualTo(0)
    }

    @Test
    fun logAppFunctionError_withFunctionType_logsCorrectly() {
        // Given
        val functionType = DeviceStateAppFunctionType.GET_STORAGE
        val callingPackage = "com.android.settings.test"
        val errorCode = 1

        // When
        appFunctionMetricsLogger.logAppFunctionError(
            callingPackage,
            errorCode,
            context,
            functionType.toMetricsId(),
        )

        // Then
        val writtenEvents = ShadowSettingsStatsLog.getWrittenEvents()
        assertThat(writtenEvents).hasSize(1)
        val event = writtenEvents[0]
        assertThat(event.atomId).isEqualTo(SettingsStatsLog.SETTINGS_APP_FUNCTION_EVENT)
        assertThat(event.functionId).isEqualTo(SettingsEnums.APP_FUNCTION_GET_STORAGE)
        assertThat(event.latencyMillis).isEqualTo(0)
        assertThat(event.errorCode).isEqualTo(errorCode)
    }

    @Test
    fun logAppFunctionError_withoutFunctionType_logsUnknown() {
        // Given
        val callingPackage = "com.android.settings.test"
        val errorCode = 1

        // When
        appFunctionMetricsLogger.logAppFunctionError(callingPackage, errorCode, context, null)

        // Then
        val writtenEvents = ShadowSettingsStatsLog.getWrittenEvents()
        assertThat(writtenEvents).hasSize(1)
        val event = writtenEvents[0]
        assertThat(event.atomId).isEqualTo(SettingsStatsLog.SETTINGS_APP_FUNCTION_EVENT)
        assertThat(event.functionId).isEqualTo(SettingsEnums.UNKNOWN_APP_FUNCTION)
        assertThat(event.latencyMillis).isEqualTo(0)
        assertThat(event.errorCode).isEqualTo(errorCode)
    }

    @org.robolectric.annotation.Implements(SettingsStatsLog::class)
    class ShadowSettingsStatsLog {

        companion object {
            private val writtenEvents: MutableList<LoggedEvent> = ArrayList()

            @JvmStatic
            @org.robolectric.annotation.Implementation
            fun write(
                atomId: Int,
                functionId: Int,
                callingPackageUid: Int,
                latencyMillis: Long,
                errorCode: Int,
            ) {
                writtenEvents.add(
                    LoggedEvent(atomId, functionId, callingPackageUid, latencyMillis, errorCode)
                )
            }

            fun getWrittenEvents(): List<LoggedEvent> {
                return writtenEvents
            }

            fun reset() {
                writtenEvents.clear()
            }
        }
    }

    data class LoggedEvent(
        val atomId: Int,
        val functionId: Int,
        val callingPackageUid: Int,
        val latencyMillis: Long,
        val errorCode: Int,
    )
}
