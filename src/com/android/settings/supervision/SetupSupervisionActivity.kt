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

import android.Manifest.permission.CREATE_USERS
import android.Manifest.permission.INTERACT_ACROSS_USERS
import android.Manifest.permission.INTERACT_ACROSS_USERS_FULL
import android.Manifest.permission.MANAGE_USERS
import android.app.ActivityManager
import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager.ACTION_SET_NEW_PASSWORD
import android.app.admin.DevicePolicyManager.EXTRA_PASSWORD_COMPLEXITY
import android.app.admin.DevicePolicyManager.PASSWORD_COMPLEXITY_LOW
import android.content.Intent
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
import android.os.UserManager.USER_TYPE_PROFILE_SUPERVISING
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.fragment.app.FragmentActivity
import com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_FINGERPRINT_ENROLLMENT_ONLY
import com.android.settingslib.supervision.SupervisionLog

/**
 * This activity starts the flow for setting up device supervision.
 *
 * Three things are required for device supervision: a supervising profile must be created, a lock
 * must be set up for this profile, and `SupervisionManager.isSupervisionEnabled()` must be set.
 * This activity handles the first two, while the third is managed by
 * `SupervisionMainSwitchPreference`.
 *
 * Returns `Activity.RESULT_OK` if all steps of setup succeed, and `Activity.RESULT_CANCELED` if
 * any step fails, or the user does not finish setting up the lock for the supervising profile.
 *
 * Usage:
 * 1. Start this activity using `startActivityForResult()`.
 * 2. Handle the result in `onActivityResult()`.
 */
class SetupSupervisionActivity : FragmentActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            enableSupervision()
        }
    }

    @RequiresPermission(anyOf = [CREATE_USERS, MANAGE_USERS])
    private fun enableSupervision() {
        val supervisionHelper = SupervisionHelper.getInstance(this)
        var supervisingUser = supervisionHelper.getSupervisingUserHandle()
        // If a supervising profile does not already exist on the device, create one
        if (supervisingUser == null) {
            val userManager = getSystemService(UserManager::class.java)
            val userInfo =
                userManager.createUser("Supervising", USER_TYPE_PROFILE_SUPERVISING, /* flags= */ 0)
            if (userInfo != null) {
                supervisingUser = userInfo.userHandle
            } else {
                // TODO(399705794): Surface this error to user
                Log.w(SupervisionLog.TAG, "Unable to create supervising profile.")
                setResult(RESULT_CANCELED)
                finish()
                return
            }
        }
        val activityManager = getSystemService(ActivityManager::class.java)
        if (!activityManager.startProfile(supervisingUser)) {
            // TODO(399705794): Surface this error to user
            Log.w(SupervisionLog.TAG, "Could not start supervising profile.")
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        startChooseLockActivity(supervisingUser)
    }

    @RequiresPermission(anyOf = [INTERACT_ACROSS_USERS_FULL, INTERACT_ACROSS_USERS])
    private fun startChooseLockActivity(userHandle: UserHandle) {
        // TODO(b/389712273) Intent directly to PIN selection screen to avoid giving the user
        //  the options for password/pattern during the initial setup flow
        val intent = Intent(ACTION_SET_NEW_PASSWORD).apply {
            putExtra(EXTRA_PASSWORD_COMPLEXITY, PASSWORD_COMPLEXITY_LOW)
            putExtra(EXTRA_KEY_FINGERPRINT_ENROLLMENT_ONLY, true)
        }
        startActivityForResultAsUser(intent, REQUEST_CODE_SET_SUPERVISION_LOCK, userHandle)
    }

    @RequiresPermission(anyOf = [INTERACT_ACROSS_USERS_FULL, MANAGE_USERS])
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val supervisionHelper = SupervisionHelper.getInstance(this)
        val supervisingUser = supervisionHelper.getSupervisingUserHandle()
        if (supervisingUser == null) {
            Log.w(SupervisionLog.TAG, "No supervising user handle found after lock setup.")
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        val activityManager = getSystemService(ActivityManager::class.java)
        tryStopProfile(supervisingUser, activityManager)
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        if (!keyguardManager.isDeviceSecure(supervisingUser.identifier)) {
            Log.w(SupervisionLog.TAG, "Lock for supervising user not set up.")
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        setResult(RESULT_OK)
        finish()
    }

    @RequiresPermission(anyOf = [INTERACT_ACROSS_USERS_FULL, MANAGE_USERS])
    private fun tryStopProfile(supervisingUser: UserHandle, activityManager: ActivityManager) {
        if (!activityManager.stopProfile(supervisingUser)) {
            Log.w(SupervisionLog.TAG, "Could not stop the supervising profile.")
        }
    }

    companion object {
        private const val REQUEST_CODE_SET_SUPERVISION_LOCK = 0
    }

}
