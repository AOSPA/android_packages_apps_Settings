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

import android.annotation.DrawableRes
import android.app.Activity
import android.app.supervision.SupervisionManager
import android.app.supervision.flags.Flags
import android.content.Context
import android.content.Intent
import androidx.preference.Preference
import com.android.settings.R
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.preference.PreferenceBinding

/**
 * Setting on PIN Management screen (Settings > Supervision > Manage Pin) that invokes the flow to
 * add the device PIN recovery method.
 */
class SupervisionAddRecoveryPreference :
    PreferenceMetadata,
    PreferenceAvailabilityProvider,
    PreferenceLifecycleProvider,
    PreferenceBinding,
    Preference.OnPreferenceClickListener {

    private lateinit var lifeCycleContext: PreferenceLifecycleContext
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.supervision_add_pin_recovery_title

    // TODO(b/409837094): get icon with dynamic color.
    override val icon: Int
        @DrawableRes get() = R.drawable.exclamation_icon

    override fun isAvailable(context: Context): Boolean {
        if (!Flags.enableSupervisionPinRecoveryScreen()) {
            return false
        }
        return context
            .getSystemService(SupervisionManager::class.java)
            ?.getSupervisionRecoveryInfo()
            ?.let { it.email.isNullOrEmpty() && it.id.isNullOrEmpty() } ?: true
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        lifeCycleContext = context
    }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.onPreferenceClickListener = this
    }

    override fun onActivityResult(
        context: PreferenceLifecycleContext,
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ): Boolean {
        if (requestCode != ADD_RECOVERY_REQUEST_CODE) {
            return false
        }
        if (resultCode == Activity.RESULT_OK) {
            context.notifyPreferenceChange(KEY)
            context.notifyPreferenceChange(SupervisionUpdateRecoveryEmailPreference.KEY)
            context.notifyPreferenceChange(SupervisionPinManagementScreen.KEY)
        }
        return true
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        val intent =
            Intent(lifeCycleContext, SupervisionPinRecoveryActivity::class.java)
                .setAction(SupervisionPinRecoveryActivity.ACTION_SETUP_VERIFIED)
        lifeCycleContext.startActivityForResult(intent, ADD_RECOVERY_REQUEST_CODE, null)
        return true
    }

    companion object {
        const val KEY = "supervision_add_recovery"
        const val ADD_RECOVERY_REQUEST_CODE = 1
    }
}
