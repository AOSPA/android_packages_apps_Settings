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

package com.android.settings.wifi

import android.Manifest.permission.NETWORK_SETTINGS
import android.Manifest.permission.NETWORK_SETUP_WIZARD
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.HardwareUnsupportedException
import com.android.settings.testutils2.InvalidPreferenceException
import com.android.settings.testutils2.MissingPermissionException
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean
import com.android.wifitrackerlib.WifiEntry
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import java.util.function.Consumer
import kotlin.test.assertFailsWith
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowNetworkCapabilities
import org.robolectric.shadows.ShadowNetworkInfo
import org.robolectric.shadows.ShadowWifiManager

@RunWith(AndroidJUnit4::class)
@Config(
    shadows =
        [
            ConfigureWifiApiScreenTest.ShadowWifiManagerExtension::class,
            ConfigureWifiApiScreenTest.ShadowWifiUtils::class,
        ]
)
class ConfigureWifiApiScreenTest {
    private val tester = ApiTester(ConfigureWifiApiScreen())
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val openNetworkNotifierHelper = OpenNetworkNotifierHelper.getInstance(context)
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val shadowWifiManager: ShadowWifiManagerExtension
        get() = Shadows.shadowOf(wifiManager) as ShadowWifiManagerExtension

    @get:Rule val setFlagsRule = SetFlagsRule()

    @Implements(WifiUtils::class)
    class ShadowWifiUtils {
        companion object {
            var isMultiuserEnabled = false

            @Implementation
            @JvmStatic
            fun isWifiMultiuserEnabled(): Boolean {
                return isMultiuserEnabled
            }
        }
    }

    @Implements(WifiManager::class)
    class ShadowWifiManagerExtension : ShadowWifiManager() {
        private var isNotifierEnabled = false
        private var wepSupported = true
        private var wepAllowed = false

        @Implementation
        fun isOpenNetworkNotifierEnabled(executor: Executor, callback: Consumer<Boolean>) {
            executor.execute { callback.accept(isNotifierEnabled) }
        }

        @Implementation
        fun setOpenNetworkNotifierEnabled(enabled: Boolean) {
            isNotifierEnabled = enabled
        }

        @Implementation
        fun isWepSupported(): Boolean {
            return wepSupported
        }

        fun setWepSupported(supported: Boolean) {
            wepSupported = supported
        }

        @Implementation
        fun queryWepAllowed(executor: Executor, callback: Consumer<Boolean>) {
            executor.execute { callback.accept(wepAllowed) }
        }

        @Implementation
        fun setWepAllowed(allowed: Boolean) {
            wepAllowed = allowed
        }

        fun getWepAllowed(): Boolean {
            return wepAllowed
        }
    }

    private fun grantPermission(permission: String) {
        val application = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(application).grantPermissions(permission)
    }

