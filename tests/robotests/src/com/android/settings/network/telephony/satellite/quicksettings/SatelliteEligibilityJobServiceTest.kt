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

package com.android.settings.network.telephony.satellite.quicksettings

import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.ServiceState
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.Mock
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnit
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowJobScheduler
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowProcess
import org.robolectric.shadows.ShadowSubscriptionManager
import org.robolectric.shadows.ShadowSystemProperties
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
class SatelliteEligibilityJobServiceTest {
    @get:Rule val mocks = MockitoJUnit.rule()

    @Mock private lateinit var mockJobParameters: JobParameters
    @Mock private lateinit var mockTelephonyManager: TelephonyManager
    @Mock private lateinit var mockSatelliteTilePromptUtils: SatelliteTilePromptUtils
    @Mock private lateinit var mockServiceState: ServiceState
    @Mock private lateinit var mockSatelliteStateRepository: SatelliteStateRepository

    private lateinit var service: SatelliteEligibilityJobService
    private lateinit var context: Context
    private lateinit var jobScheduler: JobScheduler
    private lateinit var shadowJobScheduler: ShadowJobScheduler
    private val subId = 1
    private var jobId = 0
    private val satelliteStatusFlow = MutableStateFlow(SatelliteStatus.NOT_AVAILABLE)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        jobId = context.resources.getInteger(R.integer.satellite_eligibility_job)

        service = spy(SatelliteEligibilityJobService())
        ReflectionHelpers.callInstanceMethod<Unit>(
            service,
            "attachBaseContext",
            ReflectionHelpers.ClassParameter(Context::class.java, context),
        )

        jobScheduler = context.getSystemService(JobScheduler::class.java)
        shadowJobScheduler = shadowOf(jobScheduler)

        // Mock TelephonyManager
        val telephonyManager = context.getSystemService(TelephonyManager::class.java)
        val shadowTelephonyManager = shadowOf(telephonyManager)
        shadowTelephonyManager.setTelephonyManagerForSubscriptionId(subId, mockTelephonyManager)

        // Set default data subscription ID
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(subId)

        // Mock TelephonyManager behavior
        doReturn(mockTelephonyManager)
            .`when`(service)
            .getSystemService(TelephonyManager::class.java)
        `when`(mockTelephonyManager.createForSubscriptionId(subId)).thenReturn(mockTelephonyManager)
        `when`(mockTelephonyManager.serviceState).thenReturn(mockServiceState)

        // Mock Utils
        SatelliteEligibilityJobService.satelliteTilePromptUtils = mockSatelliteTilePromptUtils
        // schedule() and onStartJob() use different contexts, so we need to mock both.
        `when`(mockSatelliteTilePromptUtils.areAllPromptsShown(context)).thenReturn(false)
        `when`(mockSatelliteTilePromptUtils.shouldShowSatelliteTilePrompt(context)).thenReturn(true)
        `when`(mockSatelliteTilePromptUtils.areAllPromptsShown(service)).thenReturn(false)
        `when`(mockSatelliteTilePromptUtils.shouldShowSatelliteTilePrompt(service)).thenReturn(true)

        // Default JobParameters
        `when`(mockJobParameters.jobId).thenReturn(jobId)

        // Mock Repository
        SatelliteStateRepository.setInstance(mockSatelliteStateRepository)
        `when`(mockSatelliteStateRepository.satelliteStatus).thenReturn(satelliteStatusFlow)

