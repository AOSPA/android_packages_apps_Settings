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

package com.android.settings.development.bluetooth

import android.app.settings.SettingsEnums
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.PowerManager
import android.os.UserManager
import android.util.Log
import com.android.settings.R
import com.android.settings.dashboard.RestrictedDashboardFragment
import com.android.settings.development.DefaultLaunchPreferenceController
import com.android.settings.development.DeveloperOptionAwareMixin
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.core.AbstractPreferenceController
import com.android.settingslib.core.lifecycle.Lifecycle
import com.android.settingslib.development.DevelopmentSettingsEnabler
import com.android.settingslib.search.SearchIndexable

@SearchIndexable
class BluetoothDevelopmentSettingsFragment :
    RestrictedDashboardFragment(UserManager.DISALLOW_CONFIG_BLUETOOTH),
    RebootDialog.OnRebootDialogListener,
    AbstractBluetoothPreferenceController.Callback,
    SnoopLogHost,
    DeveloperOptionAwareMixin {

    private val adapter: BluetoothAdapter? by lazy {
        requireContext().getSystemService(BluetoothManager::class.java)?.adapter
    }
    private val a2dpConfigStore = A2dpConfigStore()
    private var a2dp: BluetoothA2dp? = null

    private val a2dpReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.d(TAG, "a2dpReceiver.onReceive intent=$intent")
                preferenceControllers
                    .flatten()
                    .filterIsInstance<ServiceConnectionListener>()
                    .forEach { it.onBluetoothCodecUpdated() }
            }
        }

    private val a2dpServiceListener =
        object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                a2dp = proxy as BluetoothA2dp
                preferenceControllers
                    .flatten()
                    .filterIsInstance<ServiceConnectionListener>()
                    .forEach { it.onBluetoothServiceConnected(a2dp) }
            }

            override fun onServiceDisconnected(profile: Int) {
                a2dp = null
                preferenceControllers
                    .flatten()
                    .filterIsInstance<ServiceConnectionListener>()
                    .forEach { it.onBluetoothServiceDisconnected() }
            }
        }

    override fun getMetricsCategory() = SettingsEnums.DEVELOPMENT_BLUETOOTH

    override fun getLogTag() = TAG

    override fun getPreferenceScreenResId() = R.xml.bluetooth_development_settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter?.getProfileProxy(context, a2dpServiceListener, BluetoothProfile.A2DP)

        val filter =
            IntentFilter().apply {
                addAction(BluetoothA2dp.ACTION_ACTIVE_DEVICE_CHANGED)
                addAction(BluetoothA2dp.ACTION_CODEC_CONFIG_CHANGED)
            }
        requireContext().registerReceiver(a2dpReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        a2dp?.let { proxy ->
            adapter?.closeProfileProxy(BluetoothProfile.A2DP, proxy)
            a2dp = null
        }
        requireContext().unregisterReceiver(a2dpReceiver)
    }

    override fun onRebootDialogConfirmed() {
        context?.getSystemService(PowerManager::class.java)?.reboot(null)
    }

    override fun onRebootDialogCanceled() {
        use(A2dpHwOffloadPreferenceController::class.java)?.onRebootDialogCanceled()
        use(LeAudioHwOffloadPreferenceController::class.java)?.onRebootDialogCanceled()
        use(LeAudioModePreferenceController::class.java)?.onRebootDialogCanceled()
    }

    override fun onBluetoothCodecChanged() {
        preferenceControllers
            .flatten()
            .filterIsInstance<AbstractBluetoothDialogPreferenceController>()
            .forEach { it.onBluetoothCodecUpdated() }
    }

    override fun onBluetoothHDAudioEnabled(enabled: Boolean) {
        preferenceControllers
            .flatten()
            .filterIsInstance<AbstractBluetoothDialogPreferenceController>()
            .forEach { it.onHDAudioEnabled(enabled) }
    }

    override fun onSettingChanged() {
        use(SnoopLogFilterProfileMapPreferenceController::class.java)?.onSettingChanged()
        use(SnoopLogFilterProfilePbapPreferenceController::class.java)?.onSettingChanged()
    }

    override fun createPreferenceControllers(context: Context) =
        buildPreferenceControllers(context, settingsLifecycle, this, a2dpConfigStore)

    companion object {
        private const val TAG = "BtDevSettingsFragment"

        private fun buildPreferenceControllers(
            context: Context,
            lifecycle: Lifecycle?,
            fragment: BluetoothDevelopmentSettingsFragment?,
            a2dpConfigStore: A2dpConfigStore?,
        ): List<AbstractPreferenceController> {
            return listOf(
                SnoopLogPreferenceController(context, fragment),
                SnoopLogSocketPreferenceController(context),
                StackLogPreferenceController(context),
                DefaultLaunchPreferenceController(context, "snoop_logger_filters_dashboard"),
                SnoopLogFilterProfilePbapPreferenceController(context),
                SnoopLogFilterProfileMapPreferenceController(context),
                DeviceNoNamePreferenceController(context),
                AbsoluteVolumePreferenceController(context),
                AvrcpVersionPreferenceController(context),
                MapVersionPreferenceController(context),
                LeAudioModePreferenceController(context, fragment),
                LeAudioDeviceDetailsPreferenceController(context),
                LeAudioAllowListPreferenceController(context),
                A2dpHwOffloadPreferenceController(context, fragment),
                LeAudioHwOffloadPreferenceController(context, fragment),
                MaxConnectedAudioDevicesPreferenceController(context),
                CodecListPreferenceController(context, lifecycle, a2dpConfigStore, fragment),
                SampleRateDialogPreferenceController(context, lifecycle, a2dpConfigStore),
                BitPerSampleDialogPreferenceController(context, lifecycle, a2dpConfigStore),
                QualityDialogPreferenceController(context, lifecycle, a2dpConfigStore),
                ChannelModeDialogPreferenceController(context, lifecycle, a2dpConfigStore),
                HDAudioPreferenceController(context, lifecycle, a2dpConfigStore, fragment),
            )
        }

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER =
            object : BaseSearchIndexProvider(R.xml.bluetooth_development_settings) {
                override fun isPageSearchEnabled(context: Context) =
                    DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(context)

                override fun createPreferenceControllers(context: Context) =
                    buildPreferenceControllers(context, null, null, null)
            }
    }
}
