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

package com.android.settings.deviceinfo.aboutphone

import android.Manifest.permission.BLUETOOTH_ADMIN
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.NETWORK_SETTINGS
import android.Manifest.permission.OVERRIDE_WIFI_CONFIG
import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.app.Application
import android.content.Context
import android.net.wifi.SoftApConfiguration
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Looper
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings.Global.DEVICE_NAME
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.FailedPreconditionException
import com.android.settings.testutils2.MissingPermissionException
import com.android.settingslib.datastore.SettingsGlobalStore
import com.android.settingslib.utils.StringUtil
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import kotlin.test.assertFailsWith
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.util.ReflectionHelpers

@Config(shadows = [SettingsShadowResources::class])
@RunWith(AndroidJUnit4::class)
class MyDeviceInfoApiFirstScreenTest {

    @get:Rule val setFlagsRule = SetFlagsRule()

    private val tester = ApiTester(MyDeviceInfoApiFirstScreen())
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val shadowApplication = shadowOf(context as Application)

    @Before
    fun setUp() {
        SettingsShadowResources.overrideResource(R.bool.config_show_device_name, true)
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
    fun getLaunchIntent_hasIntent() {
        assertThat(tester.getLaunchIntent()).isNotNull()
    }

    @Test
    fun getDeviceNamePreference_withoutConfig_throwsException() {
        SettingsShadowResources.overrideResource(R.bool.config_show_device_name, false)

        assertFailsWith<FailedPreconditionException> {
            tester.get<String>(MyDeviceInfoApiFirstScreen.DEVICE_NAME_KEY)
        }
    }

    @Test
    fun getDeviceNamePreference_defaultName_returnsDefaultName() {
        SettingsGlobalStore.get(context).setString(DEVICE_NAME, TEST_DEVICE_NAME)

        assertThat(tester.get<String>(MyDeviceInfoApiFirstScreen.DEVICE_NAME_KEY))
            .isEqualTo(TEST_DEVICE_NAME)
    }

    @Test
    fun getDeviceNamePreference_defaultNull_returnsBuildModel() {
        SettingsGlobalStore.get(context).setString(DEVICE_NAME, null)

        assertThat(tester.get<String>(MyDeviceInfoApiFirstScreen.DEVICE_NAME_KEY))
            .isEqualTo(Build.MODEL)
    }

    @Test
    fun setDeviceNamePreference_noPermission_throwsException() {
        shadowApplication.denyPermissions(
            WRITE_SECURE_SETTINGS,
            BLUETOOTH_ADMIN,
            BLUETOOTH_CONNECT,
            NETWORK_SETTINGS,
            OVERRIDE_WIFI_CONFIG,
        )
        assertFailsWith<MissingPermissionException> {
            tester.set(MyDeviceInfoApiFirstScreen.DEVICE_NAME_KEY, TEST_DEVICE_NAME)
        }
    }

    @Test
    @Config(shadows = [MyShadowWifiManager::class])
    fun setDeviceNamePreference_asTestDeviceName_returnsTestDeviceName() {
        shadowApplication.grantPermissions(
            WRITE_SECURE_SETTINGS,
            BLUETOOTH_ADMIN,
            BLUETOOTH_CONNECT,
            NETWORK_SETTINGS,
        )

        tester.set(MyDeviceInfoApiFirstScreen.DEVICE_NAME_KEY, TEST_DEVICE_NAME)

        assertThat(SettingsGlobalStore.get(context).getString(DEVICE_NAME))
            .isEqualTo(TEST_DEVICE_NAME)
    }

    @Test
    @Config(shadows = [MyShadowWifiManager::class])
    fun setDeviceNamePreference_asTestDeviceNameandAnyofPermission_returnsTestDeviceName() {
        shadowApplication.grantPermissions(
            WRITE_SECURE_SETTINGS,
            BLUETOOTH_ADMIN,
            BLUETOOTH_CONNECT,
            OVERRIDE_WIFI_CONFIG,
        )

        tester.set(MyDeviceInfoApiFirstScreen.DEVICE_NAME_KEY, TEST_DEVICE_NAME)

        assertThat(SettingsGlobalStore.get(context).getString(DEVICE_NAME))
            .isEqualTo(TEST_DEVICE_NAME)
    }

    @Test
    @Config(shadows = [MyShadowWifiManager::class])
    fun setDeviceNamePreference_invalidDeviceName_throwsException() {
        assertFailsWith<FailedPreconditionException> {
            tester.set(MyDeviceInfoApiFirstScreen.DEVICE_NAME_KEY, INVALID_DEVICE_NAME)
        }
    }

    @Test
    @Config(shadows = [MyShadowWifiManager::class])
    fun setDeviceNamePreference_deviceNameTooLong_throwsException() {
        assertFailsWith<FailedPreconditionException> {
            tester.set(MyDeviceInfoApiFirstScreen.DEVICE_NAME_KEY, "A".repeat(MAX_LENGTH + 1))
        }
    }

    @Test
    fun getBuildNumber_returnsValue() {
        ReflectionHelpers.setStaticField(Build::class.java, "DISPLAY", "MYBUILD.456")
        assertThat(tester.get<String>(MyDeviceInfoApiFirstScreen.BUILD_NUMBER_KEY))
            .isEqualTo("MYBUILD.456")
    }

    @Test
    fun getUptime_returnsFormattedValue() {
        val uptime = Duration.ofDays(2).plusHours(3).plusMinutes(45).plusSeconds(12)

        shadowOf(Looper.getMainLooper()).idleFor(uptime)

        val expectedFormat =
            StringUtil.formatElapsedTime(context, uptime.toMillis().toDouble(), false, false)
                .toString()
        val uptimeResult = tester.get<String>(MyDeviceInfoApiFirstScreen.UPTIME_KEY)
        assertThat(uptimeResult).isEqualTo(expectedFormat)
    }

    companion object {
        private const val TEST_DEVICE_NAME = "Terepan_Device"
        private const val INVALID_DEVICE_NAME = "    "
        private const val MAX_LENGTH = 16
    }
}

@Implements(WifiManager::class)
class MyShadowWifiManager {
    private val configuration = SoftApConfiguration.Builder().setSsid("test-ssid").build()

    @Implementation
    protected fun getSoftApConfiguration(): SoftApConfiguration? {
        return configuration
    }
}
