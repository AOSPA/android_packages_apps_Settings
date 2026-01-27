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

package com.android.settings.users

import android.Manifest.permission.CREATE_USERS
import android.Manifest.permission.INTERACT_ACROSS_USERS
import android.Manifest.permission.MANAGE_USERS
import android.Manifest.permission.QUERY_USERS
import android.app.Application
import android.content.Context
import android.content.pm.UserInfo
import android.os.UserManager
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.flags.Flags
import com.android.settings.testutils.shadow.ShadowUserManager
import com.android.settings.testutils.shadow.ShadowUtils
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.HardwareUnsupportedException
import com.android.settings.testutils2.MissingPermissionException
import com.android.settings.testutils2.Parameters
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowSystemProperties

@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowUserManager::class, ShadowSystemProperties::class, ShadowUtils::class])
class UserDetailsSettingsScreenApiTest {
    private val tester = ApiTester(UserDetailsSettingsScreenApi())
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val userManager: UserManager = context.getSystemService(UserManager::class.java)
    private lateinit var shadowUserManager: ShadowUserManager
    private lateinit var userInfo: UserInfo // Selected user details

    @get:Rule val setFlagsRule = SetFlagsRule()

    private val PARAM_TARGET_USER_ID = "targetUserId"

    @Before
    fun setUp() {
        shadowUserManager = Shadow.extract(userManager)
        ShadowUserManager.reset()
        userInfo =
            UserInfo(
                1,
                "Test",
                null,
                UserInfo.FLAG_FULL or UserInfo.FLAG_INITIALIZED,
                UserManager.USER_TYPE_FULL_SECONDARY,
            )
        shadowUserManager.addAliveUser(userInfo)
        ShadowUtils.setIsUserAMonkey(false)
        shadowUserManager.setSupportsMultipleUsers(true)
        tester.initializeScreenParameters(Parameters(PARAM_TARGET_USER_ID to "1"))
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_isNotNull() {
        assertThat(tester.getScreen()).isNotNull()
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagDisabled_isNull() {
        assertThat(tester.getScreen()).isNull()
    }

    @Test
    fun getLaunchIntent_settingUnavailable_fails() {
        ShadowUtils.setIsUserAMonkey(true)
        shadowUserManager.setSupportsMultipleUsers(false)

        val failure = assertFailsWith<HardwareUnsupportedException> { tester.getLaunchIntent() }
        assertThat(failure.reason).isEqualTo("This device does not support multiuser feature.")
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getEnableCalling_doesNotHaveManageUsersPermission_fails() {
        val application: Application = ApplicationProvider.getApplicationContext()
        shadowOf(application).denyPermissions(MANAGE_USERS, INTERACT_ACROSS_USERS)
        assertFailsWith<MissingPermissionException> { tester.get("enable_calling") }
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getEnableCalling_returnsExpectedResult() {
        val application: Application = ApplicationProvider.getApplicationContext()
        shadowOf(application).grantPermissions(MANAGE_USERS, CREATE_USERS, QUERY_USERS)
        shadowUserManager.addProfile(userInfo)

        shadowUserManager.setUserRestriction(
            userInfo.userHandle,
            UserManager.DISALLOW_OUTGOING_CALLS,
            true,
        )
        assertThat(tester.get<AnyBoolean>("enable_calling")).isEqualTo(false)

        shadowUserManager.setUserRestriction(
            userInfo.userHandle,
            UserManager.DISALLOW_OUTGOING_CALLS,
            false,
        )
        assertThat(tester.get<AnyBoolean>("enable_calling")).isEqualTo(true)

        shadowUserManager.setUserRestriction(
            userInfo.userHandle,
            UserManager.DISALLOW_OUTGOING_CALLS,
            true,
        )
        assertThat(tester.get<AnyBoolean>("enable_calling")).isEqualTo(false)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getUserGrantAdmin_doesNotHaveManageUsersPermission_fails() {
        val application: Application = ApplicationProvider.getApplicationContext()
        shadowOf(application).denyPermissions(MANAGE_USERS, CREATE_USERS, QUERY_USERS)
        assertFailsWith<MissingPermissionException> { tester.get("user_grant_admin") }
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun setUserGrantAdmin_doesNotHaveManageUsersPermission_fails() {
        val application: Application = ApplicationProvider.getApplicationContext()
        shadowOf(application).denyPermissions(MANAGE_USERS)
        assertFailsWith<MissingPermissionException> { tester.set("user_grant_admin", false) }
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getUserGrantAdmin_preconditionsMet_returnsFalse() {
        val application: Application = ApplicationProvider.getApplicationContext()
        shadowOf(application).grantPermissions(MANAGE_USERS)
        ShadowUserManager.setIsMultipleAdminEnabled(true)
        shadowUserManager.addProfile(userInfo)

        userManager.revokeUserAdmin(userInfo.id)
        assertThat(tester.get<AnyBoolean>("user_grant_admin")).isEqualTo(false)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getUserGrantAdmin_preconditionsMet_returnsTrue() {
        val application: Application = ApplicationProvider.getApplicationContext()
        shadowOf(application).grantPermissions(MANAGE_USERS)
        ShadowUserManager.setIsMultipleAdminEnabled(true)
        shadowUserManager.addProfile(userInfo)

        userManager.setUserAdmin(userInfo.id)
        assertThat(tester.get<AnyBoolean>("user_grant_admin")).isEqualTo(true)
    }
}
