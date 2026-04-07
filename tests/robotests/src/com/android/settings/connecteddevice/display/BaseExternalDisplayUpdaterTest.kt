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

package com.android.settings.connecteddevice.display

import android.app.admin.DevicePolicyIdentifiers
import android.app.admin.DevicePolicyManager
import android.app.admin.DpcAuthority
import android.app.admin.EnforcingAdmin
import android.app.admin.PolicyEnforcementInfo
import android.app.admin.flags.Flags.FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.os.UserHandle
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.SettingsActivity
import com.android.settings.connecteddevice.DevicePreferenceCallback
import com.android.settings.flags.FakeFeatureFlagsImpl
import com.android.settingslib.RestrictedPreference
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
class BaseExternalDisplayUpdaterTest : ExternalDisplayTestBase() {

    @get:Rule val setFlagsRule = SetFlagsRule()

    @Mock private lateinit var callback: DevicePreferenceCallback
    private lateinit var updater: TestBaseExternalDisplayUpdater
    private val flags = FakeFeatureFlagsImpl()

    @Before
    override fun setUp() {
        super.setUp()
        updater = TestBaseExternalDisplayUpdater(callback, /* metricsCategory= */ 0)

        // Use mContext from ExternalDisplayTestBase which is a spy
        updater.initPreference(mContext, mMockedInjector)
        Mockito.doNothing().`when`(mContext).startActivity(safeAny(Intent::class.java))
    }

    @Test
    fun registerCallback_registersListener() {
        updater.registerCallback()
        verify(mMockedInjector).registerDisplayListener(safeAny(DisplayManager.DisplayListener::class.java), anyBoolean())
    }

    @Test
    fun unregisterCallback_unregistersListener() {
        updater.unregisterCallback()
        verify(mMockedInjector).unregisterDisplayListener(safeAny(DisplayManager.DisplayListener::class.java))
    }

    @Test
    fun refreshPreference_postsUpdateRunnable() {
        updater.refreshPreference()

        // The injector's handler is mHandler in ExternalDisplayTestBase
        mHandler.flush()

        // Verify update() was called
        assert(updater.isUpdateCalled)
    }

    @Test
    fun launchSettings_flagEnabled_launchesTabbedFragment() {
        updater.callLaunchSettings(mContext, null)

        val captor = ArgumentCaptor.forClass(Intent::class.java)
        verify(mContext).startActivity(captor.capture())

        val intent = captor.value
        val destination = intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT)
        assert(destination == TabbedDisplayPreferenceFragment::class.java.name)
    }

    @Test
    @EnableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED)
    fun applyUsbDataSignalingPolicy_refactorEnabled_setsDisabledByAdminFromDpm() {
        val admin = EnforcingAdmin("pkg", DpcAuthority.DPC_AUTHORITY, UserHandle.of(0), null)
        val policyInfo = mock(PolicyEnforcementInfo::class.java)
        doReturn(admin).`when`(policyInfo).mostImportantEnforcingAdmin
        doReturn(policyInfo).`when`(mDevicePolicyManager).getEnforcingAdminsForPolicy(
            eq(DevicePolicyIdentifiers.USB_DATA_SIGNALING_POLICY), anyInt()
        )
        doReturn(mDevicePolicyManager).`when`(mContext).getSystemService(DevicePolicyManager::class.java)

        val preference = RestrictedPreference(mContext)
        updater.callApplyUsbDataSignalingPolicy(preference, mContext)

        assert(preference.isDisabledByAdmin)
    }

    @Test
    @DisableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED)
    fun applyUsbDataSignalingPolicy_refactorDisabled_setsDisabledByAdminFromLockUtils() {
        doReturn(false).`when`(mDevicePolicyManager).isUsbDataSignalingEnabled
        val componentName = ComponentName("pkg", "cls")
        doReturn(componentName).`when`(mDevicePolicyManager).getProfileOwnerAsUser(anyInt())

        val preference = RestrictedPreference(mContext)
        updater.callApplyUsbDataSignalingPolicy(preference, mContext)

        assert(preference.isDisabledByAdmin)
    }

    class TestBaseExternalDisplayUpdater(
        callback: DevicePreferenceCallback,
        metricsCategory: Int
    ) : BaseExternalDisplayUpdater(callback, metricsCategory) {
        var isUpdateCalled = false

        override fun update() {
            isUpdateCalled = true
        }

        fun callLaunchSettings(context: Context, args: Bundle?) {
            launchSettings(context, args)
        }

        fun callApplyUsbDataSignalingPolicy(preference: RestrictedPreference, context: Context) {
            applyUsbDataSignalingPolicy(preference, context)
        }
    }

    private fun <T> safeAny(type: Class<T>): T {
        Mockito.any(type)
        return uninitialized()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> uninitialized(): T = null as T
}
