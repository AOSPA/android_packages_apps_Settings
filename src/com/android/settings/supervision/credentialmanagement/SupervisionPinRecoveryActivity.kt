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
package com.android.settings.supervision.credentialmanagement

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.app.settings.SettingsEnums
import android.app.supervision.ISupervisionManager
import android.app.supervision.SupervisionManager
import android.app.supervision.SupervisionRecoveryInfo
import android.app.supervision.SupervisionRecoveryInfo.EXTRA_SUPERVISION_RECOVERY_INFO
import android.app.supervision.flags.Flags
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.os.ServiceManager
import android.os.UserHandle
import android.os.UserManager
import android.os.UserManager.USER_TYPE_PROFILE_SUPERVISING
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.fragment.app.FragmentActivity
import com.android.internal.widget.LockPatternUtils
import com.android.settings.overlay.FeatureFactory
import com.android.settings.password.ChooseLockGeneric
import com.android.settings.password.ChooseLockPassword
import com.android.settings.supervision.ApprovalMethodChooserDialogFragment
import com.android.settings.supervision.shared.supervisingUserHandle
import com.android.settingslib.supervision.SupervisionIntentProvider
import com.android.settingslib.supervision.SupervisionLog

/** Activity class for device supervision pin recovery flow. */
class SupervisionPinRecoveryActivity : FragmentActivity() {
    // ActivityResultLaunchers
    private val contract = ActivityResultContracts.StartActivityForResult()
    private val confirmPinLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(contract) { result -> onPinConfirmed(result.resultCode) }

    private val verificationLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(contract) { result ->
            onVerification(result.resultCode, result.data)
        }
    private val setPinLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(contract) { result -> onPinSet(result.resultCode) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val actionType = intent.action
        when (actionType) {
            ACTION_SETUP -> startRecoverySetup()
            ACTION_SETUP_VERIFIED -> startConfirmPin()
            ACTION_RECOVERY -> startVerification()
            ACTION_UPDATE -> startConfirmPin()
            ACTION_POST_SETUP_VERIFY -> startConfirmPin()
            else ->
                setResultCanceledAndFinish("PIN recovery result unknown actionType: $actionType")
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
            setResultCanceledAndFinish("No activity found for SETUP PIN recovery.")
        }
    }

    private fun startConfirmPin() {
        val confirmPinIntent =
            SupervisionIntentProvider.getConfirmSupervisionCredentialsIntent(
                context = this,
                forceConfirm = true,
            )
        if (confirmPinIntent != null) {
            confirmPinLauncher.launch(confirmPinIntent)
        } else {
            setResultCanceledAndFinish("No activity found for confirm PIN.")
        }
    }

    private fun startVerification() {
        if (Flags.enableSupervisionSettingsUiUpdates()) {
            val supervisionManager = getSystemService(SupervisionManager::class.java)
            val recoveryInfo = supervisionManager?.getSupervisionRecoveryInfo()

            // If there is a recovery email set, default to the existing email recovery flow.
            if (recoveryInfo != null) {
                val verifiedEmailRecoveryIntent =
                    SupervisionIntentProvider.getPinRecoveryIntent(
                        this,
                        SupervisionIntentProvider.PinRecoveryAction.VERIFY,
                    )
                if (verifiedEmailRecoveryIntent != null) {
                    verifiedEmailRecoveryIntent.apply {
                        putExtra(EXTRA_SUPERVISION_RECOVERY_INFO, recoveryInfo)
                        verificationLauncher.launch(this)
                    }
                    return
                }
            }

            // Get other approval methods
            val binder = ServiceManager.getService(Context.SUPERVISION_SERVICE)
            val iSupervisionManager = ISupervisionManager.Stub.asInterface(binder)
            val approvalMethods =
                iSupervisionManager.querySupervisionApprovalActivities(getUserId())

            when (approvalMethods.size) {
                0 -> {
                    setResultCanceledAndFinish("No supervision recovery methods available.")
                }
                1 -> {
                    // If there's only one supervision authentication method, go directly to that
                    // method.
                    val resolveInfo = approvalMethods[0]
                    val activityInfo = resolveInfo.activityInfo
                    val intent =
                        Intent(SupervisionManager.ACTION_CONFIRM_SUPERVISION_APPROVAL).apply {
                            component = ComponentName(activityInfo.packageName, activityInfo.name)
                            putExtra(EXTRA_SUPERVISION_RECOVERY_INFO, recoveryInfo)
                        }
                    verificationLauncher.launch(intent)
                }
                else -> {
                    // If there are multiple available supervision methods, show a picker UI.
                    showApprovalMethodChooser(approvalMethods)
                }
            }
        } else {
            // Original logic when flag is off
            val emailRecoveryIntent =
                SupervisionIntentProvider.getPinRecoveryIntent(
                    this,
                    SupervisionIntentProvider.PinRecoveryAction.VERIFY,
                )
            if (emailRecoveryIntent != null) {
                val supervisionManager = getSystemService(SupervisionManager::class.java)
                val recoveryInfo = supervisionManager?.getSupervisionRecoveryInfo()

                emailRecoveryIntent.apply {
                    putExtra(EXTRA_SUPERVISION_RECOVERY_INFO, recoveryInfo)
                    verificationLauncher.launch(this)
                }
            } else {
                setResultCanceledAndFinish("No supervision recovery methods available.")
            }
        }
    }

