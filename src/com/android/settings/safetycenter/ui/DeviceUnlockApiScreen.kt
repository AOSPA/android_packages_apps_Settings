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

package com.android.settings.safetycenter.ui

import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Custom
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability

// LINT.IfChange
@ProvidePreferenceScreen(DeviceUnlockApiScreen.KEY)
class DeviceUnlockApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.SECURITY,
        fragment = DeviceUnlockSubpageFragment::class,
        purpose = R.string.device_unlock_pref_screen_purpose,
    ) {
    init {
        flag { Flags.catalystMigration26q2() && Flags.enableSafetyCenterNewUi() }

        preconditions(R.string.device_unlock_subpage_screen_preconditions) {
            if (
                SafetyCenterSubpageRegistry.isSubpageAvailable(
                    context,
                    SafetyCenterSubpageRegistry.DEVICE_UNLOCK_SUBPAGE_KEY,
                )
            ) {
                Allowed
            } else {
                Custom(
                    R.string.device_unlock_subpage_screen_unavailable,
                    stability = PreconditionStability.UNSTABLE,
                )
            }
        }
    }

    companion object {
        const val KEY = "device_unlock_pref_screen"
    }
}
// LINT.ThenChange(DeviceUnlockSubpageFragment.kt)
