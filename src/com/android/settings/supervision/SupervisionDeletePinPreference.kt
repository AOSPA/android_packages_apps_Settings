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

import android.app.Activity
import android.app.settings.SettingsEnums
import android.app.supervision.SupervisionManager
import android.content.Context
import android.content.Intent
import android.os.UserManager
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.core.SubSettingLauncher
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
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
    Preference.OnPreferenceClickListener {

    private lateinit var lifeCycleContext: PreferenceLifecycleContext
    private lateinit var confirmPinLauncher: ActivityResultLauncher<Intent>

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.supervision_delete_pin_preference_title

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
        } else if (areAnyUsersExceptCurrentSupervised(supervisionManager, userManager)) {
            builder
                .setTitle(R.string.supervision_delete_pin_supervision_enabled_header)
                .setMessage(R.string.supervision_delete_pin_supervision_enabled_message)
                .setPositiveButton(R.string.okay, null)
        } else {
            builder
                .setTitle(R.string.supervision_delete_pin_confirm_header)
                .setMessage(R.string.supervision_delete_pin_confirm_message)
                .setPositiveButton(R.string.delete) { _, _ -> onConfirmDeleteClick() }
                .setNegativeButton(R.string.cancel, null)
        }
        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
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

    /** Returns whether any users except the current user are supervised on this device. */
    @VisibleForTesting
    fun areAnyUsersExceptCurrentSupervised(
        supervisionManager: SupervisionManager,
        userManager: UserManager,
    ): Boolean {
        return userManager.users.any {
            lifeCycleContext.userId != it.id &&
                supervisionManager.isSupervisionEnabledForUser(it.id)
        }
    }

    @VisibleForTesting
    fun onConfirmDeleteClick() {
        val intent =
            Intent(lifeCycleContext, ConfirmSupervisionCredentialsActivity::class.java).apply {
                putExtra(ConfirmSupervisionCredentialsActivity.EXTRA_FORCE_CONFIRMATION, true)
            }
        confirmPinLauncher.launch(intent)
    }

    private fun onPinConfirmed(resultCode: Int) {
        if (resultCode == Activity.RESULT_OK) {
            val userManager = lifeCycleContext.getSystemService(UserManager::class.java)
            val supervisionManager =
                lifeCycleContext.getSystemService(SupervisionManager::class.java)
            if (userManager == null || supervisionManager == null) {
                Log.e(TAG, "Can't delete supervision data; system services cannot be found.")
                return
            }
            val supervisingUser = lifeCycleContext.supervisingUserHandle
            if (supervisingUser == null) {
                Log.e(TAG, "Can't delete supervision data; supervising user does not exist.")
                return
            }

            // Supervision must be disabled before the supervising profile can be removed
            supervisionManager.setSupervisionEnabled(false)
            lifeCycleContext.notifyPreferenceChange(KEY)
            if (userManager.removeUser(supervisingUser)) {
                supervisionManager.setSupervisionRecoveryInfo(null)
                SubSettingLauncher(lifeCycleContext)
                    .setDestination(SupervisionDashboardFragment::class.java.name)
                    .setSourceMetricsCategory(SettingsEnums.SUPERVISION_DASHBOARD)
                    .launch()
            } else {
                Log.e(TAG, "Can't delete supervision data; unable to delete supervising profile.")
                showErrorDialog(lifeCycleContext)
            }
        }
    }

    companion object {
        const val KEY = "supervision_delete_pin"
    }
}
