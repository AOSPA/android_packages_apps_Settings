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

package com.android.settings.security

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.app.Application
import android.app.admin.DevicePolicyIdentifiers
import android.app.admin.DevicePolicyManager
import android.app.admin.PolicyEnforcementInfo
import android.app.admin.flags.Flags.FLAG_POLICY_TRANSPARENCY_REFACTOR_V2
import android.content.ComponentName
import android.content.Context
import android.content.pm.UserInfo
import android.os.UserHandle
import android.os.UserManager
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.DeviceConfig
import android.provider.Settings
import android.view.contentcapture.ContentCaptureManager
import androidx.core.content.getSystemService
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.internal.R.string.config_defaultContentProtectionService
import com.android.settings.flags.Flags.FLAG_CATALYST_MIGRATION_26Q2
import com.android.settings.testutils.DevicePolicyUtils.DPC_ADMIN
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settings.testutils.shadow.ShadowDevicePolicyManager
import com.android.settings.testutils.shadow.ShadowUserManager
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.FailedPreconditionException
import com.android.settings.testutils2.MissingPermissionException
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.spy
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow

@RunWith(AndroidJUnit4::class)
@Config(
    shadows =
        [SettingsShadowResources::class, ShadowUserManager::class, ShadowDevicePolicyManager::class]
)
class ContentProtectionScreenApiTest {

    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()
    private val spyPrimaryContext: Context = spy(applicationContext)
    private val shadowUserManager: ShadowUserManager =
        Shadow.extract(spyPrimaryContext.getSystemService<UserManager>())
    private val shadowDevicePolicyManager: ShadowDevicePolicyManager =
        Shadow.extract(spyPrimaryContext.getSystemService<DevicePolicyManager>())
    private lateinit var tester: ApiTester

    @get:Rule val setFlagsRule = SetFlagsRule()

    @Before
    fun setUp() {
        SettingsShadowResources.overrideResource(
            config_defaultContentProtectionService,
            "com.test.package/TestClass",
        )

        shadowUserManager.addUser(
            0,
            "Personal profile",
            UserInfo.FLAG_ADMIN or UserInfo.FLAG_MAIN or UserInfo.FLAG_FULL,
        )
        initializeTester(spyPrimaryContext)
    }

