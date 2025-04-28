/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.supervision

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.app.supervision.SupervisionManager
import android.app.supervision.SupervisionRecoveryInfo
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import com.android.settings.R
import com.android.settings.password.ChooseLockSettingsHelper
import com.android.settingslib.supervision.SupervisionIntentProvider
import com.android.settingslib.supervision.SupervisionLog

/** Activity class for device supervision pin recovery flow. */
class SupervisionPinRecoveryActivity : FragmentActivity() {
    // ActivityResultLaunchers
    private val contract = ActivityResultContracts.StartActivityForResult()
    private val confirmPinLauncher: ActivityResultLauncher<Intent> by lazy {
        registerForActivityResult(contract) { result -> onPinConfirmed(result.resultCode) }
    }
    private val verificationLauncher: ActivityResultLauncher<Intent> by lazy {
        registerForActivityResult(contract) { result ->
            onVerification(result.resultCode, result.data)
        }
    }
    private val setPinLauncher: ActivityResultLauncher<Intent> by lazy {
        registerForActivityResult(contract) { result -> onPinSet(result.resultCode) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val actionType = intent.action
        when (actionType) {
            ACTION_SETUP -> startRecoverySetup()
            ACTION_SETUP_VERIFIED -> startConfirmPin()
            ACTION_RECOVERY -> startVerification()
            ACTION_UPDATE -> startConfirmPin()
            ACTION_POST_SETUP_VERIFY -> startConfirmPin()
            else -> handleError("PIN recovery result unknown actionType: $actionType")
        }
    }

    private fun startRecoverySetup() {
        val setupIntent =
            SupervisionIntentProvider.getPinRecoveryIntent(
                this,
                SupervisionIntentProvider.PinRecoveryAction.SET,
            )
        if (setupIntent != null) {
            verificationLauncher.launch(setupIntent)
        } else {
            handleError("No activity found for SETUP PIN recovery.")
        }
    }

    private fun startConfirmPin() {
        val confirmPinIntent =
            SupervisionIntentProvider.getConfirmSupervisionCredentialsIntent(this)
        if (confirmPinIntent != null) {
            confirmPinLauncher.launch(confirmPinIntent)
        } else {
            handleError("No activity found for confirm PIN.")
        }
    }

    private fun startVerification() {
        val recoveryIntent =
            SupervisionIntentProvider.getPinRecoveryIntent(
                this,
                SupervisionIntentProvider.PinRecoveryAction.VERIFY,
            )
        if (recoveryIntent != null) {
            val supervisionManager =
                applicationContext.getSystemService(SupervisionManager::class.java)
            val recoveryInfo = supervisionManager?.getSupervisionRecoveryInfo()

            recoveryIntent.apply {
                // Pass along any available recovery information.
                // TODO(b/409805806): will expose the parcelable as system API and pass it instead.
                recoveryInfo?.email?.let { putExtra(EXTRA_RECOVERY_EMAIL, it) }
                recoveryInfo?.id?.let { putExtra(EXTRA_RECOVERY_ID, it) }
                verificationLauncher.launch(this)
            }
        } else {
            handleError("No activity found for VERIFY PIN recovery.")
        }
    }

    private fun onPinConfirmed(resultCode: Int) {
        if (resultCode == Activity.RESULT_OK) {
            val nextAction = intent.action
            when (nextAction) {
                ACTION_SETUP_VERIFIED -> {
                    val setIntent =
                        SupervisionIntentProvider.getPinRecoveryIntent(
                            this,
                            SupervisionIntentProvider.PinRecoveryAction.SET_VERIFIED,
                        )
                    if (setIntent != null) {
                        verificationLauncher.launch(setIntent)
                    } else {
                        handleError("No activity found for SET_VERIFIED PIN recovery.")
                    }
                }
                ACTION_UPDATE -> {
                    val updatePinIntent =
                        SupervisionIntentProvider.getPinRecoveryIntent(
                            this,
                            SupervisionIntentProvider.PinRecoveryAction.UPDATE,
                        )
                    if (updatePinIntent != null) {
                        verificationLauncher.launch(updatePinIntent)
                    } else {
                        handleError("No activity found for UPDATE PIN recovery.")
                    }
                }
                ACTION_POST_SETUP_VERIFY -> {
                    val postSetupVerifyIntent =
                        SupervisionIntentProvider.getPinRecoveryIntent(
                            this,
                            SupervisionIntentProvider.PinRecoveryAction.POST_SETUP_VERIFY,
                        )
                    if (postSetupVerifyIntent != null) {
                        verificationLauncher.launch(postSetupVerifyIntent)
                    } else {
                        handleError("No activity found for post setup PIN recovery verify.")
                    }
                }
                else -> handleError("Unknown action after PIN confirmation: $nextAction")
            }
        } else {
            handleError("PIN confirmation failed with result: $resultCode")
        }
    }

    private fun onVerification(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            val action = intent.action
            when (action) {
                ACTION_RECOVERY -> startResetPinActivity() // Continue to set PIN after verification
                ACTION_SETUP,
                ACTION_UPDATE,
                ACTION_SETUP_VERIFIED,
                ACTION_POST_SETUP_VERIFY -> {
                    if (data != null) {
                        val supervisionManager =
                            applicationContext.getSystemService(SupervisionManager::class.java)
                        val recoveryInfo = SupervisionRecoveryInfo()
                        recoveryInfo.email = data.getStringExtra(EXTRA_RECOVERY_EMAIL)
                        recoveryInfo.id = data.getStringExtra(EXTRA_RECOVERY_ID)
                        supervisionManager?.setSupervisionRecoveryInfo(recoveryInfo)
                        handleSuccess()
                    } else {
                        handleError("Cannot save recovery info, no recovery info from result.")
                    }
                }
                else -> handleError("Unknown action after verification: $action")
            }
        } else {
            handleError(
                "Verification process failed with result: $resultCode, action: ${intent.action}"
            )
        }
    }

