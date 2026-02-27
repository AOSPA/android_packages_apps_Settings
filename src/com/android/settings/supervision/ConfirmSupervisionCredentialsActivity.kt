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
import android.Manifest.permission.SET_BIOMETRIC_DIALOG_ADVANCED
import android.Manifest.permission.USE_BIOMETRIC_INTERNAL
import android.annotation.ColorRes
import android.annotation.DrawableRes
import android.app.ActivityManager
import android.app.settings.SettingsEnums
import android.app.supervision.ISupervisionManager
import android.app.supervision.SupervisionManager
import android.app.supervision.SupervisionManager.ACTION_CONFIRM_SUPERVISION_APPROVAL
import android.app.supervision.flags.Flags
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.hardware.biometrics.BiometricPrompt.AuthenticationCallback
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ServiceManager
import android.os.UserHandle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.android.settings.R
import com.android.settings.overlay.FeatureFactory
import com.android.settings.supervision.SupervisionSessionController.Companion.SUPERVISION_AUTH_SESSION_KEY
import com.android.settings.supervision.credentialmanagement.SupervisionPinRecoveryActivity
import com.android.settings.supervision.shared.canLaunchPinRecovery
import com.android.settings.supervision.shared.isSupervisingCredentialSet
import com.android.settings.supervision.shared.supervisingUserHandle
import com.android.settingslib.supervision.SupervisionLog.TAG

/**
 * Activity for confirming supervision credentials.
 *
 * This activity orchestrates the process of verifying a supervising user's credentials. Its
 * behavior differs based on whether a supervision device credential has been configured.
 *
 * If a platform supervision credential is set, this activity prioritizes showing a biometric prompt
 * for verification.
 *
 * If a platform supervision credential is NOT configured, it checks for other available approval
 * methods:
 * - If only one alternative approval method is found, it is launched directly.
 * - If multiple methods are found, it displays a chooser dialog.
 *
 * It returns `Activity.RESULT_OK` if authentication succeeds, and `Activity.RESULT_CANCELED` if it
 * fails or is canceled. The activity also handles starting and stopping the supervising user's
 * profile as needed for the authentication process.
 */
class ConfirmSupervisionCredentialsActivity : FragmentActivity() {

    private lateinit var mSupervisingUser: UserHandle
    @VisibleForTesting var mProfileStarted = false
    private var mBiometricPromptShown = false
    private var mUserStateChangeReceiverRegistered = false
    private lateinit var mApprovalMethods: List<ResolveInfo>