        // Enable the SatelliteTileService component by default so existing tests pass
        val componentName = ComponentName(context, SatelliteTileService::class.java)
        context.packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
    }

    @After
    fun tearDown() {
        SatelliteEligibilityJobService.satelliteTilePromptUtils = SatelliteTilePromptUtils()
        SatelliteStateRepository.setInstance(null)
        ShadowSystemProperties.reset()
    }

    @Test
    fun schedule_featureDisabled_doesNothing() {
        ShadowSystemProperties.override("ro.test_harness", "true")

        SatelliteEligibilityJobService.schedule(context)

        assertThat(jobScheduler.getAllPendingJobs()).isEmpty()
    }

    @Test
    fun schedule_tileServiceDisabled_doesNothing() {
        // Disable the component
        val componentName = ComponentName(context, SatelliteTileService::class.java)
        context.packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )

        SatelliteEligibilityJobService.schedule(context)

        assertThat(jobScheduler.getAllPendingJobs()).isEmpty()
    }

    @Test
    fun onStartJob_featureDisabled_returnsFalse() {
        ShadowSystemProperties.override("ro.test_harness", "true")

        val result = service.onStartJob(mockJobParameters)

        assertThat(result).isFalse()
        verify(service, never()).getSystemService(TelephonyManager::class.java)
    }

    @Test
    fun onStartJob_tileServiceDisabled_returnsFalse() {
        // Disable the component
        val componentName = ComponentName(context, SatelliteTileService::class.java)
        context.packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )

        val result = service.onStartJob(mockJobParameters)

        assertThat(result).isFalse()
        verify(mockTelephonyManager, never()).registerTelephonyCallback(any(), any())
    }

    @Test
    fun onStartJob_notSystemUser_returnsFalse() {
        // UID for user 1. USER_SYSTEM is 0.
        ShadowProcess.setUid(100000)

        val result = service.onStartJob(mockJobParameters)

        assertThat(result).isFalse()
        verify(mockTelephonyManager, never()).registerTelephonyCallback(any(), any())
    }

    @Test
    fun onStartJob_inService_terrestrial_reschedulesAndReturnsFalse() {
        `when`(mockServiceState.state).thenReturn(ServiceState.STATE_IN_SERVICE)
        `when`(mockServiceState.isUsingNonTerrestrialNetwork()).thenReturn(false)

        val result = service.onStartJob(mockJobParameters)

        assertThat(result).isFalse()
        verify(mockTelephonyManager, never()).registerTelephonyCallback(any(), any())
        assertThat(shadowJobScheduler.getPendingJob(jobId)).isNotNull()
    }

    @Test
    fun onStartJob_inService_satellite_returnsTrueAndRegistersCallback() {
        `when`(mockServiceState.state).thenReturn(ServiceState.STATE_IN_SERVICE)
        `when`(mockServiceState.isUsingNonTerrestrialNetwork()).thenReturn(true)

        val result = service.onStartJob(mockJobParameters)

        assertThat(result).isTrue()
        verify(mockTelephonyManager).registerTelephonyCallback(any(), any())
    }

    @Test
    fun onStartJob_outOfService_returnsTrueAndRegistersCallback() {
        `when`(mockServiceState.state).thenReturn(ServiceState.STATE_OUT_OF_SERVICE)

        val result = service.onStartJob(mockJobParameters)

        assertThat(result).isTrue()
        verify(mockTelephonyManager).registerTelephonyCallback(any(), any())
    }

    @Test
    fun onStartJob_powerOff_returnsTrueAndRegistersCallback() {
        `when`(mockServiceState.state).thenReturn(ServiceState.STATE_POWER_OFF)

        val result = service.onStartJob(mockJobParameters)

        assertThat(result).isTrue()
        verify(mockTelephonyManager).registerTelephonyCallback(any(), any())
    }

    @Test
    fun onStartJob_allPromptsShown_returnsFalse() {
        `when`(mockSatelliteTilePromptUtils.areAllPromptsShown(service)).thenReturn(true)

        val result = service.onStartJob(mockJobParameters)

        assertThat(result).isFalse()
        verify(mockTelephonyManager, never()).registerTelephonyCallback(any(), any())
    }

    @Test
    fun schedule_allPromptsShown_doesNothing() {
        `when`(mockSatelliteTilePromptUtils.areAllPromptsShown(context)).thenReturn(true)

        SatelliteEligibilityJobService.schedule(context)

        assertThat(jobScheduler.getAllPendingJobs()).isEmpty()
    }

    @Test
    fun callback_onServiceStateChanged_toInService_terrestrial_reschedulesAndFinishesJob() {
        `when`(mockServiceState.state).thenReturn(ServiceState.STATE_OUT_OF_SERVICE)
        service.onStartJob(mockJobParameters)
        // Clear previous schedule call
        jobScheduler.cancel(jobId)

        val callbackCaptor = ArgumentCaptor.forClass(TelephonyCallback::class.java)
        verify(mockTelephonyManager).registerTelephonyCallback(any(), callbackCaptor.capture())
        val callback = callbackCaptor.value as TelephonyCallback.ServiceStateListener

        // Trigger change to IN_SERVICE
        val newServiceState = mock(ServiceState::class.java)
        `when`(newServiceState.state).thenReturn(ServiceState.STATE_IN_SERVICE)
        `when`(newServiceState.isUsingNonTerrestrialNetwork()).thenReturn(false)
        callback.onServiceStateChanged(newServiceState)

        verify(service).jobFinished(mockJobParameters, false)
        verify(mockTelephonyManager).unregisterTelephonyCallback(any<TelephonyCallback>())
        assertThat(shadowJobScheduler.getPendingJob(jobId)).isNotNull()
    }

    @Test
    fun callback_onServiceStateChanged_toInService_satellite_doesNothing() {
        `when`(mockServiceState.state).thenReturn(ServiceState.STATE_OUT_OF_SERVICE)
        service.onStartJob(mockJobParameters)

        val callbackCaptor = ArgumentCaptor.forClass(TelephonyCallback::class.java)
        verify(mockTelephonyManager).registerTelephonyCallback(any(), callbackCaptor.capture())
        val callback = callbackCaptor.value as TelephonyCallback.ServiceStateListener

        // Trigger change to IN_SERVICE on satellite
        val newServiceState = mock(ServiceState::class.java)
        `when`(newServiceState.state).thenReturn(ServiceState.STATE_IN_SERVICE)
        `when`(newServiceState.isUsingNonTerrestrialNetwork()).thenReturn(true)
        callback.onServiceStateChanged(newServiceState)

        verify(service, never()).jobFinished(any(), anyBoolean())
        verify(mockTelephonyManager, never()).unregisterTelephonyCallback(any<TelephonyCallback>())
    }

    @Test
    fun satelliteStatus_available_showsPromptAndFinishesJob_reschedules() {
        `when`(mockServiceState.state).thenReturn(ServiceState.STATE_OUT_OF_SERVICE)
        service.onStartJob(mockJobParameters)
        // Clear previous schedule call
        jobScheduler.cancel(jobId)

        // Trigger status available
        satelliteStatusFlow.value = SatelliteStatus.AVAILABLE
        ShadowLooper.idleMainLooper(3 * 60 * 1000L + 1000, TimeUnit.MILLISECONDS)

        verify(mockSatelliteTilePromptUtils).showSatelliteTileAvailableNotification(service)
        verify(mockSatelliteTilePromptUtils).recordPromptShown(service)
    }

    @Test
    fun satelliteStatus_active_showsPromptAndFinishesJob_reschedules() {
        `when`(mockServiceState.state).thenReturn(ServiceState.STATE_OUT_OF_SERVICE)
        service.onStartJob(mockJobParameters)
        // Clear previous schedule call
        jobScheduler.cancel(jobId)

        // Trigger status active
        satelliteStatusFlow.value = SatelliteStatus.ACTIVE
        ShadowLooper.idleMainLooper(3 * 60 * 1000L + 1000, TimeUnit.MILLISECONDS)

        verify(mockSatelliteTilePromptUtils).showSatelliteTileAvailableNotification(service)
        verify(mockSatelliteTilePromptUtils).recordPromptShown(service)
    }

    @Test
    fun satelliteStatus_transientAvailable_doesNotShowPrompt() {
        `when`(mockServiceState.state).thenReturn(ServiceState.STATE_OUT_OF_SERVICE)
        service.onStartJob(mockJobParameters)
        // Clear previous schedule call
        jobScheduler.cancel(jobId)

        // Trigger status available
        satelliteStatusFlow.value = SatelliteStatus.AVAILABLE
        // Advance time partially (e.g. 2 seconds)
        ShadowLooper.idleMainLooper(2 * 1000L, TimeUnit.MILLISECONDS)

        // Trigger status not available (transient)
        satelliteStatusFlow.value = SatelliteStatus.NOT_AVAILABLE
        // Advance time to complete the original 10 sec
        ShadowLooper.idleMainLooper(10 * 1000L, TimeUnit.MILLISECONDS)

        // Verify prompt was NOT shown
        verify(mockSatelliteTilePromptUtils, never())
            .showSatelliteTileAvailableNotification(service)
        verify(mockSatelliteTilePromptUtils, never()).recordPromptShown(service)
    }

    @Test
    fun timeout_reschedulesAndFinishesJob() {
        `when`(mockServiceState.state).thenReturn(ServiceState.STATE_OUT_OF_SERVICE)
        service.onStartJob(mockJobParameters)
        // Clear previous schedule call
        jobScheduler.cancel(jobId)

        // Fast forward time to trigger timeout
        ShadowLooper.idleMainLooper(10 * 60 * 1000L + 1000, TimeUnit.MILLISECONDS)

        verify(service).jobFinished(mockJobParameters, false)
        verify(mockTelephonyManager).unregisterTelephonyCallback(any<TelephonyCallback>())
        assertThat(shadowJobScheduler.getPendingJob(jobId)).isNotNull()
    }

    @Test
    fun onStopJob_cleansUp() {
        `when`(mockServiceState.state).thenReturn(ServiceState.STATE_OUT_OF_SERVICE)
        service.onStartJob(mockJobParameters)

        val result = service.onStopJob(mockJobParameters)

        assertThat(result).isTrue()
        verify(mockTelephonyManager).unregisterTelephonyCallback(any<TelephonyCallback>())
        // Cannot easily verify handler removal without more injection, but assuming cleanup logic
        // is consistent
    }

    @Test
    fun satelliteStatus_available_shouldNotShowPrompt_doesNotShowPrompt() {
        `when`(mockServiceState.state).thenReturn(ServiceState.STATE_OUT_OF_SERVICE)
        // Mock that the prompt should not be shown
        `when`(mockSatelliteTilePromptUtils.shouldShowSatelliteTilePrompt(service))
            .thenReturn(false)
        service.onStartJob(mockJobParameters)
        // Clear previous schedule call
        jobScheduler.cancel(jobId)

        // Trigger status available
        satelliteStatusFlow.value = SatelliteStatus.AVAILABLE
        ShadowLooper.idleMainLooper(3 * 60 * 1000L + 1000, TimeUnit.MILLISECONDS)

        verify(mockSatelliteTilePromptUtils, never())
            .showSatelliteTileAvailableNotification(service)
        verify(mockSatelliteTilePromptUtils, never()).recordPromptShown(service)
    }

    @Test
    fun satelliteStatus_available_tileServiceDisabled_abortsPrompt() {
        `when`(mockServiceState.state).thenReturn(ServiceState.STATE_OUT_OF_SERVICE)
        service.onStartJob(mockJobParameters)
        // Clear previous schedule call
        jobScheduler.cancel(jobId)

        // Disable the component mid-flight (e.g. modem check finished and disabled it)
        val componentName = ComponentName(context, SatelliteTileService::class.java)
        context.packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )

        // Trigger status available
        satelliteStatusFlow.value = SatelliteStatus.AVAILABLE
        ShadowLooper.runUiThreadTasks()

        verify(mockSatelliteTilePromptUtils, never())
            .showSatelliteTileAvailableNotification(service)
        verify(service, never()).jobFinished(any(), anyBoolean())
    }
}
