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
package com.android.settings.applications.contacts

import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Custom
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability

// LINT.IfChange
@ProvidePreferenceScreen(ContactsStorageApiScreen.KEY)
class ContactsStorageApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.APPS,
        fragment = ContactsStorageSettings::class,
        purpose = R.string.contacts_storage_settings_purpose,
    ) {
    init {        // TODO(b/464954587): Add preference migration for the screen.
        flag { Flags.catalystMigration26q2() }
        preconditions(R.string.contacts_storage_screen_preconditions) {
            if (ContactsStoragePreferenceController.isContactsStorageAvailable(context)) {
                Allowed
            } else if (!ContactsStoragePreferenceController.newDefaultAccountApiEnabled()) {
                Custom("This device does not support this screen.", stability = PreconditionStability.STABLE_UNTIL_APK_UPDATE)
            } else {
                Custom("Error reading the default account.", stability = PreconditionStability.UNSTABLE)
            }
        }
    }

    companion object {
        const val KEY = "contacts_storage_settings"
    }
}
// LINT.ThenChange(ContactsStorageSettings.java, ContactsStoragePreferenceController.java)
