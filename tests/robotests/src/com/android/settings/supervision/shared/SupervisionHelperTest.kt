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
package com.android.settings.supervision.shared

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.app.role.RoleManager
import android.app.supervision.ISupervisionManager
import android.app.supervision.SupervisionManager
import android.app.supervision.flags.Flags
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
import android.content.pm.PackageManager.DONT_KILL_APP
import android.content.pm.UserInfo
import android.content.res.Resources
import android.os.UserManager
import android.os.UserManager.USER_TYPE_FULL_SECONDARY
import android.os.UserManager.USER_TYPE_FULL_SYSTEM
import android.os.UserManager.USER_TYPE_PROFILE_SUPERVISING
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.supervision.ipc.SupervisionMessengerClient
import com.google.common.truth.Truth.assertThat
import kotlin.collections.listOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowDevicePolicyManager
import org.robolectric.shadows.ShadowPackageManager
import org.robolectric.shadows.ShadowServiceManager

@RunWith(AndroidJUnit4::class)
class SupervisionHelperTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private val mockRoleManager = mock<RoleManager>()
    private val mockSupervisionManager = mock<SupervisionManager>()
    private val mockUserManager = mock<UserManager>()
    private val mockResources = mock<Resources>()
    private val mockISupervisionManager = mock<ISupervisionManager>()
    private val mockKeyguardManager = mock<KeyguardManager>()

    private val context = contextOf(roleManager = mockRoleManager)
    private lateinit var packageManager: PackageManager
    private lateinit var shadowPackageManager: ShadowPackageManager

    private lateinit var dpm: DevicePolicyManager
    private lateinit var shadowDpm: ShadowDevicePolicyManager

    @Before
    fun setup() {
        val applicationContext = ApplicationProvider.getApplicationContext<Context>()
        packageManager = applicationContext.packageManager
        shadowPackageManager = shadowOf(packageManager)
        dpm = applicationContext.getSystemService(DevicePolicyManager::class.java)
        shadowDpm = shadowOf(dpm) as ShadowDevicePolicyManager
        ShadowServiceManager.addBinderService(
            Context.SUPERVISION_SERVICE,
            ISupervisionManager::class.java,
            mockISupervisionManager,
        )
    }

    @Test
    fun systemSupervisionPackageName_roleManagerReturnsPackageName_returnsPackageName() {
        val testPackageName = "com.android.supervision"
        mockRoleManager.stub {
            on { getRoleHolders(RoleManager.ROLE_SYSTEM_SUPERVISION) } doReturn
                listOf(testPackageName)
        }

        assertThat(context.systemSupervisionPackageName).isEqualTo(testPackageName)
        verify(mockRoleManager).getRoleHolders(RoleManager.ROLE_SYSTEM_SUPERVISION)
    }

    @Test
    fun systemSupervisionPackageName_roleManagerReturnsEmptyList_returnsNull() {
        mockRoleManager.stub {
            on { getRoleHolders(RoleManager.ROLE_SYSTEM_SUPERVISION) } doReturn emptyList()
        }

        assertThat(context.systemSupervisionPackageName).isNull()
        verify(mockRoleManager).getRoleHolders(RoleManager.ROLE_SYSTEM_SUPERVISION)
    }

    @Test
    fun systemSupervisionPackageName_roleManagerUnavailable_returnsNull() {
        val context = contextOf(roleManager = null)

        assertThat(context.systemSupervisionPackageName).isNull()
    }

    @Test
    fun supervisionRoleHolders_roleManagerReturnsPackageName_returnsPackageNames() {
        val fooPackageName = "com.android.foo"
        val barPackageName = "com.android.bar"
        mockRoleManager.stub {
            on { getRoleHolders(RoleManager.ROLE_SUPERVISION) } doReturn
                listOf(fooPackageName, barPackageName)
        }

        assertThat(context.supervisionRoleHolders).containsExactly(fooPackageName, barPackageName)
        verify(mockRoleManager).getRoleHolders(RoleManager.ROLE_SUPERVISION)
    }

    @Test
    fun supervisionRoleHolders_roleManagerUnavailable_returnsEmptyList() {
        val context = contextOf(roleManager = null)

        assertThat(context.supervisionRoleHolders).isEmpty()
    }

    @Test
    fun areAnyUsersSupervisedExceptCurrent_currentUserSupervised_returnsFalse() {
        mockUserManager.stub { on { users } doReturn listOf(MAIN_USER, SECONDARY_USER) }
        mockSupervisionManager.stub {
            on { isSupervisionEnabledForUser(MAIN_USER_ID) } doReturn true
            on { isSupervisionEnabledForUser(SECONDARY_USER_ID) } doReturn false
        }

        assertThat(
                context.areAnyUsersExceptCurrentSupervised(mockSupervisionManager, mockUserManager)
            )
            .isFalse()
    }

    @Test
    fun areAnyUsersSupervisedExceptCurrent_noUsers_returnsFalse() {
        mockUserManager.stub { on { users } doReturn emptyList() }

        assertThat(
                context.areAnyUsersExceptCurrentSupervised(mockSupervisionManager, mockUserManager)
            )
            .isFalse()
    }

    @Test
    fun areAnyUsersSupervisedExceptCurrent_secondaryUserSupervised_returnsTrue() {
        mockUserManager.stub { on { users } doReturn listOf(MAIN_USER, SECONDARY_USER) }
        mockSupervisionManager.stub {
            on { isSupervisionEnabledForUser(MAIN_USER_ID) } doReturn true
            on { isSupervisionEnabledForUser(SECONDARY_USER_ID) } doReturn true
        }

        assertThat(
                context.areAnyUsersExceptCurrentSupervised(mockSupervisionManager, mockUserManager)
            )
            .isTrue()
    }

    @Test
    fun deleteSupervisionData_currentUserSupervised_deletesSupervisionData() {
        mockUserManager.stub {
            on { users } doReturn listOf(MAIN_USER, SECONDARY_USER, SUPERVISING_PROFILE)
            on { removeUserEvenWhenDisallowed(SUPERVISING_USER_ID) } doReturn true
        }
        mockSupervisionManager.stub {
            on { isSupervisionEnabledForUser(MAIN_USER_ID) } doReturn true
            on { isSupervisionEnabledForUser(SECONDARY_USER_ID) } doReturn false
            on { isSupervisionEnabledForUser(SUPERVISING_USER_ID) } doReturn false
        }

        val result = context.deleteSupervisionData(disableSupervision = true)

        assertThat(result).isTrue()
        verify(mockSupervisionManager).setSupervisionEnabled(false)
        verify(mockSupervisionManager).setSupervisionRecoveryInfo(null)
        verify(mockUserManager).removeUserEvenWhenDisallowed(eq(SUPERVISING_USER_ID))
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun deleteSupervisionData_secondaryUserSupervised_keepsSupervisionData() {
        mockUserManager.stub {
            on { users } doReturn listOf(MAIN_USER, SECONDARY_USER, SUPERVISING_PROFILE)
        }
        mockSupervisionManager.stub {
            on { isSupervisionEnabledForUser(MAIN_USER_ID) } doReturn true
            on { isSupervisionEnabledForUser(SECONDARY_USER_ID) } doReturn true
            on { isSupervisionEnabledForUser(SUPERVISING_USER_ID) } doReturn false
        }

        val result = context.deleteSupervisionData(disableSupervision = true)

        assertThat(result).isFalse()
        verify(mockSupervisionManager, never()).setSupervisionEnabled(any())
        verify(mockSupervisionManager, never()).setSupervisionRecoveryInfo(any())
        verify(mockUserManager, never()).removeUserEvenWhenDisallowed(any<Int>())
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun deleteSupervisionData_secondaryUserSupervised_deletesSupervisionData() {
        mockUserManager.stub {
            on { users } doReturn listOf(MAIN_USER, SECONDARY_USER, SUPERVISING_PROFILE)
            on { removeUserEvenWhenDisallowed(SUPERVISING_USER_ID) } doReturn true
        }
        mockSupervisionManager.stub {
            on { isSupervisionEnabledForUser(MAIN_USER_ID) } doReturn true
            on { isSupervisionEnabledForUser(SECONDARY_USER_ID) } doReturn true
            on { isSupervisionEnabledForUser(SUPERVISING_USER_ID) } doReturn false
        }

        val result = context.deleteSupervisionData(disableSupervision = true)

        assertThat(result).isTrue()
        verify(mockSupervisionManager).setSupervisionEnabled(false)
        verify(mockSupervisionManager).setSupervisionRecoveryInfo(null)
        verify(mockUserManager).removeUserEvenWhenDisallowed(eq(SUPERVISING_USER_ID))
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun deleteSupervisionData_doNotDisableSupervision() {
        mockUserManager.stub {
            on { users } doReturn listOf(MAIN_USER, SECONDARY_USER, SUPERVISING_PROFILE)
            on { removeUserEvenWhenDisallowed(SUPERVISING_USER_ID) } doReturn true
        }
        mockSupervisionManager.stub {
            on { isSupervisionEnabledForUser(MAIN_USER_ID) } doReturn true
            on { isSupervisionEnabledForUser(SECONDARY_USER_ID) } doReturn false
            on { isSupervisionEnabledForUser(SUPERVISING_USER_ID) } doReturn false
        }

        val result = context.deleteSupervisionData(disableSupervision = false)

        assertThat(result).isTrue()
        verify(mockSupervisionManager, never()).setSupervisionEnabled(any())
        verify(mockSupervisionManager).setSupervisionRecoveryInfo(null)
        verify(mockUserManager).removeUserEvenWhenDisallowed(eq(SUPERVISING_USER_ID))
    }

    @Test
    fun hasNecessarySupervisionComponent_defaultPackageName_enabledComponent() {
        val testPackageName = "com.android.supervision"

        mockResources.stub {
            on { getString(com.android.internal.R.string.config_systemSupervision) }
                .thenReturn(testPackageName)
        }
        setUpMessengerServiceComponent(packageName = testPackageName, disabled = false)

        assertThat(context.hasNecessarySupervisionComponent()).isTrue()
    }

    @Test
    fun hasNecessarySupervisionComponent_defaultPackageName_disabledComponent() {
        val testPackageName = "com.android.supervision"

        mockResources.stub {
            on { getString(com.android.internal.R.string.config_systemSupervision) }
                .thenReturn(testPackageName)
        }
        setUpMessengerServiceComponent(packageName = testPackageName, disabled = true)

        assertThat(context.hasNecessarySupervisionComponent()).isFalse()
    }

    @Test
    fun hasNecessarySupervisionComponent_defaultPackageName_disabledComponent_matchAll() {
        val testPackageName = "com.android.supervision"

        mockResources.stub {
            on { getString(com.android.internal.R.string.config_systemSupervision) }
                .thenReturn(testPackageName)
        }
        setUpMessengerServiceComponent(packageName = testPackageName, disabled = true)

        assertThat(context.hasNecessarySupervisionComponent(matchAll = true)).isTrue()
    }

    @Test
    fun hasNecessarySupervisionComponent_usesPackageNameArg_enabledComponent() {
        val testPackageName = "com.android.supervision2"

        setUpMessengerServiceComponent(packageName = testPackageName, disabled = false)

        assertThat(context.hasNecessarySupervisionComponent(testPackageName)).isTrue()
    }

    @Test
    fun hasNecessarySupervisionComponent_usesPackageNameArg_disabledComponent() {
        val testPackageName = "com.android.supervision2"

        setUpMessengerServiceComponent(packageName = testPackageName, disabled = true)

        assertThat(context.hasNecessarySupervisionComponent(testPackageName)).isFalse()
    }

    @Test
    fun hasNecessarySupervisionComponent_usesPackageNameArg_disabledComponent_matchAll() {
        val testPackageName = "com.android.supervision2"

        setUpMessengerServiceComponent(packageName = testPackageName, disabled = true)

        assertThat(context.hasNecessarySupervisionComponent(testPackageName, true)).isTrue()
    }

    @Test
    fun getSupervisionAppInstallActivityInfo() {
        val testPackageName = "com.android.supervision"
        mockResources.stub {
            on { getString(com.android.internal.R.string.config_systemSupervision) }
                .thenReturn(testPackageName)
        }

        setUpInstallSupervisionAppComponent(packageName = testPackageName)

        val info = context.getSupervisionAppInstallActivityInfo()
        assertThat(info).isNotNull()
        assertThat(info?.packageName).isEqualTo(testPackageName)
        assertThat(info?.componentName?.className).isEqualTo("FakeInstallComponent")
    }

    @Test
    fun readSystemSupervisionPackageNameFromResources_returnsPackageName() {
        val testPackageName = "com.android.supervision"
        mockResources.stub {
            on { getString(com.android.internal.R.string.config_systemSupervision) }
                .thenReturn(testPackageName)
        }

        assertThat(context.readSystemSupervisionPackageNameFromResources())
            .isEqualTo(testPackageName)
    }

    @Test
    fun readSystemSupervisionPackageNameFromResources_resourceNotFound_returnsNull() {
        mockResources.stub {
            on { getString(com.android.internal.R.string.config_systemSupervision) }
                .thenThrow(Resources.NotFoundException())
        }

        assertThat(context.readSystemSupervisionPackageNameFromResources()).isNull()
    }

    @Test
    fun readDefaultSupervisionPackageNameFromResources_returnsPackageName() {
        val testComponentName = "com.android.supervision.default/.ProfileOwnerReceiver"
        val expectedPackageName = "com.android.supervision.default"
        mockResources.stub {
            on {
                    getString(
                        com.android.internal.R.string.config_defaultSupervisionProfileOwnerComponent
                    )
                }
                .thenReturn(testComponentName)
        }

        assertThat(context.readDefaultSupervisionPackageNameFromResources())
            .isEqualTo(expectedPackageName)
    }

    @Test
    fun readDefaultSupervisionPackageNameFromResources_resourceNotFound_returnsNull() {
        mockResources.stub {
            on {
                    getString(
                        com.android.internal.R.string.config_defaultSupervisionProfileOwnerComponent
                    )
                }
                .thenThrow(Resources.NotFoundException())
        }

        assertThat(context.readDefaultSupervisionPackageNameFromResources()).isNull()
    }

    @Test
    fun isSupervisionPackageProfileOwner_defaultSupervisionPackageIsProfileOwner_returnsTrue() {
        val systemSupervisionPackageName = "com.android.supervision"
        val defaultComponentString = "com.android.supervision.default/.ProfileOwnerReceiver"
        mockResources.stub {
            on { getString(com.android.internal.R.string.config_systemSupervision) }
                .thenReturn(systemSupervisionPackageName)
            on {
                    getString(
                        com.android.internal.R.string.config_defaultSupervisionProfileOwnerComponent
                    )
                }
                .thenReturn(defaultComponentString)
        }
        shadowDpm.setProfileOwner(ComponentName.unflattenFromString(defaultComponentString))

        assertThat(context.isSupervisionPackageProfileOwner()).isTrue()
    }

    @Test
    fun isSupervisionPackageProfileOwner_systemSupervisionPackageIsProfileOwner_returnsTrue() {
        val systemSupervisionPackageName = "com.android.supervision"
        val defaultComponentString = "com.android.supervision.default/.ProfileOwnerReceiver"
        mockResources.stub {
            on { getString(com.android.internal.R.string.config_systemSupervision) }
                .thenReturn(systemSupervisionPackageName)
            on {
                    getString(
                        com.android.internal.R.string.config_defaultSupervisionProfileOwnerComponent
                    )
                }
                .thenReturn(defaultComponentString)
        }
        val profileOwnerComponent =
            ComponentName(
                systemSupervisionPackageName,
                "com.android.supervision.system.SomeReceiver",
            )
        shadowDpm.setProfileOwner(profileOwnerComponent)

        assertThat(context.isSupervisionPackageProfileOwner()).isTrue()
    }

    @Test
    fun isSupervisionPackageProfileOwner_supervisionPackageIsNotProfileOwner_returnsFalse() {
        val systemSupervisionPackageName = "com.android.supervision"
        val defaultComponentString = "com.android.supervision.default/.ProfileOwnerReceiver"
        mockResources.stub {
            on { getString(com.android.internal.R.string.config_systemSupervision) }
                .thenReturn(systemSupervisionPackageName)
            on {
                    getString(
                        com.android.internal.R.string.config_defaultSupervisionProfileOwnerComponent
                    )
                }
                .thenReturn(defaultComponentString)
        }
        val profileOwnerComponent =
            ComponentName(" com.android.other", "com.android.other.SomeReceiver")
        shadowDpm.setProfileOwner(profileOwnerComponent)

        assertThat(context.isSupervisionPackageProfileOwner()).isFalse()
    }

    @Test
    fun isSupervisionPackageProfileOwner_noProfileOwner_returnsFalse() {
        shadowDpm.setProfileOwner(null)

        assertThat(context.isSupervisionPackageProfileOwner()).isFalse()
    }

    @Test
    fun shouldDisplayPinRecoveryReminders_hasValidRecoveryMethod_returnsFalse() {
        whenever(mockISupervisionManager.hasValidRecoveryMethod(any())).thenReturn(true)

        assertThat(context.shouldDisplayPinRecoveryReminders()).isFalse()
    }

    @Test
    fun shouldDisplayPinRecoveryReminders_noValidRecoveryMethod_returnsTrue() {
        whenever(mockISupervisionManager.hasValidRecoveryMethod(any())).thenReturn(false)

        assertThat(context.shouldDisplayPinRecoveryReminders()).isTrue()
    }

    @Test
    fun canLaunchPinRecovery_whenRecoveryFlowIsLaunchable_returnsTrue() {
        whenever(mockISupervisionManager.canLaunchPinRecovery(any())).thenReturn(true)

        assertThat(context.canLaunchPinRecovery()).isTrue()
    }

    @Test
    fun canLaunchPinRecovery_whenRecoveryFlowIsNotConfigured_returnsFalse() {
        whenever(mockISupervisionManager.canLaunchPinRecovery(any())).thenReturn(false)

        assertThat(context.canLaunchPinRecovery()).isFalse()
    }

    @Test
    fun supervisingUserHandle_hasSupervisingUser_returnsUserHandle() {
        // A supervising user exists in the user list.
        mockUserManager.stub { on { getUsers() } doReturn listOf(MAIN_USER, SUPERVISING_PROFILE) }

        // Verify the UserManager extension function returns the correct handle.
        assertThat(mockUserManager.supervisingUserHandle())
            .isEqualTo(SUPERVISING_PROFILE.userHandle)
        // Verify the Context extension function also returns the correct handle.
        assertThat(context.supervisingUserHandle()).isEqualTo(SUPERVISING_PROFILE.userHandle)
    }

    @Test
    fun supervisingUserHandle_noSupervisingUser_returnsNull() {
        // No supervising user exists in the user list.
        mockUserManager.stub { on { getUsers() } doReturn listOf(MAIN_USER, SECONDARY_USER) }

        // Verify both extension functions return null.
        assertThat(mockUserManager.supervisingUserHandle()).isNull()
        assertThat(context.supervisingUserHandle()).isNull()
    }

    @Test
    fun supervisingUserHandle_noUsers_returnsNull() {
        // The user list is empty.
        mockUserManager.stub { on { getUsers() } doReturn emptyList() }

        // Verify both extension functions return null.
        assertThat(mockUserManager.supervisingUserHandle()).isNull()
        assertThat(context.supervisingUserHandle()).isNull()
    }

    @Test
    fun supervisingUserHandle_nullUserManager_returnsNull() {
        // The UserManager instance itself is null.
        val userManager: UserManager? = null
        assertThat(userManager.supervisingUserHandle()).isNull()
    }

    @Test
    fun isSupervisingCredentialSet_noSupervisingUser_returnsFalse() {
        // No supervising user exists.
        mockUserManager.stub { on { getUsers() } doReturn listOf(MAIN_USER, SECONDARY_USER) }

        // The function should return false.
        assertThat(context.isSupervisingCredentialSet()).isFalse()
    }

    @Test
    fun isSupervisingCredentialSet_keyguardManagerNotAvailable_returnsFalse() {
        // Create a context where KeyguardManager is not available.
        val contextWithNoKeyguardManager =
            contextOf(roleManager = mockRoleManager, keyguardManager = null)
        // A supervising user exists.
        mockUserManager.stub { on { getUsers() } doReturn listOf(SUPERVISING_PROFILE) }

        // The function should return false because the service is unavailable.
        assertThat(contextWithNoKeyguardManager.isSupervisingCredentialSet()).isFalse()
    }

    @Test
    fun isSupervisingCredentialSet_credentialNotSet_returnsFalse() {
        // A supervising user exists.
        mockUserManager.stub { on { getUsers() } doReturn listOf(SUPERVISING_PROFILE) }
        // The device is not secure for that user.
        whenever(mockKeyguardManager.isDeviceSecure(SUPERVISING_USER_ID)).thenReturn(false)

        // The function should return false.
        assertThat(context.isSupervisingCredentialSet()).isFalse()
    }

    @Test
    fun isSupervisingCredentialSet_credentialSet_returnsTrue() {
        // A supervising user exists.
        mockUserManager.stub { on { getUsers() } doReturn listOf(SUPERVISING_PROFILE) }
        // The device is secure for that user.
        whenever(mockKeyguardManager.isDeviceSecure(SUPERVISING_USER_ID)).thenReturn(true)

        // The function should return true.
        assertThat(context.isSupervisingCredentialSet()).isTrue()
    }

    @Test
    fun isSupervisingCredentialSet_withUserHandleArgument_usesArgument() {
        // The device is secure for the supervising user.
        whenever(mockKeyguardManager.isDeviceSecure(SUPERVISING_USER_ID)).thenReturn(true)

        // Call the function with the supervising user handle directly.
        assertThat(context.isSupervisingCredentialSet(SUPERVISING_PROFILE.userHandle)).isTrue()
        // Verify that the provided handle was used and we didn't try to look it up again.
        verify(mockUserManager, never()).getUsers()
    }

    @Test
    fun isSupervisingCredentialSet_withNullUserHandleArgument_returnsFalse() {
        // Call the function with a null user handle.
        assertThat(context.isSupervisingCredentialSet(null)).isFalse()
        // Verify no interactions with KeyguardManager or UserManager occurred.
        verify(mockKeyguardManager, never()).isDeviceSecure(any())
        verify(mockUserManager, never()).getUsers()
    }

    private fun setUpMessengerServiceComponent(packageName: String, disabled: Boolean) {
        val serviceComponentName = ComponentName(packageName, "FakeSupervisionMessengerService")
        val intentFilter =
            IntentFilter(SupervisionMessengerClient.SUPERVISION_MESSENGER_SERVICE_BIND_ACTION)

        if (disabled) {
            packageManager.setComponentEnabledSetting(
                serviceComponentName,
                COMPONENT_ENABLED_STATE_DISABLED,
                DONT_KILL_APP,
            )
        }

        shadowPackageManager.addServiceIfNotPresent(serviceComponentName)
        shadowPackageManager.addIntentFilterForService(serviceComponentName, intentFilter)
    }

    private fun setUpInstallSupervisionAppComponent(packageName: String) {
        val installComponentName = ComponentName(packageName, "FakeInstallComponent")
        val intentFilter = IntentFilter(SupervisionHelper.INSTALL_SUPERVISION_APP_ACTION)

        shadowPackageManager.addActivityIfNotPresent(installComponentName)
        shadowPackageManager.addIntentFilterForActivity(installComponentName, intentFilter)
    }

    private fun contextOf(
        roleManager: RoleManager?,
        keyguardManager: KeyguardManager? = mockKeyguardManager,
    ): Context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getSystemService(name: String): Any? =
                when (name) {
                    ROLE_SERVICE -> roleManager
                    SUPERVISION_SERVICE -> mockSupervisionManager
                    USER_SERVICE -> mockUserManager
                    KEYGUARD_SERVICE -> keyguardManager
                    else -> super.getSystemService(name)
                }

            override fun getResources(): Resources? = mockResources
        }

    companion object {
        private const val MAIN_USER_ID = 0
        private const val SECONDARY_USER_ID = 1
        private const val SUPERVISING_USER_ID = 10
        private val MAIN_USER = UserInfo(MAIN_USER_ID, "Main", null, 0, USER_TYPE_FULL_SYSTEM)
        private val SECONDARY_USER =
            UserInfo(SECONDARY_USER_ID, "Secondary", null, 0, USER_TYPE_FULL_SECONDARY)
        private val SUPERVISING_PROFILE =
            UserInfo(SUPERVISING_USER_ID, "Supervising", null, 0, USER_TYPE_PROFILE_SUPERVISING)
    }
}
