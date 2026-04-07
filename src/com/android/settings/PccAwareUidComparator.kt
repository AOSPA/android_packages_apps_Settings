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

package com.android.settings

import android.app.privatecompute.flags.Flags
import android.content.Context
import android.os.Process
import android.os.UserHandle

/** Utility for comparing UIDs with awareness of Private Compute Core (PCC) isolated processes. */
object PccAwareUidComparator {
    /**
     * Returns true if the two UIDs belong to the same application, taking into account isolated PCC
     * processes.
     */
    @JvmStatic
    fun isSameApp(context: Context, uid1: Int, uid2: Int): Boolean {
        return UserHandle.isSameApp(getAppUid(context, uid1), getAppUid(context, uid2))
    }

    private fun getAppUid(context: Context, uid: Int): Int {
        if (Flags.enablePccFrameworkSupport() && Process.isPrivateComputeCoreUid(uid)) {
            val appUid =
                try {
                    context.packageManager.getAppUidForPrivateComputeCoreUid(uid)
                } catch (e: Exception) {
                    Process.INVALID_UID
                }
            // A result of INVALID_UID (-1) indicates an invalid mapping.
            // Fall back to the original UID for the comparison.
            if (appUid != Process.INVALID_UID) {
                return appUid
            }
        }
        return uid
    }
}