    private fun onPinSet(resultCode: Int) {
        if (resultCode == Activity.RESULT_OK) {
            // After the new PIN being set.
            Toast.makeText(
                    this,
                    getString(R.string.supervision_pin_reset_success_toast),
                    Toast.LENGTH_SHORT,
                )
                .show()
            handleSuccess()
        } else {
            handleError("Setting new PIN failed with result: $resultCode")
        }
    }

    /** Starts the reset supervision PIN activity for the supervising user. */
    private fun startResetPinActivity() {
        // TODO(b/407064075): reset the user or use other activity to skip entering current PIN.
        val intent =
            Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD).apply {
                putExtra(
                    DevicePolicyManager.EXTRA_PASSWORD_COMPLEXITY,
                    DevicePolicyManager.PASSWORD_COMPLEXITY_LOW,
                )
                putExtra(ChooseLockSettingsHelper.EXTRA_KEY_FINGERPRINT_ENROLLMENT_ONLY, true)
                putExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_CHOOSE_LOCK_SCREEN_TITLE,
                    getString(R.string.supervision_lock_setup_title),
                )
                putExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_CHOOSE_LOCK_SCREEN_DESCRIPTION,
                    getString(R.string.supervision_lock_setup_description),
                )
            }
        setPinLauncher.launch(intent)
    }

    /** Helper method to handle errors consistently. */
    private fun handleError(errorMessage: String) {
        Log.e(SupervisionLog.TAG, errorMessage)
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    /** Helper method to handle success consistently. */
    private fun handleSuccess() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    companion object {
        // Action types for the PIN recovery activity.
        const val ACTION_SETUP = "android.app.supervision.action.SETUP_PIN_RECOVERY"
        const val ACTION_RECOVERY = "android.app.supervision.action.PERFORM_PIN_RECOVERY"
        const val ACTION_UPDATE = "android.app.supervision.action.UPDATE_PIN_RECOVERY"
        const val ACTION_SETUP_VERIFIED =
            "android.app.supervision.action.SETUP_VERIFIED_PIN_RECOVERY"
        const val ACTION_POST_SETUP_VERIFY =
            "android.app.supervision.action.POST_SETUP_VERIFY_PIN_RECOVERY"

        // Extra keys
        private const val EXTRA_RECOVERY_EMAIL = "recoveryEmail"
        private const val EXTRA_RECOVERY_ID = "recoveryId"
    }
}
