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
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.PreferenceCategory
import com.android.settingslib.metadata.PreferenceHierarchy
import com.android.settingslib.metadata.PreferenceIconProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

@ProvidePreferenceScreen(ManageAccountsScreen.KEY)
class ManageAccountsScreen : PreferenceScreenMixin, PreferenceIconProvider {
    override val key: String
        get() = KEY

    override val indexable
        get() = Flags.enableAccountsAndBackupScreen()

    // TODO(b/446191970): Add search keywords.
    override val keywords: Int
        get() = if (Flags.enableAccountsAndBackupScreen()) R.string.keywords_accounts else 0

    override val title: Int
        get() = R.string.accounts_and_backup_manage_accounts_title

    // TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.top_level_accounts_and_backup_purpose

    override fun getIcon(context: Context) = R.drawable.ic_manage_accounts

    override val highlightMenuKey: Int
        get() = R.string.menu_key_accounts_and_backup

    override fun getMetricsCategory() = SettingsEnums.ACCOUNTS_AND_BACKUP

    override fun fragmentClass(): Class<out Fragment>? = ManageAccountsDashboardFragment::class.java

    override fun hasCompleteHierarchy() = false

    override fun getPreferenceHierarchy(
        context: Context,
        coroutineScope: CoroutineScope,
    ): PreferenceHierarchy {
        return preferenceHierarchy(context) {}
    }

    companion object {
        const val KEY = "manage_accounts_screen"
    }
}
