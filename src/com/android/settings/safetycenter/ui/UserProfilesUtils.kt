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

package com.android.settings.safetycenter.ui

import android.content.Context
import android.os.Flags as OsFlags
import android.os.UserHandle
import android.os.UserManager
import com.android.permission.flags.Flags as PermissionFlags
import com.android.settingslib.safetycenter.SafetySourcePreference

/** Contains helper methods that deal with user IDs, user handles and user profiles. */
object UserProfilesUtils {

    /**
     * Identifies the right [SafetySourcePreference.Profile] of the given user
     *
     * @return returns the matching [SafetySourcePreference.Profile] for the given user, or null if
     *   the user isn't either a private profile, managed profile or personal/primary profile.
     */
    fun getUserProfile(context: Context, user: UserHandle): SafetySourcePreference.Profile? {
        val userManager = context.getSystemService(UserManager::class.java)
        val userInfo = userManager.getUserInfo(user.identifier) ?: return null

        if (PermissionFlags.privateProfileSupported() && userInfo.isPrivateProfile) {
            return SafetySourcePreference.Profile.PRIVATE
        }
        if (userInfo.isManagedProfile) {
            return SafetySourcePreference.Profile.WORK
        }
        if (!userInfo.isProfile) {
            return SafetySourcePreference.Profile.PERSONAL
        }

        // The user is some other more special type of profile that we don't need to handle.
        return null
    }

    /**
     * Finds the [UserHandle] that matches the specified [SafetySourcePreference.Profile] type.
     *
     * @param profileType The type of profile (PERSONAL, WORK, PRIVATE) to search for.
     * @return The [UserHandle] of the matching profile, or null if not found.
     */
    fun getUserHandleForProfile(
        context: Context,
        profileType: SafetySourcePreference.Profile,
    ): UserHandle? {
        val userManager = context.getSystemService(UserManager::class.java)
        return userManager.userProfiles.firstOrNull { user ->
            getUserProfile(context, user) == profileType
        }
    }
}
