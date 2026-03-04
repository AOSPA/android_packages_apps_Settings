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
import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.app.settings.SettingsEnums.ACTION_SUPERVISION_ENABLE_SUPERVISION
import android.app.settings.SettingsEnums.ACTION_SUPERVISION_PIN_SET_UP
import android.app.settings.SettingsEnums.ACTION_SUPERVISION_SKIP_PIN_RECOVERY
import android.app.supervision.SupervisionManager
import android.app.supervision.flags.Flags
import android.content.Intent
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
import android.os.UserManager.USER_TYPE_PROFILE_SUPERVISING
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.android.internal.widget.LockPatternUtils
import com.android.settings.R
import com.android.settings.overlay.FeatureFactory
import com.android.settings.password.ChooseLockGeneric
import com.android.settings.supervision.credentialmanagement.SupervisionPinRecoveryActivity
import com.android.settings.supervision.shared.canLaunchPinRecovery
import com.android.settings.supervision.shared.isSupervisingCredentialSet
import com.android.settings.supervision.shared.supervisingUserHandle
import com.android.settingslib.HelpUtils
import com.android.settingslib.collapsingtoolbar.R.drawable.settingslib_expressive_icon_back as EXPRESSIVE_BACK_ICON
import com.android.settingslib.supervision.SupervisionLog
import com.android.settingslib.widget.SettingsThemeHelper
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupcompat.template.FooterButton
import com.google.android.setupdesign.GlifLayout
import com.google.android.setupdesign.util.ThemeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

    private val chooseLockLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleSetLockResult(result)
        }

    private val confirmSupervisionCredentialsLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleConfirmSupervisionCredentialsResult(result)
        }

    private val setupRecoveryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handlePinRecoveryResult(result)
        }

    private var mIsEnablingSupervision = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            mIsEnablingSupervision =
                savedInstanceState.getBoolean(KEY_IS_ENABLING_SUPERVISION, false)
        }

        if (!Flags.enableSupervisionSettingsUiUpdates()) {
            if (isSupervisingCredentialSet()) {
                setResult(RESULT_CANCELED)
                finish()
                return
            }

            if (savedInstanceState == null) {
                // Set up loading screen before enabling supervision
                setTheme(R.style.Theme_Settings)
                setContentView(R.layout.supervision_dashboard_loading_screen)
                enableSupervision()
            }
            return
        }

        val supervisionManager = getSystemService(SupervisionManager::class.java)
        val platformCredentialExists = isSupervisingCredentialSet()

        if (supervisionManager?.isSupervisionEnabled() == true && platformCredentialExists) {
            // Nothing to set up if we don't need to enable supervision or create a platform
            // credential.
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        // If there are profiles other than the main profile and the supervising profile,
        // block enabling supervision. Those other profiles could allow the user to bypass
        // supervision.
        if (hasMultipleNonSupervisingProfiles()) {
            showMultiProfileErrorDialog()
            return
        }

        if (platformCredentialExists) {
            // Supervision is not enabled but platform credentials are set, so confirm
            // credentials then enable supervision.
            confirmSupervisionCredentialsLauncher.launch(
                Intent(this, ConfirmSupervisionCredentialsActivity::class.java).apply {
                    putExtra(ConfirmSupervisionCredentialsActivity.EXTRA_FORCE_CONFIRMATION, true)
                }
            )
            return
        }

        // Default path: start the supervision credential creation flow.
        ThemeHelper.trySetSuwTheme(this)
        setContentView(R.layout.supervision_setup_introduction)

        val layout = findViewById<GlifLayout?>(R.id.supervision_setup_introduction)
        val iconDrawable = getDrawable(R.drawable.ic_account_child_invert_48)!!
        iconDrawable.mutate()
        iconDrawable.setTintList(layout?.getPrimaryColor())
        layout?.setIcon(iconDrawable)
        layout
            ?.getDescriptionTextView()
            ?.setContentDescription(getString(R.string.supervision_intro_content_description))

        val footer = layout?.getMixin(FooterBarMixin::class.java)
        footer?.setPrimaryButton(
            FooterButton.Builder(this)
                .setText(R.string.next_label)
                .setButtonType(FooterButton.ButtonType.NEXT)
                .setListener {
                    mIsEnablingSupervision = true
                    // Show loading indicator while waiting for the supervising profile to be
                    // created.
                    layout?.setProgressBarShown(true)

                    // Hide the buttons while waiting for the supervising profile to be created.
                    footer?.getButtonContainer()?.setVisibility(View.GONE)

                    enableSupervision()
                }
                .build()
        )
        footer?.setSecondaryButton(
            FooterButton.Builder(this)
                .setText(R.string.cancel)
                .setButtonType(FooterButton.ButtonType.CANCEL)
                .setListener {
                    setResult(RESULT_CANCELED)
                    finish()
                }
                .build()
        )

        if (mIsEnablingSupervision) {
            layout?.setProgressBarShown(true)
            footer?.getButtonContainer()?.setVisibility(View.GONE)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_IS_ENABLING_SUPERVISION, mIsEnablingSupervision)
    }

    public override fun onResume() {
        super.onResume()

        getActionBar()?.let {
            it.elevation = 0f
            it.setDisplayHomeAsUpEnabled(true)
            if (SettingsThemeHelper.isExpressiveTheme(this)) {
                it.setHomeAsUpIndicator(EXPRESSIVE_BACK_ICON)
            }
        }

        if (!Flags.enableSupervisionSettingsUiUpdates() && isSupervisingCredentialSet()) {
            setResult(RESULT_OK)
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            setResult(RESULT_CANCELED)
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @RequiresPermission(anyOf = [CREATE_USERS, MANAGE_USERS])
    private fun enableSupervision() {
        // Create user profile in the background to avoid blocking the loading indicator UI
        lifecycleScope.launch(ioDispatcher) {
            val supervisingUser: UserHandle? = setupSupervisingUser()

            // Update UI on the main thread
            lifecycleScope.launch(Dispatchers.Main) {
                if (supervisingUser != null) {
                    startChooseLockActivity(supervisingUser)
                } else {
                    showGenericErrorScreen()
                }
            }
        }
    }

    /** Displays the generic error screen */
    private fun showGenericErrorScreen() {
        startActivity(Intent(this, SupervisionErrorActivity::class.java))
        setResult(RESULT_CANCELED)
        finish()
    }

    @RequiresPermission(anyOf = [CREATE_USERS, MANAGE_USERS])
    private fun setupSupervisingUser(): UserHandle? {
        val userManager = getSystemService(UserManager::class.java)
        var userHandle = userManager.supervisingUserHandle()
        // If a supervising profile does not already exist on the device, create one
        if (userHandle == null) {
            val userInfo =
                userManager?.createProfileForUserEvenWhenDisallowed(
                    "Supervising",
                    USER_TYPE_PROFILE_SUPERVISING,
                    /* flags= */ 0,
                    UserHandle.USER_NULL,
                    /* disallowedPackages= */ null,
                )
            if (userInfo == null) {
                Log.w(SupervisionLog.TAG, "Could not create supervising profile.")
                return null
            }

            userHandle = userInfo.userHandle
        }

        return userHandle
    }

    private fun handleConfirmSupervisionCredentialsResult(result: ActivityResult) {
        if (result.resultCode == RESULT_OK) {
            setResult(RESULT_OK)
            getSystemService(SupervisionManager::class.java)?.setSupervisionEnabled(true)
            FeatureFactory.featureFactory.metricsFeatureProvider.action(
                this,
                ACTION_SUPERVISION_ENABLE_SUPERVISION,
            )
        } else {
            setResult(RESULT_CANCELED)
        }
        finish()
    }

    @RequiresPermission(anyOf = [INTERACT_ACROSS_USERS_FULL, INTERACT_ACROSS_USERS])
    private fun startChooseLockActivity(userHandle: UserHandle) {
        val intent =
            Intent(this, ChooseLockGeneric::class.java).apply {
                putExtra(
                    LockPatternUtils.PASSWORD_TYPE_KEY,
                    DevicePolicyManager.PASSWORD_QUALITY_NUMERIC,
                )
                putExtra(Intent.EXTRA_USER_ID, userHandle.identifier)
            }
        chooseLockLauncher.launch(intent)
    }

    private fun handleSetLockResult(result: ActivityResult) {
        val supervisingUser = supervisingUserHandle()
        if (supervisingUser == null) {
            Log.w(SupervisionLog.TAG, "No supervising user handle found after lock setup.")
            setResult(RESULT_CANCELED)
            finish()
            return
        }
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
        if (Flags.enableSupervisionSettingsUiUpdates()) {
            FeatureFactory.featureFactory.metricsFeatureProvider.action(
                this,
                ACTION_SUPERVISION_ENABLE_SUPERVISION,
            )
            FeatureFactory.featureFactory.metricsFeatureProvider.action(
                this,
                ACTION_SUPERVISION_PIN_SET_UP,
            )
        }

        // Start PIN recovery setup if a recovery method does not already exist.
        if (!Flags.enableSupervisionSettingsUiUpdates() || !canLaunchPinRecovery()) {
            startSetupRecoveryFlow()
        } else {
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun handlePinRecoveryResult(result: ActivityResult) {
        if (result.resultCode == RESULT_CANCELED) {
            Log.i(SupervisionLog.TAG, "PIN recovery setup was skipped by the user.")

            FeatureFactory.featureFactory.metricsFeatureProvider.action(
                this,
                ACTION_SUPERVISION_SKIP_PIN_RECOVERY,
            )
        }
        setResult(RESULT_OK)
        finish()
    }

    private fun startSetupRecoveryFlow() {
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
        setupRecoveryLauncher.launch(intent)
    }

    private fun hasMultipleNonSupervisingProfiles(): Boolean {
        val userManager = getSystemService(UserManager::class.java) ?: return false

        val supervisingProfileHandle: UserHandle? = userManager.supervisingUserHandle()
        val nonSupervisingProfilesCount =
            userManager.userProfiles.count { it != supervisingProfileHandle }

        // More than one profile remains (the main user + at least one other)
        return nonSupervisingProfilesCount > 1
    }

    private fun showMultiProfileErrorDialog() {
        if (SettingsThemeHelper.isExpressiveTheme(this)) {
            setTheme(R.style.Transparent_Expressive)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.supervision_setup_multi_profile_error_title)
            .setMessage(R.string.supervision_setup_multi_profile_error_message)
            .setPositiveButton(android.R.string.ok) { _, _ -> null }
            .setNeutralButton(R.string.learn_more) { _, _ -> onSupervisionUnavailableLearnMore() }
            .setOnDismissListener { multiProfileErrorDialogDismiss() }
            .show()
    }

    private fun multiProfileErrorDialogDismiss() {
        setResult(RESULT_CANCELED)
        finish()
    }

    private fun onSupervisionUnavailableLearnMore() {
        val intent =
            HelpUtils.getHelpIntent(
                this,
                getString(R.string.supervision_unavailable_learn_more_link),
                this::class.java.name,
            )
        if (intent != null) {
            startActivity(intent)
        } else {
            Log.w(SupervisionLog.TAG, "HelpIntent is null")
        }
    }

    companion object {
        @VisibleForTesting var ioDispatcher = Dispatchers.IO
        private const val KEY_IS_ENABLING_SUPERVISION = "key_is_enabling_supervision"
    }
}
