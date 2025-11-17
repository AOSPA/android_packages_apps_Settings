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

import android.content.Context
import android.os.OutcomeReceiver
import android.service.settings.preferences.SettingsPreferenceServiceClient
import android.util.Log
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Manages the connection to the [SettingsPreferenceServiceClient]. The client is created
 * asynchronously upon the first call to [initialize] and cached for future use.
 */
object SettingsPreferenceServiceClientManager {
    private const val TAG = "SettingsPreferenceServiceClientManager"
    private const val PACKAGE_NAME = "com.android.settings"

    @Volatile
    var client: SettingsPreferenceServiceClient? = null
        private set

    private val backgroundExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    fun initialize(context: Context, executor: ExecutorService = backgroundExecutor) {
        if (client == null) {
            Log.d(TAG, "Attempting to connect to SettingsPreferenceServiceClient.")
            SettingsPreferenceServiceClient(
                context,
                PACKAGE_NAME,
                executor,
                object : OutcomeReceiver<SettingsPreferenceServiceClient, Exception> {
                    override fun onResult(result: SettingsPreferenceServiceClient) {
                        Log.d(TAG, "Successfully connected to SettingsPreferenceServiceClient")
                        this@SettingsPreferenceServiceClientManager.client = result
                    }

                    override fun onError(error: Exception) {
                        Log.e(TAG, "Error connecting to SettingsPreferenceServiceClient: $error")
                    }
                },
            )
        }
    }
}
