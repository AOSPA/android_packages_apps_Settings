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

import android.Manifest.permission.CREATE_USERS
import android.Manifest.permission.GET_ACCOUNTS_PRIVILEGED
import android.Manifest.permission.MANAGE_USERS
import android.Manifest.permission.QUERY_USERS
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.flags.Flags
import com.android.settingslib.RestrictedLockUtilsInternal
import com.android.settingslib.datastore.Permissions.Companion.anyOf
import com.android.settingslib.metadata.HERO_SET
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Custom
import com.android.settingslib.metadata.preferencesapi.preconditions.EnterpriseRestriction
import com.android.settingslib.metadata.preferencesapi.preconditions.HardwareUnsupported
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean
import com.android.settingslib.metadata.preferencesapi.types.AnyString
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability

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
            key = "multiple_users_main_switch",
            purpose = R.string.user_settings_main_switch_purpose,
            type = AnyBoolean,
        ) {
            sensitivityLevel(SensitivityLevel.DEEP_LINK_ONLY)

            permissions(anyOf(MANAGE_USERS, CREATE_USERS, QUERY_USERS))
            // TODO(b/476520565): Tidy this section when the improved way of defining user
            //  restrictions is added.
            preconditions(R.string.user_settings_main_switch_precondition) {
                val userManager = context.getSystemService(UserManager::class.java)
                if (
                    !context.resources.getBoolean(
                        com.android.internal.R.bool.config_allowChangeUserSwitcherEnabled
                    )
                ) {
                    HardwareUnsupported(R.string.user_settings_main_switch_config_unsupported)
                } else if (userManager.isGuestUser) {
                    Custom(R.string.user_settings_main_switch_guest_user_unavailable, stability = PreconditionStability.STABLE_UNTIL_APK_UPDATE)
                } else {
                    Allowed
                }
            }

            get { execute { UserCapabilities.create(context).mUserSwitcherEnabled } }

            set {
                preconditions(R.string.user_settings_main_switch_set_precondition) {
                    val userCaps = UserCapabilities.create(context)
                    if (android.app.admin.flags.Flags.policyTransparencyRefactorEnabled()) {
                        if (
                            !userCaps.mDisallowSwitchUserRestrictionEnforcementInfo
                                .getAllAdmins()
                                .isEmpty() &&
                                !userCaps.mDisallowSwitchUserRestrictionEnforcementInfo
                                    .isOnlyEnforcedBySystem()
                        ) {
                            EnterpriseRestriction(R.string.user_settings_restricted_by_work_policy)
                        }
                    } else {
                        val enforcedAdmin =
                            RestrictedLockUtilsInternal.checkIfRestrictionEnforced(
                                context,
                                UserManager.DISALLOW_USER_SWITCH,
                                UserHandle.myUserId(),
                            )
                        if (enforcedAdmin != null) {
                            EnterpriseRestriction(R.string.user_settings_restricted_by_work_policy)
                        }
                    }
                    if (!userCaps.mIsMain) {
                        Custom(R.string.user_settings_main_switch_non_main_user_restricted, stability = PreconditionStability.STABLE_UNTIL_APK_UPDATE)
                    } else if (userCaps.mDisallowSwitchUser) {
                        // This one should not ever happen once the flag is released
                        Custom(R.string.user_settings_main_switch_config_unsupported, stability = PreconditionStability.STABLE_UNTIL_APK_UPDATE)
                    } else {
                        Allowed
                    }
                }
                execute { value ->
                    Settings.Global.putInt(
                        context.contentResolver,
                        Settings.Global.USER_SWITCHER_ENABLED,
                        if (value) ON else OFF,
                    )
                }
            }
        }

        preference(
            key = "edit_user_name",
            purpose = R.string.user_settings_name_edit_purpose,
            type = AnyString,
        ) {
            get {
                permissions(anyOf(MANAGE_USERS, CREATE_USERS, QUERY_USERS, GET_ACCOUNTS_PRIVILEGED))
                execute { context.getSystemService(UserManager::class.java).userName }
            }

            set {
                permissions(MANAGE_USERS)
                preconditions(R.string.user_settings_edit_name_precondition) {
                    val userManager = context.getSystemService(UserManager::class.java)
                    if (userManager.isGuestUser || userManager.isProfile) {
                        Custom(R.string.user_settings_edit_name_restricted, stability = PreconditionStability.UNSTABLE)
                    } else {
                        Allowed
                    }
                }
                execute { value ->
                    context.getSystemService(UserManager::class.java).setUserName(value)
                }
            }
        }

        preference(
            key = "remove_guest_on_exit",
            purpose = R.string.user_settings_remove_guest_on_exit_pref_purpose,
            type = AnyBoolean,
        ) {
            sensitivityLevel(SensitivityLevel.DO_NOT_EXPOSE)

            preconditions(R.string.user_settings_remove_guest_on_exit_precondition) {
                val userManager = context.getSystemService(UserManager::class.java)
                if (
                    UserManager.isGuestUserAlwaysEphemeral() ||
                        !UserManager.isGuestUserAllowEphemeralStateChange()
                ) {
                    HardwareUnsupported(R.string.user_settings_remove_guest_on_exit_unavailable)
                } else if (!userManager.isAdminUser) {
                    Custom(R.string.user_settings_unavailable_user_not_admin, stability = PreconditionStability.UNSTABLE)
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
            sensitivityLevel(SensitivityLevel.DEEP_LINK_ONLY)

            permissions(MANAGE_USERS)
            preconditions(R.string.user_settings_enable_guest_calling_precondition) {
                val userManager = context.getSystemService(UserManager::class.java)
                if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
                    HardwareUnsupported(R.string.user_settings_guest_telephony_config_unsupported)
                } else if (!userManager.isAdminUser) {
                    Custom(R.string.user_settings_unavailable_user_not_admin, stability = PreconditionStability.UNSTABLE)
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
            sensitivityLevel(SensitivityLevel.REQUIRES_CONFIRMATION)

            preconditions(R.string.user_settings_add_users_from_lockscreen_precondition) {
                val userManager = context.getSystemService(UserManager::class.java)
                val userCaps = UserCapabilities.create(context)
                if (userCaps.mDisallowAddUser) {
                    EnterpriseRestriction(R.string.user_settings_restricted_by_work_policy)
                } else if (
                    context.resources.getBoolean(
                        com.android.internal.R.bool.config_userSwitchingMustGoThroughLoginScreen
                    )
                ) {
                    HardwareUnsupported(
                        R.string.user_settings_add_users_from_lockscreen_config_unsupported
                    )
                } else if (!userManager.isAdminUser) {
                    Custom(R.string.user_settings_unavailable_user_not_admin, stability = PreconditionStability.UNSTABLE)
                } else {
                    Allowed
                }
            }

            tags(HERO_SET)

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
