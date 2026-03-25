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

package com.android.settings.accounts

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Intent.EXTRA_USER
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import com.android.settings.R
import com.android.settings.accounts.AccountDetailDashboardFragment.KEY_ACCOUNT
import com.android.settings.accounts.AccountDetailDashboardFragment.KEY_ACCOUNT_TYPE
import com.android.settings.accounts.AccountDetailDashboardFragment.KEY_USER_HANDLE
import com.android.settings.flags.Flags
import com.android.settings.overlay.FeatureFactory
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Custom
import com.android.settingslib.metadata.preferencesapi.preconditions.HardwareUnsupported
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import kotlin.time.measureTimedValue

// LINT.IfChange
@ProvidePreferenceScreen(AccountDetailApiScreen.KEY, parameterized = true)
class AccountDetailApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.ACCOUNTS,
        fragment = AccountDetailDashboardFragment::class,
        purpose = R.string.account_detail_screen_purpose,
    ) {

    init {
        flag { Flags.catalystMigration26q2() }

        parameters {
            parameter(
                name = ACCOUNT_NAME,
                purpose = R.string.account_detail_screen_parameter_purpose,
                required = true,
                type = Accounts,
            )

            prepareScreenExtras { keyParameters, extras ->
                val accountName = keyParameters[ACCOUNT_NAME]
                val accounts = getAccountsAsUser(Process.myUserHandle())
                accounts
                    .find { it.name == accountName }
                    ?.let {
                        extras.putParcelable(KEY_ACCOUNT, it)
                        extras.putString(KEY_ACCOUNT_TYPE, it.type)
                        extras.putParcelable(KEY_USER_HANDLE, Process.myUserHandle())
                        extras.putParcelable(EXTRA_USER, Process.myUserHandle())
                    } ?: Custom("Can't find account with name $accountName", stability = PreconditionStability.UNSTABLE)
            }
        }

        preconditions(R.string.account_detail_screen_preconditions) {
            val userManager: UserManager =
                context.getSystemService(UserManager::class.java)
                    ?: error("UserManager is not available.")
            // TODO(b/479499461): Add support for multiple users.
            val userInfo = userManager.getUserInfo(Process.myUserHandle().identifier)
            if (!userInfo.isEnabled) {
                Custom(PRECONDITIONS_USER_DISABLED, stability = PreconditionStability.UNSTABLE)
            } else {
                if (getAccountsAsUser(Process.myUserHandle()).isNotEmpty()) {
                    Allowed
                } else {
                    Custom(PRECONDITIONS_NO_ACCOUNT, stability = PreconditionStability.UNSTABLE)
                }
            }
        }
    }

    private fun getAccountsAsUser(userHandle: UserHandle): Array<Account> {
        val (accounts, timeTaken) =
            measureTimedValue {
                AccountManager.get(FeatureFactory.appContext)
                    .getAccountsAsUser(userHandle.identifier)
            }
        Log.d(TAG, "getAccountsAsUser: It took $timeTaken.")
        return accounts
    }

    companion object {
        const val KEY = "account_detail_screen"

        const val ACCOUNT_NAME = "ACCOUNT_NAME"

        internal const val PRECONDITIONS_USER_DISABLED = "User is disabled."
        internal const val PRECONDITIONS_NO_ACCOUNT = "No account exists in AccountManager."

        private const val TAG = "AccountDetailApiScreen"
    }
}
// LINT.ThenChange(AccountDetailDashboardFragment.java,
//                 AccountPreferenceController.java)
