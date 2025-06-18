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
package com.android.settings.supervision

import android.app.role.RoleManager
import android.app.supervision.SupervisionManager
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
import android.content.pm.PackageManager.DONT_KILL_APP
import android.content.pm.UserInfo
import android.os.UserHandle
import android.os.UserManager
import android.os.UserManager.USER_TYPE_FULL_SECONDARY
import android.os.UserManager.USER_TYPE_FULL_SYSTEM
import android.os.UserManager.USER_TYPE_PROFILE_SUPERVISING
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.supervision.ipc.SupervisionMessengerClient
import com.google.common.truth.Truth.assertThat
import kotlin.collections.listOf
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowPackageManager

@RunWith(AndroidJUnit4::class)
class SupervisionHelperTest {

    private val mockRoleManager = mock<RoleManager>()
    private val mockSupervisionManager = mock<SupervisionManager>()
    private val mockUserManager = mock<UserManager>()
    private val context = contextOf(mockRoleManager, mockUserManager)

    private lateinit var packageManager: PackageManager
    private lateinit var shadowPackageManager: ShadowPackageManager

    @Before
    fun setup() {
        val applicationContext = ApplicationProvider.getApplicationContext<Context>()
        packageManager = applicationContext.packageManager
        shadowPackageManager = shadowOf(packageManager)
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
        val context = contextOf(/* roleManager= */ null, /* userManager= */ null)

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
        val context = contextOf(/* roleManager= */ null, /* userManager= */ null)

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
            on { removeUser(UserHandle(SUPERVISING_USER_ID)) } doReturn true
        }
        mockSupervisionManager.stub {
            on { isSupervisionEnabledForUser(MAIN_USER_ID) } doReturn true
            on { isSupervisionEnabledForUser(SECONDARY_USER_ID) } doReturn false
            on { isSupervisionEnabledForUser(SUPERVISING_USER_ID) } doReturn false
        }

        val result = context.deleteSupervisionData()

        assertThat(result).isTrue()
        verify(mockSupervisionManager).setSupervisionEnabled(false)
        verify(mockSupervisionManager).setSupervisionRecoveryInfo(null)
        verify(mockUserManager).removeUser(eq(UserHandle(SUPERVISING_USER_ID)))
    }

    @Test
    fun deleteSupervisionData_secondaryUserSupervised_keepsSupervisionData() {
        mockUserManager.stub {
            on { users } doReturn listOf(MAIN_USER, SECONDARY_USER, SUPERVISING_PROFILE)
        }
        mockSupervisionManager.stub {
            on { isSupervisionEnabledForUser(MAIN_USER_ID) } doReturn true
            on { isSupervisionEnabledForUser(SECONDARY_USER_ID) } doReturn true
            on { isSupervisionEnabledForUser(SUPERVISING_USER_ID) } doReturn false
        }

        val result = context.deleteSupervisionData()

        assertThat(result).isFalse()
        verify(mockSupervisionManager, never()).setSupervisionEnabled(any())
        verify(mockSupervisionManager, never()).setSupervisionRecoveryInfo(any())
        verify(mockUserManager, never()).removeUser(any<UserHandle>())
    }

    @Test
    fun hasNecessarySupervisionComponent_defaultPackageName_enabledComponent() {
        val testPackageName = "com.android.supervision"

        mockRoleManager.stub {
            on { getRoleHolders(RoleManager.ROLE_SYSTEM_SUPERVISION) } doReturn
                listOf(testPackageName)
        }
        setUpMessengerServiceComponent(packageName = testPackageName, disabled = false)

        assertThat(context.hasNecessarySupervisionComponent()).isTrue()
        verify(mockRoleManager).getRoleHolders(RoleManager.ROLE_SYSTEM_SUPERVISION)
    }

    @Test
    fun hasNecessarySupervisionComponent_defaultPackageName_disabledComponent() {
        val testPackageName = "com.android.supervision"

        mockRoleManager.stub {
            on { getRoleHolders(RoleManager.ROLE_SYSTEM_SUPERVISION) } doReturn
                listOf(testPackageName)
        }
        setUpMessengerServiceComponent(packageName = testPackageName, disabled = true)

        assertThat(context.hasNecessarySupervisionComponent()).isFalse()
        verify(mockRoleManager).getRoleHolders(RoleManager.ROLE_SYSTEM_SUPERVISION)
    }

    @Test
    fun hasNecessarySupervisionComponent_defaultPackageName_disabledComponent_matchAll() {
        val testPackageName = "com.android.supervision"

        mockRoleManager.stub {
            on { getRoleHolders(RoleManager.ROLE_SYSTEM_SUPERVISION) } doReturn
                listOf(testPackageName)
        }
        setUpMessengerServiceComponent(packageName = testPackageName, disabled = true)

        assertThat(context.hasNecessarySupervisionComponent(matchAll = true)).isTrue()
        verify(mockRoleManager).getRoleHolders(RoleManager.ROLE_SYSTEM_SUPERVISION)
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

    private fun contextOf(roleManager: RoleManager?, userManager: UserManager?): Context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getSystemService(name: String): Any? =
                when (name) {
                    ROLE_SERVICE -> roleManager
                    SUPERVISION_SERVICE -> mockSupervisionManager
                    USER_SERVICE -> mockUserManager
                    else -> super.getSystemService(name)
                }
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
