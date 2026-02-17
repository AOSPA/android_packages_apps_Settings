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
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.accounts.ManageAccountsScreen
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.PreferenceCategory
import com.android.settingslib.metadata.PreferenceIconProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.widget.SettingsThemeHelper.isExpressiveTheme
import kotlinx.coroutines.CoroutineScope

// LINT.IfChange
@ProvidePreferenceScreen(AccountsAndBackupScreen.KEY)
open class AccountsAndBackupScreen : PreferenceScreenMixin, PreferenceIconProvider {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.accounts_and_backup_title

    override val summary: Int
        get() = R.string.accounts_and_backup_summary

    override val indexable
        get() = Flags.enableAccountsAndBackupScreen()

    // TODO(b/446191970): Add search keywords.
    override val keywords: Int
        get() = 0

    // TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.top_level_accounts_and_backup_purpose

    override fun getIcon(context: Context) =
        when {
            isExpressiveTheme(context) -> R.drawable.ic_homepage_accounts_and_backup
            else -> R.drawable.ic_settings_accounts_and_backup_filled
        }

    override val highlightMenuKey: Int
        get() = R.string.menu_key_accounts_and_backup

    override fun isFlagEnabled(context: Context): Boolean = Flags.enableAccountsAndBackupScreen()

    override fun fragmentClass(): Class<out Fragment>? =
        AccountsAndBackupDashboardFragment::class.java

    override fun getMetricsCategory() = SettingsEnums.ACCOUNTS_AND_BACKUP

    override fun hasCompleteHierarchy() = false

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +PreferenceCategory(
                key = KEY_ACCOUNTS,
                purpose = R.string.accounts_category_purpose,
                title = R.string.account_settings,
            ) +=
                {
                    +ManageAccountsScreen.KEY
                }
            +PreferenceCategory(
                key = KEY_BACKUP,
                purpose = R.string.backup_category_purpose,
                title = R.string.accounts_and_backup_backup_category_title,
            )
        }

    companion object {
        const val KEY = "top_level_accounts_and_backup"
        @VisibleForTesting const val KEY_ACCOUNTS = "accounts_and_backup_accounts_category"
        @VisibleForTesting const val KEY_BACKUP = "accounts_and_backup_backup_category"
    }
}
// LINT.ThenChange(AccountsAndBackupDashboardFragment.kt)
