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

package com.android.settings.notification

import android.Manifest.permission.WRITE_SETTINGS
import android.provider.Settings
import com.android.server.notification.Flags as NotificationFlags
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean

// LINT.IfChange
@ProvidePreferenceScreen(PoliteNotificationsApiScreen.KEY)
class PoliteNotificationsApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.NOTIFICATIONS,
        fragment = PoliteNotificationsPreferenceFragment::class,
        purpose = R.string.notification_cooldown_screen_purpose,
    ) {
    init {
        flag { Flags.catalystMigration26q2() && NotificationFlags.politeNotifications() }
        tags(APP_FUNCTION_NOTIFICATIONS)
        preference(
            key = MAIN_SWITCH_KEY,
            purpose = R.string.polite_notification_global_pref_purpose,
            type = AnyBoolean,
        ) {
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)
            get {
                execute {
                    Settings.System.getInt(
                        context.contentResolver,
                        Settings.System.NOTIFICATION_COOLDOWN_ENABLED,
                        ON,
                    ) == ON
                }
            }
            set {
                permissions(WRITE_SETTINGS)
                execute { value ->
                    Settings.System.putInt(
                        context.contentResolver,
                        Settings.System.NOTIFICATION_COOLDOWN_ENABLED,
                        if (value) ON else OFF,
                    )
                }
            }
        }
    }

    companion object {
        const val KEY = "notification_cooldown_screen"
        internal const val MAIN_SWITCH_KEY = "polite_notification_global_pref"
        internal const val ON = 1
        internal const val OFF = 0
    }
}
// LINT.ThenChange(PoliteNotificationsPreferenceFragment.java,
//                 PoliteNotificationsPreferenceController.java,
//                 PoliteNotificationGlobalPreferenceController.java)