    private fun showApprovalMethodChooser(approvalMethods: List<ResolveInfo>) {
        registerApprovalMethodChooserResultListener()
        val chooserFragment = ApprovalMethodChooserDialogFragment.newInstance(approvalMethods)
        chooserFragment.show(supportFragmentManager, "ApprovalMethodChooser")
    }

    private fun registerApprovalMethodChooserResultListener() {
        supportFragmentManager.setFragmentResultListener(
            ApprovalMethodChooserDialogFragment.REQUEST_KEY_APPROVAL_RESULT,
            this,
        ) { requestKey, bundle ->
            if (requestKey == ApprovalMethodChooserDialogFragment.REQUEST_KEY_APPROVAL_RESULT) {
                val resultCode =
                    bundle.getInt(
                        ApprovalMethodChooserDialogFragment.BUNDLE_KEY_RESULT_CODE,
                        RESULT_CANCELED,
                    )
                onVerification(resultCode, null)
            }
        }
    }

    private fun onPinConfirmed(resultCode: Int) {
        if (resultCode == RESULT_OK) {
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
                        setResultCanceledAndFinish(
                            "No activity found for SET_VERIFIED PIN recovery."
                        )
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
                        setResultCanceledAndFinish("No activity found for UPDATE PIN recovery.")
                    }
                }
                ACTION_POST_SETUP_VERIFY -> {
                    val postSetupVerifyIntent =
                        SupervisionIntentProvider.getPinRecoveryIntent(
                            this,
                            SupervisionIntentProvider.PinRecoveryAction.POST_SETUP_VERIFY,
                        )
                    if (postSetupVerifyIntent != null) {
                        val supervisionManager = getSystemService(SupervisionManager::class.java)
                        val recoveryInfo = supervisionManager?.getSupervisionRecoveryInfo()
                        postSetupVerifyIntent.apply {
                            putExtra(EXTRA_SUPERVISION_RECOVERY_INFO, recoveryInfo)
                            verificationLauncher.launch(postSetupVerifyIntent)
                        }
                    } else {
                        setResultCanceledAndFinish(
                            "No activity found for post setup PIN recovery verify."
                        )
                    }
                }
                else ->
                    setResultCanceledAndFinish("Unknown action after PIN confirmation: $nextAction")
            }
        } else {
            setResultCanceledAndFinish("PIN confirmation failed with result: $resultCode")
        }
    }

    private fun onVerification(resultCode: Int, data: Intent?) {
        if (intent.action == null) {
            setResultCanceledAndFinish("Null action received in onVerification.")
            return
        }

        val action = intent.action!!

        if (resultCode != RESULT_OK) {
            logRecoveryResult(action, success = false)
            setResultCanceledAndFinish(
                "Verification process failed with result: $resultCode, action: $action"
            )
            return
        }

        val actionNeedsData =
            when (action) {
                ACTION_SETUP,
                ACTION_UPDATE,
                ACTION_SETUP_VERIFIED,
                ACTION_POST_SETUP_VERIFY -> true
                else -> false
            }

        if (actionNeedsData && data == null) {
            setResultCanceledAndFinish("Cannot save recovery info, no result data. Action: $action")
            return
        }

        when (action) {
            ACTION_RECOVERY -> {
                startResetPinActivity() // reset PIN after verification
            }
            ACTION_SETUP,
            ACTION_UPDATE,
            ACTION_SETUP_VERIFIED,
            ACTION_POST_SETUP_VERIFY -> {
                val recoveryInfo =
                    data!!.getParcelableExtra(
                        EXTRA_SUPERVISION_RECOVERY_INFO,
                        SupervisionRecoveryInfo::class.java,
                    )

                if (recoveryInfo == null) {
                    setResultCanceledAndFinish(
                        "Cannot save recovery info, no valid recovery info from result. Action: $action"
                    )
                    return
                }

                val supervisionManager = getSystemService(SupervisionManager::class.java)
                supervisionManager?.setSupervisionRecoveryInfo(recoveryInfo)
                logRecoveryResult(action, success = true)
                setResultOkAndFinish()
            }
            else -> {
                setResultCanceledAndFinish("Unknown action after successful verification: $action")
            }
        }
    }

    private fun onPinSet(resultCode: Int) {
        if (resultCode != RESULT_CANCELED) {
            // After the new PIN being set.
            logRecoveryResult(intent.action, success = true)
            setResultOkAndFinish()
        } else {
            logRecoveryResult(intent.action, success = false)
            setResultCanceledAndFinish("Setting new PIN failed with result cancelled.")
        }
    }

    /** Starts the reset supervision PIN activity for the supervising user. */
    @RequiresPermission(
        anyOf = [Manifest.permission.CREATE_USERS, Manifest.permission.MANAGE_USERS]
    )
    private fun startResetPinActivity() {
        if (!resetSupervisionUser()) {
            setResultCanceledAndFinish("Failed to reset supervision user.")
            logRecoveryResult(ACTION_RECOVERY, success = false)
            return
        }
        val intent =
            Intent(this, ChooseLockGeneric::class.java).apply {
                putExtra(
                    LockPatternUtils.PASSWORD_TYPE_KEY,
                    DevicePolicyManager.PASSWORD_QUALITY_NUMERIC,
                )
                putExtra(Intent.EXTRA_USER_ID, supervisingUserHandle()?.identifier)
                if (Flags.enableSupervisionSettingsUiUpdates()) {
                    putExtra(ChooseLockPassword.EXTRA_KEY_FOR_SUPERVISION_RESET, true)
                }
            }
        setPinLauncher.launch(intent)
    }

    /** Helper method to handle errors consistently. */
    private fun setResultCanceledAndFinish(errorMessage: String) {
        Log.e(SupervisionLog.TAG, errorMessage)
        setResult(RESULT_CANCELED)
        finish()
    }

    /** Helper method to handle success consistently. */
    private fun setResultOkAndFinish() {
        setResult(RESULT_OK)
        finish()
    }

    /** Logs the result of the PIN recovery process. */
    private fun logRecoveryResult(action: String?, success: Boolean) {
        val metricsFeatureProvider = FeatureFactory.featureFactory.metricsFeatureProvider
        when (action) {
            // Always log for the PIN reset flow
            ACTION_RECOVERY -> {
                metricsFeatureProvider.action(
                    this,
                    SettingsEnums.ACTION_SUPERVISION_PIN_RESET_SUCCEED,
                    success,
                )
            }

            ACTION_SETUP,
            ACTION_SETUP_VERIFIED,
            ACTION_POST_SETUP_VERIFY -> {
                if (!Flags.enableSupervisionPinMetrics()) {
                    return
                }
                val logAction =
                    when (action) {
                        ACTION_SETUP ->
                            SettingsEnums.ACTION_SUPERVISION_ADD_RECOVERY_ON_SETUP_SUCCESS
                        ACTION_SETUP_VERIFIED ->
                            SettingsEnums.ACTION_SUPERVISION_ADD_RECOVERY_POST_SETUP_SUCCESS
                        ACTION_POST_SETUP_VERIFY ->
                            SettingsEnums.ACTION_SUPERVISION_VERIFY_RECOVERY_SUCCESS
                        else -> return
                    }
                metricsFeatureProvider.action(this, logAction, success)
            }
        }
    }

    /**
     * Resets the supervision user by removing the existing one and creating a new one.
     *
     * This method first retrieves the current supervising user handle. If it exists, the user is
     * removed. Then, a new supervising user is created.
     *
     * @return True if the reset was successful, false otherwise.
     */
    @RequiresPermission(
        anyOf = [Manifest.permission.CREATE_USERS, Manifest.permission.MANAGE_USERS]
    )
    private fun resetSupervisionUser(): Boolean {
        val userManager = getSystemService(UserManager::class.java)
        userManager?.supervisingUserHandle()?.let {
            userManager.removeUserEvenWhenDisallowed(it.identifier)
        }
        val userInfo =
            userManager.createProfileForUserEvenWhenDisallowed(
                /* name= */ "Supervising",
                /* userType= */ USER_TYPE_PROFILE_SUPERVISING,
                /* flags= */ 0,
                /* userId= */ UserHandle.USER_NULL,
                /* disallowedPackages = */ null,
            )
        if (userInfo != null) {
            return true
        } else {
            Log.e(SupervisionLog.TAG, "Unable to create supervising profile.")
            return false
        }
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
    }
}
