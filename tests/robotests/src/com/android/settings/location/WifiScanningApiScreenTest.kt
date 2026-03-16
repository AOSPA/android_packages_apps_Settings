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

package com.android.settings.location

import android.Manifest.permission.NETWORK_SETTINGS
import android.app.Application
import android.content.Context
import android.net.wifi.WifiManager
import android.os.UserManager
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.FailedPreconditionException
import com.android.settings.testutils2.HardwareUnsupportedException
import com.android.settings.testutils2.MissingPermissionException
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowContextImpl

@RunWith(AndroidJUnit4::class)
@Config(shadows = [SettingsShadowResources::class])
class WifiScanningApiScreenTest {
    private val tester = ApiTester(WifiScanningApiScreen())
    private val context: Context = ApplicationProvider.getApplicationContext()

    @get:Rule val setFlagsRule = SetFlagsRule()

    @Before
    fun setUp() {
        // Establish a valid default environment for testing:
        // 1. Simulate that the device hardware supports Wi-Fi scanning.
        SettingsShadowResources.overrideResource(R.bool.config_show_location_scanning, true)
        // 2. Grant the NETWORK_SETTINGS permission by default.
        Shadows.shadowOf(ApplicationProvider.getApplicationContext<Application>())
            .grantPermissions(NETWORK_SETTINGS)
    }

    @After
    fun tearDown() {
        SettingsShadowResources.reset()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagEnabled_isNotNull() {
        assertThat(tester.getScreen()).isNotNull()
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagDisabled_isNull() {
        assertThat(tester.getScreen()).isNull()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun wifiScanning_set_noPermissions_throwsException() {
        Shadows.shadowOf(ApplicationProvider.getApplicationContext<Application>())
            .denyPermissions(NETWORK_SETTINGS)

        assertFailsWith<MissingPermissionException> { tester.set("wifi_scanning", true) }
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun wifiScanning_get_returnsSystemState() {
        val wifiManager = context.getSystemService(WifiManager::class.java)
        val shadowWifiManager = Shadows.shadowOf(wifiManager)

        shadowWifiManager.setIsScanAlwaysAvailable(true)
        assertThat(tester.get<AnyBoolean>("wifi_scanning")).isEqualTo(true)

        shadowWifiManager.setIsScanAlwaysAvailable(false)
        assertThat(tester.get<AnyBoolean>("wifi_scanning")).isEqualTo(false)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun wifiScanning_set_updatesSystemState() {
        val mockWifiManager = mock(WifiManager::class.java)
        val application = context as Application
        val shadowContext = Shadow.extract<ShadowContextImpl>(application.baseContext)
        shadowContext.setSystemService(Context.WIFI_SERVICE, mockWifiManager)

        tester.set("wifi_scanning", true)
        verify(mockWifiManager).setScanAlwaysAvailable(true)

        tester.set("wifi_scanning", false)
        verify(mockWifiManager).setScanAlwaysAvailable(false)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun wifiScanning_hardwareUnsupported_throwsException() {
        SettingsShadowResources.overrideResource(R.bool.config_show_location_scanning, false)

        assertFailsWith<HardwareUnsupportedException> { tester.get<AnyBoolean>("wifi_scanning") }
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun wifiScanning_set_userRestricted_doesNotUpdateSystemState() {
        val mockWifiManager = mock(WifiManager::class.java)
        val application = context as Application
        val shadowContext = Shadow.extract<ShadowContextImpl>(application.baseContext)
        shadowContext.setSystemService(Context.WIFI_SERVICE, mockWifiManager)

        val userManager = context.getSystemService(UserManager::class.java)
        // Use the deprecated setUserRestriction method because it is the direct way
        // to configure the ShadowUserManager state in Robolectric tests. Using DevicePolicyManager
        // in a unit test adds unnecessary complexity
        @Suppress("DEPRECATION")
        userManager.setUserRestriction(UserManager.DISALLOW_CONFIG_LOCATION, true)

        assertFailsWith<FailedPreconditionException> { tester.set("wifi_scanning", false) }
        verify(mockWifiManager, never()).setScanAlwaysAvailable(anyBoolean())
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun wifiScanning_set_userManagerNull_throwsException() {
        val application = context as Application
        val shadowContext = Shadow.extract<ShadowContextImpl>(application.baseContext)
        shadowContext.setSystemService(Context.USER_SERVICE, null)

        assertFailsWith<IllegalStateException> { tester.set("wifi_scanning", true) }
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun wifiScanning_set_wifiManagerNull_throwsException() {
        val application = context as Application
        val shadowContext = Shadow.extract<ShadowContextImpl>(application.baseContext)
        shadowContext.setSystemService(Context.WIFI_SERVICE, null)

        assertFailsWith<IllegalStateException> { tester.set("wifi_scanning", true) }
    }
}
