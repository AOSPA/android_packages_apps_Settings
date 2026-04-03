/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings.security.trustagent

import android.os.UserHandle
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.overlay.FeatureFactory
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Custom
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability

// LINT.IfChange
@ProvidePreferenceScreen(TrustAgentApiScreen.KEY)
class TrustAgentApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.SECURITY,
        fragment = TrustAgentSettings::class,
        purpose = R.string.trust_agents_purpose,
    ) {
    init {
        flag { Flags.catalystMigration26q2() }
        preconditions("The device must support managing trust agents from settings, and the user must have a lock screen set.") {
            if (
                context.resources.getBoolean(R.bool.config_show_manage_trust_agents) &&
                    FeatureFactory.featureFactory.securityFeatureProvider
                        .getLockPatternUtils(context)
                        .isSecure(UserHandle.myUserId())
            ) {
                Allowed
            } else {
                if (!context.resources.getBoolean(R.bool.config_show_manage_trust_agents)) {
                    Custom(R.string.trust_agents_not_available, stability = PreconditionStability.STABLE_UNTIL_APK_UPDATE)
                } else {
                    Custom(
                        R.string.trust_agents_unset_lock_screen,
                        stability = PreconditionStability.UNSTABLE,
                    )
                }
            }
        }
    }

    companion object {
        const val KEY = "trust_agents"
    }
}
// LINT.ThenChange(TrustAgentSettings.java,
//                 ManageTrustAgentsPreferenceController.java)
