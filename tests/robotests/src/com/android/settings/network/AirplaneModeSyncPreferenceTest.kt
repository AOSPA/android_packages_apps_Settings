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

import android.app.Application
import android.app.settings.SettingsEnums.ACTION_AIRPLANE_MODE_SYNC_TOGGLE
import android.bluetooth.BluetoothAdapter
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.CompanionDeviceManager
import android.companion.CompanionDeviceManager.FEATURE_CROSS_DEVICE_SYNC
import android.companion.CompanionDeviceManager.FLAG_AIRPLANE_MODE
import android.companion.DevicePresenceEvent
import android.content.Context
import android.content.Intent
import android.os.PersistableBundle
import android.os.UserHandle
import android.os.UserHandle.USER_ALL
import android.provider.Settings.Global
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsGlobalStore
import com.android.settingslib.metadata.PreferenceChangeReason
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import java.util.function.Consumer
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowBluetoothAdapter
import org.robolectric.shadows.ShadowContextImpl
import org.robolectric.shadows.ShadowLooper

@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowBluetoothAdapter::class])
class AirplaneModeSyncPreferenceTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val companionDeviceManager = mock<CompanionDeviceManager>()
    private lateinit var preference: AirplaneModeSyncPreference
    private val bluetoothAdapter: ShadowBluetoothAdapter =
        Shadow.extract(BluetoothAdapter.getDefaultAdapter())

    @Before
    fun setUp() {
        Shadow.extract<ShadowContextImpl>(context.baseContext)
            .setSystemService(Context.COMPANION_DEVICE_SERVICE, companionDeviceManager)
        preference = AirplaneModeSyncPreference(context)
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
    fun isEnabled_whenBluetoothEnabledAndWatchPresent_returnsTrue() {
        bluetoothAdapter.setEnabled(true)
        setupWatch()

        assertThat(preference.isEnabled(context)).isTrue()
    }

    @Test
    fun isEnabled_whenBluetoothEnabledAndNoWatchPresent_returnsFalse() {
        bluetoothAdapter.setEnabled(true)
        setupWatch(isPresent = false)

        assertThat(preference.isEnabled(context)).isFalse()
    }

    @Test
    fun isEnabled_whenBluetoothDisabledAndWatchPresent_returnsFalse() {
        bluetoothAdapter.setEnabled(false)
        setupWatch()

        assertThat(preference.isEnabled(context)).isFalse()
    }

    @Test
    fun isEnabled_whenBluetoothDisabledAndNoWatchPresent_returnsFalse() {
        bluetoothAdapter.setEnabled(false)
        setupWatch(isPresent = false)

        assertThat(preference.isEnabled(context)).isFalse()
    }

    @Test
    fun getSummary_whenBluetoothEnabled_returnsNull() {
        bluetoothAdapter.setEnabled(true)

        assertThat(preference.getSummary(context)).isNull()
    }

    @Test
    fun getSummary_whenBluetoothDisabled_returnsBluetoothOffSummary() {
        bluetoothAdapter.setEnabled(false)

        assertThat(preference.getSummary(context))
            .isEqualTo(context.getString(R.string.apm_sync_bluetooth_off_summary))
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
        storage.addObserver(observer, Runnable::run)

        context.sendBroadcast(Intent(BluetoothAdapter.ACTION_STATE_CHANGED))
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        verify(observer).onKeyChanged(Global.AIRPLANE_MODE_SYNC, PreferenceChangeReason.STATE)
    }

    @Test
    fun storage_notifiesChange_whenDevicePresenceChanges() {
        bluetoothAdapter.setEnabled(true)
        val observer = mock<KeyedObserver<String?>>()
        setupWatch(isPresent = false, observer)
        assertThat(preference.isEnabled(context)).isFalse()
        clearInvocations(observer)

        val listenerCaptor = argumentCaptor<Consumer<DevicePresenceEvent>>()
        verify(companionDeviceManager)
            .setOnDevicePresenceEventListener(
                eq(intArrayOf(1)),
                eq(context.packageName),
                any(),
                listenerCaptor.capture(),
            )
        companionDeviceManager.stub { on { isDevicePresent(1) } doReturn true }
        listenerCaptor.lastValue.accept(
            DevicePresenceEvent.Builder()
                .setEvent(DevicePresenceEvent.EVENT_BT_CONNECTED)
                .setAssociationId(1)
                .build()
        )

        verify(observer).onKeyChanged(Global.AIRPLANE_MODE_SYNC, PreferenceChangeReason.STATE)
        assertThat(preference.isEnabled(context)).isTrue()
    }

    @Test
    fun sensitivityLevel_isNoSensitivity() {
        assertThat(preference.sensitivityLevel).isEqualTo(SensitivityLevel.DO_NOT_EXPOSE)
    }

    @Test
    fun preferenceActionMetrics_returnsCorrectValue() {
        assertThat(preference.preferenceActionMetrics).isEqualTo(ACTION_AIRPLANE_MODE_SYNC_TOGGLE)
    }

    @Test
    fun createAndBindWidget_whenBluetoothIsDisabled_switchIsNotChecked() {
        bluetoothAdapter.setEnabled(false)
        setupWatch()
        Global.putInt(context.contentResolver, Global.AIRPLANE_MODE_SYNC, 1)

        val switchPreference = preference.createAndBindWidget<SwitchPreferenceCompat>(context)

        assertThat(switchPreference.isChecked).isFalse()
    }

    @Test
    fun createAndBindWidget_whenSettingIsTrue_switchIsChecked() {
        bluetoothAdapter.setEnabled(true)
        setupWatch()
        Global.putInt(context.contentResolver, Global.AIRPLANE_MODE_SYNC, 1)

        val switchPreference = preference.createAndBindWidget<SwitchPreferenceCompat>(context)

        assertThat(switchPreference.isChecked).isTrue()
    }

    @Test
    fun createAndBindWidget_whenSettingIsNotSet_switchIsChecked() {
        bluetoothAdapter.setEnabled(true)
        setupWatch()
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
        setupWatch()
        Global.putInt(context.contentResolver, Global.AIRPLANE_MODE_SYNC, 0)
        val switchPreference = preference.createAndBindWidget<SwitchPreferenceCompat>(context)

        switchPreference.performClick()
        assertThat(Global.getInt(context.contentResolver, Global.AIRPLANE_MODE_SYNC)).isEqualTo(1)
        assertThat(switchPreference.isChecked).isTrue()
    }

    @Test
    fun performClickTwice_whenSettingIsFalse_disablesSettingAndIsNotChecked() {
        bluetoothAdapter.setEnabled(true)
        setupWatch()
        Global.putInt(context.contentResolver, Global.AIRPLANE_MODE_SYNC, 0)
        val switchPreference = preference.createAndBindWidget<SwitchPreferenceCompat>(context)

        switchPreference.performClick()
        switchPreference.performClick()

        assertThat(Global.getInt(context.contentResolver, Global.AIRPLANE_MODE_SYNC)).isEqualTo(0)
        assertThat(switchPreference.isChecked).isFalse()
    }

    private fun setupWatch(isPresent: Boolean = true, observer: KeyedObserver<String?> = mock()) {
        val metadata =
            PersistableBundle().apply {
                putPersistableBundle(
                    FEATURE_CROSS_DEVICE_SYNC,
                    PersistableBundle().apply { putBoolean(APM_SYNC_SUPPORTED, true) },
                )
            }
        val associationInfo =
            AssociationInfo.Builder(1, UserHandle.myUserId(), context.packageName)
                .setDeviceProfile(AssociationRequest.DEVICE_PROFILE_WATCH)
                .setDisplayName("Watch")
                .setMetadata(metadata)
                .setSystemDataSyncFlags(FLAG_AIRPLANE_MODE)
                .build()

        companionDeviceManager.stub {
            on { getAllAssociations(USER_ALL) } doReturn listOf(associationInfo)
            on { isDevicePresent(1) } doReturn isPresent
        }
        preference.storage(context).addObserver(observer, Runnable::run)
    }
}
