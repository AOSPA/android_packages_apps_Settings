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

package com.android.settings.inputmethod

import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Custom
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import java.util.TreeSet

// LINT.IfChange
@ProvidePreferenceScreen(UserDictionaryListApiScreen.KEY)
class UserDictionaryListApiScreen :
    PreferencesApiScreen(
        key = UserDictionaryListApiScreen.KEY,
        topLevelSettingsCategory = Category.SYSTEM,
        fragment = UserDictionaryList::class,
        purpose = R.string.user_dict_list_purpose,
    ) {
    init {
        flag { Flags.catalystMigration26q2() }

        preconditions(R.string.user_dict_list_preconditions) {
            val localeSet: TreeSet<String> =
                UserDictionaryListPreferenceController.getUserDictionaryLocalesSet(context)
            if (localeSet.size > 1) {
                Allowed
            } else {
                Custom(
                    R.string.user_dict_list_not_supported,
                    stability = PreconditionStability.UNSTABLE,
                )
            }
        }
    }

    companion object {
        const val KEY = "user_dict_list"
    }
}
// LINT.ThenChange(UserDictionaryListApiScreen.java,
//                 ../language/UserDictionaryPreferenceController.java)
