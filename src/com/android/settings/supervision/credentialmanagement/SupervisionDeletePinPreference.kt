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

import android.app.Activity
import android.app.settings.SettingsEnums.ACTION_SUPERVISION_DELETE_PIN
import android.app.settings.SettingsEnums.ACTION_SUPERVISION_MULTIPLE_PROVIDERS_DELETE_PIN
import android.app.settings.SettingsEnums.ACTION_SUPERVISION_MULTIPLE_USERS_DISABLE_SUPERVISION
import android.app.settings.SettingsEnums.SUPERVISION_MANAGE_PIN_SCREEN
import android.app.supervision.ISupervisionManager
import android.app.supervision.SupervisionManager
import android.app.supervision.flags.Flags
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.UserInfo
import android.os.ServiceManager
import android.os.UserHandle
import android.os.UserManager
import android.text.Html
import android.text.Spanned
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.overlay.FeatureFactory
import com.android.settings.spa.network.getActivity
import com.android.settings.supervision.ConfirmSupervisionCredentialsActivity
import com.android.settings.supervision.shared.SupervisionHelper.KEY_RECOVERY_BANNER_DISMISSED
import com.android.settings.supervision.shared.SupervisionHelper.SHARED_PREFS_NAME
import com.android.settings.supervision.shared.areAnyUsersExceptCurrentSupervised
import com.android.settings.supervision.shared.deleteSupervisionData
import com.android.settings.supervision.shared.supervisionRoleHolders
import com.android.settingslib.HelpUtils
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.supervision.SupervisionLog.TAG

/**
 * Setting on PIN Management screen (Settings > Supervision > Manage Pin) that invokes the flow to
 * delete the device PIN.
 */
