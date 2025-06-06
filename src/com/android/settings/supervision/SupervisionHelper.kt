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
package com.android.settings.supervision

import android.app.KeyguardManager
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.UserHandle
import android.os.UserManager
import android.os.UserManager.USER_TYPE_PROFILE_SUPERVISING
import android.util.Log
import com.android.settings.supervision.ipc.SupervisionMessengerClient.Companion.SUPERVISION_MESSENGER_SERVICE_BIND_ACTION
import com.android.settingslib.supervision.SupervisionLog.TAG

val Context.isSupervisingCredentialSet: Boolean
    get() {
        val supervisingUserId = supervisingUserHandle?.identifier ?: return false
        return getSystemService(KeyguardManager::class.java)?.isDeviceSecure(supervisingUserId) ==
            true
    }

val Context.supervisingUserHandle: UserHandle?
    get() = getSystemService(UserManager::class.java).supervisingUserHandle

val UserManager?.supervisingUserHandle: UserHandle?
    get() = this?.users?.firstOrNull { it.userType == USER_TYPE_PROFILE_SUPERVISING }?.userHandle

/** Returns the package name of the system supervision app, or null if not found. */
val Context.supervisionPackageName: String?
    get() {
        val roleManager = getSystemService(RoleManager::class.java)
        if (roleManager == null) {
            Log.w(TAG, "RoleManager service not available.")
            return null
        }

        val roleHolders =
            roleManager.getRoleHolders(RoleManager.ROLE_SYSTEM_SUPERVISION) ?: emptyList<String>()
        if (roleHolders.isEmpty()) Log.w(TAG, "No package holding the system supervision role.")

        // supervision role is exclusive, only one app may hold this role in a user
        return roleHolders.firstOrNull()
    }

fun Context.hasNecessarySupervisionComponent() =
    hasNecessarySupervisionComponent(supervisionPackageName)

fun Context.hasNecessarySupervisionComponent(packageName: String?): Boolean {
    if (packageName == null) return false
    val intent = Intent(SUPERVISION_MESSENGER_SERVICE_BIND_ACTION).setPackage(packageName)
    return packageManager?.queryIntentServices(intent, 0)?.isNotEmpty() == true
}
