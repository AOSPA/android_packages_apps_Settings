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

import android.Manifest
import android.app.settings.SettingsEnums
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.UserManager
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.accessibility.HearingDevicePairingFragment
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.restriction.PreferenceRestrictionMixin
import com.android.settingslib.RestrictedPreference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceHierarchy
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceBinding
import kotlinx.coroutines.CoroutineScope
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_UNCATEGORIZED

// LINT.IfChange
@ProvidePreferenceScreen(PairHearingDeviceScreen.KEY)
open class PairHearingDeviceScreen(context: Context) :
    PreferenceScreenMixin,
    PreferenceBinding,
    PreferenceSummaryProvider,
    PreferenceAvailabilityProvider,
    PreferenceRestrictionMixin,
    PreferenceLifecycleProvider {
    override fun tags(context: Context) = arrayOf(APP_FUNCTION_UNCATEGORIZED)

    private var lifecycleContext: PreferenceLifecycleContext? = null
    private val bluetoothAdapter: BluetoothAdapter? =
        context.getSystemService(BluetoothManager::class.java)?.adapter
    private val receiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                lifecycleContext?.notifyPreferenceChange(KEY)
            }
        }
    private val intentFilter: IntentFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.hearing_device_add_bt_devices_purpose

    override val icon: Int
        get() = R.drawable.ic_add_24dp

    override val title: Int
        get() = R.string.bluetooth_pairing_pref_title

    override val highlightMenuKey: Int
        get() = R.string.menu_key_accessibility

    override val restrictionKeys: Array<String>
        get() = arrayOf(UserManager.DISALLOW_CONFIG_BLUETOOTH)

    override val useAdminDisabledSummary: Boolean
        get() = true

    override fun createWidget(context: Context): Preference = RestrictedPreference(context)

    override val availabilityDescription =
        "The device must support Bluetooth."

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)

    override fun isEnabled(context: Context) = super<PreferenceRestrictionMixin>.isEnabled(context)

    override fun isFlagEnabled(context: Context): Boolean = Flags.catalystMigration26q2()

    override fun fragmentClass(): Class<out Fragment> = HearingDevicePairingFragment::class.java

    override fun getMetricsCategory(): Int = SettingsEnums.HEARING_AID_PAIRING

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        if (isEntryPoint(context)) {
            lifecycleContext = context
        }
    }

    override fun onStart(context: PreferenceLifecycleContext) {
        super.onStart(context)
        if (isEntryPoint(context)) {
            ContextCompat.registerReceiver(
                context,
                receiver,
                intentFilter,
                ContextCompat.RECEIVER_EXPORTED,
            )
        }
    }

    override fun onStop(context: PreferenceLifecycleContext) {
        super.onStop(context)
        if (isEntryPoint(context)) {
            context.unregisterReceiver(receiver)
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH)
    override fun getSummary(context: Context): CharSequence? =
        if (bluetoothAdapter?.isEnabled == false) {
            context.getString(R.string.connected_device_add_device_summary)
        } else {
            ""
        }

    override fun hasCompleteHierarchy(): Boolean = false

    override fun getPreferenceHierarchy(
        context: Context,
        coroutineScope: CoroutineScope,
    ): PreferenceHierarchy = preferenceHierarchy(context) {}

    companion object {
        const val KEY = "hearing_device_add_bt_devices"
    }
}
// LINT.ThenChange(../../HearingDevicePairingFragment.java,
//                 ../../../connecteddevice/AddDevicePreferenceController.java)