    @After
    fun tearDown() {
        SettingsShadowResources.reset()
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagEnabled_isNotNull() {
        assertThat(tester.getScreen()).isNotNull()
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_secondaryUser_flagEnabled_isNotNull() {
        switchToSecondaryUser()

        assertThat(tester.getScreen()).isNotNull()
    }

    @Test
    @DisableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagDisabled_isNull() {
        assertThat(tester.getScreen()).isNull()
    }

    @Test
    @DisableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_secondaryUser_flagDisabled_isNull() {
        switchToSecondaryUser()

        assertThat(tester.getScreen()).isNull()
    }

    @Test
    fun getIntent_contentProtectionEnabled_isNotNull() {
        setContentProtectionEnabled(true)

        assertThat(tester.getLaunchIntent()).isNotNull()
    }

    @Test
    fun getIntent_secondaryUser_contentProtectionEnabled_isNotNull() {
        setContentProtectionEnabled(true)

        assertThat(tester.getLaunchIntent()).isNotNull()
    }

    fun getIntent_contentProtectionDisabled_preconditionCheckFails() {
        setContentProtectionEnabled(false)

        assertFailsWith<FailedPreconditionException> { tester.getLaunchIntent() }
    }

    fun getIntent_secondaryUser_contentProtectionDisabled_preconditionCheckFails() {
        setContentProtectionEnabled(false)
        switchToSecondaryUser()

        assertFailsWith<FailedPreconditionException> { tester.getLaunchIntent() }
    }

    @Test
    fun getMainSwitch_withoutWorkProfile_returnsExpectedResult() {
        setContentProtectionEnabled(true)

        // Turn the setting ON
        Settings.Global.putInt(
            spyPrimaryContext.contentResolver,
            KEY_CONTENT_PROTECTION_PREFERENCE,
            ON,
        )
        assertThat(tester.get<AnyBoolean>("content_protection_preference_user_consent_switch"))
            .isEqualTo(true)

        // Turn the setting OFF
        Settings.Global.putInt(
            spyPrimaryContext.contentResolver,
            KEY_CONTENT_PROTECTION_PREFERENCE,
            OFF,
        )
        assertThat(tester.get<AnyBoolean>("content_protection_preference_user_consent_switch"))
            .isEqualTo(false)
    }

    @Test
    fun getMainSwitch_secondaryUser_withoutWorkProfile_preconditionCheckFails() {
        setContentProtectionEnabled(true)
        switchToSecondaryUser()

        // Turn the setting ON
        Settings.Global.putInt(
            spyPrimaryContext.contentResolver,
            KEY_CONTENT_PROTECTION_PREFERENCE,
            ON,
        )
        assertFailsWith<FailedPreconditionException> {
            tester.get<AnyBoolean>("content_protection_preference_user_consent_switch")
        }

        // Turn the setting OFF
        Settings.Global.putInt(
            spyPrimaryContext.contentResolver,
            KEY_CONTENT_PROTECTION_PREFERENCE,
            OFF,
        )
        assertFailsWith<FailedPreconditionException> {
            tester.get<AnyBoolean>("content_protection_preference_user_consent_switch")
        }
    }

    @Test
    @EnableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2)
    fun getMainSwitch_withWorkProfile_withoutPolicy_returnsExpectedResult() {
        setContentProtectionEnabled(true)

        setUpWorkProfile()

        // Turn the setting ON
        Settings.Global.putInt(
            spyPrimaryContext.contentResolver,
            KEY_CONTENT_PROTECTION_PREFERENCE,
            ON,
        )
        assertThat(tester.get<AnyBoolean>("content_protection_preference_user_consent_switch"))
            .isEqualTo(true)

        // Turn the setting OFF
        Settings.Global.putInt(
            spyPrimaryContext.contentResolver,
            KEY_CONTENT_PROTECTION_PREFERENCE,
            OFF,
        )
        assertThat(tester.get<AnyBoolean>("content_protection_preference_user_consent_switch"))
            .isEqualTo(false)
    }

    // TODO(b/486770406): add getMainSwitch_fromWorkProfile_withoutPolicy_returnsExpectedResult

    @Test
    @EnableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2)
    fun getMainSwitch_secondaryUser_withWorkProfile_withoutPolicy_preconditionCheckFails() {
        setContentProtectionEnabled(true)
        switchToSecondaryUser()

        setUpWorkProfile()

        // Turn the setting ON
        Settings.Global.putInt(
            spyPrimaryContext.contentResolver,
            KEY_CONTENT_PROTECTION_PREFERENCE,
            ON,
        )
        assertFailsWith<FailedPreconditionException> {
            tester.get<AnyBoolean>("content_protection_preference_user_consent_switch")
        }

        // Turn the setting OFF
        Settings.Global.putInt(
            spyPrimaryContext.contentResolver,
            KEY_CONTENT_PROTECTION_PREFERENCE,
            OFF,
        )
        assertFailsWith<FailedPreconditionException> {
            tester.get<AnyBoolean>("content_protection_preference_user_consent_switch")
        }
    }

