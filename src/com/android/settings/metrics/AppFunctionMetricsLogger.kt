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

package com.android.settings.metrics

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.pm.PackageManager
import com.android.settings.appfunctions.DeviceStateAppFunctionType
import com.android.settings.core.instrumentation.SettingsStatsLog

class AppFunctionMetricsLogger {
    fun logAppFunction(
        functionType: DeviceStateAppFunctionType,
        callingPackageName: String,
        latencyMs: Long,
        context: Context,
    ) {
        val uid = callingPackageName.toUid(context)
        SettingsStatsLog.write(
            SettingsStatsLog.SETTINGS_APP_FUNCTION_EVENT,
            functionType.toMetricsId(),
            uid,
            latencyMs,
            0,
        )
    }

    fun logAppFunctionError(
        callingPackageName: String,
        errorCode: Int,
        context: Context,
        functionType: DeviceStateAppFunctionType? = null,
    ) {
        val uid = callingPackageName.toUid(context)
        SettingsStatsLog.write(
            SettingsStatsLog.SETTINGS_APP_FUNCTION_EVENT,
            functionType?.toMetricsId() ?: SettingsEnums.UNKNOWN_APP_FUNCTION,
            uid,
            0,
            errorCode,
        )
    }

    private fun DeviceStateAppFunctionType.toMetricsId() =
        when (this) {
            DeviceStateAppFunctionType.GET_UNCATEGORIZED ->
                SettingsEnums.APP_FUNCTION_GET_UNCATEGORIZED
            DeviceStateAppFunctionType.GET_STORAGE -> SettingsEnums.APP_FUNCTION_GET_STORAGE
            DeviceStateAppFunctionType.GET_BATTERY -> SettingsEnums.APP_FUNCTION_GET_BATTERY
            DeviceStateAppFunctionType.GET_MOBILE_DATA -> SettingsEnums.APP_FUNCTION_GET_MOBILE_DATA
            DeviceStateAppFunctionType.GET_NOTIFICATIONS ->
                SettingsEnums.APP_FUNCTION_GET_NOTIFICATIONS
            DeviceStateAppFunctionType.GET_APPS -> SettingsEnums.APP_FUNCTION_GET_APPS
            DeviceStateAppFunctionType.GET_METADATA -> SettingsEnums.APP_FUNCTION_GET_METADATA
            DeviceStateAppFunctionType.SET_DEVICE_STATE ->
                SettingsEnums.APP_FUNCTION_SET_DEVICE_STATE
            DeviceStateAppFunctionType.ADJUST_DEVICE_STATE_BY_PERCENTAGE ->
                SettingsEnums.APP_FUNCTION_ADJUST_DEVICE_STATE_BY_PERCENTAGE
            DeviceStateAppFunctionType.OFFSET_DEVICE_STATE_BY_VALUE ->
                SettingsEnums.APP_FUNCTION_OFFSET_DEVICE_STATE_BY_VALUE
        }

    private fun String.toUid(context: Context) =
        try {
            context.packageManager.getPackageUid(this, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            PACKAGE_ID_UNKNOWN
        }

    private companion object {
        private const val PACKAGE_ID_UNKNOWN = 0
    }
}
