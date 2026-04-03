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

import android.accounts.AccountManager
import android.app.settings.SettingsEnums
import android.content.Context
import android.os.UserHandle
import android.os.UserManager
import android.provider.SearchIndexableResource
import android.provider.Settings.EXTRA_AUTHORITIES
import com.android.settings.R
import com.android.settings.dashboard.DashboardFragment
import com.android.settings.dashboard.profileselector.ProfileSelectFragment.EXTRA_PROFILE
import com.android.settings.dashboard.profileselector.ProfileSelectFragment.ProfileType.ALL
import com.android.settings.dashboard.profileselector.ProfileSelectFragment.ProfileType.PERSONAL
import com.android.settings.dashboard.profileselector.ProfileSelectFragment.ProfileType.PRIVATE
import com.android.settings.dashboard.profileselector.ProfileSelectFragment.ProfileType.WORK
import com.android.settings.flags.Flags
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settings.users.AutoSyncDataPreferenceController
import com.android.settings.users.AutoSyncPersonalDataPreferenceController
import com.android.settings.users.AutoSyncPrivateDataPreferenceController
import com.android.settings.users.AutoSyncWorkDataPreferenceController
import com.android.settingslib.core.AbstractPreferenceController
import com.android.settingslib.search.SearchIndexable
import com.android.settingslib.search.SearchIndexableRaw

@SearchIndexable
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
        val profileType = getProfileType()
        val accountPrefController =
            AccountPreferenceController(
                context,
                this, /* parent */
                authorities,
                profileType,
            )
        settingsLifecycle.addObserver(accountPrefController)
        controllers.add(accountPrefController)

        controllers.add(
            AutoSyncDataPreferenceController(
                context,
                /* parent */ this)
        )
        controllers.add(
            AutoSyncPersonalDataPreferenceController(
                context,
                /* parent */ this,
                profileType == WORK || profileType == PRIVATE)
        )
        controllers.add(
            AutoSyncWorkDataPreferenceController(
                context,
                /* parent */ this,
                /* forceDisable */ profileType != WORK,
            )
        )
        controllers.add(
            AutoSyncPrivateDataPreferenceController(
                context,
                /* parent */ this,
                /* forceDisable */ profileType != PRIVATE,
            )
        )

        return controllers
    }

    private fun getProfileType(): Int {
        // If we're running as a profile user, return profile type based on the current user.
        (context?.getSystemService(Context.USER_SERVICE) as UserManager)?.let { userManager ->
            if (userManager.isManagedProfile()) {
                return WORK
            }
            if (userManager.isPrivateProfile) {
                return PRIVATE
            }
        }

        // Otherise, infer from the fragment args.
        return arguments?.getString(FRAGMENT_ARGS_KEY)?.let {
            // If FRAGMENT_ARGS_KEY is set, we're launched from search results.
            when (it) {
                AUTO_SYNC_WORK_ACCOUNT -> WORK
                AUTO_SYNC_PRIVATE_ACCOUNT -> PRIVATE
                else -> PERSONAL
            }
        } ?: (arguments?.getInt(EXTRA_PROFILE, PERSONAL) ?: PERSONAL)
    }

    companion object {
        private const val FRAGMENT_ARGS_KEY = ":settings:fragment_args_key"
        private const val AUTO_SYNC_WORK_ACCOUNT = "auto_sync_work_account_data"
        private const val AUTO_SYNC_PRIVATE_ACCOUNT = "auto_sync_private_account_data"

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER: BaseSearchIndexProvider =
            object : BaseSearchIndexProvider() {
                override fun getXmlResourcesToIndex(
                    context: Context,
                    enabled: Boolean,
                ): List<SearchIndexableResource> {
                    if (!Flags.enableAccountsAndBackupScreen()) {
                        return emptyList()
                    }
                    val sir = SearchIndexableResource(context)
                    sir.xmlResId = R.xml.manage_accounts_dashboard_settings
                    return listOf(sir)
                }

                override fun createPreferenceControllers(
                    context: Context
                ): List<AbstractPreferenceController> {
                    if (!Flags.enableAccountsAndBackupScreen()) {
                        return emptyList()
                    }
                    val controllers: MutableList<AbstractPreferenceController> = ArrayList()
                    // For indexing, don't disable any controllers as all profiles should be
                    // indexed.
                    controllers.add(
                        AccountPreferenceController(
                            context,
                            /* parent= */ null,
                            /* authorities= */ null,
                            /* profileType= */ ALL,
                        )
                    )
                    controllers.add(AutoSyncDataPreferenceController(context, /* parent= */ null))
                    controllers.add(
                        AutoSyncPersonalDataPreferenceController(context, /* parent= */ null)
                    )
                    controllers.add(
                        AutoSyncWorkDataPreferenceController(context, /* parent= */ null)
                    )
                    controllers.add(
                        AutoSyncPrivateDataPreferenceController(
                            context,
                            /* parent= */ null,
                            // Private space needs to be excluded from indexing if it's locked.
                            /* forceDisable= */ isPrivateSpaceLocked(context),
                        )
                    )

                    return controllers
                }

                override fun getDynamicRawDataToIndex(
                    context: Context,
                    enabled: Boolean,
                ): List<SearchIndexableRaw> {
                    val indexRaws = ArrayList<SearchIndexableRaw>()
                    if (!Flags.enableAccountsAndBackupScreen()) {
                        return indexRaws
                    }

                    val userManager = getUserManager(context)
                    if (
                        userManager.getProfiles(UserHandle.myUserId()).any { it.isManagedProfile() }
                    ) {
                        return indexRaws
                    }

                    val accounts = AccountManager.get(context).getAccounts()
                    for (account in accounts) {
                        indexRaws.add(
                            SearchIndexableRaw(context).apply {
                                key = AccountTypePreference.buildKey(account)
                                title = account.name
                            }
                        )
                    }

                    return indexRaws
                }
            }

        private fun getUserManager(context: Context): UserManager =
            context.getSystemService(Context.USER_SERVICE) as UserManager

        private fun isPrivateSpaceLocked(context: Context): Boolean =
            getUserManager(context)
                .getProfiles(UserHandle.myUserId())
                .firstOrNull { it.isPrivateProfile }
                ?.let { getUserManager(context).isQuietModeEnabled(it.userHandle) } ?: false
    }
}
