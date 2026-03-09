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

package com.android.settings.wifi.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import com.android.wifitrackerlib.SavedNetworkTracker
import com.android.wifitrackerlib.WifiEntry
import java.time.Clock
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

/** Lightweight representation of a saved Wi-Fi network. */
data class SavedNetworkInfo(val ssid: String, val key: String) {
    val lookupKey: String
        get() = "${key.hashCode()}_$ssid"
}

/** Data model containing internal Wi-Fi entries. */
data class SavedNetworksModel(
    val savedEntries: List<WifiEntry> = emptyList(),
    val subscriptionEntries: List<WifiEntry> = emptyList(),
)

/** Repository for managing saved Wi-Fi networks via [SavedNetworkTracker]. */
open class SavedNetworkRepository
@JvmOverloads
constructor(
    private val context: Context,
    private val scope: CoroutineScope = DEFAULT_SCOPE,
    savedNetworkTracker: SavedNetworkTracker? = null,
) {
    private val wifiManager: WifiManager? =
        context.applicationContext.getSystemService(WifiManager::class.java)

    private var workerThread: HandlerThread? = null

    /** Internal or injected tracker instance. */
    private val tracker: SavedNetworkTracker = savedNetworkTracker ?: initTracker()

    private val isTrackerStarted = AtomicBoolean(false)

    /** Private backing flow for Wi-Fi entries updates. */
    private val _wifiEntries =
        MutableSharedFlow<SavedNetworksModel>(
            replay = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    /** Reactive StateFlow emitting the latest SavedNetworksModel. */
    val wifiEntries: StateFlow<SavedNetworksModel> =
        _wifiEntries
            .onStart { emit(getCurrentModel()) }
            .mergeWith(trackerLifecycleFlow())
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(LIFECYCLE_GRACE_PERIOD_MS),
                initialValue = SavedNetworksModel(),
            )

    init {
        startTracker()
        _wifiEntries.tryEmit(getCurrentModel())

        scope.launch {
            refresh()
            delay(LIFECYCLE_GRACE_PERIOD_MS)
            if (_wifiEntries.subscriptionCount.value == 0) {
                stopTracker()
            }
        }
    }

    private fun trackerLifecycleFlow(): Flow<SavedNetworksModel> = callbackFlow {
        startTracker()
        trySend(getCurrentModel())
        awaitClose { stopTracker() }
    }

    private fun initTracker(): SavedNetworkTracker {
        val callback =
            object : SavedNetworkTracker.SavedNetworkTrackerCallback {
                override fun onSavedWifiEntriesChanged() {
                    _wifiEntries.tryEmit(getCurrentModel())
                }

                override fun onSubscriptionWifiEntriesChanged() {
                    _wifiEntries.tryEmit(getCurrentModel())
                }

                override fun onWifiStateChanged() {}
            }

        val dummyLifecycle =
            object : Lifecycle() {
                override fun addObserver(observer: LifecycleObserver) {}

                override fun removeObserver(observer: LifecycleObserver) {}

                override val currentState: State
                    get() = State.STARTED
            }

        workerThread =
            HandlerThread("SavedNetworkRepositoryWorker", Process.THREAD_PRIORITY_BACKGROUND)
                .apply { start() }

        val connectivityManager =
            context.applicationContext.getSystemService(ConnectivityManager::class.java)!!

        return SavedNetworkTracker(
            dummyLifecycle,
            context,
            wifiManager!!,
            connectivityManager,
            Handler(Looper.getMainLooper()),
            Handler(workerThread!!.looper),
            Clock.system(ZoneOffset.UTC),
            MAX_SCAN_AGE_MS,
            SCAN_INTERVAL_MS,
            callback,
        )
    }

    private fun startTracker() {
        if (isTrackerStarted.compareAndSet(false, true)) {
            tracker.onStart()
        }
    }

    private fun stopTracker() {
        if (isTrackerStarted.compareAndSet(true, false)) {
            tracker.onStop()
        }
    }

    private fun getCurrentModel() =
        SavedNetworksModel(tracker.savedWifiEntries, tracker.subscriptionWifiEntries)

    /** Updates the flow with a fresh tracker snapshot. */
    suspend fun refresh() =
        withContext(Dispatchers.Default) {
            val isRunning = _wifiEntries.subscriptionCount.value > 0
            try {
                if (!isRunning) startTracker()
                _wifiEntries.emit(getCurrentModel())
                yield()
            } finally {
                if (!isRunning) stopTracker()
            }
        }

    /** Returns SavedNetworkInfo list. Polls tracker if initial data is missing. */
    suspend fun getSavedNetworksInfo(): List<SavedNetworkInfo> =
        withContext(Dispatchers.Default) {
            val cachedModel = wifiEntries.value
            val cachedEntries = cachedModel.savedEntries + cachedModel.subscriptionEntries
            if (cachedEntries.isNotEmpty()) {
                return@withContext cachedEntries.map { SavedNetworkInfo(it.title, it.key) }
            }

            val configuredCount = wifiManager?.configuredNetworks?.size ?: 0
            if (configuredCount > 0) {
                repeat(MAX_POLL_ATTEMPTS) {
                    val currentSnapshot = getCurrentModel()
                    val allEntries =
                        currentSnapshot.savedEntries + currentSnapshot.subscriptionEntries

                    if (allEntries.isNotEmpty()) {
                        _wifiEntries.emit(currentSnapshot)
                        return@withContext allEntries.map { SavedNetworkInfo(it.title, it.key) }
                    }
                    delay(POLL_INTERVAL_MS)
                }
                Log.w(TAG, "getSavedNetworksInfo: configs=$configuredCount, tracker empty")
            }
            emptyList()
        }

    open fun findSavedNetworkInfo(lookupKey: String?): SavedNetworkInfo? {
        if (lookupKey == null) return null
        return fetchSavedNetworksInfo().find { it.lookupKey == lookupKey }
    }

    open fun fetchSavedNetworksInfo(): List<SavedNetworkInfo> = runBlocking {
        try {
            getSavedNetworksInfo()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching saved networks info: $e")
            emptyList()
        }
    }

    /** Returns the latest WifiConfiguration from WifiManager for the given lookupKey. */
    open fun getWifiConfiguration(lookupKey: String?): WifiConfiguration? {
        if (lookupKey == null) return null
        val entry = findWifiEntry(lookupKey)
        if (entry == null) {
            Log.w(TAG, "getWifiConfiguration: Entry not found for $lookupKey")
            return null
        }
        val networkId = entry.wifiConfiguration?.networkId ?: WifiConfiguration.INVALID_NETWORK_ID
        val config = findWifiConfiguration(networkId)
        if (config == null) {
            Log.w(TAG, "getWifiConfiguration: Config not found for networkId $networkId")
        }
        return config
    }

    /** Saves modifications to the WifiConfiguration for the given lookupKey. */
    open fun setWifiConfiguration(lookupKey: String?, action: (WifiConfiguration) -> Unit) {
        if (wifiManager == null) {
            Log.e(TAG, "setWifiConfiguration: WifiManager is null")
            return
        }
        val config = getWifiConfiguration(lookupKey)
        if (config == null) {
            Log.w(TAG, "setWifiConfiguration: Cannot update, config is null for $lookupKey")
            return
        }
        action(config)
        wifiManager.save(config, null)
    }

    /** Internal helper to find a WifiEntry by lookupKey. */
    private fun findWifiEntry(lookupKey: String): WifiEntry? {
        val currentSnapshot = getCurrentModel()
        val allEntries = currentSnapshot.savedEntries + currentSnapshot.subscriptionEntries
        return allEntries.find { SavedNetworkInfo(it.title, it.key).lookupKey == lookupKey }
    }

    /** Finds the latest WifiConfiguration by networkId using privileged configs. */
    private fun findWifiConfiguration(networkId: Int): WifiConfiguration? {
        if (networkId == WifiConfiguration.INVALID_NETWORK_ID || wifiManager == null) return null
        return wifiManager.privilegedConfiguredNetworks?.find { it.networkId == networkId }
    }

    private fun <T> Flow<T>.mergeWith(other: Flow<T>): Flow<T> = merge(this, other)

    companion object {
        private const val TAG = "SavedNetworkRepository"
        private const val LIFECYCLE_GRACE_PERIOD_MS = 5000L
        private const val POLL_INTERVAL_MS = 50L
        private const val MAX_POLL_ATTEMPTS = 10
        private const val MAX_SCAN_AGE_MS = 15_000L
        private const val SCAN_INTERVAL_MS = 10_000L

        private val DEFAULT_SCOPE =
            CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineName(TAG))
    }
}
