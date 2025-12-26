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

package com.android.settings.accounts

import android.app.settings.SettingsEnums
import android.content.Context
import android.os.UserManager
import android.provider.Settings.EXTRA_AUTHORITIES
import com.android.settings.R
import com.android.settings.dashboard.DashboardFragment
import com.android.settings.dashboard.profileselector.ProfileSelectFragment.ProfileType.PERSONAL
import com.android.settings.dashboard.profileselector.ProfileSelectFragment.ProfileType.WORK
import com.android.settingslib.core.AbstractPreferenceController

class ManageAccountsDashboardFragment : DashboardFragment() {
    override fun getPreferenceScreenResId() = R.xml.manage_accounts_dashboard_settings

    override fun getMetricsCategory() = SettingsEnums.ACCOUNTS_AND_BACKUP

    override fun getPreferenceScreenBindingKey(context: Context): String? = ManageAccountsScreen.KEY

    override fun getLogTag(): String = "ManageAccounts"

    // Code is being moved from AccountDashboardFragment.java. Account related functionality will
    // be remove from that fragment once ManageAccountsDasbhoardFragment launches.
    override fun createPreferenceControllers(context: Context): List<AbstractPreferenceController> {
        val controllers: MutableList<AbstractPreferenceController> = ArrayList()
        val authorities = activity?.intent?.getStringArrayExtra(EXTRA_AUTHORITIES)
        val accountPrefController =
            AccountPreferenceController(
                context,
                this, /* parent */
                authorities,
                if (isProfileUser(context)) WORK else PERSONAL,
            )
        settingsLifecycle.addObserver(accountPrefController)
        controllers.add(accountPrefController)
        return controllers
    }

    private fun isProfileUser(context: Context): Boolean {
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        return userManager.isProfile
    }
}