class SupervisionDeletePinPreference() :
    PreferenceMetadata,
    PreferenceBinding,
    PreferenceLifecycleProvider,
    Preference.OnPreferenceClickListener,
    PreferenceTitleProvider {

    private lateinit var lifeCycleContext: PreferenceLifecycleContext
    private lateinit var confirmPinLauncher: ActivityResultLauncher<Intent>

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.supervision_delete_pin_purpose

    override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)

    override fun getTitle(context: Context): CharSequence {
        if (!Flags.enableSupervisionSettingsUiUpdates()) {
            return context.getString(R.string.supervision_delete_pin_preference_title)
        }

        if (context.supervisionRoleHolders.isEmpty()) {
            return context.getString(R.string.supervision_delete_pin_turn_off_controls_button_label)
        }

        return context.getString(R.string.supervision_delete_pin_preference_title)
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        lifeCycleContext = context

        confirmPinLauncher =
            context.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                result ->
                onPinConfirmed(result.resultCode)
            }
    }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.onPreferenceClickListener = this
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        FeatureFactory.featureFactory.metricsFeatureProvider.clicked(
            SUPERVISION_MANAGE_PIN_SCREEN,
            KEY,
        )
        showDeletionDialog(preference.context)
        return true
    }

    @VisibleForTesting
    fun showDeletionDialog(context: Context) {
        val builder = AlertDialog.Builder(context)

        val supervisionManager = context.getSystemService(SupervisionManager::class.java)
        val userManager = context.getSystemService(UserManager::class.java)

        if (supervisionManager == null || userManager == null) {
            // TODO(b/415995161): Improve error handling
            builder
                .setTitle(R.string.supervision_delete_pin_error_header)
                .setMessage(R.string.supervision_delete_pin_error_message)
                .setPositiveButton(R.string.okay, null)
            showDialog(builder)
            return
        }

        if (!Flags.enableSupervisionSettingsUiUpdates()) {
            if (context.areAnyUsersExceptCurrentSupervised(supervisionManager, userManager)) {
                builder
                    .setTitle(R.string.supervision_delete_pin_supervision_enabled_header)
                    .setMessage(R.string.supervision_delete_pin_supervision_enabled_message)
                    .setPositiveButton(R.string.okay, null)
                    .setNegativeButton(R.string.learn_more, { _, _ -> onLearnMore() })
            } else {
                builder
                    .setTitle(R.string.supervision_delete_pin_confirm_header)
                    .setMessage(R.string.supervision_delete_pin_confirm_message)
                    .setPositiveButton(R.string.supervision_delete_button_label) { _, _ ->
                        onConfirmDeleteClick()
                    }
                    .setNegativeButton(R.string.cancel, null)
            }
            showDialog(builder)
            return
        }

        val roleHolders = context.supervisionRoleHolders
        val internalSupervisionManager =
            ISupervisionManager.Stub.asInterface(
                ServiceManager.getService(Context.SUPERVISION_SERVICE)
            )
        val usersThatRequirePin =
            internalSupervisionManager.getUsersThatRequirePlatformCredential().map { it.id }
        val currentUserRequiresPin = context.user.identifier in usersThatRequirePin
        val otherUsersRequirePin = usersThatRequirePin.any { it != context.user.identifier }

        if (currentUserRequiresPin && !roleHolders.isEmpty()) {
            // Current user has a supervision app that requires the platform credential
            builder
                .setTitle(R.string.supervision_delete_pin_supervision_enabled_header)
                .setMessage(getCantDeletePinMultipleProviderMessage(roleHolders, context))
                .setPositiveButton(R.string.okay, null)
        } else if (otherUsersRequirePin) {
            if (currentUserRequiresPin) {
                // At least one other user requires the platform credential, so we can't delete
                // but we can turn off supervision for the user
                builder
                    .setTitle(R.string.supervision_delete_pin_supervision_enabled_header)
                    .setMessage(R.string.supervision_cant_delete_pin_multi_user_switch_message)
                    .setPositiveButton(
                        R.string.supervision_turn_off_controls_button_label,
                        { _, _ -> onConfirmDeleteClick() },
                    )
                    .setNegativeButton(R.string.cancel, null)
            } else {
                // Current user does not require the platform credential, but at least one other
                // user does. User should not be able to delete the pin.
                builder
                    .setTitle(R.string.supervision_delete_pin_supervision_enabled_header)
                    .setMessage(R.string.supervision_cant_delete_pin_in_use_message)
                    .setPositiveButton(R.string.okay, null)
            }
        } else if (roleHolders.isEmpty()) {
            // Platform supervision only. Can delete the pin and disable supervision for this
            // user
            builder
                .setTitle(R.string.supervision_delete_pin_turn_off_controls_confirm_header)
                .setMessage(R.string.supervision_delete_pin_turn_off_controls_confirm_message)
                .setPositiveButton(R.string.supervision_delete_and_turn_off_button_label) { _, _ ->
                    onConfirmDeleteClick()
                }
                .setNegativeButton(R.string.cancel, null)
        } else {
            // There are supervision apps, but none of them require the platform credential.
            // We can delete the pin while keeping supervision enabled for the current user.
            builder
                .setTitle(R.string.supervision_delete_pin_confirm_header)
                .setMessage(R.string.supervision_delete_pin_confirm_message)
                .setPositiveButton(R.string.supervision_delete_button_label) { _, _ ->
                    onConfirmDeleteClick()
                }
                .setNegativeButton(R.string.cancel, null)
        }

        showDialog(builder)
    }

    fun getCantDeletePinMultipleProviderMessage(
        roleHolders: List<String>,
        context: Context,
    ): Spanned {
        val packageManager =
            context.createContextAsUser(UserHandle.of(context.user.identifier), 0).packageManager
        // TODO(b/458474042): Only list role holders that are actually blocking PIN deletion
        val roleHolderAppNames = roleHolders.map { getApplicationLabel(it, packageManager) }
        val listHtml =
            roleHolderAppNames.joinToString(prefix = "<ul>", postfix = "</ul>", separator = "") {
                "<li>/t$it</li>"
            }
        return Html.fromHtml(
            context.resources.getString(
                R.string.supervision_cant_delete_pin_multiple_role_holders_message,
                listHtml,
            ),
            HtmlCompat.FROM_HTML_SEPARATOR_LINE_BREAK_LIST_ITEM,
        )
    }

    fun getApplicationLabel(packageName: String, packageManager: PackageManager): String {
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            return packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            return ""
        }
    }

    fun showDialog(dialogBuilder: AlertDialog.Builder) {
        val dialog = dialogBuilder.create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }

    @VisibleForTesting
    fun onLearnMore() {
        val intent =
            HelpUtils.getHelpIntent(
                lifeCycleContext,
                lifeCycleContext.getString(R.string.supervision_pin_learn_more_link),
                lifeCycleContext::class.java.name,
            )
        if (intent != null) {
            lifeCycleContext.startActivity(intent)
        } else {
            Log.w(TAG, "HelpIntent is null")
        }
    }

    private fun showErrorDialog(context: Context) {
        // TODO(b/415995161): Improve error handling
        AlertDialog.Builder(context)
            .setTitle(R.string.supervision_delete_pin_error_header)
            .setMessage(R.string.supervision_delete_pin_error_message)
            .setPositiveButton(R.string.okay, null)
            .create()
            .show()
    }

    @VisibleForTesting
    fun onConfirmDeleteClick() {
        val intent =
            Intent(lifeCycleContext, ConfirmSupervisionCredentialsActivity::class.java).apply {
                putExtra(ConfirmSupervisionCredentialsActivity.EXTRA_FORCE_CONFIRMATION, true)
            }
        confirmPinLauncher.launch(intent)
    }

    fun onPinConfirmed(resultCode: Int) {
        if (resultCode == Activity.RESULT_OK) {
            if (!Flags.enableSupervisionSettingsUiUpdates()) {
                deleteSupervisionData(
                    lifeCycleContext,
                    disableSupervision = true,
                    ACTION_SUPERVISION_DELETE_PIN,
                )
                return
            }

            val roleHolders = lifeCycleContext.supervisionRoleHolders
            val supervisionManager =
                lifeCycleContext.getSystemService(SupervisionManager::class.java)
            val internalSupervisionManager =
                ISupervisionManager.Stub.asInterface(
                    ServiceManager.getService(Context.SUPERVISION_SERVICE)
                )
            val usersThatRequirePin =
                internalSupervisionManager
                    .getUsersThatRequirePlatformCredential()
                    .map(UserInfo::id)
                    .toList()
            val otherUsersRequirePin =
                usersThatRequirePin.any { it != lifeCycleContext.user.identifier }

            if (otherUsersRequirePin) {
                // At least one other user requires the platform credential, so we can't delete the
                // pin but we can disable supervision.
                if (supervisionManager == null) {
                    Log.e(TAG, "Can't disable supervision; system services cannot be found.")
                    return
                }

                supervisionManager.setSupervisionEnabled(false)
                logAction(lifeCycleContext, ACTION_SUPERVISION_MULTIPLE_USERS_DISABLE_SUPERVISION)
            } else if (roleHolders.isEmpty()) {
                // Platform supervision only, can delete the pin and disable supervision.
                deleteSupervisionData(
                    lifeCycleContext,
                    disableSupervision = true,
                    ACTION_SUPERVISION_DELETE_PIN,
                )
            } else {
                // There are supervision apps, but none of  them require the platform credential.
                // We can delete the pin while keeping supervision enabled for the current user.
                deleteSupervisionData(
                    lifeCycleContext,
                    disableSupervision = false,
                    ACTION_SUPERVISION_MULTIPLE_PROVIDERS_DELETE_PIN,
                )
            }
        }
    }

    @VisibleForTesting
    fun deleteSupervisionData(
        lifecycleContext: PreferenceLifecycleContext,
        disableSupervision: Boolean,
        logAction: Int,
    ) {
        if (lifeCycleContext.deleteSupervisionData(disableSupervision)) {
            if (Flags.enableSupervisionSettingsUiUpdates()) {
                clearBannerDismissalState(lifecycleContext)
            }

            showDeletePinSuccessToast(lifeCycleContext)
            lifeCycleContext.notifyPreferenceChange(KEY)
            logAction(lifeCycleContext, logAction)
            navigateToDashboard(lifeCycleContext)
        } else {
            Log.e(TAG, "Can't delete supervision data; unable to delete supervising profile.")
            showErrorDialog(lifeCycleContext)
        }
    }

    fun showDeletePinSuccessToast(lifecycleContext: PreferenceLifecycleContext) {
        Toast.makeText(
                lifecycleContext,
                lifecycleContext.getString(R.string.supervision_pin_deleted),
                Toast.LENGTH_SHORT,
            )
            .show()
    }

    /**
     * Programmatically trigger back press to properly return to the supervision dashboard with a
     * correct back stack.
     *
     * <p> Called when delete pin dialog confirmation button is clicked.
     */
    fun navigateToDashboard(lifecycleContext: PreferenceLifecycleContext) {
        val activity = (lifeCycleContext.baseContext.getActivity() as? ComponentActivity)
        activity?.onBackPressedDispatcher?.onBackPressed()
    }

    fun logAction(lifecycleContext: PreferenceLifecycleContext, action: Int) {
        FeatureFactory.featureFactory.metricsFeatureProvider.action(lifeCycleContext, action)
    }

    /**
     * Resets the persistent dismissal state for the Supervision Recovery Banner. This ensures the
     * banner reappears again if the user deletes and sets a PIN again.
     */
    private fun clearBannerDismissalState(context: Context) {
        val prefs: SharedPreferences =
            context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_RECOVERY_BANNER_DISMISSED).commit()
    }

    companion object {
        const val KEY = "supervision_delete_pin"
    }
}
