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

package com.android.settings.security

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.annotation.SuppressLint
import android.annotation.UserIdInt
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import androidx.core.content.getSystemService
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.security.ContentProtectionPreferenceUtils.getContentProtectionPolicy
import com.android.settings.security.ContentProtectionPreferenceUtils.getManagedProfile
import com.android.settingslib.RestrictedLockUtilsInternal
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.multiusers.PreferenceTarget
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Custom
import com.android.settingslib.metadata.preferencesapi.preconditions.EnterpriseRestriction
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability

// LINT.IfChange
@ProvidePreferenceScreen(ContentProtectionScreenApi.KEY)
class ContentProtectionScreenApi :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.SECURITY,
        fragment = ContentProtectionPreferenceFragment::class,
        purpose = R.string.content_protection_purpose,
    ) {
    init {
        flag { Flags.catalystMigration26q2() }
        preconditions(R.string.content_protection_preconditions) {
            if (ContentProtectionPreferenceUtils.isAvailable(context)) {
                Allowed
            } else {
                Custom(R.string.content_protection_unsupported, stability = PreconditionStability.STABLE_UNTIL_APK_UPDATE)
            }
        }

        // Toggle to turn Scanning for Deceptive Apps on or off.
        preference(
            key = "content_protection_preference_user_consent_switch",
            purpose = R.string.content_protection_switch_purpose,
            type = AnyBoolean,
            appliesTo = PreferenceTarget.DEVICE,
        ) {
            preconditions(R.string.content_protection_switch_preconditions) {
                if (preferenceAvailableToUser(context, userId)) {
                    Allowed
                } else {
                    // For non-primary user, it is impossible to identify the Preference state,
                    // as it may depend on the work profile policy of the primary user.
                    Custom(R.string.content_protection_switch_unsupported, stability = PreconditionStability.STABLE_UNTIL_APK_UPDATE)
                }
            }
            get {
                execute {
                    if (hasAdmin(context)) {
                        val contentProtectionPolicy =
                            getContentProtectionPolicy(context, getManagedProfile(context))

                        @SuppressLint("SwitchIntDef")
                        when (contentProtectionPolicy) {
                            DevicePolicyManager.CONTENT_PROTECTION_DISABLED -> return@execute false
                            DevicePolicyManager.CONTENT_PROTECTION_ENABLED -> return@execute true
                        }
                    }
                    Settings.Global.getInt(
                        context.getContentResolver(),
                        KEY_CONTENT_PROTECTION_PREFERENCE,
                        0,
                    ) >= 0
                }
            }
            set {
                permissions(WRITE_SECURE_SETTINGS)
                preconditions(R.string.content_protection_switch_set_preconditions) {
                    if (
                        hasAdmin(context) &&
                            contentProtectionControlledByPolicy(
                                context = context,
                                managedProfile = getManagedProfile(context),
                            )
                    ) {
                        EnterpriseRestriction(R.string.content_protection_switch_set_controlled_by_device_admin)
                    } else {
                        Allowed
                    }
                }
                execute { isChecked ->
                    Settings.Global.putInt(
                        context.getContentResolver(),
                        KEY_CONTENT_PROTECTION_PREFERENCE,
                        if (isChecked) 1 else -1,
                    )
                }
            }
        }
    }

    private fun hasAdmin(context: Context) =
        if (android.app.admin.flags.Flags.policyTransparencyRefactorV2()) {
            ContentProtectionPreferenceUtils.getContentProtectionEnforcingAdmin(
                context,
                getManagedProfile(context),
            )
        } else {
            RestrictedLockUtilsInternal.getDeviceOwner(context)
        } != null

    private fun preferenceAvailableToUser(context: Context, @UserIdInt userId: Int): Boolean {
        val userManager = context.getSystemService<UserManager>() ?: return false
        val userInfo = userManager.getUserInfo(userId) ?: return false
        return userInfo.isAdmin || userInfo.isManagedProfile
    }

    private fun contentProtectionControlledByPolicy(
        context: Context,
        managedProfile: UserHandle?,
    ): Boolean =
        getContentProtectionPolicy(context, managedProfile) !=
            DevicePolicyManager.CONTENT_PROTECTION_NOT_CONTROLLED_BY_POLICY

    companion object {
        const val KEY = "content_protection_preference_subpage"
        const val KEY_CONTENT_PROTECTION_PREFERENCE = "content_protection_user_consent"
    }
}
// LINT.ThenChange(ContentProtectionPreferenceFragment.java,
//                 ContentProtectionPreferenceUtils.java
//                 ContentProtectionTogglePreferenceController.java)
