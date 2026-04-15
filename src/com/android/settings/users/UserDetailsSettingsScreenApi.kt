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
import android.Manifest.permission.INTERACT_ACROSS_USERS
import android.Manifest.permission.INTERACT_ACROSS_USERS_FULL
import android.Manifest.permission.MANAGE_USERS
import android.Manifest.permission.QUERY_USERS
import android.os.UserHandle
import android.os.UserManager
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.flags.Flags
import com.android.settingslib.datastore.Permissions.Companion.allOf
import com.android.settingslib.datastore.Permissions.Companion.anyOf
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Custom
import com.android.settingslib.metadata.preferencesapi.preconditions.HardwareUnsupported
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean
import com.android.settingslib.metadata.preferencesapi.types.GeneratedParameterType
import com.android.settingslib.metadata.preferencesapi.types.GeneratedValue
import java.util.Collections.emptyList
import com.android.settingslib.metadata.preferencesapi.safe
import com.android.settingslib.metadata.preferencesapi.unsafe
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability

// LINT.IfChange
@ProvidePreferenceScreen(UserDetailsSettingsScreenApi.KEY, parameterized = true)
class UserDetailsSettingsScreenApi :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.SYSTEM,
        fragment = UserDetailsSettings::class,
        purpose = R.string.user_details_settings_pref_screen_purpose,
    ) {
    init {
        flag { Flags.catalystMigration26q2() }
        sensitivityLevel(SensitivityLevel.DO_NOT_EXPOSE)
        parameters {
            parameter(
                name = PARAM_TARGET_USER_ID,
                purpose = R.string.user_details_settings_user_name_param_purpose,
                required = true,
                // TODO(b/479151776): Replace to a user id type once it is implemented
                type =
                    GeneratedParameterType(
                        R.string.user_details_settings_user_name_description,
                        key = "UserDetailsTargetUserId",
                    ) {
                        val um = context.getSystemService(UserManager::class.java)
                        um.aliveUsers
                            ?.filter { it.isUiSwitchableHumanUser() }
                            ?.map { userInfo ->
                                GeneratedValue(
                                    value = Integer.toString(userInfo.id).safe(),
                                    description = userInfo.name?.unsafe() ?: "".safe(),
                                )
                            } ?: emptyList() // Added elvis operator to return emptyList() if null
                    },
            )
            prepareScreenExtras { parameters, extras ->
                val inputUserId = Integer.parseInt(parameters[PARAM_TARGET_USER_ID])

                // This is not a newly created user, so do not apply default app restrictions.
                extras.putBoolean(AppRestrictionsFragment.EXTRA_NEW_USER, false)
                extras.putInt(UserDetailsSettings.EXTRA_USER_ID, inputUserId)
            }
        }
        preconditions(R.string.user_details_settings_precondition) {
            if (
                !(UserHandle.MU_ENABLED &&
                    UserManager.supportsMultipleUsers() &&
                    !Utils.isMonkeyRunning())
            ) {
                HardwareUnsupported(R.string.user_details_settings_unsupported)
            } else if (
                !Flags.showUserDetailsSettingsForSelf() &&
                    Integer.parseInt(parameters[PARAM_TARGET_USER_ID]).equals(context.userId)
            ) {
                HardwareUnsupported(R.string.user_details_settings_for_self_unavailable)
            } else {
                Allowed
            }
        }

        preference(
            key = "enable_calling",
            purpose = R.string.user_details_settings_enable_calling_purpose,
            type = AnyBoolean,
        ) {
            sensitivityLevel(SensitivityLevel.DO_NOT_EXPOSE)
            preconditions(R.string.user_details_enable_calling_precondition) {
                val userManager = context.getSystemService(UserManager::class.java)
                val paramUserId = Integer.parseInt(parameters[PARAM_TARGET_USER_ID])
                val paramUserInfo = userManager.getUserInfo(paramUserId)
                if (!Utils.isVoiceCapable(context)) {
                    HardwareUnsupported(R.string.user_details_enable_calling_unsupported)
                } else if (!userManager.isAdminUser()) {
                    Custom(R.string.user_details_setting_unavailable_user_not_admin, stability = PreconditionStability.UNSTABLE)
                } else if (
                    paramUserInfo.isMain() || paramUserInfo.isRestricted || paramUserInfo.isGuest()
                ) {
                    Custom(R.string.user_details_setting_unavailable_for_selected_user, stability = PreconditionStability.UNSTABLE)
                } else {
                    Allowed
                }
            }

            get {
                permissions(anyOf(MANAGE_USERS, INTERACT_ACROSS_USERS))
                execute {
                    val paramUserId = Integer.parseInt(parameters[PARAM_TARGET_USER_ID])
                    val userManager = context.getSystemService(UserManager::class.java)
                    !userManager.hasUserRestriction(
                        UserManager.DISALLOW_OUTGOING_CALLS,
                        UserHandle(paramUserId),
                    )
                }
            }

            set {
                permissions(MANAGE_USERS)
                execute { value ->
                    val paramUserId = Integer.parseInt(parameters[PARAM_TARGET_USER_ID])
                    val userManager = context.getSystemService(UserManager::class.java)
                    val userProfiles: IntArray = userManager.getProfileIdsWithDisabled(paramUserId)
                    for (userId in userProfiles) {
                        val user = UserHandle.of(userId)
                        userManager.setUserRestriction(
                            UserManager.DISALLOW_OUTGOING_CALLS,
                            !value,
                            user,
                        )
                        userManager.setUserRestriction(UserManager.DISALLOW_SMS, !value, user)
                    }
                }
            }
        }

        preference(
            key = "user_grant_admin",
            purpose = R.string.user_details_settings_grant_admin_purpose,
            type = AnyBoolean,
        ) {
            sensitivityLevel(SensitivityLevel.DO_NOT_EXPOSE)
            preconditions(R.string.user_details_grant_admin_precondition) {
                val userManager = context.getSystemService(UserManager::class.java)
                val paramUserId = Integer.parseInt(parameters[PARAM_TARGET_USER_ID])
                val paramUserInfo = userManager.getUserInfo(paramUserId)
                if (!UserManager.isMultipleAdminEnabled()) {
                    HardwareUnsupported(R.string.user_details_grant_admin_unsupported)
                } else if (!userManager.isAdminUser()) {
                    Custom(R.string.user_details_setting_unavailable_user_not_admin, stability = PreconditionStability.UNSTABLE)
                } else if (
                    paramUserInfo.isMain() ||
                        paramUserInfo.isGuest() ||
                        userManager.hasUserRestrictionForUser(
                            UserManager.DISALLOW_GRANT_ADMIN,
                            paramUserInfo.getUserHandle(),
                        )
                ) {
                    Custom(R.string.user_details_setting_unavailable_for_selected_user, stability = PreconditionStability.UNSTABLE)
                } else {
                    Allowed
                }
            }

            get {
                permissions(anyOf(MANAGE_USERS, CREATE_USERS, QUERY_USERS))
                execute {
                    val paramUserId = Integer.parseInt(parameters[PARAM_TARGET_USER_ID])
                    val userManager = context.getSystemService(UserManager::class.java)
                    userManager.isUserAdmin(paramUserId)
                }
            }

            set {
                permissions(allOf(MANAGE_USERS, INTERACT_ACROSS_USERS_FULL))
                execute { value ->
                    val paramUserId = Integer.parseInt(parameters[PARAM_TARGET_USER_ID])
                    val userManager = context.getSystemService(UserManager::class.java)
                    if (value) {
                        userManager.setUserAdmin(paramUserId)
                    } else {
                        userManager.revokeUserAdmin(paramUserId)
                    }
                }
            }
        }
    }

    companion object {
        const val KEY = "user_details_settings_screen"
        const val PARAM_TARGET_USER_ID = "targetUserId" // Consistent parameter key
    }
}
// LINT.ThenChange(UserDetailsSettings.java)
