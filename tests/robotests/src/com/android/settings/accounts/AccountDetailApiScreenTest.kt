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
import android.os.UserHandle
import android.os.UserManager
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.flags.Flags
import com.android.settings.testutils.shadow.ShadowAccountManager
import com.android.settings.testutils.shadow.ShadowUserManager
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.FailedPreconditionException
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow

@RunWith(AndroidJUnit4::class)
class AccountDetailApiScreenTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private val tester = ApiTester(AccountDetailApiScreen())

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagEnabled_isNotNull() {
        assertThat(tester.getScreen()).isNotNull()
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagDisabled_isNull() {
        assertThat(tester.getScreen()).isNull()
    }

    @Test
    @Config(shadows = [ShadowAccountManager::class])
    fun getLaunchIntent_withAccount_hasIntent() {
        ShadowAccountManager.addAccountForUser(
            Process.myUserHandle().identifier,
            Account("test@test.com", "com.test"),
        )
        assertThat(tester.getLaunchIntent()).isNotNull()
        ShadowAccountManager.reset()
    }

    @Test
    @Config(shadows = [ShadowAccountManager::class])
    fun getLaunchIntent_withoutAccount_throwsCustomException() {
        assertFailsWith<FailedPreconditionException> { tester.getLaunchIntent() }
    }

    @Test
    @Config(shadows = [ShadowAccountManager::class, ShadowUserManager::class])
    fun getLaunchIntent_disabledUserInfo_throwsCustomException() {
        val userManager =
            Shadow.extract<ShadowUserManager?>(
                context.getSystemService<UserManager?>(UserManager::class.java)
            )
        userManager?.addUserProfile(UserHandle(0))
        userManager?.setIsUserEnabled(0, false)

        assertFailsWith<FailedPreconditionException> { tester.getLaunchIntent() }
    }
}
