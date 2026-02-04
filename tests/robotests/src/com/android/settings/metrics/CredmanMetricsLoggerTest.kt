/*
 * Copyright (C) 2026 The Android Open Source Project
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

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Process
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.internal.util.FrameworkStatsLog
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doCallRealMethod
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowSystemClock

/**
 * Unit tests for [CredmanMetricsLogger].
 *
 * Use the command `atest SettingsRoboTests:com.android.settings.metrics.CredmanMetricsLoggerTest`
 * to run all tests.
 */
@RunWith(AndroidJUnit4::class)
class CredmanMetricsLoggerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val testPackageName = "valid.test.package"
    private val testPackageUid = 12345
    private val testDurationMs = 1234L

    private val credmanMetricsLogger = spy(CredmanMetricsLogger(context))
    private val mockLifecycle = mock<Lifecycle>()
    private val mockLifecycleOwner = mock<LifecycleOwner>()

    @Before
    fun setUp() {
        val testPackageInfo =
            PackageInfo().apply {
                this.packageName = testPackageName
                applicationInfo = ApplicationInfo().apply { uid = testPackageUid }
            }
        shadowOf(context.packageManager).installPackage(testPackageInfo)
        credmanMetricsLogger.stub { on { logEvent(any(), anyOrNull(), any()) } doAnswer {} }
        // Clearing invocation records is necessary to avoid the stubbing style interfering with
        // verification calls in actual tests.
        clearInvocations(credmanMetricsLogger)
    }

    @Test
    fun constructor_withLifecycle_addsObserver() {
        val credmanMetricsLogger = CredmanMetricsLogger(context, mockLifecycle)
        verify(mockLifecycle).addObserver(credmanMetricsLogger)
    }

    @Test
    fun onResume_afterSinglePreferredServiceOutbound_callsLogEventWithCorrectParameters() {
        credmanMetricsLogger.recordPreferredServiceOutboundLaunch(testPackageName)
        ShadowSystemClock.advanceBy(Duration.ofMillis(testDurationMs))

        credmanMetricsLogger.onResume(mockLifecycleOwner)

        verify(credmanMetricsLogger)
            .logEvent(
                eq(
                    FrameworkStatsLog
                        .CREDENTIAL_MANAGER_SETTINGS_EVENT_REPORTED__EVENT_TYPE__RETURN_FROM_PREFERRED_SERVICE
                ),
                eq(testPackageName),
                eq(testDurationMs),
            )
    }

    @Test
    fun onResume_afterSingleAdditionalServiceOutbound_callsLogEventWithCorrectParameters() {
        credmanMetricsLogger.recordAdditionalServiceOutboundLaunch(testPackageName)
        ShadowSystemClock.advanceBy(Duration.ofMillis(testDurationMs))

        credmanMetricsLogger.onResume(mockLifecycleOwner)

        verify(credmanMetricsLogger)
            .logEvent(
                eq(
                    FrameworkStatsLog
                        .CREDENTIAL_MANAGER_SETTINGS_EVENT_REPORTED__EVENT_TYPE__RETURN_FROM_ADDITIONAL_SERVICE
                ),
                eq(testPackageName),
                eq(testDurationMs),
            )
    }

    @Test
    fun onResume_afterMultipleOutboundSessions_callsLogEventWithCorrectParameters() {
        credmanMetricsLogger.recordPreferredServiceOutboundLaunch(testPackageName)
        ShadowSystemClock.advanceBy(Duration.ofMillis(4321L))
        credmanMetricsLogger.onResume(mockLifecycleOwner)
        credmanMetricsLogger.recordAdditionalServiceOutboundLaunch(testPackageName)
        ShadowSystemClock.advanceBy(Duration.ofMillis(testDurationMs))

        credmanMetricsLogger.onResume(mockLifecycleOwner)

        verify(credmanMetricsLogger)
            .logEvent(
                eq(
                    FrameworkStatsLog
                        .CREDENTIAL_MANAGER_SETTINGS_EVENT_REPORTED__EVENT_TYPE__RETURN_FROM_ADDITIONAL_SERVICE
                ),
                eq(testPackageName),
                eq(testDurationMs),
            )
    }

    @Test
    fun onResume_withoutOutboundSession_doesNotLog() {
        credmanMetricsLogger.onResume(mockLifecycleOwner)
        verify(credmanMetricsLogger, never()).logEvent(any(), anyOrNull(), any())
    }

    @Test
    fun onDestroy_clearsOutboundSession() {
        credmanMetricsLogger.recordPreferredServiceOutboundLaunch()
        credmanMetricsLogger.onDestroy(mockLifecycleOwner)

        credmanMetricsLogger.onResume(mockLifecycleOwner)

        verify(credmanMetricsLogger, never()).logEvent(any(), anyOrNull(), any())
    }

    @Test
    fun logEditPreferredServiceEvent_callsLogEventWithCorrectParameters() {
        credmanMetricsLogger.logEditPreferredServiceEvent(testPackageName)
        verify(credmanMetricsLogger)
            .logEvent(
                eq(
                    FrameworkStatsLog
                        .CREDENTIAL_MANAGER_SETTINGS_EVENT_REPORTED__EVENT_TYPE__EDIT_PREFERRED_SERVICE
                ),
                eq(testPackageName),
                eq(0L),
            )
    }

    @Test
    fun logAddPreferredServiceEvent_callsLogEventWithCorrectParameters() {
        credmanMetricsLogger.logAddPreferredServiceEvent()
        verify(credmanMetricsLogger)
            .logEvent(
                eq(
                    FrameworkStatsLog
                        .CREDENTIAL_MANAGER_SETTINGS_EVENT_REPORTED__EVENT_TYPE__ADD_PREFERRED_SERVICE
                ),
                eq(null),
                eq(0L),
            )
    }

    @Test
    fun logEnableAdditionalServiceEvent_callsLogEventWithCorrectParameters() {
        credmanMetricsLogger.logEnableAdditionalServiceEvent(testPackageName)
        verify(credmanMetricsLogger)
            .logEvent(
                eq(
                    FrameworkStatsLog
                        .CREDENTIAL_MANAGER_SETTINGS_EVENT_REPORTED__EVENT_TYPE__ENABLE_ADDITIONAL_SERVICE
                ),
                eq(testPackageName),
                eq(0L),
            )
    }

    @Test
    fun logDisableAdditionalServiceEvent_callsLogEventWithCorrectParameters() {
        credmanMetricsLogger.logDisableAdditionalServiceEvent(testPackageName)
        verify(credmanMetricsLogger)
            .logEvent(
                eq(
                    FrameworkStatsLog
                        .CREDENTIAL_MANAGER_SETTINGS_EVENT_REPORTED__EVENT_TYPE__DISABLE_ADDITIONAL_SERVICE
                ),
                eq(testPackageName),
                eq(0L),
            )
    }

    @Test
    fun logHitAdditionalServiceLimitEvent_callsLogEventWithCorrectParameters() {
        credmanMetricsLogger.logHitAdditionalServiceLimitEvent(testPackageName)
        verify(credmanMetricsLogger)
            .logEvent(
                eq(
                    FrameworkStatsLog
                        .CREDENTIAL_MANAGER_SETTINGS_EVENT_REPORTED__EVENT_TYPE__HIT_ADDITIONAL_SERVICE_LIMIT
                ),
                eq(testPackageName),
                eq(0L),
            )
    }

    @Test
    fun logEvent_callsGetUidWithCorrectParameters() {
        // Override the stub from setUp to call the actual implementation.
        doCallRealMethod().whenever(credmanMetricsLogger).logEvent(any(), anyOrNull(), any())

        credmanMetricsLogger.logEvent(
            FrameworkStatsLog
                .CREDENTIAL_MANAGER_SETTINGS_EVENT_REPORTED__EVENT_TYPE__RETURN_FROM_PREFERRED_SERVICE,
            testPackageName,
            testDurationMs,
        )

        verify(credmanMetricsLogger).getUid(eq(testPackageName))
    }

    @Test
    fun getUid_returnsCorrectUid() {
        val actualUid = credmanMetricsLogger.getUid(testPackageName)
        assertThat(actualUid).isEqualTo(testPackageUid)
    }

    @Test
    fun getUid_withoutPackageName_returnsInvalidUid() {
        val actualUid = credmanMetricsLogger.getUid(null)
        assertThat(actualUid).isEqualTo(Process.INVALID_UID)
    }

    @Test
    fun getUid_withInvalidPackage_returnsInvalidUid() {
        val actualUid = credmanMetricsLogger.getUid("invalid.test.package")
        assertThat(actualUid).isEqualTo(Process.INVALID_UID)
    }
}
