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
import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class SupervisionHelperTest {

    private val mockRoleManager = mock<RoleManager>()
    private var context = contextOfRoleManager(mockRoleManager)

    @Test
    fun systemSupervisionPackageName_roleManagerReturnsPackageName_shouldReturnPackageName() {
        val testPackageName = "com.android.supervision"
        mockRoleManager.stub {
            on { getRoleHolders(RoleManager.ROLE_SYSTEM_SUPERVISION) } doReturn
                listOf(testPackageName)
        }

        assertThat(context.systemSupervisionPackageName).isEqualTo(testPackageName)
        verify(mockRoleManager).getRoleHolders(RoleManager.ROLE_SYSTEM_SUPERVISION)
    }

    @Test
    fun systemSupervisionPackageName_roleManagerReturnsEmptyList_shouldReturnNull() {
        mockRoleManager.stub {
            on { getRoleHolders(RoleManager.ROLE_SYSTEM_SUPERVISION) } doReturn emptyList()
        }

        assertThat(context.systemSupervisionPackageName).isNull()
        verify(mockRoleManager).getRoleHolders(RoleManager.ROLE_SYSTEM_SUPERVISION)
    }

    @Test
    fun systemSupervisionPackageName_roleManagerReturnsNull_shouldReturnNull() {
        context = contextOfRoleManager(null)

        assertThat(context.systemSupervisionPackageName).isNull()
    }

    @Test
    fun supervisionRoleHolders_roleManagerReturnsPackageName_shouldReturnPackageNames() {
        val fooPackageName = "com.android.foo"
        val barPackageName = "com.android.bar"
        mockRoleManager.stub {
            on { getRoleHolders(RoleManager.ROLE_SUPERVISION) } doReturn
                listOf(fooPackageName, barPackageName)
        }

        assertThat(context.supervisionRoleHolders).containsExactly(fooPackageName, barPackageName)
        verify(mockRoleManager).getRoleHolders(RoleManager.ROLE_SUPERVISION)
    }

    fun supervisionRoleHolders_roleManagerUnavailable_shouldReturnEmptyList() {
        context = contextOfRoleManager(null)

        assertThat(context.supervisionRoleHolders).isEmpty()
    }

    private fun contextOfRoleManager(roleManager: RoleManager?): Context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getSystemService(name: String): Any? =
                when (name) {
                    ROLE_SERVICE -> roleManager
                    else -> super.getSystemService(name)
                }
        }
}
