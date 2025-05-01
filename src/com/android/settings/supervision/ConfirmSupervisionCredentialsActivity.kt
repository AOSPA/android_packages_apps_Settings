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

import android.Manifest.permission.INTERACT_ACROSS_USERS_FULL
import android.Manifest.permission.MANAGE_USERS
import android.Manifest.permission.USE_BIOMETRIC_INTERNAL
import android.app.ActivityManager
import android.app.role.RoleManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.hardware.biometrics.BiometricPrompt.AuthenticationCallback
import android.os.Binder
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Process
import android.util.Log
import androidx.annotation.OpenForTesting
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.android.settings.R
import com.android.settingslib.supervision.SupervisionLog.TAG

/**
 * Activity for confirming supervision credentials using device credential authentication.
 *
 * This activity displays an authentication prompt to the user, requiring them to authenticate using
 * their device credentials (PIN, pattern, or password). It is specifically designed for verifying
 * credentials for supervision purposes.
 *
 * It returns `Activity.RESULT_OK` if authentication succeeds, and `Activity.RESULT_CANCELED` if
 * authentication fails or is canceled by the user.
 *
 * Usage:
 * 1. Start this activity using `startActivityForResult()`.
 * 2. Handle the result in `onActivityResult()`.
 *
 * Permissions:
 * - Requires `android.permission.USE_BIOMETRIC`.
 */
@OpenForTesting
open class ConfirmSupervisionCredentialsActivity : FragmentActivity() {

    private val mAuthenticationCallback =
        object : AuthenticationCallback() {
            @RequiresPermission(anyOf = [INTERACT_ACROSS_USERS_FULL, MANAGE_USERS])
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                tryStopProfile()
                Log.w(TAG, "onAuthenticationError(errorCode=$errorCode, errString=$errString)")
                setResult(RESULT_CANCELED)
                finish()
            }

            @RequiresPermission(anyOf = [INTERACT_ACROSS_USERS_FULL, MANAGE_USERS])
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                tryStopProfile()
                setResult(RESULT_OK)
                finish()
            }

            @RequiresPermission(anyOf = [INTERACT_ACROSS_USERS_FULL, MANAGE_USERS])
            override fun onAuthenticationFailed() {
                tryStopProfile()
                setResult(RESULT_CANCELED)
                finish()
            }
        }

    @RequiresPermission(anyOf = [INTERACT_ACROSS_USERS_FULL, MANAGE_USERS])
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!callerHasSupervisionRole() && !callerIsSystemUid()) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        val supervisingUser = supervisingUserHandle
        if (supervisingUser == null) {
            Log.w(TAG, "No supervising user exists, cannot verify credentials.")
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        val activityManager = getSystemService(ActivityManager::class.java)
        if (!activityManager.startProfile(supervisingUser)) {
            Log.w(TAG, "Unable to start supervising user, cannot verify credentials.")
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        showBiometricPrompt(supervisingUser.identifier)
    }

    @RequiresPermission(USE_BIOMETRIC_INTERNAL)
    fun showBiometricPrompt(userId: Int) {
        val biometricPrompt =
            BiometricPrompt.Builder(this)
                .setTitle(getString(R.string.supervision_full_screen_pin_verification_title))
                .setConfirmationRequired(true)
                .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build()
        biometricPrompt.authenticateUser(
            CancellationSignal(),
            ContextCompat.getMainExecutor(this),
            mAuthenticationCallback,
            userId,
        )
    }

    private fun callerHasSupervisionRole(): Boolean {
        val roleManager = getSystemService(RoleManager::class.java)
        if (roleManager == null) {
            Log.w(TAG, "null RoleManager")
            return false
        }
        return roleManager
            .getRoleHolders(RoleManager.ROLE_SYSTEM_SUPERVISION)
            .contains(callingPackage)
    }

    private fun callerIsSystemUid(): Boolean {
        val callingUid = Binder.getCallingUid()
        if (callingUid != Process.SYSTEM_UID) {
            Log.w(TAG, "callingUid: $callingUid is not SYSTEM_UID")
            return false
        }
        return true
    }

    @RequiresPermission(anyOf = [INTERACT_ACROSS_USERS_FULL, MANAGE_USERS])
    private fun tryStopProfile() {
        val supervisingUser = supervisingUserHandle
        if (supervisingUser == null) {
            Log.w(TAG, "Cannot stop supervising profile because it does not exist.")
            return
        }
        val activityManager = getSystemService(ActivityManager::class.java)
        if (!activityManager.stopProfile(supervisingUser)) {
            Log.w(TAG, "Could not stop the supervising profile.")
        }
    }
}