    @Test
    @EnableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2)
    fun getMainSwitch_withWorkProfile_withPolicy_returnsExpectedResult() {
        setContentProtectionEnabled(true)

        val spyManagedDevicePolicyManager = setUpWorkProfile()

        spyManagedDevicePolicyManager.stub {
            on { getContentProtectionPolicy(anyOrNull()) } doReturn
                DevicePolicyManager.CONTENT_PROTECTION_DISABLED
        }
        assertThat(tester.get<AnyBoolean>("content_protection_preference_user_consent_switch"))
            .isEqualTo(false)

        spyManagedDevicePolicyManager.stub {
            on { getContentProtectionPolicy(anyOrNull()) } doReturn
                DevicePolicyManager.CONTENT_PROTECTION_ENABLED
        }
        assertThat(tester.get<AnyBoolean>("content_protection_preference_user_consent_switch"))
            .isEqualTo(true)
    }

    // TODO(b/486770406): add getMainSwitch_fromWorkProfile_withPolicy_returnsExpectedResult

    @Test
    @EnableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2)
    fun getMainSwitch_secondaryUser_withWorkProfile_withPolicy_preconditionCheckFails() {
        setContentProtectionEnabled(true)
        switchToSecondaryUser()

        val spyManagedDevicePolicyManager = setUpWorkProfile()

        spyManagedDevicePolicyManager.stub {
            on { getContentProtectionPolicy(anyOrNull()) } doReturn
                DevicePolicyManager.CONTENT_PROTECTION_DISABLED
        }
        assertFailsWith<FailedPreconditionException> {
            tester.get<AnyBoolean>("content_protection_preference_user_consent_switch")
        }

        spyManagedDevicePolicyManager.stub {
            on { getContentProtectionPolicy(anyOrNull()) } doReturn
                DevicePolicyManager.CONTENT_PROTECTION_ENABLED
        }
        assertFailsWith<FailedPreconditionException> {
            tester.get<AnyBoolean>("content_protection_preference_user_consent_switch")
        }
    }

    @Test
    fun setMainSwitch_withoutWriteSecureSettingsPermission_permissionCheckFails() {
        setContentProtectionEnabled(true)
        shadowOf(applicationContext).denyPermissions(WRITE_SECURE_SETTINGS)

        assertFailsWith<MissingPermissionException> {
            tester.set("content_protection_preference_user_consent_switch", false)
        }
        assertFailsWith<MissingPermissionException> {
            tester.set("content_protection_preference_user_consent_switch", true)
        }
    }

    @Test
    fun setMainSwitch_withoutWorkProfile_setsExpectedResult() {
        setContentProtectionEnabled(true)
        shadowOf(applicationContext).grantPermissions(WRITE_SECURE_SETTINGS)

        tester.set("content_protection_preference_user_consent_switch", false)
        assertThat(
                Settings.Global.getInt(
                    spyPrimaryContext.contentResolver,
                    KEY_CONTENT_PROTECTION_PREFERENCE,
                )
            )
            .isEqualTo(OFF)

        tester.set("content_protection_preference_user_consent_switch", true)
        assertThat(
                Settings.Global.getInt(
                    spyPrimaryContext.contentResolver,
                    KEY_CONTENT_PROTECTION_PREFERENCE,
                )
            )
            .isEqualTo(ON)
    }

    @Test
    fun setMainSwitch_secondaryUser_withoutWorkProfile_preconditionCheckFails() {
        setContentProtectionEnabled(true)
        switchToSecondaryUser()

        assertFailsWith<FailedPreconditionException> {
            tester.set("content_protection_preference_user_consent_switch", false)
        }
        assertFailsWith<FailedPreconditionException> {
            tester.set("content_protection_preference_user_consent_switch", true)
        }
    }

    @Test
    @EnableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2)
    fun setMainSwitch_withWorkProfile_withoutPolicy_setsExpectedResult() {
        setContentProtectionEnabled(true)
        shadowOf(applicationContext).grantPermissions(WRITE_SECURE_SETTINGS)

        setUpWorkProfile()

        tester.set("content_protection_preference_user_consent_switch", false)
        assertThat(
                Settings.Global.getInt(
                    spyPrimaryContext.contentResolver,
                    KEY_CONTENT_PROTECTION_PREFERENCE,
                )
            )
            .isEqualTo(OFF)

        tester.set("content_protection_preference_user_consent_switch", true)
        assertThat(
                Settings.Global.getInt(
                    spyPrimaryContext.contentResolver,
                    KEY_CONTENT_PROTECTION_PREFERENCE,
                )
            )
            .isEqualTo(ON)
    }

    // TODO(b/486770406): add setMainSwitch_fromWorkProfile_withoutPolicy_preconditionCheckFails

    @Test
    fun setMainSwitch_secondaryUser_withWorkProfile_withoutPolicy_preconditionCheckFails() {
        setContentProtectionEnabled(true)
        setUpWorkProfile()
        switchToSecondaryUser()

        assertFailsWith<FailedPreconditionException> {
            tester.set("content_protection_preference_user_consent_switch", false)
        }
        assertFailsWith<FailedPreconditionException> {
            tester.set("content_protection_preference_user_consent_switch", true)
        }
    }

    @Test
    @EnableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2)
    fun setMainSwitch_withWorkProfile_withPolicy_preconditionCheckFails() {
        setContentProtectionEnabled(true)
        shadowOf(applicationContext).grantPermissions(WRITE_SECURE_SETTINGS)
        val spyManagedDevicePolicyManager = setUpWorkProfile()
        spyManagedDevicePolicyManager.stub {
            on { getContentProtectionPolicy(anyOrNull()) } doReturn
                DevicePolicyManager.CONTENT_PROTECTION_DISABLED
        }

        assertFailsWith<FailedPreconditionException> {
            tester.set("content_protection_preference_user_consent_switch", false)
        }
        assertFailsWith<FailedPreconditionException> {
            tester.set("content_protection_preference_user_consent_switch", true)
        }
    }

    // TODO(b/486770406): add setMainSwitch_fromWorkProfile_withPolicy_preconditionCheckFails

    @Test
    @EnableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2)
    fun setMainSwitch_secondaryUser_withWorkProfile_withPolicy_preconditionCheckFails() {
        setContentProtectionEnabled(true)
        shadowOf(applicationContext).grantPermissions(WRITE_SECURE_SETTINGS)
        val spyManagedDevicePolicyManager = setUpWorkProfile()
        spyManagedDevicePolicyManager.stub {
            on { getContentProtectionPolicy(anyOrNull()) } doReturn
                DevicePolicyManager.CONTENT_PROTECTION_DISABLED
        }
        switchToSecondaryUser()

        assertFailsWith<FailedPreconditionException> {
            tester.set("content_protection_preference_user_consent_switch", false)
        }
        assertFailsWith<FailedPreconditionException> {
            tester.set("content_protection_preference_user_consent_switch", true)
        }
    }

    private fun setUpWorkProfile(): DevicePolicyManager {
        shadowUserManager.addProfile(
            0,
            10,
            "Test work profile",
            UserInfo.FLAG_PROFILE or UserInfo.FLAG_MANAGED_PROFILE,
        )
        val workProfileHandle = UserHandle.of(10)
        shadowUserManager.addUserProfile(workProfileHandle)
        val spyManagedContext: Context =
            spy(
                spyPrimaryContext.createPackageContextAsUser(
                    spyPrimaryContext.getPackageName(),
                    /* flags= */ 0,
                    workProfileHandle,
                )
            )
        spyPrimaryContext.stub {
            on {
                createPackageContextAsUser(
                    spyPrimaryContext.getPackageName(),
                    /* flags= */ 0,
                    workProfileHandle,
                )
            } doReturn spyManagedContext
        }
        val managedDevicePolicyManager = spyManagedContext.getSystemService<DevicePolicyManager>()
        val spyDevicePolicyManager: DevicePolicyManager = spy(managedDevicePolicyManager!!)

        spyManagedContext.stub {
            on { getSystemService(Context.DEVICE_POLICY_SERVICE) } doReturn spyDevicePolicyManager
        }

        shadowDevicePolicyManager.setActiveAdmin(ComponentName("test", "test"))
        shadowDevicePolicyManager.setPolicyEnforcementInfoForPolicy(
            DevicePolicyIdentifiers.CONTENT_PROTECTION_POLICY,
            PolicyEnforcementInfo(listOf(DPC_ADMIN)),
        )

        return spyDevicePolicyManager
    }

    private fun switchToSecondaryUser() {
        shadowUserManager.addUser(11, "Secondary profile", UserInfo.FLAG_FULL)
        val secondaryUserHandle = UserHandle.of(11)
        val secondaryContext: Context =
            spyPrimaryContext.createPackageContextAsUser(
                spyPrimaryContext.getPackageName(),
                /* flags= */ 0,
                secondaryUserHandle,
            )
        shadowUserManager.switchUser(11)
        initializeTester(secondaryContext)
    }

    private fun initializeTester(context: Context) {
        tester = ApiTester(ContentProtectionScreenApi(), context)
    }

    private fun setContentProtectionEnabled(enabled: Boolean) {
        DeviceConfig.setProperty(
            DeviceConfig.NAMESPACE_CONTENT_CAPTURE,
            ContentCaptureManager.DEVICE_CONFIG_PROPERTY_ENABLE_CONTENT_PROTECTION_RECEIVER,
            if (enabled) "true" else "false",
            /* makeDefault= */ false,
        )
    }

    companion object {
        private const val KEY_CONTENT_PROTECTION_PREFERENCE = "content_protection_user_consent"
        private const val ON = 1
        private const val OFF = -1
    }
}