    @Before
    fun setUp() {
        val application: Application = ApplicationProvider.getApplicationContext()
        // Reset permissions before each test
        Shadows.shadowOf(application).denyPermissions(NETWORK_SETTINGS, NETWORK_SETUP_WIZARD)
        ShadowWifiUtils.isMultiuserEnabled = false
        shadowWifiManager.setWepSupported(true)
        shadowWifiManager.setWepAllowed(false)
        shadowWifiManager.setConnectionInfo(null)
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
    fun notifyOpenNetworksPreference_set_noPermissions_throwsException() {
        assertFailsWith<MissingPermissionException> {
            tester.set("wifi_notify_open_networks", true)
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun notifyOpenNetworksPreference_set_onlyNetworkSettings_succeeds() {
        grantPermission(NETWORK_SETTINGS)

        tester.set("wifi_notify_open_networks", false)

        assertThat(tester.get<AnyBoolean>("wifi_notify_open_networks")).isEqualTo(false)

        tester.set("wifi_notify_open_networks", true)

        assertThat(tester.get<AnyBoolean>("wifi_notify_open_networks")).isEqualTo(true)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun notifyOpenNetworksPreference_set_onlyNetworkSetupWizard_succeeds() {
        grantPermission(NETWORK_SETUP_WIZARD)

        tester.set("wifi_notify_open_networks", false)

        assertThat(tester.get<AnyBoolean>("wifi_notify_open_networks")).isEqualTo(false)

        tester.set("wifi_notify_open_networks", true)

        assertThat(tester.get<AnyBoolean>("wifi_notify_open_networks")).isEqualTo(true)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun notifyOpenNetworksPreference_set_multiuserEnabled_succeeds() {
        grantPermission(NETWORK_SETTINGS)
        ShadowWifiUtils.isMultiuserEnabled = true

        tester.set("wifi_notify_open_networks", true)

        assertThat(tester.get<AnyBoolean>("wifi_notify_open_networks")).isEqualTo(true)

        tester.set("wifi_notify_open_networks", false)

        assertThat(tester.get<AnyBoolean>("wifi_notify_open_networks")).isEqualTo(false)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun allowWepNetworksPreference_get_noPermissions_throwsException() {
        assertFailsWith<MissingPermissionException> {
            tester.get<AnyBoolean>("wifi_allow_wep_networks")
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun allowWepNetworksPreference_set_noPermissions_throwsException() {
        assertFailsWith<MissingPermissionException> { tester.set("wifi_allow_wep_networks", true) }
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun allowWepNetworksPreference_get_wepNotSupported() {
        grantPermission(NETWORK_SETTINGS)
        shadowWifiManager.setWepSupported(false)

        val e =
            assertFailsWith<HardwareUnsupportedException> {
                tester.set("wifi_allow_wep_networks", true)
            }
        assertThat(e.reason)
            .contains(context.getString(R.string.wifi_allow_wep_networks_precondition))
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun allowWepNetworksPreference_get_wepAllowed_returnsTrue() {
        grantPermission(NETWORK_SETTINGS)
        shadowWifiManager.setWepAllowed(true)

        assertThat(tester.get<AnyBoolean>("wifi_allow_wep_networks")).isEqualTo(true)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun allowWepNetworksPreference_get_wepNotAllowed_returnsFalse() {
        grantPermission(NETWORK_SETTINGS)
        assertThat(tester.get<AnyBoolean>("wifi_allow_wep_networks")).isEqualTo(false)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun allowWepNetworksPreference_setTrue_withPermission_succeeds() {
        grantPermission(NETWORK_SETTINGS)
        tester.set("wifi_allow_wep_networks", true)

        assertThat(shadowWifiManager.getWepAllowed()).isTrue()
        assertThat(tester.get<AnyBoolean>("wifi_allow_wep_networks")).isEqualTo(true)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun allowWepNetworksPreference_setFalse_withPermission_succeeds() {
        grantPermission(NETWORK_SETTINGS)
        tester.set("wifi_allow_wep_networks", false)

        assertThat(shadowWifiManager.getWepAllowed()).isFalse()
        assertThat(tester.get<AnyBoolean>("wifi_allow_wep_networks")).isEqualTo(false)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun allowWepNetworksPreference_setFalse_connectedToWep_throwsException() {
        grantPermission(NETWORK_SETTINGS)

        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        val shadowConnectivityManager = Shadows.shadowOf(connectivityManager)

        val mockWifiInfo = mock<WifiInfo>()
        whenever(mockWifiInfo.currentSecurityType).thenReturn(WifiEntry.SECURITY_WEP)

        val networkCapabilities = ShadowNetworkCapabilities.newInstance()
        Shadows.shadowOf(networkCapabilities).addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        Shadows.shadowOf(networkCapabilities).setTransportInfo(mockWifiInfo)

        val wifiNetworkInfo =
            ShadowNetworkInfo.newInstance(
                null,
                ConnectivityManager.TYPE_WIFI,
                0, // subType
                true, // isAvailable
                NetworkInfo.State.CONNECTED,
            )
        shadowConnectivityManager.setActiveNetworkInfo(wifiNetworkInfo)
        val activeNetwork = connectivityManager.activeNetwork
        assertThat(activeNetwork).isNotNull()

        shadowConnectivityManager.setNetworkCapabilities(activeNetwork, networkCapabilities)

        val e =
            assertFailsWith<InvalidPreferenceException> {
                tester.set("wifi_allow_wep_networks", false)
            }
        assertThat(e.reason)
            .contains(context.getString(R.string.wifi_allow_wep_networks_set_invalid_preference))
    }
}
