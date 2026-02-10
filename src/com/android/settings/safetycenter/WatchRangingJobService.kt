/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.safetycenter

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.database.ContentObserver
import android.hardware.biometrics.Flags
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.provider.Settings
import android.proximity.IProximityResultCallback
import android.proximity.ProximityResultCode
import android.safetycenter.SafetyEvent
import android.safetycenter.SafetyEvent.SAFETY_EVENT_TYPE_SOURCE_STATE_CHANGED
import android.security.authenticationpolicy.AuthenticationPolicyManager
import android.util.Log
import com.android.internal.annotations.VisibleForTesting
import com.android.settingslib.utils.ThreadUtils
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import java.util.concurrent.Executor

/**
 * A [JobService] that checks for watch ranging availability and updates the global setting
 * [Settings.Global.WATCH_RANGING_SUPPORTED_BY_PRIMARY_DEVICE].
 *
 * This service is intended for background syncing of availability only. Other time-sensitive watch
 * ranging logic should not be added to this class.
 *
 * This service also registers a [ContentObserver] to listen for changes to this setting and updates
 * the Identity Check safety source data accordingly.
 */
class WatchRangingJobService : JobService() {

    override fun onStartJob(params: JobParameters): Boolean {
        finishJobWhenWatchRangingStatusKnown(ThreadUtils.getBackgroundExecutor(), params)
        return SERVICE_RUNNING
    }

    override fun onStopJob(params: JobParameters): Boolean {
        return RESCHEDULE_JOB
    }

    @VisibleForTesting
    fun finishJobWhenWatchRangingStatusKnown(executor: Executor, params: JobParameters) {
        executor.execute {
            val handlerThread = HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND)
            handlerThread.start()
            WatchContentObserver(this, Handler(handlerThread.looper)).registerContentObserver()
            val watchRangingFuture = getWatchRangingAvailabilityFuture()
            watchRangingFuture.addListener(
                {
                    try {
                        val isSupported = watchRangingFuture.get()
                        setWatchRangingSupported(isSupported)
                        Log.d(TAG, "Watch ranging status updated to $isSupported")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to get watch ranging device status", e)
                    } finally {
                        jobFinished(params, DO_NOT_RESCHEDULE)
                    }
                },
                executor,
            )
        }
    }

    private companion object {
        const val TAG = "WatchRangingJobService"
        const val DO_NOT_RESCHEDULE = false
        const val SERVICE_RUNNING = true
        const val RESCHEDULE_JOB = true

        private fun Context.getWatchRangingAvailabilityFuture(): ListenableFuture<Boolean> {
            val future = SettableFuture.create<Boolean>()
            val authenticationPolicyManager =
                getSystemService(AuthenticationPolicyManager::class.java)

            if (Flags.identityCheckWatch()) {
                if (authenticationPolicyManager == null) {
                    Log.e(TAG, "Authentication policy manager is null. Setting future to false.")
                    future.set(false)
                } else {
                    authenticationPolicyManager.isWatchRangingAvailable(
                        object : IProximityResultCallback.Stub() {
                            override fun onError(errorCode: Int) {
                                val supported =
                                    errorCode !=
                                        ProximityResultCode.PRIMARY_DEVICE_RANGING_NOT_SUPPORTED
                                future.set(supported)
                            }

                            override fun onSuccess(p0: Int) {
                                future.set(true)
                            }
                        }
                    )
                }
            } else {
                future.set(false)
            }
            return future
        }

        private fun Context.setWatchRangingSupported(boolean: Boolean) {
            Settings.Global.putInt(
                contentResolver,
                Settings.Global.WATCH_RANGING_SUPPORTED_BY_PRIMARY_DEVICE,
                if (boolean) 1 else 0,
            )
        }
    }

    /**
     * A [ContentObserver] that listens for changes to the watch ranging supported setting and
     * notifies the Identity Check safety source.
     */
    class WatchContentObserver(private val context: Context, handler: Handler) :
        ContentObserver(handler) {
        private val WATCH_RANGING_SUPPORTED_BY_PRIMARY_DEVICE =
            Settings.Global.getUriFor(Settings.Global.WATCH_RANGING_SUPPORTED_BY_PRIMARY_DEVICE)

        fun registerContentObserver() {
            context.contentResolver.registerContentObserver(
                WATCH_RANGING_SUPPORTED_BY_PRIMARY_DEVICE,
                false, /* notifyForDescendants */
                this,
            )
        }

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            if (WATCH_RANGING_SUPPORTED_BY_PRIMARY_DEVICE.equals(uri)) {
                IdentityCheckSafetySource.setSafetySourceData(
                    context,
                    SafetyEvent.Builder(SAFETY_EVENT_TYPE_SOURCE_STATE_CHANGED).build(),
                )
            }
        }
    }
}
