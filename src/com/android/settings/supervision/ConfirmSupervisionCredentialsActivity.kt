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

import android.Manifest.permission.USE_BIOMETRIC_INTERNAL
import android.app.Activity
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
import com.android.settingslib.supervision.SupervisionLog

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
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Log.w(
                    SupervisionLog.TAG,
                    "onAuthenticationError(errorCode=$errorCode, errString=$errString)",
                )
                setResult(Activity.RESULT_CANCELED)
                finish()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                setResult(Activity.RESULT_OK)
                finish()
            }

            override fun onAuthenticationFailed() {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }

    @RequiresPermission(USE_BIOMETRIC_INTERNAL)
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!callerHasSupervisionRole() && !callerIsSystemUid()) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }
        showBiometricPrompt()
    }

    @RequiresPermission(USE_BIOMETRIC_INTERNAL)
    fun showBiometricPrompt() {
        val supervisingUserId =
            SupervisionHelper.getInstance(this).getSupervisingUserHandle()?.identifier
        if (supervisingUserId == null) {
            Log.w(SupervisionLog.TAG, "supervisingUserId is null")
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

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
            supervisingUserId,
        )
    }

    private fun callerHasSupervisionRole(): Boolean {
        val roleManager = getSystemService(RoleManager::class.java)
        if (roleManager == null) {
            Log.w(SupervisionLog.TAG, "null RoleManager")
            return false
        }
        return roleManager
            .getRoleHolders(RoleManager.ROLE_SYSTEM_SUPERVISION)
            .contains(callingPackage)
    }

    private fun callerIsSystemUid(): Boolean {
        val callingUid = Binder.getCallingUid()
        if (callingUid != Process.SYSTEM_UID) {
            Log.w(SupervisionLog.TAG, "callingUid: $callingUid is not SYSTEM_UID")
            return false
        }
        return true
    }
}
