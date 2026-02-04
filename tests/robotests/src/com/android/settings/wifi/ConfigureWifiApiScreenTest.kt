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

import android.Manifest
import android.app.Application
import android.content.Context
import android.net.wifi.WifiManager
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.flags.Flags
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.MissingPermissionException
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import java.util.function.Consumer
import kotlin.test.assertFailsWith
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowWifiManager

@RunWith(AndroidJUnit4::class)
@Config(shadows = [ConfigureWifiApiScreenTest.ShadowWifiManagerExtension::class])
class ConfigureWifiApiScreenTest {
    private val tester = ApiTester(ConfigureWifiApiScreen())
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val openNetworkNotifierHelper = OpenNetworkNotifierHelper.getInstance(context)
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val directExecutor = Executor { it.run() }

    @get:Rule val setFlagsRule = SetFlagsRule()

    @Implements(WifiManager::class)
    class ShadowWifiManagerExtension : ShadowWifiManager() {
        private var isNotifierEnabled = false

        @Implementation
        fun isOpenNetworkNotifierEnabled(executor: Executor, callback: Consumer<Boolean>) {
            executor.execute { callback.accept(isNotifierEnabled) }
        }

        @Implementation
        fun setOpenNetworkNotifierEnabled(enabled: Boolean) {
            isNotifierEnabled = enabled
        }
    }

    private fun setNotifierEnabled(enabled: Boolean) {
        openNetworkNotifierHelper.setEnabled(wifiManager, directExecutor, enabled)
    }

    @Before
    fun setUp() {
        val application: Application = ApplicationProvider.getApplicationContext()
        // Reset permissions before each test
        Shadows.shadowOf(application)
            .denyPermissions(
                Manifest.permission.NETWORK_SETTINGS,
                Manifest.permission.NETWORK_SETUP_WIZARD,
            )
        setNotifierEnabled(false)
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
    fun notifyOpenNetworksPreference_get_whenEnabled_noPermissionsNeeded() {
        setNotifierEnabled(true)
        assertThat(tester.get<AnyBoolean>("wifi_notify_open_networks")).isEqualTo(true)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun notifyOpenNetworksPreference_get_whenDisabled_noPermissionsNeeded() {
        assertThat(tester.get<AnyBoolean>("wifi_notify_open_networks")).isEqualTo(false)
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
        val application: Application = ApplicationProvider.getApplicationContext()
        Shadows.shadowOf(application).grantPermissions(Manifest.permission.NETWORK_SETTINGS)

        tester.set("wifi_notify_open_networks", true)
        assertThat(tester.get<AnyBoolean>("wifi_notify_open_networks")).isEqualTo(true)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun notifyOpenNetworksPreference_set_onlyNetworkSetupWizard_succeeds() {
        val application: Application = ApplicationProvider.getApplicationContext()
        Shadows.shadowOf(application).grantPermissions(Manifest.permission.NETWORK_SETUP_WIZARD)
        setNotifierEnabled(true)

        tester.set("wifi_notify_open_networks", false)
        assertThat(tester.get<AnyBoolean>("wifi_notify_open_networks")).isEqualTo(false)
    }
}
