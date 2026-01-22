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

package com.android.settings.network.tether

import android.annotation.SuppressLint
import android.content.Context
import android.net.TetheringManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import java.util.concurrent.Executor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Repository for tethering management. Singleton instance should be managed by an external
 * provider.
 */
@SuppressLint("ObsoleteSdkInt")
@RequiresApi(Build.VERSION_CODES.R)
open class TetheringRepository(context: Context, tetheringManager: TetheringManager? = null) {
    private val appContext: Context = context.applicationContext

    // Internal manager: uses constructor-passed manager or fetches from system
    private val tm: TetheringManager? =
        tetheringManager ?: appContext.getSystemService(TetheringManager::class.java)

    // Repository lifecycle scope for background synchronization
    private val repositoryScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Executor bridged from Coroutine Dispatcher for system callbacks
    private val tetheringExecutor: Executor = Dispatchers.IO.asExecutor()

    // Cache to store the latest tethered interfaces to minimize IPC overhead
    @Volatile private var lastTetheredIfaces: List<String> = emptyList()

    // Map to manage shared StateFlows per tethering type
    private val enabledFlows: MutableMap<TetherType, StateFlow<Boolean>> = mutableMapOf()

    enum class TetherType(val value: Int) {
        WIFI(TetheringManager.TETHERING_WIFI),
        USB(TetheringManager.TETHERING_USB),
        BLUETOOTH(TetheringManager.TETHERING_BLUETOOTH),
        ETHERNET(TetheringManager.TETHERING_ETHERNET),
    }

    /** Returns a concatenated string of the cached tethered interfaces. */
    @VisibleForTesting
    open fun getTetheredInterfaces(): String {
        return lastTetheredIfaces.joinToString(separator = ", ")
    }

    /** Provides a shared StateFlow for observing tethering state. */
    open fun isEnabledFlow(type: TetherType): StateFlow<Boolean> {
        if (tm == null) {
            Log.e(TAG, "isEnabledFlow: manager null")
            return MutableStateFlow(false)
        }
        return synchronized(enabledFlows) {
            enabledFlows.getOrPut(type) {
                createTetheringCallbackFlow(type)
                    .stateIn(
                        scope = repositoryScope,
                        started = SharingStarted.WhileSubscribed(5000),
                        initialValue = false,
                    )
            }
        }
    }

    /** Checks if tethering is active based on local cache. */
    open suspend fun isEnabled(type: TetherType): Boolean =
        withContext(Dispatchers.IO) {
            if (tm == null) return@withContext false
            if (lastTetheredIfaces.isEmpty()) refreshTetheredIfaces()
            checkInterfaceMatch(type, lastTetheredIfaces)
        }

    private fun refreshTetheredIfaces() {
        try {
            lastTetheredIfaces = tm?.tetheredIfaces?.toList() ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "refresh failed: $e")
        }
    }

    /** Enables or disables tethering for the specified type. */
    open suspend fun setEnabled(type: TetherType, enable: Boolean) =
        withContext(Dispatchers.IO) {
            if (tm == null) {
                Log.e(TAG, "setEnabled failed: manager null")
                return@withContext
            }
            if (enable) {
                startTetheringAction(type).firstOrNull()
            } else {
                tm.stopTethering(type.value)
            }
        }

    private fun createTetheringCallbackFlow(type: TetherType): Flow<Boolean> =
        callbackFlow {
                if (tm == null) {
                    send(false)
                    close()
                    return@callbackFlow
                }

                val callback =
                    object : TetheringManager.TetheringEventCallback {
                        override fun onTetheredInterfacesChanged(interfaces: List<String>) {
                            lastTetheredIfaces = interfaces
                            launch { send(checkInterfaceMatch(type, interfaces)) }
                        }

                        override fun onError(ifName: String, error: Int) {
                            Log.e(TAG, "onError: $ifName, err: $error")
                            launch { send(false) }
                        }
                    }

                tm.registerTetheringEventCallback(tetheringExecutor, callback)
                refreshTetheredIfaces()
                send(isEnabled(type))

                awaitClose { tm.unregisterTetheringEventCallback(callback) }
            }
            .flowOn(Dispatchers.IO)

    private fun startTetheringAction(type: TetherType): Flow<Unit> =
        callbackFlow {
                if (tm == null) {
                    close(IllegalStateException("manager null"))
                    return@callbackFlow
                }

                val request =
                    TetheringManager.TetheringRequest.Builder(type.value)
                        .setShouldShowEntitlementUi(false)
                        .build()

                val callback =
                    object : TetheringManager.StartTetheringCallback {
                        override fun onTetheringStarted() {
                            trySend(Unit)
                            close()
                        }

                        override fun onTetheringFailed(error: Int) {
                            Log.e(TAG, "start failed: $type, err: $error")
                            close(RuntimeException("Failed: $error"))
                        }
                    }

                tm.startTethering(request, tetheringExecutor, callback)
                awaitClose {}
            }
            .flowOn(Dispatchers.IO)

    private fun checkInterfaceMatch(type: TetherType, interfaces: List<String>): Boolean {
        val prefix =
            when (type) {
                TetherType.WIFI -> PREFIX_WIFI
                TetherType.USB -> PREFIX_USB
                TetherType.BLUETOOTH -> PREFIX_BT
                TetherType.ETHERNET -> PREFIX_ETH
            }
        return interfaces.any { it.contains(prefix, ignoreCase = true) }
    }

    companion object {
        private const val TAG = "TetheringRepository"
        private const val PREFIX_WIFI = "wlan"
        private const val PREFIX_USB = "usb"
        private const val PREFIX_BT = "bt-pan"
        private const val PREFIX_ETH = "eth"
    }
}
