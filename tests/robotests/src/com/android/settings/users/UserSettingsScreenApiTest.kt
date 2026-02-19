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
import android.Manifest.permission.GET_ACCOUNTS_PRIVILEGED
import android.Manifest.permission.MANAGE_USERS
import android.Manifest.permission.QUERY_USERS
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.UserManager
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.internal.R
import com.android.settings.flags.Flags.FLAG_CATALYST_MIGRATION_26Q2
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settings.testutils.shadow.ShadowUserManager
import com.android.settings.testutils.shadow.ShadowUtils
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.HardwareUnsupportedException
import com.android.settings.testutils2.MissingPermissionException
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
class UserSettingsScreenApiTest {
    private val tester = ApiTester(UserSettingsScreenApi())
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val userManager: UserManager = context.getSystemService(UserManager::class.java)
    private lateinit var shadowUserManager: ShadowUserManager

    @get:Rule val setFlagsRule = SetFlagsRule()

    private val ON = 1
    private val OFF = 0

    @Before
    fun setUp() {
        shadowUserManager = Shadow.extract(userManager)
        ShadowUserManager.reset()
        ShadowUtils.setIsUserAMonkey(false)
        shadowUserManager.setSupportsMultipleUsers(true)
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_isNotNull() {
        assertThat(tester.getScreen()).isNotNull()
    }

    @Test
    @DisableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagDisabled_isNull() {
        assertThat(tester.getScreen()).isNull()
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getLaunchIntent_settingUnavailable_fails() {
        ShadowUtils.setIsUserAMonkey(true)
        shadowUserManager.setSupportsMultipleUsers(false)

        val failure = assertFailsWith<HardwareUnsupportedException> { tester.getLaunchIntent() }
        assertThat(failure.reason).isEqualTo("This device does not support multiuser feature.")
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getEnableGuestCalling_doesNotHaveManageUsersPermission_fails() {
        val application: Application = ApplicationProvider.getApplicationContext()
        shadowOf(application).denyPermissions(MANAGE_USERS)
        assertFailsWith<MissingPermissionException> { tester.get("enable_guest_calling") }
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getMainSwitch_missingPermission_fails() {
        val application: Application = ApplicationProvider.getApplicationContext()
        shadowOf(application).denyPermissions(MANAGE_USERS, CREATE_USERS, QUERY_USERS)
        assertFailsWith<MissingPermissionException> { tester.get("multiple_users_main_switch") }
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getMainSwitch_returnsExpectedResult() {
        val application: Application = ApplicationProvider.getApplicationContext()
        shadowOf(application).grantPermissions(MANAGE_USERS, CREATE_USERS, QUERY_USERS)
        val allowChangeUserSwitcherResId = R.bool.config_allowChangeUserSwitcherEnabled
        SettingsShadowResources.overrideResource(allowChangeUserSwitcherResId, true)

        shadowUserManager.setUserSwitcherEnabled(true)
        assertThat(tester.get<AnyBoolean>("multiple_users_main_switch")).isEqualTo(true)

        shadowUserManager.setUserSwitcherEnabled(false)
        assertThat(tester.get<AnyBoolean>("multiple_users_main_switch")).isEqualTo(false)
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun setMainSwitch_setsExpectedResult() {
        val application: Application = ApplicationProvider.getApplicationContext()

        shadowOf(application).grantPermissions(MANAGE_USERS, CREATE_USERS, QUERY_USERS)
        val allowChangeUserSwitcherResId = R.bool.config_allowChangeUserSwitcherEnabled
        SettingsShadowResources.overrideResource(allowChangeUserSwitcherResId, true)

        tester.set("multiple_users_main_switch", false)
        assertThat(
                Settings.Global.getInt(
                    context.contentResolver,
                    Settings.Global.USER_SWITCHER_ENABLED,
                )
            )
            .isEqualTo(0)

        tester.set("multiple_users_main_switch", true)
        assertThat(
                Settings.Global.getInt(
                    context.contentResolver,
                    Settings.Global.USER_SWITCHER_ENABLED,
                )
            )
            .isEqualTo(1)
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getUserName_returnsExpectedValue() {
        // Set up precondition requirements to pass

        val application: Application = ApplicationProvider.getApplicationContext()
        shadowOf(application)
            .grantPermissions(MANAGE_USERS, CREATE_USERS, QUERY_USERS, GET_ACCOUNTS_PRIVILEGED)
        shadowUserManager.setIsAdminUser(true)
        val guestAlwaysEphemeralresId = R.bool.config_guestUserEphemeral
        SettingsShadowResources.overrideResource(guestAlwaysEphemeralresId, false)
        val allowEphemeralStateChangeResId = R.bool.config_guestUserAllowEphemeralStateChange
        SettingsShadowResources.overrideResource(allowEphemeralStateChangeResId, true)

        val expectedResult = userManager.userName
        assertThat(tester.get<AnyBoolean>("edit_user_name")).isEqualTo(expectedResult)
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun setUserName_updatesName() {
        // Set up precondition requirements to pass
        val application: Application = ApplicationProvider.getApplicationContext()
        shadowOf(application).grantPermissions(MANAGE_USERS)

        val newName = "Test"
        tester.set("edit_user_name", newName)
        assertThat(userManager.userName).isEqualTo(newName)
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getRemoveGuestOnExit_returnsExpectedValue() {
        // Set up precondition requirements to pass
        shadowUserManager.setIsAdminUser(true)
        val guestAlwaysEphemeralresId = R.bool.config_guestUserEphemeral
        SettingsShadowResources.overrideResource(guestAlwaysEphemeralresId, false)
        val allowEphemeralStateChangeResId = R.bool.config_guestUserAllowEphemeralStateChange
        SettingsShadowResources.overrideResource(allowEphemeralStateChangeResId, true)

        // Turn the setting ON
        Settings.Global.putInt(context.contentResolver, Settings.Global.REMOVE_GUEST_ON_EXIT, ON)
        assertThat(tester.get<AnyBoolean>("remove_guest_on_exit")).isEqualTo(true)

        // Turn the setting OFF
        Settings.Global.putInt(context.contentResolver, Settings.Global.REMOVE_GUEST_ON_EXIT, OFF)
        assertThat(tester.get<AnyBoolean>("remove_guest_on_exit")).isEqualTo(false)
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun setRemoveGuestOnExit_updatesSetting() {
        // Set up precondition requirements to pass
        shadowUserManager.setIsAdminUser(true)
        val guestAlwaysEphemeralresId = R.bool.config_guestUserEphemeral
        SettingsShadowResources.overrideResource(guestAlwaysEphemeralresId, false)
        val allowEphemeralStateChangeResId = R.bool.config_guestUserAllowEphemeralStateChange
        SettingsShadowResources.overrideResource(allowEphemeralStateChangeResId, true)

        // Use set API to turn setting OFF
        tester.set("remove_guest_on_exit", false)
        val settingValue1 =
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.REMOVE_GUEST_ON_EXIT,
                -1,
            )
        assertThat(settingValue1).isEqualTo(OFF)

        // Use set API to turn setting ON
        tester.set("remove_guest_on_exit", true)
        val settingValue2 =
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.REMOVE_GUEST_ON_EXIT,
                -1,
            )
        assertThat(settingValue2).isEqualTo(ON)
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getEnableGuestCalling_returnsExpectedValue() {
        // Set up precondition requirements to pass
        val shadowPackageManager = shadowOf(context.packageManager)
        shadowUserManager.setIsAdminUser(true)
        shadowPackageManager.setSystemFeature(PackageManager.FEATURE_TELEPHONY, true)
        val application: Application = ApplicationProvider.getApplicationContext()
        shadowOf(application).grantPermissions(MANAGE_USERS)

        // Turn the setting ON
        val guestRestrictions1: Bundle = userManager.getDefaultGuestRestrictions()
        guestRestrictions1.putBoolean(UserManager.DISALLOW_OUTGOING_CALLS, false)
        userManager.setDefaultGuestRestrictions(guestRestrictions1)
        assertThat(tester.get<AnyBoolean>("enable_guest_calling")).isEqualTo(true)

        // Turn the setting OFF
        val guestRestrictions2: Bundle = userManager.getDefaultGuestRestrictions()
        guestRestrictions2.putBoolean(UserManager.DISALLOW_OUTGOING_CALLS, true)
        userManager.setDefaultGuestRestrictions(guestRestrictions2)
        assertThat(tester.get<AnyBoolean>("enable_guest_calling")).isEqualTo(false)
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun setEnableGuestCalling_updatesSetting() {
        // Set up precondition requirements to pass
        val shadowPackageManager = shadowOf(context.packageManager)
        shadowUserManager.setIsAdminUser(true)
        shadowPackageManager.setSystemFeature(PackageManager.FEATURE_TELEPHONY, true)
        val application: Application = ApplicationProvider.getApplicationContext()
        shadowOf(application).grantPermissions(MANAGE_USERS)

        // Use set API to turn setting OFF
        tester.set("enable_guest_calling", false)
        // When setting is OFF, restriction is enabled
        assertThat(
                userManager
                    .getDefaultGuestRestrictions()
                    .getBoolean(UserManager.DISALLOW_OUTGOING_CALLS, false)
            )
            .isEqualTo(true)

        // Use set API to turn setting ON
        tester.set("enable_guest_calling", true)
        // When setting is ON, restriction is disabled
        assertThat(
                userManager
                    .getDefaultGuestRestrictions()
                    .getBoolean(UserManager.DISALLOW_OUTGOING_CALLS, false)
            )
            .isEqualTo(false)
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getAddUsersWhenLocked_returnsExpectedValue() {
        // Set up precondition requirements to pass
        shadowUserManager.setIsAdminUser(true)
        val resId = R.bool.config_userSwitchingMustGoThroughLoginScreen
        SettingsShadowResources.overrideResource(resId, false)
        userManager.setUserRestriction(UserManager.DISALLOW_ADD_USER, false)

        // Turn the setting ON
        Settings.Global.putInt(context.contentResolver, Settings.Global.ADD_USERS_WHEN_LOCKED, ON)
        assertThat(tester.get<AnyBoolean>("user_settings_add_users_when_locked")).isEqualTo(true)

        // Turn the setting OFF
        Settings.Global.putInt(context.contentResolver, Settings.Global.ADD_USERS_WHEN_LOCKED, OFF)
        assertThat(tester.get<AnyBoolean>("user_settings_add_users_when_locked")).isEqualTo(false)
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun setAddUsersWhenLocked_updatesSetting() {
        // Set up precondition requirements to pass
        shadowUserManager.setIsAdminUser(true)
        Settings.Global.putInt(context.contentResolver, Settings.Global.ADD_USERS_WHEN_LOCKED, ON)
        val resId = R.bool.config_userSwitchingMustGoThroughLoginScreen
        SettingsShadowResources.overrideResource(resId, false)
        userManager.setUserRestriction(UserManager.DISALLOW_ADD_USER, false)

        // Use set API to turn setting ON
        tester.set("user_settings_add_users_when_locked", true)
        val settingValue1 =
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.ADD_USERS_WHEN_LOCKED,
                -1,
            )
        assertThat(settingValue1).isEqualTo(ON)

        // Use set API to turn setting OFF
        tester.set("user_settings_add_users_when_locked", false)
        val settingValue2 =
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.ADD_USERS_WHEN_LOCKED,
                -1,
            )
        assertThat(settingValue2).isEqualTo(OFF)
    }
}
