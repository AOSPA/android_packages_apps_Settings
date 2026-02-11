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

package com.android.settings.metrics

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.pm.PackageManager
import com.android.settings.appfunctions.DeviceStateAppFunctionType
import com.android.settings.core.instrumentation.SettingsStatsLog
import com.android.settingslib.metadata.AppFunctionMetricsLoggerInterface

class AppFunctionMetricsLogger : AppFunctionMetricsLoggerInterface {
    override fun logAppFunction(
        functionType: Int,
        callingPackage: String,
        latencyMs: Long,
        context: Context,
    ) {
        val callingPackageUid = getCallingPackageUid(callingPackage, context)
        SettingsStatsLog.write(
            SettingsStatsLog.SETTINGS_APP_FUNCTION_EVENT,
            functionType,
            callingPackageUid,
            latencyMs,
            0, /* errorCode */
        )
    }

    override fun logAppFunctionError(
        callingPackage: String,
        errorCode: Int,
        context: Context,
        functionType: Int?,
    ) {
        val callingPackageUid = getCallingPackageUid(callingPackage, context)
        SettingsStatsLog.write(
            SettingsStatsLog.SETTINGS_APP_FUNCTION_EVENT,
            functionType ?: SettingsEnums.UNKNOWN_APP_FUNCTION,
            callingPackageUid,
            0, /* latencyMillis */
            errorCode,
        )
    }

    private fun getCallingPackageUid(callingPackage: String, context: Context): Int {
        return try {
            context.packageManager.getPackageUid(callingPackage, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            -1
        }
    }
}

// Keep the extension function for internal use within this file or other Settings app components
fun DeviceStateAppFunctionType.toMetricsId(): Int {
    return when (this) {
        DeviceStateAppFunctionType.GET_STORAGE -> SettingsEnums.APP_FUNCTION_GET_STORAGE
        DeviceStateAppFunctionType.GET_BATTERY -> SettingsEnums.APP_FUNCTION_GET_BATTERY
        DeviceStateAppFunctionType.GET_MOBILE_DATA -> SettingsEnums.APP_FUNCTION_GET_MOBILE_DATA
        DeviceStateAppFunctionType.GET_NOTIFICATIONS -> SettingsEnums.APP_FUNCTION_GET_NOTIFICATIONS
        DeviceStateAppFunctionType.GET_APPS -> SettingsEnums.APP_FUNCTION_GET_APPS
        DeviceStateAppFunctionType.GET_METADATA -> SettingsEnums.APP_FUNCTION_GET_METADATA
        DeviceStateAppFunctionType.GET_DEVICE_STATE -> SettingsEnums.APP_FUNCTION_GET_DEVICE_STATE
        DeviceStateAppFunctionType.SET_DEVICE_STATE -> SettingsEnums.APP_FUNCTION_SET_DEVICE_STATE
        DeviceStateAppFunctionType.ADJUST_DEVICE_STATE_BY_PERCENTAGE ->
            SettingsEnums.APP_FUNCTION_ADJUST_DEVICE_STATE_BY_PERCENTAGE
        DeviceStateAppFunctionType.OFFSET_DEVICE_STATE_BY_VALUE ->
            SettingsEnums.APP_FUNCTION_OFFSET_DEVICE_STATE_BY_VALUE
        DeviceStateAppFunctionType.GET_UNCATEGORIZED -> SettingsEnums.APP_FUNCTION_GET_UNCATEGORIZED
    }
}
