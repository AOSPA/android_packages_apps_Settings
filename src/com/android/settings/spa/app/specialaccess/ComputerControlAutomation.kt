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

package com.android.settings.spa.app.specialaccess

import android.app.AppOpsManager
import android.companion.virtual.VirtualDeviceManager
import android.content.Context
import android.content.pm.ApplicationInfo
import com.android.settingslib.spaprivileged.framework.common.appOpsManager
import com.android.settingslib.spaprivileged.model.app.AppOps
import com.android.settingslib.spaprivileged.model.app.PackageManagers
import com.android.settingslib.spaprivileged.model.app.getOpMode
import com.android.settingslib.spaprivileged.model.app.opModeFlow
import com.android.settingslib.spaprivileged.model.app.userId
import kotlinx.coroutines.flow.Flow

internal const val PACKAGE_NAME = "rt_packageName"
internal const val USER_ID = "rt_userId"

/**
 * The controller for managing the consent for automation.
 *
 * This controller is responsible for getting and setting the app op mode for the
 * [AppOpsManager.OP_COMPUTER_CONTROL] permission.
 *
 * This controller interacts with [VirtualDeviceManager] to get, remove, or clear the list of apps
 * that the agent is allowed to control.
 */
internal class ComputerControlConsentController(
    private val context: Context,
    private val app: ApplicationInfo,
) {

    private val vdm = context.getSystemService(VirtualDeviceManager::class.java)
    private val consentManager = vdm?.computerControlConsentManager
    private val appOpsManager = context.appOpsManager
    private val appOps =
        AppOps(
            op = AppOpsManager.OP_COMPUTER_CONTROL,
            modeForNotAllowed = AppOpsManager.MODE_IGNORED,
        )
    val appOpsModeFlow: Flow<Int> = appOpsManager.opModeFlow(appOps.op, app)
    val appOpsMode: Int
        get() = appOpsManager.getOpMode(appOps.op, app)

    fun setAppOpMode(mode: Int) {
        appOpsManager.setMode(appOps.op, app.uid, app.packageName, mode)
    }

    fun getAutomatablePackages(): Set<String> {
        return consentManager?.getAutomatableAppListForAgent(app.uid, app.packageName)?.toSet()
            ?: emptySet()
    }

    fun clearAutomatablePackages() {
        consentManager?.clearAutomatableAppListForAgent(app.uid, app.packageName)
    }

    fun removeAutomatablePackage(targetPackageName: String) {
        consentManager?.removeAppFromAutomatableAppListForAgent(
            app.uid,
            app.packageName,
            targetPackageName,
        )
    }

    /**
     * Converts a set of package names into a sorted list of [ApplicationInfo] objects for display.
     */
    fun getDisplayApps(packageNames: Set<String>): List<ApplicationInfo> {
        return packageNames
            .mapNotNull { pkgName ->
                PackageManagers.getPackageInfoAsUser(pkgName, app.userId)?.applicationInfo
            }
            .sortedBy { targetApp ->
                context.packageManager.getApplicationLabel(targetApp).toString()
            }
    }
}

fun <T> T.runIfComputerControlEnabled(block: T.() -> T): T {
    if (android.companion.virtualdevice.flags.Flags.computerControlAccess()) {
        return block()
    }
    return this
}
