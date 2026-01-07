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
import com.android.settings.dashboard.profileselector.ProfileSelectFragment.ProfileType.PERSONAL
import com.android.settingslib.drawer.Tile

class AccountsAndBackupDashboardFragment : CatalystFragment() {

    /* User (personal of work) for which the screen is launched. */
    private val user: UserHandle by lazy {
        if (arguments?.getInt(EXTRA_PROFILE, PERSONAL) == PERSONAL) {
            UserHandle.of(myUserId())
        } else {
            val userManager = getSystemService(UserManager::class.java) as UserManager
            userManager
                .getProfiles(myUserId())
                .first { it.isManagedProfile && it.id != myUserId() }
                ?.userHandle ?: UserHandle.of(myUserId())
        }
    }

    override fun getMetricsCategory() = SettingsEnums.ACCOUNTS_AND_BACKUP

    override fun getPreferenceScreenBindingKey(context: Context) = AccountsAndBackupScreen.KEY

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference.key == ManageAccountsScreen.KEY) {
            arguments?.getInt(EXTRA_PROFILE)?.let { profileType ->
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
