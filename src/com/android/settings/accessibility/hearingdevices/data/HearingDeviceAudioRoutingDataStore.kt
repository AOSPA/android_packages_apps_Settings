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

package com.android.settings.accessibility.hearingdevices.data

import android.bluetooth.BluetoothProfile
import android.content.Context
import android.media.AudioDeviceAttributes
import android.media.audiopolicy.AudioProductStrategy
import android.util.Log
import com.android.settings.bluetooth.Utils
import com.android.settingslib.bluetooth.CachedBluetoothDevice
import com.android.settingslib.bluetooth.HearingAidAudioRoutingConstants.RoutingValue
import com.android.settingslib.bluetooth.HearingAidAudioRoutingHelper
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.datastore.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HearingDeviceAudioRoutingDataStore(
    private val context: Context,
    private val attributeSdkUsageList: IntArray,
    private val preferenceKey: String,
    private val settingsProviderKey: String,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val settingsStore: SettingsStore = SettingsSecureStore.get(context),
    private val audioRoutingHelper: HearingAidAudioRoutingHelper =
        HearingAidAudioRoutingHelper(context),
) : AbstractKeyedDataObservable<String>(), KeyedObserver<String>, KeyValueStore {

    private val localBluetoothManager = Utils.getLocalBluetoothManager(context)

    override fun contains(key: String) = settingsStore.contains(settingsProviderKey)

    override fun onFirstObserverAdded() {
        settingsStore.addObserver(settingsProviderKey, this, HandlerExecutor.main)
    }

    override fun onLastObserverRemoved() {
        settingsStore.removeObserver(settingsProviderKey, this)
    }

    override fun onKeyChanged(key: String, reason: Int) {
        notifyChange(preferenceKey, reason)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
        val routingValue = settingsStore.getInt(settingsProviderKey) ?: RoutingValue.AUTO
        val isRouteToHearing =
            (routingValue == RoutingValue.AUTO || routingValue == RoutingValue.HEARING_DEVICE)
        return isRouteToHearing as T?
    }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        val isChecked = value as? Boolean ?: false
        val routeToHearing = if (isChecked) RoutingValue.AUTO else RoutingValue.BUILTIN_DEVICE

        settingsStore.setInt(settingsProviderKey, routeToHearing)

        coroutineScope.launch {
            val activeHearingDevice = getActiveHearingDevice()
            val hearingDeviceAttributes: AudioDeviceAttributes? =
                activeHearingDevice?.let {
                    audioRoutingHelper.getMatchedHearingDeviceAttributesForOutput(it)
                }

            val supportedStrategies =
                audioRoutingHelper.getSupportedStrategies(attributeSdkUsageList)
            if (supportedStrategies.isNotEmpty()) {
                trySetAudioRouting(supportedStrategies, hearingDeviceAttributes, routeToHearing)
            }
        }
    }

    private fun getActiveHearingDevice(): CachedBluetoothDevice? {
        val cachedDeviceManager = localBluetoothManager?.cachedDeviceManager ?: return null

        return cachedDeviceManager.cachedDevicesCopy.firstOrNull { device ->
            device.isHearingDevice &&
                (device.isActiveDevice(BluetoothProfile.HEARING_AID) ||
                    device.isActiveDevice(BluetoothProfile.LE_AUDIO))
        }
    }

    private fun trySetAudioRouting(
        supportedStrategies: List<AudioProductStrategy>,
        hearingDevice: AudioDeviceAttributes?,
        @RoutingValue routingValue: Int,
    ) {
        if (supportedStrategies.isEmpty() || hearingDevice == null) {
            return
        }

        val result =
            audioRoutingHelper.configureRoutingStrategies(
                supportedStrategies,
                hearingDevice,
                routingValue,
            )
        if (!result && DEBUG) {
            Log.d(TAG, "Fail to configureRoutingStrategies for routingValue: $routingValue")
        }
    }

    companion object {
        private const val TAG = "HearingDeviceAudioRoutingDataStore"
        private const val DEBUG = false
    }
}
