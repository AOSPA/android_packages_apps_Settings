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

package com.android.settings.network

import android.app.settings.SettingsEnums.ACTION_AIRPLANE_MODE_SYNC_TOGGLE
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.provider.Settings.Global
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsGlobalStore
import com.android.settingslib.metadata.PreferenceChangeReason
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowBluetoothAdapter
import org.robolectric.shadows.ShadowLooper

@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowBluetoothAdapter::class, SettingsShadowResources::class])
class AirplaneModeSyncPreferenceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preference = AirplaneModeSyncPreference()
    private val bluetoothAdapter: ShadowBluetoothAdapter =
        Shadow.extract(BluetoothAdapter.getDefaultAdapter())

    @After
    fun tearDown() {
        SettingsShadowResources.reset()
    }

    @Test
    fun key_returnsCorrectKey() {
        assertThat(preference.key).isEqualTo(Global.AIRPLANE_MODE_SYNC)
    }

    @Test
    fun title_returnsCorrectTitle() {
        assertThat(preference.title).isEqualTo(R.string.sync_across_devices_title)
    }

    @Test
    fun isEnabled_whenBluetoothEnabled_returnsTrue() {
        bluetoothAdapter.setEnabled(true)

        assertThat(preference.isEnabled(context)).isTrue()
    }

    @Test
    fun isEnabled_whenBluetoothDisabled_returnsFalse() {
        bluetoothAdapter.setEnabled(false)

        assertThat(preference.isEnabled(context)).isFalse()
    }

    @Test
    fun getReadPermissions_returnsSettingsGlobalStorePermissions() {
        assertThat(preference.getReadPermissions(context))
            .isEqualTo(SettingsGlobalStore.getReadPermissions())
    }

    @Test
    fun getWritePermissions_returnsSettingsGlobalStorePermissions() {
        assertThat(preference.getWritePermissions(context))
            .isEqualTo(SettingsGlobalStore.getWritePermissions())
    }

    @Test
    fun getReadPermit_returnsAllow() {
        assertThat(preference.getReadPermit(context, 0, 0)).isEqualTo(ReadWritePermit.ALLOW)
    }

    @Test
    fun getWritePermit_returnsAllow() {
        assertThat(preference.getWritePermit(context, 0, 0)).isEqualTo(ReadWritePermit.ALLOW)
    }

    @Test
    fun storage_notifiesChange_whenBluetoothStateChanges() {
        val storage = preference.storage(context)
        val observer = mock<KeyedObserver<String?>>()
        storage.addObserver(observer) { it.run() }

        context.sendBroadcast(Intent(BluetoothAdapter.ACTION_STATE_CHANGED))
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        verify(observer).onKeyChanged(Global.AIRPLANE_MODE_SYNC, PreferenceChangeReason.STATE)
    }

    @Test
    fun sensitivityLevel_isNoSensitivity() {
        assertThat(preference.sensitivityLevel).isEqualTo(SensitivityLevel.NO_SENSITIVITY)
    }

    @Test
    fun preferenceActionMetrics_returnsCorrectValue() {
        assertThat(preference.preferenceActionMetrics).isEqualTo(ACTION_AIRPLANE_MODE_SYNC_TOGGLE)
    }

    @Test
    fun createAndBindWidget_whenBluetoothIsDisabled_switchIsNotChecked() {
        bluetoothAdapter.setEnabled(false)
        Global.putInt(context.contentResolver, Global.AIRPLANE_MODE_SYNC, 1)

        val switchPreference = preference.createAndBindWidget<SwitchPreferenceCompat>(context)

        assertThat(switchPreference.isChecked).isFalse()
    }

    @Test
    fun createAndBindWidget_whenSettingIsTrue_switchIsChecked() {
        bluetoothAdapter.setEnabled(true)
        Global.putInt(context.contentResolver, Global.AIRPLANE_MODE_SYNC, 1)

        val switchPreference = preference.createAndBindWidget<SwitchPreferenceCompat>(context)

        assertThat(switchPreference.isChecked).isTrue()
    }

    @Test
    fun createAndBindWidget_whenSettingIsNotSet_switchIsChecked() {
        bluetoothAdapter.setEnabled(true)
        Global.putString(context.contentResolver, Global.AIRPLANE_MODE_SYNC, null)

        val switchPreference = preference.createAndBindWidget<SwitchPreferenceCompat>(context)

        assertThat(switchPreference.isChecked).isTrue()
    }

    @Test
    fun createAndBindWidget_whenSettingIsFalse_switchIsNotChecked() {
        Global.putInt(context.contentResolver, Global.AIRPLANE_MODE_SYNC, 0)

        val switchPreference = preference.createAndBindWidget<SwitchPreferenceCompat>(context)

        assertThat(switchPreference.isChecked).isFalse()
    }

    @Test
    fun performClick_whenSettingIsFalse_enablesSettingAndIsChecked() {
        bluetoothAdapter.setEnabled(true)
        Global.putInt(context.contentResolver, Global.AIRPLANE_MODE_SYNC, 0)
        val switchPreference = preference.createAndBindWidget<SwitchPreferenceCompat>(context)

        switchPreference.performClick()
        assertThat(Global.getInt(context.contentResolver, Global.AIRPLANE_MODE_SYNC)).isEqualTo(1)
        assertThat(switchPreference.isChecked).isTrue()
    }

    @Test
    fun performClickTwice_whenSettingIsFalse_disablesSettingAndIsNotChecked() {
        bluetoothAdapter.setEnabled(true)
        Global.putInt(context.contentResolver, Global.AIRPLANE_MODE_SYNC, 0)
        val switchPreference = preference.createAndBindWidget<SwitchPreferenceCompat>(context)

        switchPreference.performClick()
        switchPreference.performClick()

        assertThat(Global.getInt(context.contentResolver, Global.AIRPLANE_MODE_SYNC)).isEqualTo(0)
        assertThat(switchPreference.isChecked).isFalse()
    }
}
