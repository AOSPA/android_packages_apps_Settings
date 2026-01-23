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
import android.content.pm.PackageManager
import android.os.Process
import android.os.SystemClock
import androidx.annotation.OpenForTesting
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.android.internal.util.FrameworkStatsLog

/**
 * Logger for Credential Manager related events in Settings.
 *
 * This class handles the reporting of user interactions—such as adding, enabling, or disabling
 * credential providers—to [FrameworkStatsLog]. It also acts as a [DefaultLifecycleObserver] to
 * automatically calculate and log the duration of "outbound sessions", which occur when a user is
 * redirected from Settings to a credential service provider.
 *
 * Note that if a [Lifecycle] is provided during initialization, this class will automatically:
 * 1. Finalize and log a return event on [onResume], provided [recordOutboundIntentLaunch] was
 *    called.
 * 2. Clear pending sessions on [onDestroy] to prevent stale data.
 */
@OpenForTesting
open class CredmanMetricsLogger
@JvmOverloads
constructor(context: Context, lifecycle: Lifecycle? = null) : DefaultLifecycleObserver {

    private val packageManager = context.applicationContext.packageManager

    /** Holds state for timing the duration a user was redirected away from Settings. */
    private data class OutboundSession(val startTime: Long, val packageName: String?)

    private var outboundSession: OutboundSession? = null

    init {
        lifecycle?.addObserver(this)
    }

    /**
     * Records the start of an outbound interaction.
     *
     * Call this when launching an Intent that takes the user out of the current Settings page
     * (e.g., to a provider's configuration screen). The time elapsed until the next [onResume] will
     * be tracked and logged.
     */
    @JvmOverloads
    fun recordOutboundIntentLaunch(packageName: String? = null) {
        outboundSession = OutboundSession(SystemClock.elapsedRealtime(), packageName)
    }

    override fun onResume(owner: LifecycleOwner) {
        outboundSession?.let { session ->
            val duration = SystemClock.elapsedRealtime() - session.startTime
            logReturnFromServiceProviderEvent(session.packageName, duration)
            outboundSession = null
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        outboundSession = null
    }

    /**
     * Logs an event when a user returns to Settings after being redirected to a service provider.
     */
    @JvmOverloads
    fun logReturnFromServiceProviderEvent(packageName: String? = null, durationMs: Long = 0L) =
        logEvent(
            FrameworkStatsLog
                .CREDENTIAL_MANAGER_SETTINGS_EVENT_REPORTED__EVENT_TYPE__RETURN_FROM_SERVICE_PROVIDER,
            packageName,
            durationMs,
        )

    /** Logs an event when a user edits the current preferred service. */
    @JvmOverloads
    fun logEditPreferredServiceEvent(packageName: String? = null) =
        logEvent(
            FrameworkStatsLog
                .CREDENTIAL_MANAGER_SETTINGS_EVENT_REPORTED__EVENT_TYPE__EDIT_PREFERRED_SERVICE,
            packageName,
        )

    /** Logs an event when a user adds a provider to the preferred services list. */
    fun logAddPreferredServiceEvent() =
        logEvent(
            FrameworkStatsLog
                .CREDENTIAL_MANAGER_SETTINGS_EVENT_REPORTED__EVENT_TYPE__ADD_PREFERRED_SERVICE
        )

    /** Logs an event when a user enables a provider in the additional services list. */
    @JvmOverloads
    fun logEnableAdditionalServiceEvent(packageName: String? = null) =
        logEvent(
            FrameworkStatsLog
                .CREDENTIAL_MANAGER_SETTINGS_EVENT_REPORTED__EVENT_TYPE__ENABLE_ADDITIONAL_SERVICE,
            packageName,
        )

    /** Logs an event when a user disables a provider in the additional services list. */
    @JvmOverloads
    fun logDisableAdditionalServiceEvent(packageName: String? = null) =
        logEvent(
            FrameworkStatsLog
                .CREDENTIAL_MANAGER_SETTINGS_EVENT_REPORTED__EVENT_TYPE__DISABLE_ADDITIONAL_SERVICE,
            packageName,
        )

    /**
     * Logs an event when a user attempts to add a provider to the additional services list but hit
     * the maximum provider limit.
     */
    @JvmOverloads
    fun logHitAdditionalServiceLimitEvent(packageName: String? = null) =
        logEvent(
            FrameworkStatsLog
                .CREDENTIAL_MANAGER_SETTINGS_EVENT_REPORTED__EVENT_TYPE__HIT_ADDITIONAL_SERVICE_LIMIT,
            packageName,
        )

    /**
     * Logs a Credential Manager Settings event to [FrameworkStatsLog].
     *
     * @param eventType The event type defined in [FrameworkStatsLog].
     * @param packageName The package name of the service provider involved, if applicable.
     * @param durationMs Duration of the event (in milliseconds), if applicable.
     */
    @OpenForTesting
    @JvmOverloads
    open fun logEvent(eventType: Int, packageName: String? = null, durationMs: Long = 0L) {
        FrameworkStatsLog.write(
            FrameworkStatsLog.CREDENTIAL_MANAGER_SETTINGS_EVENT_REPORTED,
            eventType,
            getUid(packageName),
            durationMs,
        )
    }

    @VisibleForTesting
    @OpenForTesting
    internal open fun getUid(packageName: String?): Int {
        return packageName?.let {
            try {
                packageManager.getPackageUid(it, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                Process.INVALID_UID
            }
        } ?: Process.INVALID_UID
    }
}
