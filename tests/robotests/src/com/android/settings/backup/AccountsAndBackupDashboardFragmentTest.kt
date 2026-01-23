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
import androidx.test.core.app.ApplicationProvider
import com.android.settingslib.drawer.CategoryKey
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AccountsAndBackupDashboardFragmentTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val fragment = AccountsAndBackupDashboardFragment()

    @Test
    fun getCategoryKey_isAccountsAndBackup() {
        assertThat(fragment.categoryKey).isEqualTo(CategoryKey.CATEGORY_ACCOUNTS_AND_BACKUP)
    }

    @Test
    fun getMetricsCategory_isAccountsAndBackup() {
        assertThat(fragment.metricsCategory).isEqualTo(SettingsEnums.ACCOUNTS_AND_BACKUP)
    }

    @Test
    fun getPreferenceScreenBindingKey_isAccountsAndBackup() {
        assertThat(fragment.getPreferenceScreenBindingKey(context))
            .isEqualTo(AccountsAndBackupScreen.KEY)
    }
}
