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

import android.app.supervision.SupervisionManager
import android.app.supervision.SupervisionRecoveryInfo.STATE_PENDING
import android.app.supervision.flags.Flags
import android.content.Context
import com.android.settings.R
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceIconProvider
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceScreenCreator
import com.android.settingslib.widget.UntitledPreferenceCategoryMetadata

/** Pin Management landing page (Settings > Supervision > Manage Pin). */
@ProvidePreferenceScreen(SupervisionPinManagementScreen.KEY)
class SupervisionPinManagementScreen :
    PreferenceScreenCreator,
    PreferenceAvailabilityProvider,
    PreferenceSummaryProvider,
    PreferenceIconProvider {
    override val key: String
        get() = KEY

    override fun isAvailable(context: Context) = context.isSupervisingCredentialSet

    override val title: Int
        get() = R.string.supervision_pin_management_preference_title

    override fun getSummary(context: Context): CharSequence? {
        if (!Flags.enableSupervisionPinRecoveryScreen()) {
            return null
        }
        val recoveryInfo =
            context.getSystemService(SupervisionManager::class.java)?.supervisionRecoveryInfo
        return when {
            recoveryInfo == null -> {
                context.getString(R.string.supervision_pin_management_preference_summary_add)
            }
            recoveryInfo.state == STATE_PENDING -> {
                context.getString(
                    R.string.supervision_pin_management_preference_summary_verify_recovery
                )
            }
            else -> null
        }
    }

    // TODO(b/409837094): get icon with dynamic color.
    override fun getIcon(context: Context): Int {
        if (Flags.enableSupervisionPinRecoveryScreen()) {
            val recoveryInfo =
                context.getSystemService(SupervisionManager::class.java)?.supervisionRecoveryInfo
            if (recoveryInfo == null || recoveryInfo.state == STATE_PENDING) {
                // if recovery is not fully setup.
                return R.drawable.exclamation_icon
            }
        }
        return R.drawable.ic_pin_outline
    }

    override fun fragmentClass() = SupervisionPinManagementFragment::class.java

    override fun getPreferenceHierarchy(context: Context) =
        preferenceHierarchy(context, this) {
            +SupervisionSetupRecoveryPreference()
            +UntitledPreferenceCategoryMetadata(GROUP_KEY) += {
                +SupervisionPinRecoveryPreference()
                // TODO(b/391992481) implement the screen.
                +SupervisionChangePinPreference()
                +SupervisionUpdateRecoveryEmailPreference()
            }
            +SupervisionDeletePinPreference()
        }

    companion object {
        const val KEY = "supervision_pin_management"
        internal const val GROUP_KEY = "pin_management_group"
    }
}
