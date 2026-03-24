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
import android.app.admin.DevicePolicyManager
import android.app.settings.SettingsEnums.ACTION_SUPERVISION_CHANGE_PIN
import android.app.supervision.flags.Flags
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.preference.Preference
import com.android.internal.widget.LockPatternUtils
import com.android.settings.R
import com.android.settings.metrics.PreferenceActionMetricsProvider
import com.android.settings.password.ChooseLockGeneric
import com.android.settings.password.ChooseLockPassword
import com.android.settings.supervision.shared.isSupervisingCredentialSet
import com.android.settings.supervision.shared.supervisingUserHandle
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.supervision.SupervisionLog.TAG

/**
 * Setting on PIN Management screen (Settings > Supervision > Manage Pin) that invokes the flow to
 * update the existing device supervision PIN.
 */
class SupervisionChangePinPreference :
    PreferenceMetadata,
    PreferenceActionMetricsProvider,
    PreferenceLifecycleProvider,
    PreferenceBinding,
    Preference.OnPreferenceClickListener {

    private lateinit var lifeCycleContext: PreferenceLifecycleContext
    private lateinit var changePinLauncher: ActivityResultLauncher<Intent>

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.supervision_change_pin_purpose

    override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)

    override val title: Int
        get() = R.string.supervision_change_pin_preference_title

    override val preferenceActionMetrics: Int
        get() = ACTION_SUPERVISION_CHANGE_PIN

    override fun onCreate(context: PreferenceLifecycleContext) {
        lifeCycleContext = context
        changePinLauncher =
            context.registerForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
                ::onChangePinComplete,
            )
    }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.onPreferenceClickListener = this
    }

    @VisibleForTesting
    fun onChangePinComplete(result: ActivityResult) {
        // Assume success if the flow was not canceled.
        if (result.resultCode != Activity.RESULT_CANCELED) {
            Toast.makeText(
                    lifeCycleContext,
                    lifeCycleContext.getString(R.string.supervision_pin_changed),
                    Toast.LENGTH_SHORT,
                )
                .show()
        } else {
            // The user cancelled the flow. No action needed.
            Log.d(TAG, "PIN change was likely canceled by the user.")
        }
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        val supervisingUserHandle = preference.context.supervisingUserHandle()
        if (!preference.context.isSupervisingCredentialSet(supervisingUserHandle)) {
            Log.w(TAG, "Supervising credential not set")
            return false
        }
        val intent =
            Intent(preference.context, ChooseLockGeneric::class.java).apply {
                putExtra(
                    LockPatternUtils.PASSWORD_TYPE_KEY,
                    DevicePolicyManager.PASSWORD_QUALITY_NUMERIC,
                )
                putExtra(Intent.EXTRA_USER_ID, supervisingUserHandle?.identifier)
                if (Flags.enableSupervisionSettingsUiUpdates()) {
                    putExtra(ChooseLockPassword.EXTRA_KEY_FOR_SUPERVISION_RESET, true)
                }
            }
        changePinLauncher.launch(intent)
        return true
    }

    companion object {
        const val KEY = "supervision_change_pin"
    }
}
