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

package com.android.settings.wifi.repository

import android.content.Context
import android.content.pm.PackageManager
import android.net.NetworkPolicyManager
import android.os.Process
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class DataUsageRepositoryTest {

    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @Mock private lateinit var packageManager: PackageManager

    @Mock private lateinit var policyManager: NetworkPolicyManager

    private lateinit var context: Context
    private lateinit var repository: DataUsageRepository

    private val packageName = "com.android.settings.test"
    private val uid = 10001
    private val policyReject = NetworkPolicyManager.POLICY_REJECT_METERED_BACKGROUND
    private val policyAllow = NetworkPolicyManager.POLICY_ALLOW_METERED_BACKGROUND
    private val policyNone = NetworkPolicyManager.POLICY_NONE

    @Before
    fun setUp() {
        context = spy(ApplicationProvider.getApplicationContext<Context>())
        context.stub {
            on { applicationContext } doReturn context
            on { packageManager } doReturn packageManager
            on { getSystemService(NetworkPolicyManager::class.java) } doReturn policyManager
        }

        repository = DataUsageRepository(context)
        packageManager.stub { on { getPackageUid(packageName, 0) } doReturn uid }
        policyManager.stub { on { getUidPolicy(uid) } doReturn policyNone }
    }

    @Test
    fun getPackageUid_validPackage_returnsUid() = runTest {
        packageManager.stub { on { getPackageUid(packageName, 0) } doReturn uid }

        val result = repository.getPackageUid(packageName)

        assertThat(result).isEqualTo(uid)
    }

    @Test
    fun getPackageUid_exception_returnsInvalidUid() = runTest {
        packageManager.stub {
            on { getPackageUid(packageName, 0) } doThrow PackageManager.NameNotFoundException()
        }

        val result = repository.getPackageUid(packageName)

        assertThat(result).isEqualTo(Process.INVALID_UID)
    }

    @Test
    fun isPolicyReject_policyReject_returnsTrue() = runTest {
        policyManager.stub { on { getUidPolicy(uid) } doReturn policyReject }

        val result = repository.isPolicyReject(packageName)

        assertThat(result).isTrue()
    }

    @Test
    fun isPolicyReject_invalidUid_returnsFalse() {
        packageManager.stub { on { getPackageUid(packageName, 0) } doReturn Process.INVALID_UID }

        val result = repository.isPolicyReject(uid)

        assertThat(result).isFalse()
    }

    @Test
    fun setPolicyReject_valueIsTrue_setUidPolicyReject() = runTest {
        packageManager.stub { on { getPackageUid(packageName, 0) } doReturn uid }

        repository.setPolicyReject(packageName, true)

        verify(policyManager).setUidPolicy(uid, policyReject)
    }

    @Test
    fun setPolicyReject_valueIsFalse_setUidPolicyNone() = runTest {
        packageManager.stub { on { getPackageUid(packageName, 0) } doReturn uid }

        repository.setPolicyReject(packageName, false)

        verify(policyManager).setUidPolicy(uid, policyNone)
    }

    @Test
    fun isPolicyAllowAvailable_policyNone_returnsTrue() = runTest {
        packageManager.stub { on { getPackageUid(packageName, 0) } doReturn uid }
        policyManager.stub { on { getUidPolicy(uid) } doReturn policyNone }

        val result = repository.isPolicyAllowAvailable(packageName)

        assertThat(result).isTrue()
    }

    @Test
    fun isPolicyAllowAvailable_policyReject_returnsFalse() = runTest {
        packageManager.stub { on { getPackageUid(packageName, 0) } doReturn uid }
        policyManager.stub { on { getUidPolicy(uid) } doReturn policyReject }

        val result = repository.isPolicyAllowAvailable(packageName)

        assertThat(result).isFalse()
    }

    @Test
    fun isPolicyAllow_notAvailable_returnsFalse() {
        policyManager.stub { on { getUidPolicy(uid) } doReturn policyReject }

        val result = repository.isPolicyAllow(uid)

        assertThat(result).isFalse()
    }

    @Test
    fun isPolicyAllow_noPolicyAllow_returnsFalse() {
        policyManager.stub { on { getUidPolicy(uid) } doReturn policyNone }

        val result = repository.isPolicyAllow(uid)

        assertThat(result).isFalse()
    }

    @Test
    fun isPolicyAllow_policyAllow_returnsTrue() {
        policyManager.stub { on { getUidPolicy(uid) } doReturn policyAllow }

        val result = repository.isPolicyAllow(uid)

        assertThat(result).isTrue()
    }

    @Test
    fun setPolicyAllow_unavailable_doesNothing() = runTest {
        policyManager.stub { on { getUidPolicy(uid) } doReturn policyReject }

        repository.setPolicyAllow(uid, true)

        verify(policyManager, never()).setUidPolicy(anyInt(), anyInt())
    }

    @Test
    fun setPolicyAllow_valueIsTrue_setUidPolicyAllow() = runTest {
        policyManager.stub { on { getUidPolicy(uid) } doReturn policyNone }

        repository.setPolicyAllow(uid, true)

        verify(policyManager).setUidPolicy(uid, policyAllow)
    }

    @Test
    fun setPolicyAllow_valueIsFalse_setUidPolicyNone() = runTest {
        policyManager.stub { on { getUidPolicy(uid) } doReturn policyNone }

        repository.setPolicyAllow(uid, false)

        verify(policyManager).setUidPolicy(uid, policyNone)
    }

    @Test
    fun getPolicy_invalidUid_doNothing() {
        val result = repository.getPolicy(Process.INVALID_UID)

        assertThat(result).isEqualTo(policyNone)
        verify(policyManager, never()).getUidPolicy(anyInt())
    }

    @Test
    fun getPolicy_policyIsNone_returnspolicyNone() {
        policyManager.stub { on { getUidPolicy(uid) } doReturn policyNone }

        val result = repository.getPolicy(uid)

        assertThat(result).isEqualTo(policyNone)
        verify(policyManager).getUidPolicy(uid)
    }

    @Test
    fun getPolicy_policyIsReject_returnsPolicyReject() {
        policyManager.stub { on { getUidPolicy(uid) } doReturn policyReject }

        val result = repository.getPolicy(uid)

        assertThat(result).isEqualTo(policyReject)
        verify(policyManager).getUidPolicy(uid)
    }

    @Test
    fun getPolicy_policyIsAllow_returnsPolicyAllow() {
        policyManager.stub { on { getUidPolicy(uid) } doReturn policyAllow }

        val result = repository.getPolicy(uid)

        assertThat(result).isEqualTo(policyAllow)
        verify(policyManager).getUidPolicy(uid)
    }

    @Test
    fun setPolicy_invalidUid_doNothing() = runTest {
        repository.setPolicy(Process.INVALID_UID, policyReject)

        verify(policyManager, never()).setUidPolicy(anyInt(), anyInt())
    }

    @Test
    fun setPolicy_policyIsNone_setUidPolicyNone() = runTest {
        repository.setPolicy(uid, policyNone)

        verify(policyManager).setUidPolicy(uid, policyNone)
    }

    @Test
    fun setPolicy_policyIsReject_setUidPolicyReject() = runTest {
        repository.setPolicy(uid, policyReject)

        verify(policyManager).setUidPolicy(uid, policyReject)
    }

    @Test
    fun setPolicy_policyIsAllow_setUidPolicyAllow() = runTest {
        repository.setPolicy(uid, policyAllow)

        verify(policyManager).setUidPolicy(uid, policyAllow)
    }
}
