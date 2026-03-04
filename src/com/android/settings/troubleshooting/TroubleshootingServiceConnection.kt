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

package com.android.settings.troubleshooting

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.os.ResultReceiver
import android.text.TextUtils
import android.util.Log
import com.android.settings.R
import com.android.settingslib.interfaces.troubleshooting.ITroubleshootingInfoProviderService
import java.util.concurrent.Executors
import kotlin.collections.iterator
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

class TroubleshootingServiceConnection(val receivers: Map<String, ResultReceiver> = emptyMap()) :
    ServiceConnection {
    protected val mLock: Any = Any()

    private var mITroubleshootingInfoProviderService: ITroubleshootingInfoProviderService? = null

    @OptIn(ExperimentalAtomicApi::class)
    private val mIsServiceConnected: AtomicBoolean = AtomicBoolean(false)

    /** Gets the troubleshooting UI content from the service. */
    var diagnosticUiInfos: Map<String, Bundle?> = emptyMap()

    @OptIn(ExperimentalAtomicApi::class)
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        mIsServiceConnected.store(true)
        mITroubleshootingInfoProviderService =
            ITroubleshootingInfoProviderService.Stub.asInterface(service)
    }

    @OptIn(ExperimentalAtomicApi::class)
    override fun onServiceDisconnected(name: ComponentName?) {
        mIsServiceConnected.store(false)
    }

    @OptIn(ExperimentalAtomicApi::class)
    fun bindService(context: Context) {
        val intent = Intent(ITroubleshootingInfoProviderService::class.java.name)
        intent.setComponent(
            ComponentName.unflattenFromString(
                context.resources.getString(
                    R.string.config_connectivity_troubleshooting_service_name
                )
            )
        )

        Log.d(LOG_TAG, "Binding to service, intent=" + intent)
        try {
            if (
                context.bindService(
                    intent,
                    Context.BIND_IMPORTANT or Context.BIND_AUTO_CREATE,
                    Executors.newSingleThreadExecutor(),
                    this,
                )
            ) {
                synchronized(mLock) {
                    while (receivers.isNotEmpty() && !mIsServiceConnected.load()) {
                        try {
                            (mLock as Object).wait(3000) // Added timeout for safety
                        } catch (e: InterruptedException) {
                            Log.e(LOG_TAG, "Error while waiting for service connection: $e")
                        }
                    }
                    if (mIsServiceConnected.load()) {
                        Log.e(LOG_TAG, "The binding is invalid.")
                        return
                    }
                    val service = mITroubleshootingInfoProviderService
                    if (service == null) {
                        Log.e(LOG_TAG, "The binding is invalid or timed out.")
                        return
                    }
                    val infoMap = mutableMapOf<String, Bundle?>()
                    try {
                        for ((functionType, receiver) in receivers) {
                            Log.d(LOG_TAG, "Registering $functionType")

                            infoMap[functionType] = service.getDiagnosticUiInfo(functionType)

                            service.registerIssueDetectionCallback(functionType, receiver)
                        }
                        // 4. Update the public map
                        this.diagnosticUiInfos = infoMap as Map<String, Bundle?>
                    } catch (e: RemoteException) {
                        Log.e(LOG_TAG, "Error during service registration: $e")
                    }
                }
            } else {
                Log.e(LOG_TAG, "unable to bind to service from intent=$intent")
            }
        } catch (e: SecurityException) {
            Log.e(LOG_TAG, "Error applying troubleshoot interaction from service: $e")
        }
    }

    @OptIn(ExperimentalAtomicApi::class)
    fun unbindService(context: Context) {
        val service = mITroubleshootingInfoProviderService
        if (service != null) {
            try {
                // Unregister all receivers stored in the initial map
                for ((functionType, receiver) in receivers) {
                    service.unregisterIssueDetectionCallback(functionType, receiver)
                }
            } catch (e: RemoteException) {
                Log.e(LOG_TAG, "Error unregistering callbacks: $e")
            } finally {
                context.unbindService(this)
                synchronized(mLock) {
                    mITroubleshootingInfoProviderService = null
                    mIsServiceConnected.store(false)
                }
            }
        }
    }

    fun isTroubleshootingServiceExists(context: Context): Boolean {
        if (isTroubleshootingServiceExists != null) {
            return isTroubleshootingServiceExists == true
        }
        try {
            val serviceInfo =
                context.resources.getString(
                    R.string.config_connectivity_troubleshooting_service_name
                )
            Log.d(LOG_TAG, "service name : $serviceInfo")
            if (TextUtils.isEmpty(serviceInfo)) {
                isTroubleshootingServiceExists = false
            } else {
                val componentName = ComponentName.unflattenFromString(serviceInfo)
                if (componentName == null) {
                    isTroubleshootingServiceExists = false
                } else {
                    val serviceObject = context.packageManager.getServiceInfo(componentName, 0)
                    isTroubleshootingServiceExists = serviceObject != null
                    return isTroubleshootingServiceExists!!
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            isTroubleshootingServiceExists = false
        }
        return false
    }

    companion object {
        private const val LOG_TAG = "TroubleshootingServiceConnection"
        var isTroubleshootingServiceExists: Boolean? = null
    }
}