    private val externalApprovalResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            onResultCode(result.resultCode)
        }

    private val userStateChangeReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val userId = intent.getIntExtra(Intent.EXTRA_USER_HANDLE, -1)

                when (intent.action) {
                    Intent.ACTION_USER_STOPPED -> {
                        if (mSupervisingUser.identifier != userId) {
                            return
                        }
                        // Restart the supervising profile since it was stopped.
                        val activityManager = getSystemService(ActivityManager::class.java)
                        if (!activityManager.startProfile(mSupervisingUser)) {
                            errorHandler("Unable to start supervising user")
                            return
                        } else {
                            mProfileStarted = true
                        }
                        if (!mBiometricPromptShown) {
                            showBiometricPrompt(mSupervisingUser.identifier)
                            mBiometricPromptShown = true
                        }
                    }
                }
            }
        }

    @VisibleForTesting
    val mAuthenticationCallback =
        object : AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Log.w(TAG, "onAuthenticationError(errorCode=$errorCode, errString=$errString)")
                setResult(RESULT_CANCELED)
                finish()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                setSuccessfulAuthenticationResultAndFinish()
            }

            override fun onAuthenticationFailed() {
                setResult(RESULT_CANCELED)
                finish()
            }
        }

    private val getResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            setResult(result.resultCode)
            finish()
        }

    /**
     * Registers a listener for results from [ApprovalMethodChooserDialogFragment]. When a result is
     * received, it sets the activity's result and finishes.
     */
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
                onResultCode(resultCode)
            }
        }
    }

    @RequiresPermission(
        allOf = [USE_BIOMETRIC_INTERNAL, SET_BIOMETRIC_DIALOG_ADVANCED, INTERACT_ACROSS_USERS_FULL]
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val supervisingUser = supervisingUserHandle()
        val isSupervisingCredentialSet = isSupervisingCredentialSet(supervisingUser)
        if (!Flags.enableSupervisionSettingsUiUpdates()) {
            if (supervisingUser == null) {
                errorHandler("No supervising user exists, cannot verify credentials.")
                return
            }
        }
        if (isSupervisingCredentialSet) {
            mSupervisingUser = supervisingUser!!
            val intentFilter = IntentFilter().apply { addAction(Intent.ACTION_USER_STOPPED) }
            registerReceiver(userStateChangeReceiver, intentFilter, RECEIVER_NOT_EXPORTED)
            mUserStateChangeReceiverRegistered = true
        }

        if (savedInstanceState != null) {
            mProfileStarted = savedInstanceState.getBoolean(KEY_PROFILE_STARTED, mProfileStarted)
            mBiometricPromptShown =
                savedInstanceState.getBoolean(KEY_BIOMETRIC_PROMPT_SHOWN, mBiometricPromptShown)
        } else {
            // BiometricPrompt persists through configuration change, so only create it on initial
            // activity creation.
            mApprovalMethods =
                if (Flags.enableSupervisionSettingsUiUpdates()) {
                    val binder = ServiceManager.getService("supervision")
                    val supervisionManager = ISupervisionManager.Stub.asInterface(binder)
                    supervisionManager.querySupervisionApprovalActivities(userId)
                } else {
                    emptyList()
                }
            if (!isSupervisingCredentialSet) {
                // --- SUPERVISION CREDENTIAL NOT SET ---
                if (!Flags.enableSupervisionSettingsUiUpdates()) {
                    // Redirects to the setup supervision flow when credential is not set.
                    val setupIntent = Intent(this, SetupSupervisionActivity::class.java)
                    getResultLauncher.launch(setupIntent)
                    return
                }
                val forceConfirmation = intent.getBooleanExtra(EXTRA_FORCE_CONFIRMATION, false)
                val isSessionActive =
                    if (Flags.enableSupervisionSettingsSessionUpdates())
                        SupervisionSessionController.getInstance(this)
                            .isSessionActive(SUPERVISION_AUTH_SESSION_KEY)
                    else SupervisionAuthController.getInstance(this).isSessionActive(taskId)
                if (!forceConfirmation && isSessionActive) {
                    Log.i(TAG, "Bypassing authentication due to active session")
                    setResult(RESULT_OK)
                    finish()
                }
                when (mApprovalMethods.size) {
                    0 -> {
                        // This activity should not be launched if there are no available approval
                        // methods.
                        errorHandler("No supervision approval methods available.")
                    }

                    1 -> {
                        // No credential set, one other method. Launching directly.
                        val resolveInfo = mApprovalMethods[0]
                        val activityInfo = resolveInfo.activityInfo
                        val newIntent =
                            Intent(ACTION_CONFIRM_SUPERVISION_APPROVAL).apply {
                                component =
                                    ComponentName(activityInfo.packageName, activityInfo.name)
                                putExtras(intent.extras ?: Bundle())
                            }
                        externalApprovalResultLauncher.launch(newIntent)
                    }

                    else -> {
                        showApprovalMethodChooser()
                    }
                }
            } else {
                // --- SUPERVISION CREDENTIAL IS SET ---
                val forceConfirmation = intent.getBooleanExtra(EXTRA_FORCE_CONFIRMATION, false)
                val isSessionActive =
                    if (Flags.enableSupervisionSettingsSessionUpdates())
                        SupervisionSessionController.getInstance(this)
                            .isSessionActive(SUPERVISION_AUTH_SESSION_KEY)
                    else SupervisionAuthController.getInstance(this).isSessionActive(taskId)
                if (forceConfirmation || !isSessionActive) {
                    val activityManager = getSystemService(ActivityManager::class.java)
                    if (!activityManager.startProfile(mSupervisingUser)) {
                        errorHandler("Unable to start supervising user, cannot verify credentials.")
                        return
                    } else {
                        mProfileStarted = true
                    }
                    val isUserRunning = activityManager.isUserRunning(mSupervisingUser.identifier)
                    if (isUserRunning) {
                        showBiometricPrompt(mSupervisingUser.identifier)
                        mBiometricPromptShown = true
                    }
                } else {
                    Log.i(TAG, "Bypassing authentication due to active session")
                    setResult(RESULT_OK)
                    finish()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_PROFILE_STARTED, mProfileStarted)
        outState.putBoolean(KEY_BIOMETRIC_PROMPT_SHOWN, mBiometricPromptShown)
        super.onSaveInstanceState(outState)
    }

    @RequiresPermission(anyOf = [INTERACT_ACROSS_USERS_FULL, MANAGE_USERS])
    @VisibleForTesting
    public override fun onDestroy() {
        super.onDestroy()
        if (mUserStateChangeReceiverRegistered) {
            unregisterReceiver(userStateChangeReceiver)
        }
        // Do not stop the profile on configuration change, since the authentication session in
        // BiometricPrompt is still active.
        if (mProfileStarted && isFinishing) {
            tryStopProfile()
        }
    }

    private fun showApprovalMethodChooser() {
        registerApprovalMethodChooserResultListener()
        val chooserFragment =
            ApprovalMethodChooserDialogFragment.newInstance(mApprovalMethods, intent.extras)
        chooserFragment.show(supportFragmentManager, "ApprovalMethodChooser")
    }

    @RequiresPermission(allOf = [USE_BIOMETRIC_INTERNAL, SET_BIOMETRIC_DIALOG_ADVANCED])
    fun showBiometricPrompt(userId: Int) {
        getBiometricPrompt()
            .authenticateUser(
                CancellationSignal(),
                ContextCompat.getMainExecutor(this),
                mAuthenticationCallback,
                userId,
            )
    }

    @RequiresPermission(value = SET_BIOMETRIC_DIALOG_ADVANCED)
    @VisibleForTesting
    fun getBiometricPrompt(): BiometricPrompt {
        val builder =
            BiometricPrompt.Builder(this)
                .setConfirmationRequired(true)
                .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL)

        if (com.android.systemui.Flags.largeScreenBp()) {
            builder
                .setTitle(getString(R.string.supervision_pin_verification_title))
                .setSubtitle(getString(R.string.supervision_pin_verification_subtitle))
                .setLogoBitmap(
                    getTintedBitmap(
                        R.drawable.ic_account_child_invert_48,
                        com.android.internal.R.color.materialColorOnSurface,
                    )
                )
                .setLogoDescription(getString(R.string.supervision_settings_title))
        } else {
            builder.setTitle(getString(R.string.supervision_full_screen_pin_verification_title))
        }

        // Add fallback for each other available approval method
        if (Flags.enableSupervisionSettingsUiUpdates()) {
            val packageManager = applicationContext.packageManager
            for (resolveInfo in mApprovalMethods) {
                val activityInfo = resolveInfo.activityInfo
                val label = resolveInfo.loadLabel(packageManager)
                val componentName = ComponentName(activityInfo.packageName, activityInfo.name)

                val listener =
                    DialogInterface.OnClickListener { _: DialogInterface?, _: Int ->
                        onApprovalMethodsClicked(componentName)
                    }
                // TODO(b/454404010): Use icon from resolveInfo once the arbitrary icon is
                // supported.
                builder.addFallbackOption(
                    label.toString(),
                    BiometricManager.ICON_TYPE_SUPERVISED,
                    ContextCompat.getMainExecutor(this),
                    listener,
                )
            }
        }

        val supportSupervisionRecovery =
            if (Flags.enableSupervisionSettingsUiUpdates()) {
                this.canLaunchPinRecovery()
            } else {
                getSystemService(SupervisionManager::class.java)?.getSupervisionRecoveryInfo() !=
                    null
            }

        if (supportSupervisionRecovery) {
            val listener =
                DialogInterface.OnClickListener { _: DialogInterface?, _: Int ->
                    onForgotPinFallbackClicked()
                }

            builder.addFallbackOption(
                getString(R.string.supervision_auth_prompt_forgot_pin_button_label),
                if (Flags.enableSupervisionSettingsUiUpdates()) BiometricManager.ICON_TYPE_RESET
                else BiometricManager.ICON_TYPE_ACCOUNT,
                ContextCompat.getMainExecutor(this),
                listener,
            )
        }
        return builder.build()
    }

    @VisibleForTesting
    fun onApprovalMethodsClicked(componentName: ComponentName) {
        val intent =
            Intent(ACTION_CONFIRM_SUPERVISION_APPROVAL)
                .setComponent(componentName)
                .putExtras(intent.extras ?: Bundle())
        externalApprovalResultLauncher.launch(intent)
    }

    @VisibleForTesting
    fun onForgotPinFallbackClicked() {
        val metricsFeatureProvider = FeatureFactory.featureFactory.metricsFeatureProvider
        metricsFeatureProvider.action(
            this,
            SettingsEnums.ACTION_SUPERVISION_FORGOT_PIN_DURING_PIN_INVOCATION,
        )
        val intent =
            Intent(this, SupervisionPinRecoveryActivity::class.java).apply {
                action = SupervisionPinRecoveryActivity.ACTION_RECOVERY
            }
        getResultLauncher.launch(intent)
    }

    private fun setSuccessfulAuthenticationResultAndFinish() {
        if (Flags.enableSupervisionSettingsSessionUpdates()) {
            val sessionsRepository =
                SupervisionSessionController.getInstance(this@ConfirmSupervisionCredentialsActivity)
            sessionsRepository.startSession(taskId, SUPERVISION_AUTH_SESSION_KEY)
        } else {
            val authController =
                SupervisionAuthController.getInstance(this@ConfirmSupervisionCredentialsActivity)
            authController.startSession(taskId)
        }
        setResult(RESULT_OK)
        finish()
    }

    private fun onResultCode(resultCode: Int) {
        if (resultCode == RESULT_OK) {
            setSuccessfulAuthenticationResultAndFinish()
        } else {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    @RequiresPermission(anyOf = [INTERACT_ACROSS_USERS_FULL, MANAGE_USERS])
    private fun tryStopProfile() {
        val supervisingUser = supervisingUserHandle()
        if (supervisingUser == null) {
            Log.w(TAG, "Cannot stop supervising profile because it does not exist.")
            return
        }
        val activityManager = getSystemService(ActivityManager::class.java)
        if (!activityManager.stopProfile(supervisingUser)) {
            Log.w(TAG, "Could not stop the supervising profile.")
        } else {
            mProfileStarted = false
        }
    }

    private fun getTintedBitmap(@DrawableRes drawableId: Int, @ColorRes colorId: Int): Bitmap {
        val drawable = getDrawable(drawableId)!!.mutate()
        val color = getColor(colorId)
        drawable.setTint(color)

        val bitmap =
            Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888,
            )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }

    private fun errorHandler(errStr: String? = null) {
        errStr?.let { Log.e(TAG, it) }
        setResult(RESULT_CANCELED)
        finish()
    }

    companion object {
        // If true, force confirmation of supervision credentials, regardless of active auth session
        @VisibleForTesting const val EXTRA_FORCE_CONFIRMATION = "force_confirmation"
        // Whether the supervising profile was started by this activity
        const val KEY_PROFILE_STARTED = "profile_started"
        const val KEY_BIOMETRIC_PROMPT_SHOWN = "biometric_prompt_shown"
    }
}
