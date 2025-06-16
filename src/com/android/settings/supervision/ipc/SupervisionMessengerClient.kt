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
package com.android.settings.supervision.ipc

import android.content.Context
import android.content.Intent
import android.util.Log
import com.android.settings.supervision.PreferenceDataProvider
import com.android.settings.supervision.SupportedAppsProvider
import com.android.settings.supervision.systemSupervisionPackageName
import com.android.settingslib.ipc.MessengerServiceClient
import com.android.settingslib.supervision.SupervisionLog

/**
 * A specialized [MessengerServiceClient] for interacting with the system supervision service.
 *
 * This class extends [MessengerServiceClient] to provide specific functionality for communicating
 * with the system supervision service. It defines the action for binding to the system supervision
 * service, and provides a method for retrieving preference data from the supervision app.
 *
 * @param context The Android Context used for binding to the service.
 */
class SupervisionMessengerClient(context: Context) :
    MessengerServiceClient(context), PreferenceDataProvider, SupportedAppsProvider {

    override val serviceIntentFactory = { Intent(SUPERVISION_MESSENGER_SERVICE_BIND_ACTION) }

    override val packageName = context.systemSupervisionPackageName

    /**
     * Retrieves preference data from the system supervision app.
     *
     * This suspend function sends a request to the supervision app for the specified preference
     * keys and returns a map of preference data. If an error occurs during the communication, an
     * empty map is returned and the error is logged.
     *
     * @param keys A list of preference keys to retrieve.
     * @return A map of preference data, where the keys are the preference keys and the values are
     *   [PreferenceData] objects, or an empty map if an error occurs.
     */
    override suspend fun getPreferenceData(keys: List<String>): Map<String, PreferenceData> =
        try {
            val targetPackageName = packageName ?: return mapOf()

            invoke(targetPackageName, PreferenceDataApi(), PreferenceDataRequest(keys = keys))
                .await()
        } catch (e: Exception) {
            Log.e(SupervisionLog.TAG, "Error fetching Preference data from supervision app", e)
            mapOf()
        }

    /**
     * Retrieves supported apps for the specified filter keys.
     *
     * This suspend function sends a request to the supervision app for the specified content filter
     * keys and returns a map of supported apps. If an error occurs during the communication, an
     * empty map is returned and the error is logged.
     *
     * @param keys A list of strings representing the keys for content filters.
     * @return A map where the keys are the requested keys, and the values are the corresponding
     *   list of supported apps.
     */
    override suspend fun getSupportedApps(keys: List<String>): Map<String, List<SupportedApp>> =
        try {
            val targetPackageName = packageName ?: return mapOf()

            invoke(targetPackageName, SupportedAppsApi(), SupportedAppsRequest(keys = keys)).await()
        } catch (e: Exception) {
            Log.e(SupervisionLog.TAG, "Error fetching supported apps from supervision app", e)
            mapOf()
        }

    companion object {
        const val SUPERVISION_MESSENGER_SERVICE_BIND_ACTION =
            "android.app.supervision.action.SUPERVISION_MESSENGER_SERVICE"
    }
}
