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
import android.app.supervision.SupervisionManager
import android.app.supervision.flags.Flags
import android.content.Intent
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
import android.os.UserManager.USER_TYPE_PROFILE_SUPERVISING
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.fragment.app.FragmentActivity
import com.android.settings.password.ChooseLockPassword
import com.android.settingslib.supervision.SupervisionLog

/**
 * This activity starts the flow for setting up device supervision.
 *
 * The flow involves three essential steps:
 * 1. Creating a dedicated supervising user profile.
 * 2. Setting up a secure lock (e.g., PIN, password) for this profile.
 * 3. Enabling device supervision using `SupervisionManager.isSupervisionEnabled()`.
 *
 * After completing these steps, the activity prompts the user to set up PIN recovery. The result of
 * the PIN recovery setup does not affect the activity's overall result.
 *
 * The activity returns:
 * - `Activity.RESULT_OK` if steps 1-3 are successful.
 * - `Activity.RESULT_CANCELED` if any of steps 1-3 fail, or if the user cancels lock setup.
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
        val userManager = getSystemService(UserManager::class.java)
        var supervisingUser = userManager.supervisingUserHandle
        // If a supervising profile does not already exist on the device, create one
        if (supervisingUser == null) {
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
        val intent = Intent(this, ChooseLockPassword::class.java)
        startActivityForResultAsUser(intent, REQUEST_CODE_SET_SUPERVISION_LOCK, userHandle)
    }

    @RequiresPermission(anyOf = [INTERACT_ACROSS_USERS_FULL, MANAGE_USERS])
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_SET_SUPERVISION_LOCK -> handleSetLockResult(resultCode)
            REQUEST_CODE_SET_PIN_RECOVERY -> handlePinRecoveryResult(resultCode)
        }
    }

    private fun handleSetLockResult(resultCode: Int) {
        val supervisingUser = supervisingUserHandle
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

        // Enable device supervision
        val supervisionManager = getSystemService(SupervisionManager::class.java)
        supervisionManager?.setSupervisionEnabled(true)

        // Start PIN recovery setup
        startPinRecoveryActivity()
    }

    private fun handlePinRecoveryResult(resultCode: Int) {
        if (resultCode == RESULT_CANCELED) {
            Log.i(SupervisionLog.TAG, "PIN recovery setup was skipped by the user.")
        }
        setResult(RESULT_OK)
        finish()
    }

    private fun startPinRecoveryActivity() {
        // prompt user to setup pin recovery if flag is enabled
        if (!Flags.enableSupervisionPinRecoveryScreen()) {
            Log.d(SupervisionLog.TAG, "PIN recovery setup is not enabled.")
            setResult(RESULT_OK)
            finish()
        }

        val intent =
            Intent(this, SupervisionPinRecoveryActivity::class.java).apply {
                action = SupervisionPinRecoveryActivity.ACTION_SETUP
            }
        startActivityForResult(intent, REQUEST_CODE_SET_PIN_RECOVERY, null)
    }

    @RequiresPermission(anyOf = [INTERACT_ACROSS_USERS_FULL, MANAGE_USERS])
    private fun tryStopProfile(supervisingUser: UserHandle, activityManager: ActivityManager) {
        if (!activityManager.stopProfile(supervisingUser)) {
            Log.w(SupervisionLog.TAG, "Could not stop the supervising profile.")
        }
    }

    companion object {
        private const val REQUEST_CODE_SET_SUPERVISION_LOCK = 0
        private const val REQUEST_CODE_SET_PIN_RECOVERY = 1
    }
}
