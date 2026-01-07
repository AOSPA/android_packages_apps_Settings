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

package com.android.settings.backup

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent.EXTRA_USER
import android.os.UserHandle
import android.os.UserHandle.myUserId
import android.os.UserManager
import androidx.preference.Preference
import com.android.settings.CatalystFragment
import com.android.settings.accounts.ManageAccountsScreen
import com.android.settings.dashboard.profileselector.ProfileSelectFragment.EXTRA_PROFILE
import com.android.settings.dashboard.profileselector.ProfileSelectFragment.ProfileType.ALL
import com.android.settings.dashboard.profileselector.ProfileSelectFragment.ProfileType.PERSONAL
import com.android.settings.dashboard.profileselector.ProfileSelectFragment.ProfileType.PRIVATE
import com.android.settings.dashboard.profileselector.ProfileSelectFragment.ProfileType.WORK
import com.android.settingslib.drawer.Tile

class AccountsAndBackupDashboardFragment : CatalystFragment() {

    /* User (personal, work or private space) for which the screen is launched. */
    private val user: UserHandle by lazy {
        val userManager = getSystemService(UserManager::class.java) as UserManager
        (arguments?.getInt(EXTRA_PROFILE) ?: PERSONAL).let { profileType ->
            when (profileType) {
                WORK ->
                    userManager
                        .getProfiles(myUserId())
                        .firstOrNull { it.id != myUserId() && it.isManagedProfile }
                        ?.userHandle ?: UserHandle.of(myUserId())
                PRIVATE ->
                    userManager
                        .getProfiles(myUserId())
                        .firstOrNull { it.id != myUserId() && it.isPrivateProfile }
                        ?.userHandle ?: UserHandle.of(myUserId())
                else -> UserHandle.of(myUserId())
            }
        }
    }

    override fun getMetricsCategory() = SettingsEnums.ACCOUNTS_AND_BACKUP

    override fun getPreferenceScreenBindingKey(context: Context) = AccountsAndBackupScreen.KEY

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference.key == ManageAccountsScreen.KEY) {
            arguments?.getInt(EXTRA_PROFILE, ALL)?.let { profileType ->
                preference.extras.putInt(EXTRA_PROFILE, profileType)
            }
        }
        return super.onPreferenceTreeClick(preference)
    }

    override fun displayTile(tile: Tile?): Boolean {
        // Ensure injected entries launch as the correct user.
        tile?.intent?.putExtra(EXTRA_USER, user)
        return super.displayTile(tile)
    }
}
