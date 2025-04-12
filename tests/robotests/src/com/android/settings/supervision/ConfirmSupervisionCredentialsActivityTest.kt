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
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
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

    private lateinit var mActivity: ConfirmSupervisionCredentialsActivity

    private val callingPackage = "com.example.caller"

    @Before
    fun setUp() {
        mActivity =
            spy(
                Robolectric.buildActivity(ConfirmSupervisionCredentialsActivity::class.java).get()
            ) {
                on { getSystemService(RoleManager::class.java) } doReturn mockRoleManager
                on { callingPackage } doReturn callingPackage
            }
    }

    @Test
    fun onCreate_noRequiredPermission_finish() {
        whenever(mActivity.checkCallingOrSelfPermission(any())).thenReturn(PackageManager.PERMISSION_DENIED)

        mActivity.onCreate(null)

        verify(mActivity).setResult(Activity.RESULT_CANCELED)
        verify(mActivity).finish()
    }

    @Test
    fun onCreate_callerHasSupervisionRole_doesNotFinish() {
        whenever(mActivity.checkCallingOrSelfPermission(any())).thenReturn(PackageManager.PERMISSION_GRANTED)
        whenever(mockRoleManager.getRoleHolders(any())).thenReturn(listOf(callingPackage))

        mActivity.onCreate(null)

        verify(mActivity, never()).finish()
    }

    @Test
    fun onCreate_callerNotHasSupervisionRole_finish() {
        val otherPackage = "com.example.other"
        whenever(mockRoleManager.getRoleHolders(any())).thenReturn(listOf(otherPackage))
        whenever(mActivity.checkCallingOrSelfPermission(any())).thenReturn(PackageManager.PERMISSION_GRANTED)

        mActivity.onCreate(null)

        verify(mActivity).setResult(Activity.RESULT_CANCELED)
        verify(mActivity).finish()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.BAKLAVA])
    fun onCreate_callerIsSystemUid_doesNotFinish() {
        whenever(mActivity.checkCallingOrSelfPermission(any())).thenReturn(PackageManager.PERMISSION_GRANTED)
        ShadowBinder.setCallingUid(Process.SYSTEM_UID)

        mActivity.onCreate(null)

        verify(mActivity, never()).finish()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.BAKLAVA])
    fun onCreate_callerIsUnknownUid_finish() {
        whenever(mActivity.checkCallingOrSelfPermission(any())).thenReturn(PackageManager.PERMISSION_GRANTED)
        ShadowBinder.setCallingUid(Process.NOBODY_UID)

        mActivity.onCreate(null)

        verify(mActivity).setResult(Activity.RESULT_CANCELED)
        verify(mActivity).finish()
    }
}
