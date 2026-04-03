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

import android.app.Application
import android.content.Context
import android.os.Process
import com.android.settings.metrics.SettingsRemoteOpMetricsLogger
import com.android.settingslib.graph.PreferenceGetterApiHandler
import com.android.settingslib.graph.PreferenceGetterRequest
import com.android.settingslib.graph.PreferenceGetterResponse
import com.android.settingslib.graph.PreferenceSetterApiHandler
import com.android.settingslib.graph.PreferenceSetterRequest
import com.android.settingslib.graph.PreferenceSetterResponse
import com.android.settingslib.ipc.ApiPermissionChecker

/**
 * Manages the connection to the PreferenceService by calling handlers directly. This is one layer
 * deeper than the IPC stack.
 */
class PreferenceServiceClient(context: Context) : AutoCloseable {

    private val application = context.applicationContext as Application

    private val getApiHandler =
        PreferenceGetterApiHandler(
            3,
            ApiPermissionChecker.alwaysAllow(),
            SettingsRemoteOpMetricsLogger(),
        )

    private val setApiHandler =
        PreferenceSetterApiHandler(
            2,
            ApiPermissionChecker.alwaysAllow(),
            SettingsRemoteOpMetricsLogger(),
        )

    /** Invokes the preference getter API directly. */
    suspend fun getPreferences(request: PreferenceGetterRequest): PreferenceGetterResponse {
        return getApiHandler.invoke(application, Process.myPid(), Process.myUid(), request)
    }

    /** Invokes the preference setter API directly. */
    suspend fun setPreferenceValue(request: PreferenceSetterRequest): PreferenceSetterResponse {
        return setApiHandler.invoke(application, Process.myPid(), Process.myUid(), request)
    }

    override fun close() {
        // No connection to close as we are calling directly
    }
}
