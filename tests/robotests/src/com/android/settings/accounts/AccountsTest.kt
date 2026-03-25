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
import android.content.Context
import android.os.Process
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.accounts.AccountDetailApiScreen.Companion.ACCOUNT_NAME
import com.android.settings.testutils.shadow.ShadowAccountManager
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.preference.PreferenceFragment
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class AccountsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    @Config(shadows = [ShadowAccountManager::class])
    fun getAllPossibleParameters_withAccountsAsUser_returnParameters() {
        val screen =
            object :
                PreferencesApiScreen(
                    key = "ApiScreen",
                    topLevelSettingsCategory = Category.ACCOUNTS,
                    fragment = PreferenceFragment::class,
                    purpose = R.string.preference_screen_purpose,
                ) {
                init {
                    parameters {
                        parameter(
                            name = ACCOUNT_NAME,
                            purpose = R.string.parameter_purpose,
                            required = true,
                            type = Accounts,
                        )
                    }
                }
            }

        ShadowAccountManager.addAccountForUser(
            Process.myUserHandle().identifier,
            Account("Terapan@nogi.com.test", "com.test"),
        )
        ShadowAccountManager.addAccountForUser(
            Process.myUserHandle().identifier,
            Account("Hinano@nogi.com.test", "com.test"),
        )

        val allPossibleParameters = runBlocking {
            screen.getAllPossibleParameters(context).toList()
        }
        assertThat(allPossibleParameters).hasSize(2)

        val possibleParameterPairs =
            allPossibleParameters.flatMap { it.values.entries }.map { it.key to it.value }
        assertThat(possibleParameterPairs)
            .containsExactly(
                ACCOUNT_NAME to "Terapan@nogi.com.test",
                ACCOUNT_NAME to "Hinano@nogi.com.test",
            )

        ShadowAccountManager.reset()
    }
}
