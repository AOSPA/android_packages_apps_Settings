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

import android.app.role.RoleManager
import android.app.role.RoleManager.ROLE_SUPERVISION
import android.app.supervision.SupervisionManager
import android.os.Bundle
import android.os.UserHandle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.android.settingslib.supervision.SupervisionLog.TAG
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.launch

/**
 * Activity for enabling device supervision.
 *
 * This activity is only available to the system supervision role and allowlisted packages.
 * It enables device supervision and finishes the activity with `Activity.RESULT_OK`.
 */
class EnableSupervisionActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val packageName = callingPackage
        val userHandle = supervisingUserHandle
        if (packageName == null || userHandle == null) {
            Log.w(TAG, "Calling package or user handle are null. Finishing activity.")
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        lifecycleScope.launch {
            if (grantSupervisionRole(packageName, userHandle)) {
                val supervisionManager = getSystemService(SupervisionManager::class.java)
                if (supervisionManager == null) {
                    Log.w(TAG, "SupervisionManager is null. Finishing activity.")
                    setResult(RESULT_CANCELED)
                }
                supervisionManager.isSupervisionEnabled = true
                setResult(RESULT_OK)
            } else {
                Log.w(TAG, "Caller cannot enable supervision. Finishing activity.")
                setResult(RESULT_CANCELED)
            }
            finish()
        }
    }

    suspend fun grantSupervisionRole(packageName: String, userHandle: UserHandle): Boolean {
        val executor = ContextCompat.getMainExecutor(this)
        val roleManager = getSystemService(RoleManager::class.java)
        if (roleManager == null) {
          Log.w(TAG, "RoleManager is null. Finishing activity.")
          return false
        }
        return suspendCoroutine  { continuation ->
            roleManager.addRoleHolderAsUser(
                ROLE_SUPERVISION,
                packageName,
                RoleManager.MANAGE_HOLDERS_FLAG_DONT_KILL_APP,
                userHandle,
                executor
            ) { isSuccessful ->
                continuation.resumeWith(Result.success(isSuccessful))
            }
        }
    }
}
