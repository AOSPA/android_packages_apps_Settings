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

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.UserHandle
import android.telephony.ServiceState
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import com.android.internal.annotations.VisibleForTesting
import com.android.settings.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "SatelliteEligibilityJobService"

/**
 * A [JobService] that monitors network status and satellite eligibility when the device loses
 * cellular data.
 *
 * This service is scheduled when the device is out of service or powered off. It stays alive for a
 * short period (e.g., 10 minutes) to listen for:
 * 1. Cellular data restoration (cancels the job).
 * 2. Satellite eligibility or mode activation (shows a prompt notification).
 */
open class SatelliteEligibilityJobService : JobService() {

    companion object {
        private const val TIMEOUT_MS = 10 * 60 * 1000L // 10 minutes
        private const val OOS_DEBOUNCE_MS = 10 * 1000L // 10 sec

        @VisibleForTesting internal var satelliteTilePromptUtils = SatelliteTilePromptUtils()

        /**
         * Schedules a JobService to monitor the data registration state.
         *
         * The job will be triggered when the data registration state changes (e.g., losing
         * service).
         *
         * @param forceImmediate If true, the job runs immediately (override deadline 0).
         */
        fun schedule(context: Context, forceImmediate: Boolean = false) {
            // Guard against scheduling if the feature is disabled (or in test harness)
            if (!SatelliteTileStateReceiver.isSatelliteTileFeatureEnabled(context)) {
                Log.i(TAG, "Feature disabled. Skipping job scheduling.")
                return
            }

            // Guard against scheduling if the tile component has been disabled (e.g., by runtime modem check)
            if (!SatelliteTileStateReceiver.isTileServiceEnabled(context)) {
                Log.i(TAG, "SatelliteTileService component is disabled. Skipping job scheduling.")
                return
            }

            val subId = SubscriptionManager.getDefaultDataSubscriptionId()
            if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                Log.i(TAG, "Invalid subscription ID, skipping job scheduling.")
                return
            }

            // We need to check if all prompts have already been shown.
            // If so, we don't need to do anything.
            if (satelliteTilePromptUtils.areAllPromptsShown(context)) {
                Log.i(TAG, "All prompts shown, skipping job scheduling.")
                return
            }

            val jobScheduler = context.getSystemService(JobScheduler::class.java)
            if (jobScheduler == null) {
                Log.e(TAG, "JobScheduler is null.")
                return
            }

            val jobId = context.resources.getInteger(R.integer.satellite_eligibility_job)
            val uri =
                android.provider.Telephony.ServiceStateTable.getUriForSubscriptionIdAndField(
                    subId,
                    "data_reg_state",
                )
            val builder =
                JobInfo.Builder(
                        jobId,
                        ComponentName(context, SatelliteEligibilityJobService::class.java),
                    )
                    .addTriggerContentUri(
                        JobInfo.TriggerContentUri(
                            uri,
                            JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS,
                        )
                    )
                    .setTriggerContentUpdateDelay(1000) // Debounce 1s
                    .setTriggerContentMaxDelay(10000) // Max delay 10s

            if (forceImmediate) {
                builder.setOverrideDeadline(0)
            }

            val result = jobScheduler.schedule(builder.build())
            if (result == JobScheduler.RESULT_SUCCESS) {
                logd { "Successfully scheduled SatelliteEligibilityJobService for subId=$subId" }
            } else {
                Log.e(TAG, "Failed to schedule SatelliteEligibilityJobService")
            }
        }
    }

    private var telephonyManager: TelephonyManager? = null
    private var telephonyCallback: TelephonyCallback? = null
    private var scope: CoroutineScope? = null

    override fun onStartJob(params: JobParameters): Boolean {
        if (UserHandle.myUserId() != UserHandle.USER_SYSTEM) {
            Log.i(TAG, "Not running on system user, ignoring.")
            return false
        }
        Log.i(TAG, "onStartJob: ${params.jobId}")

        // Guard against executing if the feature is disabled (kills legacy jobs)
        if (!SatelliteTileStateReceiver.isSatelliteTileFeatureEnabled(this)) {
            Log.i(TAG, "Feature disabled. Stopping job.")
            return false // Job is done, do not reschedule
        }

        if (!SatelliteTileStateReceiver.isTileServiceEnabled(this)) {
            Log.i(TAG, "SatelliteTileService component is disabled. Stopping job.")
            return false // Job is done, do not reschedule
        }

        val subId = SubscriptionManager.getDefaultDataSubscriptionId()
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            Log.i(TAG, "Invalid subscription ID, finishing job.")
            return false
        }

        telephonyManager =
            getSystemService(TelephonyManager::class.java)?.createForSubscriptionId(subId)
        if (telephonyManager == null) {
            Log.i(TAG, "TelephonyManager is null.")
            return false
        }

        // If all prompts have been shown (or the tile was added), do not proceed.
        if (satelliteTilePromptUtils.areAllPromptsShown(this)) {
            Log.i(TAG, "All prompts shown; stopping job.")
            return false
        }

        val serviceState = telephonyManager?.serviceState
        val state = serviceState?.state
        Log.i(TAG, "Current ServiceState: $state")

        if (state == ServiceState.STATE_IN_SERVICE) {
            val isNtn = serviceState?.isUsingNonTerrestrialNetwork() == true
            if (!isNtn) {
                Log.i(TAG, "Device is IN_SERVICE (Terrestrial), rescheduling job.")
                schedule(this)
                return false
            }
            Log.i(TAG, "Device is IN_SERVICE (Satellite), proceeding to monitor.")
        }

        scope = CoroutineScope(Dispatchers.Main + Job())
        scope?.launch {
            // Schedule timeout
            launch {
                kotlinx.coroutines.delay(TIMEOUT_MS)
                Log.i(TAG, "Timed out waiting for satellite eligibility after 10m.")
                cleanup()
                schedule(this@SatelliteEligibilityJobService)
                jobFinished(params, false)
            }

            // Monitor satellite status
            SatelliteStateRepository.getInstance(this@SatelliteEligibilityJobService)
                .satelliteStatus
                .collectLatest { status ->
                    Log.i(TAG, "Received satelliteStatus update: $status")
                    if (status == SatelliteStatus.AVAILABLE) {
                        Log.i(TAG, "Satellite Status: $status. Waiting for 10 sec  debounce.")
                        kotlinx.coroutines.delay(OOS_DEBOUNCE_MS)
                        Log.i(TAG, "Satellite Status sustained at $status. Showing prompt.")
                        showPromptAndFinish(params)
                    } else if (status == SatelliteStatus.ACTIVE) {
                        Log.i(TAG, "Satellite Status $status. Showing prompt.")
                        showPromptAndFinish(params)
                    } else {
                        Log.i(TAG, "Satellite Status is $status, debounce timer (if any) cancelled.")
                    }
                }
        }

        // Register callback to listen for state changes
        registerTelephonyCallback(params)

        return true // Work is ongoing
    }

    override fun onStopJob(params: JobParameters): Boolean {
        Log.i(TAG, "onStopJob called for jobId: ${params.jobId}. Reason: ${params.stopReason}")
        cleanup()
        return true
    }

    private fun registerTelephonyCallback(params: JobParameters) {
        telephonyCallback =
            object : TelephonyCallback(), TelephonyCallback.ServiceStateListener {

                override fun onServiceStateChanged(serviceState: ServiceState) {
                    if (serviceState.state == ServiceState.STATE_IN_SERVICE) {
                        val isNtn = serviceState.isUsingNonTerrestrialNetwork()
                        if (!isNtn) {
                            Log.i(TAG, "Cellular service restored (Terrestrial), rescheduling job.")
                            cleanup()
                            schedule(this@SatelliteEligibilityJobService)
                            jobFinished(params, false)
                        } else {
                            logd {
                                "Device is IN_SERVICE (Satellite), ignoring service restoration."
                            }
                        }
                    }
                }
            }

        telephonyManager?.registerTelephonyCallback(mainExecutor, telephonyCallback!!)
        logd { "Registered telephony callback." }
    }

    private fun showPromptAndFinish(params: JobParameters) {
        if (!SatelliteTileStateReceiver.isTileServiceEnabled(this)) {
            Log.w(TAG, "SatelliteTileService component is disabled. Aborting prompt.")
            cleanup()
            jobFinished(params, false)
            return
        }

        if (satelliteTilePromptUtils.shouldShowSatelliteTilePrompt(this)) {
            satelliteTilePromptUtils.showSatelliteTileAvailableNotification(this)
            satelliteTilePromptUtils.recordPromptShown(this)
        } else {
            Log.i(TAG, "Should not show prompt yet, interval has not passed.")
        }
        cleanup()
        // Schedule the job again in the case that the user doesn't interact with the
        // notification to add the tile, or for the next retry interval. We will fully stop
        // the job once all prompts have been shown.
        schedule(this@SatelliteEligibilityJobService)
        jobFinished(params, false)
    }

    private fun cleanup() {
        scope?.cancel()
        scope = null
        if (telephonyCallback != null) {
            telephonyManager?.unregisterTelephonyCallback(telephonyCallback!!)
            telephonyCallback = null
        }
    }
}

private inline fun logd(message: () -> String) {
    if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, message())
    }
}
