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
package com.android.settings.spa.accessibility

import android.app.UiModeManager
import android.content.Context
import android.content.pm.ApplicationInfo

class ForceDarkAppExceptionsRepository(private val context: Context, private val userId: Int) {

    private val forceInvertAlwaysDisableSet = HashSet<String>()

    init {
        readForceInvertAlwaysDisableApps()
    }

    fun isAppForceDarkAlwaysDisable(app: ApplicationInfo): Boolean {
        return forceInvertAlwaysDisableSet.contains(app.packageName)
    }

    fun setIsAppForceDarkAlwaysDisable(app: ApplicationInfo, isException: Boolean): Boolean {
        val result =
            if (isException) forceInvertAlwaysDisableSet.add(app.packageName)
            else forceInvertAlwaysDisableSet.remove(app.packageName)

        if (result) {
            val newState =
                if (isException) UiModeManager.FORCE_INVERT_PACKAGE_ALWAYS_DISABLE
                else UiModeManager.FORCE_INVERT_PACKAGE_ALLOWED
            updatePackageForceInvertOverrideStateToSettings(app.packageName, newState)
        }

        return result
    }

    private fun readForceInvertAlwaysDisableApps() {
        forceInvertAlwaysDisableSet.clear()

        val uiModeManager: UiModeManager = context.getSystemService(UiModeManager::class.java)
        val exceptionListValue = uiModeManager.getAllForceInvertAlwaysDisableApps(userId)

        if (exceptionListValue != null && exceptionListValue.isNotEmpty()) {
            forceInvertAlwaysDisableSet.addAll(exceptionListValue)
        }
    }

    private fun updatePackageForceInvertOverrideStateToSettings(
        packageName: String,
        newState: Int,
    ) {
        val uiModeManager: UiModeManager = context.getSystemService(UiModeManager::class.java)
        uiModeManager.setForceInvertOverrideStateForApp(packageName, newState, userId)
    }
}
