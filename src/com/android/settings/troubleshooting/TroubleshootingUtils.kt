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

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.util.Log

class TroubleshootingUtils {

    companion object {
        private const val TAG = "TroubleshootingUtils"
        private const val EXTRA_WIFI_SSID = "ssid"

        @JvmStatic
        fun startWifiTroubleShootingActivity(
            activity: Activity,
            packageName: String,
            className: String,
            action: String,
            ssid: String,
        ) {
            val intent = Intent()
            if (packageName.isNotEmpty() && className.isNotEmpty()) {
                intent.component = ComponentName(packageName, className)
            } else if (packageName.isNotEmpty()) {
                intent.setPackage(packageName)
            } else {
                Log.d(TAG, "No necessary package name for Wi-Fi.")
                return
            }

            intent.action = action
            intent.putExtra(EXTRA_WIFI_SSID, ssid)
            try {
                activity.startActivityForResult(intent, 0)
            } catch (e: ActivityNotFoundException) {
                Log.e(TAG, "Failed to start activity: ${intent.action ?: intent.component}", e)
            }
        }

        @JvmStatic
        fun startMobileTroubleshootingActivity(
            activity: Activity,
            packageName: String,
            className: String,
            action: String,
            subId: Int,
        ) {
            val intent = Intent()
            if (packageName.isNotEmpty() && className.isNotEmpty()) {
                intent.component = ComponentName(packageName, className)
            } else if (packageName.isNotEmpty()) {
                intent.setPackage(packageName)
            } else {
                Log.d(TAG, "No necessary package name for mobile.")
                return
            }
            intent.action = action
            intent.putExtra(android.provider.Settings.EXTRA_SUB_ID, subId)
            try {
                activity.startActivityForResult(intent, 0)
            } catch (e: ActivityNotFoundException) {
                Log.e(TAG, "Failed to start activity: ${intent.action ?: intent.component}", e)
            }
        }
    }
}
