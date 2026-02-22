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
import android.credentials.CredentialManager
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.AccountDashboardActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceIconProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.widget.SettingsThemeHelper.isExpressiveTheme
import kotlinx.coroutines.CoroutineScope
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_UNCATEGORIZED

// LINT.IfChange
@ProvidePreferenceScreen(AccountScreen.KEY)
open class AccountScreen : PreferenceScreenMixin, PreferenceTitleProvider, PreferenceIconProvider {
    override fun tags(context: Context) = arrayOf(APP_FUNCTION_UNCATEGORIZED)

    override val key: String
        get() = KEY

    // TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.top_level_accounts_purpose

    override val summary: Int
        get() = R.string.account_dashboard_default_summary

    override val keywords: Int
        get() = if (Flags.enableAccountsAndBackupScreen()) 0 else R.string.keywords_accounts

    override fun getTitle(context: Context): CharSequence =
        context.getText(context.getAccountScreenTitle())

    override fun getIcon(context: Context) =
        when {
            isExpressiveTheme(context) -> R.drawable.ic_homepage_accounts
            else -> R.drawable.ic_settings_passwords_filled
        }

    override val highlightMenuKey: Int
        get() = R.string.menu_key_accounts

    override fun getMetricsCategory() = SettingsEnums.ACCOUNT

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass(): Class<out Fragment>? = AccountDashboardFragment::class.java

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {}

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, AccountDashboardActivity::class.java, metadata?.key)

    companion object {
        const val KEY = "top_level_accounts"

        @JvmStatic
        fun Context.getAccountScreenTitle(): Int =
            when {
                !CredentialManager.isServiceEnabled(this) -> R.string.account_dashboard_title
                Flags.enableAccountsAndBackupScreen() -> R.string.dashboard_title_passwords_passkeys
                else -> R.string.account_dashboard_title_with_passkeys
            }
    }
}
// LINT.ThenChange(AccountDashboardFragment.java)
