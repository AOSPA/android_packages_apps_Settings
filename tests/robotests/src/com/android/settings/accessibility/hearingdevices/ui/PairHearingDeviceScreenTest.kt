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

package com.android.settings.accessibility.hearingdevices.ui

import android.app.settings.SettingsEnums
import android.bluetooth.BluetoothAdapter.STATE_OFF
import android.bluetooth.BluetoothAdapter.STATE_ON
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.pm.PackageManager
import android.os.UserManager
import com.android.settings.R
import com.android.settings.accessibility.HearingDevicePairingFragment
import com.android.settings.flags.Flags
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowPackageManager

@Config(shadows = [ShadowBluetoothAdapter::class])
class PairHearingDeviceScreenTest : SettingsCatalystTestCase() {

    override val preferenceScreenCreator = PairHearingDeviceScreen(appContext)
    override val flagName: String = Flags.FLAG_CATALYST_MIGRATION_26Q2
    private val bluetoothManager = appContext.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter: ShadowBluetoothAdapter = Shadow.extract(bluetoothManager?.adapter)
    private val packageManager: ShadowPackageManager = shadowOf(appContext.packageManager)

    @Before
    fun setUp() {
        packageManager.setSystemFeature(PackageManager.FEATURE_BLUETOOTH, true)
        bluetoothAdapter.apply {
            addSupportedProfiles(BluetoothProfile.HEARING_AID)
            addSupportedProfiles(BluetoothProfile.HAP_CLIENT)
        }
    }

    @Test
    fun isAvailable_bluetoothConfigSupported_returnTrue() {
        packageManager.setSystemFeature(PackageManager.FEATURE_BLUETOOTH, true)

        assertThat(preferenceScreenCreator.isAvailable(appContext)).isTrue()
    }

    @Test
    fun isAvailable_bluetoothConfigNotSupported_returnFalse() {
        packageManager.setSystemFeature(PackageManager.FEATURE_BLUETOOTH, false)

        assertThat(preferenceScreenCreator.isAvailable(appContext)).isFalse()
    }

    @Test
    fun getSummary_bluetoothEnabled_assertSummaryEmpty() {
        bluetoothAdapter.setState(STATE_ON)

        assertThat(preferenceScreenCreator.getSummary(appContext)).isEqualTo("")
    }

    @Test
    fun getSummary_bluetoothDisabled_assertSummaryCorrect() {
        bluetoothAdapter.setState(STATE_OFF)

        assertThat(preferenceScreenCreator.getSummary(appContext))
            .isEqualTo(appContext.getString(R.string.connected_device_add_device_summary))
    }

    @Test
    fun fragmentClass_returnCorrectFragment() {
        assertThat(preferenceScreenCreator.fragmentClass())
            .isEqualTo(HearingDevicePairingFragment::class.java)
    }

    @Test
    fun getMetricsCategory_returnCorrectCategory() {
        assertThat(preferenceScreenCreator.getMetricsCategory())
            .isEqualTo(SettingsEnums.HEARING_AID_PAIRING)
    }

    @Test
    fun restrictionKeys_returnCorrectKey() {
        assertThat(preferenceScreenCreator.restrictionKeys)
            .asList()
            .containsExactly(UserManager.DISALLOW_CONFIG_BLUETOOTH)
    }
}
