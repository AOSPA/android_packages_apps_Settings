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

import android.content.Context
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import com.android.settings.backup.AccountsAndBackupScreen.Companion.KEY_ACCOUNTS
import com.android.settings.backup.AccountsAndBackupScreen.Companion.KEY_BACKUP
import com.android.settings.flags.Flags
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.TestScope
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AccountsAndBackupScreenTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private val preferenceScreen = AccountsAndBackupScreen()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val testScope = TestScope()

    @Test
    fun getPreferenceHierarchy_returnsCorrectHierarchy() {
        val hierarchy = preferenceScreen.getPreferenceHierarchy(context, testScope)

        assertThat(hierarchy.find(KEY_BACKUP)).isNotNull()
        assertThat(hierarchy.find(KEY_ACCOUNTS)).isNotNull()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_ACCOUNTS_AND_BACKUP_SCREEN)
    fun isFlagEnabled_accountAndBackupFlagEnabled_returnsTrue() {
        assertThat(preferenceScreen.isFlagEnabled(context)).isTrue()
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_ACCOUNTS_AND_BACKUP_SCREEN)
    fun isFlagEnabled_accountAndBackupFlagDisabled_returnsFalse() {
        assertThat(preferenceScreen.isFlagEnabled(context)).isFalse()
    }
}