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

package com.android.settings.appfunctions.providers

import android.content.Context
import android.util.Log
import com.android.settings.appfunctions.DeviceStateCategory
import com.android.settings.appfunctions.sources.AdaptiveBrightnessStateSource
import com.android.settings.appfunctions.sources.AppsStorageStateSource
import com.android.settings.appfunctions.sources.BatterySaverStateSource
import com.android.settings.appfunctions.sources.BatteryStatusStateSource
import com.android.settings.appfunctions.sources.BubblesStateSource
import com.android.settings.appfunctions.sources.DeviceStateSource
import com.android.settings.appfunctions.sources.LockScreenStateSource
import com.android.settings.appfunctions.sources.ManagedProfileStateSource
import com.android.settings.appfunctions.sources.MediaOutputStateSource
import com.android.settings.appfunctions.sources.NfcStateSource
import com.android.settings.appfunctions.sources.NotificationHistoryStateSource
import com.android.settings.appfunctions.sources.NotificationsStateSource
import com.android.settings.appfunctions.sources.OpenByDefaultStateSource
import com.android.settings.appfunctions.sources.RecentAppsStateSource
import com.android.settings.appfunctions.sources.ScreenTimeoutStateSource
import com.android.settings.appfunctions.sources.SharedDeviceStateData
import com.android.settings.appfunctions.sources.ZenModesStateSource
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * A [DeviceStateProvider] that gathers device state information directly from Android APIs rather
 * than using Catalyst.
 *
 * @param context The application context.
 */
class AndroidApiStateProvider(private val context: Context) : DeviceStateProvider {

    // List of all active DeviceStateSource
    private val settingStates: List<DeviceStateSource> =
        listOf(
            AdaptiveBrightnessStateSource(),
            AppsStorageStateSource(),
            BatterySaverStateSource(),
            BatteryStatusStateSource(),
            BubblesStateSource(),
            LockScreenStateSource(),
            ManagedProfileStateSource(),
            MediaOutputStateSource(),
            NfcStateSource(),
            NotificationHistoryStateSource(),
            NotificationsStateSource(),
            OpenByDefaultStateSource(),
            RecentAppsStateSource(),
            ScreenTimeoutStateSource(),
            ZenModesStateSource(),
            // Add other sources instances here
        )

    override suspend fun provide(requestCategory: DeviceStateCategory): DeviceStateProviderResult {
        val sharedDeviceStateData = SharedDeviceStateData(context)

        val states = coroutineScope {
            settingStates
                .filter { it.category == requestCategory }
                .map { provider ->
                    async {
                        val providerName = provider::class.simpleName
                        try {
                            Log.v(TAG, "Getting device state from $providerName")
                            val state = provider.get(context, sharedDeviceStateData)
                            Log.v(TAG, "Got device state from $providerName")
                            state
                        } catch (e: Exception) {
                            Log.e(TAG, "Error getting device state from $providerName", e)
                            null
                        }
                    }
                }
                .mapNotNull { it.await() }
        }

        return DeviceStateProviderResult(states = states)
    }

    companion object {
        private const val TAG = "AndroidApiStateProvider"
    }
}
