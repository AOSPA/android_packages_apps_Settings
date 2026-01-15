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

package com.android.settings.users

import android.Manifest.permission.MANAGE_USERS
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Custom
import com.android.settingslib.metadata.preferencesapi.preconditions.HardwareUnsupported
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean

// LINT.IfChange
@ProvidePreferenceScreen(UserSettingsScreenApi.KEY)
class UserSettingsScreenApi :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.SYSTEM,
        fragment = UserSettings::class,
        purpose = R.string.user_settings_pref_screen_purpose,
    ) {

    init {
        flag { Flags.catalystMigration26q2() }

        preconditions(R.string.user_settings_precondition) {
            if (
                UserHandle.MU_ENABLED &&
                    UserManager.supportsMultipleUsers() &&
                    !Utils.isMonkeyRunning()
            ) {
                Allowed
            } else {
                HardwareUnsupported(R.string.user_settings_unsupported)
            }
        }

        preference(
            key = "remove_guest_on_exit",
            purpose = R.string.user_settings_remove_guest_on_exit_pref_purpose,
            type = AnyBoolean,
        ) {
            preconditions(R.string.user_settings_remove_guest_on_exit_precondition) {
                val userManager = context.getSystemService(UserManager::class.java)
                if (
                    UserManager.isGuestUserAlwaysEphemeral() ||
                        !UserManager.isGuestUserAllowEphemeralStateChange()
                ) {
                    HardwareUnsupported(R.string.user_settings_remove_guest_on_exit_unavailable)
                } else if (!userManager.isAdminUser) {
                    Custom(R.string.user_settings_unavailable_user_not_admin)
                } else {
                    Allowed
                }
            }

            get {
                execute {
                    Settings.Global.getInt(
                        context.getContentResolver(),
                        Settings.Global.REMOVE_GUEST_ON_EXIT,
                        ON,
                    ) == ON
                }
            }

            set {
                execute { value ->
                    Settings.Global.putInt(
                        context.contentResolver,
                        Settings.Global.REMOVE_GUEST_ON_EXIT,
                        if (value) ON else OFF,
                    )
                }
            }
        }

        preference(
            key = "enable_guest_calling",
            purpose = R.string.user_settings_enable_guest_calling_pref_purpose,
            type = AnyBoolean,
        ) {
            permissions(MANAGE_USERS)
            preconditions(R.string.user_settings_enable_guest_calling_precondition) {
                val userManager = context.getSystemService(UserManager::class.java)
                // TODO(b/474008291) : Remove HSUM since it is no longer a blocker for telephony
                if (
                    !context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) ||
                        UserManager.isHeadlessSystemUserMode()
                ) {
                    HardwareUnsupported(R.string.user_settings_guest_telephony_config_unsupported)
                } else if (!userManager.isAdminUser) {
                    Custom(R.string.user_settings_unavailable_user_not_admin)
                } else {
                    Allowed
                }
            }

            get {
                execute {
                    // True if restrictions are NOT applied
                    !context
                        .getSystemService(UserManager::class.java)
                        .getDefaultGuestRestrictions()
                        .getBoolean(UserManager.DISALLOW_OUTGOING_CALLS, false)
                }
            }

            set {
                execute { value ->
                    val userManager = context.getSystemService(UserManager::class.java)
                    val guestRestrictions: Bundle = userManager.getDefaultGuestRestrictions()
                    // TODO(b/474010197) : Investigate if there is a better way to handle it
                    guestRestrictions.putBoolean(UserManager.DISALLOW_SMS, true)
                    guestRestrictions.putBoolean(UserManager.DISALLOW_OUTGOING_CALLS, !value)
                    userManager.setDefaultGuestRestrictions(guestRestrictions)
                }
            }
        }

        preference(
            key = "user_settings_add_users_when_locked",
            purpose = R.string.user_settings_add_users_when_locked_pref_purpose,
            type = AnyBoolean,
        ) {
            preconditions(R.string.user_settings_add_users_from_lockscreen_precondition) {
                val userManager = context.getSystemService(UserManager::class.java)
                val userCaps = UserCapabilities.create(context)
                if (userCaps.mDisallowAddUser || userCaps.mDisallowAddUserSetByAdmin) {
                    Custom(R.string.user_settings_restricted_by_work_policy)
                } else if (
                    context.resources.getBoolean(
                        com.android.internal.R.bool.config_userSwitchingMustGoThroughLoginScreen
                    )
                ) {
                    HardwareUnsupported(
                        R.string.user_settings_add_users_from_lockscreen_config_unsupported
                    )
                } else if (!userManager.isAdminUser) {
                    Custom(R.string.user_settings_unavailable_user_not_admin)
                } else {
                    Allowed
                }
            }

            get {
                execute {
                    Settings.Global.getInt(
                        context.contentResolver,
                        Settings.Global.ADD_USERS_WHEN_LOCKED,
                        OFF,
                    ) == ON
                }
            }

            set {
                execute { value ->
                    Settings.Global.putInt(
                        context.contentResolver,
                        Settings.Global.ADD_USERS_WHEN_LOCKED,
                        if (value) ON else OFF,
                    )
                }
            }
        }
    }

    companion object {
        const val KEY = "user_settings_screen"
        const val ON = 1
        const val OFF = 0
    }
}
// LINT.ThenChange(UserSettings.java, MultiUserPreferenceController.java)
