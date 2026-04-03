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
import android.util.Log
import com.android.settings.R
import com.android.settingslib.interfaces.troubleshooting.ITroubleshootingInfoProviderService
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Provide an easier way to connect the troubleshooting service. TODO Move this to
 * SettingsInterfaceLib
 */
class TroubleshootingServiceConnection(val receivers: Map<String, ResultReceiver> = emptyMap()) :
    ServiceConnection {
    private var troubleshootingService: ITroubleshootingInfoProviderService? = null
    var serviceConnectionListener: ServiceConnectionListener? = null
    val isServiceConnected = AtomicBoolean(false)

    /** Gets the troubleshooting UI content from the service. */
    var diagnosticUiInfos: Map<String, Bundle> = emptyMap()

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        Log.d(TAG, "onServiceConnected")
        val binder = ITroubleshootingInfoProviderService.Stub.asInterface(service)
        troubleshootingService = binder
        isServiceConnected.set(true)

        initializeServiceCommunication(binder)
    }

    override fun onNullBinding(name: ComponentName?) {
        Log.w(TAG, "onNullBinding: Service $name refused the bind request.")
        isServiceConnected.set(false)
        troubleshootingService = null
        serviceConnectionListener?.onServiceConnectedState(false)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Log.i(TAG, "onServiceDisconnected")
        isServiceConnected.set(false)
        troubleshootingService = null
        serviceConnectionListener?.onServiceConnectedState(false)
    }

    private fun initializeServiceCommunication(service: ITroubleshootingInfoProviderService) {
        val infoMap = mutableMapOf<String, Bundle>()
        try {
            receivers.mapValues { (functionType, receiver) ->
                Log.d(TAG, "Registering $functionType")
                infoMap[functionType] = service.getDiagnosticUiInfo(functionType)
                service.registerIssueDetectionCallback(functionType, receiver)
            }
            this.diagnosticUiInfos = infoMap
            serviceConnectionListener?.onServiceConnectedState(true)
        } catch (e: RemoteException) {
            Log.e(TAG, "Error during service registration", e)
        }
    }

    fun bindService(context: Context) {
        val serviceInfo = getTroubleshootingServiceInfo(context)
        if (serviceInfo.isNullOrBlank()) {
            Log.w(TAG, "Service info is empty, cannot bind.")
            return
        }
        if (isServiceConnected.get()) {
            Log.d(TAG, "Already bind.")
            return
        }

        val component = ComponentName.unflattenFromString(serviceInfo)
        val intent =
            Intent(ITroubleshootingInfoProviderService::class.java.name).apply {
                setComponent(component)
            }

        Log.i(TAG, "Binding to service: $component")
        try {
            val success = context.bindService(intent, this, Context.BIND_AUTO_CREATE)
            if (!success) {
                Log.e(TAG, "Unable to bind to service")
                unbindService(context)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while binding service", e)
            unbindService(context)
        }
    }

    fun unbindService(context: Context) {
        try {
            troubleshootingService?.let { service: ITroubleshootingInfoProviderService ->
                for ((functionType, receiver) in receivers) {
                    service.unregisterIssueDetectionCallback(functionType, receiver)
                }
            }
        } catch (e: RemoteException) {
            Log.e(TAG, "Error unregistering callbacks", e)
        } finally {
            try {
                context.unbindService(this)
            } catch (e: IllegalArgumentException) {
                // This happens if the service wasn't actually registered; safe to ignore
                Log.d(TAG, "Service was not registered")
            }
            troubleshootingService = null
            isServiceConnected.set(false)
        }
    }

    private fun getTroubleshootingServiceInfo(context: Context): String? =
        context.resources.getString(R.string.config_connectivity_troubleshooting_service_name)

    fun isTroubleshootingServiceExists(context: Context): Boolean {
        cachedExists?.let {
            return it
        }

        val serviceInfo = getTroubleshootingServiceInfo(context)
        if (serviceInfo.isNullOrBlank()) {
            cachedExists = false
            return false
        }

        return try {
            val componentName = ComponentName.unflattenFromString(serviceInfo)
            val exists =
                componentName?.let { context.packageManager.getServiceInfo(it, 0) != null } ?: false
            cachedExists = exists
            exists
        } catch (e: PackageManager.NameNotFoundException) {
            cachedExists = false
            false
        }
    }

    fun interface ServiceConnectionListener {
        fun onServiceConnectedState(isConnected: Boolean)
    }

    companion object {
        private const val TAG = "TroubleshootSvcConn"
        var cachedExists: Boolean? = null
    }
}
