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

package com.android.settings

import android.app.privatecompute.flags.Flags as PccFlags
import android.content.Context
import android.content.pm.PackageManager
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

@RunWith(AndroidJUnit4::class)
class PccAwareUidComparatorTest {

    @get:Rule val mSetFlagsRule = SetFlagsRule()

    @Mock private lateinit var mContext: Context

    @Mock private lateinit var mPackageManager: PackageManager

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        `when`(mContext.packageManager).thenReturn(mPackageManager)
    }

    @Test
    fun isSameApp_sameUid_returnsTrue() {
        val uid = 12345
        assertThat(PccAwareUidComparator.isSameApp(mContext, uid, uid)).isTrue()
    }

    @Test
    fun isSameApp_differentUid_returnsFalse() {
        val uid1 = 12345
        val uid2 = 67890
        assertThat(PccAwareUidComparator.isSameApp(mContext, uid1, uid2)).isFalse()
    }

    @Test
    fun isSameApp_pccFlagDisabled_returnsStandardComparison() {
        mSetFlagsRule.disableFlags(PccFlags.FLAG_ENABLE_PCC_FRAMEWORK_SUPPORT)
        val uid = 12345

        assertThat(PccAwareUidComparator.isSameApp(mContext, uid, uid)).isTrue()
    }

    @Test
    fun isSameApp_pccFlagEnabled_invalidMapping_returnsTrueForSameUid() {
        mSetFlagsRule.enableFlags(PccFlags.FLAG_ENABLE_PCC_FRAMEWORK_SUPPORT)
        val uid = 12345
        // This test might not hit the branch if Process.isPrivateComputeCoreUid(uid) is false,
        // but it still verifies that same UID returns true.
        `when`(mPackageManager.getAppUidForPrivateComputeCoreUid(uid)).thenReturn(-1)

        assertThat(PccAwareUidComparator.isSameApp(mContext, uid, uid)).isTrue()
    }
}
