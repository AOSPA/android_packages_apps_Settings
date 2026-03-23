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

import android.accounts.AccountManager
import android.content.Context
import android.os.Process
import com.android.settings.R
import com.android.settingslib.metadata.preferencesapi.types.DirectFiniteOptionsType
import com.android.settingslib.metadata.preferencesapi.types.EType
import com.android.settingslib.metadata.preferencesapi.unsafe

/** Provides a list of accounts associated with a associated with the current process user. */
object Accounts : DirectFiniteOptionsType<String> {
    override val externalType: EType<String> = EType.String

    override fun getDescription(context: Context): String =
        context.getString(R.string.accounts_as_user_type_description)

    // TODO(b/479499461): Add support for multiple users.
    override suspend fun getOptions(context: Context) =
        AccountManager.get(context).getAccountsAsUser(Process.myUserHandle().identifier).map {
            account ->
            account.name.unsafe() to account.name.unsafe()
        }

    override fun getKey(): String = "Accounts"
}
