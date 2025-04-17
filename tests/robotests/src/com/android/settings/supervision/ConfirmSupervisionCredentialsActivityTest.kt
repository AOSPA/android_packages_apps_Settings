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

import android.app.Activity
import android.app.role.RoleManager
import android.content.pm.UserInfo
import android.os.Build
import android.os.Process
import android.os.UserManager
import android.os.UserManager.USER_TYPE_PROFILE_SUPERVISING
import android.os.UserManager.USER_TYPE_PROFILE_TEST
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBinder

@RunWith(RobolectricTestRunner::class)
class ConfirmSupervisionCredentialsActivityTest {
    private val mockRoleManager = mock<RoleManager>()
    private val mockUserManager = mock<UserManager>()

    private lateinit var mActivity: ConfirmSupervisionCredentialsActivity

    private val callingPackage = "com.example.caller"

    @Before
    fun setUp() {
        SupervisionHelper.sInstance = null
        mActivity =
            spy(
                Robolectric.buildActivity(ConfirmSupervisionCredentialsActivity::class.java).get()
            ) {
                on { getSystemService(RoleManager::class.java) } doReturn mockRoleManager
                on { getSystemService(UserManager::class.java) } doReturn mockUserManager
                on { callingPackage } doReturn callingPackage
            }
    }

    @Test
    fun onCreate_callerHasSupervisionRole_doesNotFinish() {
        whenever(mockRoleManager.getRoleHolders(any())).thenReturn(listOf(callingPackage))
        whenever(mockUserManager.users).thenReturn(listOf(SUPERVISING_USER_INFO))

        mActivity.onCreate(null)

        verify(mActivity, never()).finish()
    }

    @Test
    fun onCreate_callerNotHasSupervisionRole_finish() {
        val otherPackage = "com.example.other"
        whenever(mockRoleManager.getRoleHolders(any())).thenReturn(listOf(otherPackage))

        mActivity.onCreate(null)

        verify(mActivity).setResult(Activity.RESULT_CANCELED)
        verify(mActivity).finish()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.BAKLAVA])
    fun onCreate_callerIsSystemUid_doesNotFinish() {
        ShadowBinder.setCallingUid(Process.SYSTEM_UID)
        whenever(mockUserManager.users).thenReturn(listOf(SUPERVISING_USER_INFO))

        mActivity.onCreate(null)

        verify(mActivity, never()).finish()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.BAKLAVA])
    fun onCreate_callerIsUnknownUid_finish() {
        ShadowBinder.setCallingUid(Process.NOBODY_UID)

        mActivity.onCreate(null)

        verify(mActivity).setResult(Activity.RESULT_CANCELED)
        verify(mActivity).finish()
    }

    @Test
    fun onCreate_noSupervisingCredential_finish() {
        whenever(mockRoleManager.getRoleHolders(any())).thenReturn(listOf(callingPackage))
        whenever(mockUserManager.users).thenReturn(listOf(TESTING_USER_INFO))

        mActivity.onCreate(null)

        verify(mActivity).setResult(Activity.RESULT_CANCELED)
        verify(mActivity).finish()
    }

    private companion object {
        const val SUPERVISING_USER_ID = 5
        val SUPERVISING_USER_INFO =
            UserInfo(
                SUPERVISING_USER_ID,
                /* name */ "supervising",
                /* iconPath */ "",
                /* flags */ 0,
                USER_TYPE_PROFILE_SUPERVISING,
            )
        const val TESTING_USER_ID = 6
        val TESTING_USER_INFO =
            UserInfo(
                TESTING_USER_ID,
                /* name */ "testing",
                /* iconPath */ "",
                /* flags */ 0,
                USER_TYPE_PROFILE_TEST,
            )
    }
}
