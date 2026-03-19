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

package com.android.settings.accessibility.hearingdevices.ui

import android.app.settings.SettingsEnums
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHapClient
import android.bluetooth.BluetoothHearingAid
import android.bluetooth.BluetoothLeAudio
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import androidx.fragment.app.Fragment
import com.android.internal.accessibility.AccessibilityShortcutController.ACCESSIBILITY_HEARING_AIDS_COMPONENT_NAME
import com.android.settings.R
import com.android.settings.Settings.HearingDevicesActivity
import com.android.settings.accessibility.AccessibilityHearingAidsFragment
import com.android.settings.accessibility.FeedbackManager
import com.android.settings.accessibility.Flags
import com.android.settings.accessibility.HearingAidHelper
import com.android.settings.accessibility.HearingAidUtils
import com.android.settings.accessibility.shared.ui.AccessibilityShortcutPreference
import com.android.settings.bluetooth.Utils
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.bluetooth.BluetoothCallback
import com.android.settingslib.bluetooth.CachedBluetoothDevice
import com.android.settingslib.bluetooth.HearingAidAudioRoutingConstants
import com.android.settingslib.bluetooth.HearingAidInfo.DeviceSide.SIDE_LEFT
import com.android.settingslib.bluetooth.HearingAidInfo.DeviceSide.SIDE_RIGHT
import com.android.settingslib.bluetooth.LocalBluetoothManager
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager
import com.android.settingslib.flags.Flags as SettingsLibFlags
import com.android.settingslib.metadata.METADATA_IN_UI
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceCategory
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_UNCATEGORIZED
import kotlinx.coroutines.CoroutineScope

@ProvidePreferenceScreen(HearingDevicesScreen.KEY)
open class HearingDevicesScreen(context: Context) :
    PreferenceScreenMixin,
    PreferenceAvailabilityProvider,
    PreferenceSummaryProvider,
    PreferenceLifecycleProvider,
    BluetoothCallback,
    LocalBluetoothProfileManager.ServiceListener {
    override fun tags(context: Context) = arrayOf(APP_FUNCTION_UNCATEGORIZED)

    override val key: String
        get() = KEY

    // TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.hearing_devices_purpose

    override val title: Int
        get() = R.string.accessibility_hearingaid_title

    override val highlightMenuKey: Int
        get() = R.string.menu_key_accessibility

    override val indexable
        get() = false

    override val keywords: Int
        get() = R.string.keywords_hearing_aids

    override val icon: Int
        get() = R.drawable.ic_hearing_aid

    private var lifecycleContext: PreferenceLifecycleContext? = null
    private val localBluetoothManager: LocalBluetoothManager by lazy {
        Utils.getLocalBluetoothManager(context)
    }
    private val profileManager: LocalBluetoothProfileManager by lazy {
        localBluetoothManager.profileManager
    }
    private val hearingAidHelper: HearingAidHelper by lazy { HearingAidHelper(context) }

    private val hearingDeviceEventChangedReceiver: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                lifecycleContext?.notifyPreferenceChange(KEY)
            }
        }
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        if (isEntryPoint(context)) {
            lifecycleContext = context
        }
    }

    override fun onStart(context: PreferenceLifecycleContext) {
        super.onStart(context)
        if (isEntryPoint(context)) {
            val filter =
                IntentFilter().apply {
                    addAction(BluetoothHearingAid.ACTION_CONNECTION_STATE_CHANGED)
                    addAction(BluetoothHapClient.ACTION_HAP_CONNECTION_STATE_CHANGED)
                    addAction(BluetoothLeAudio.ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED)
                    addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                }
            context.registerReceiver(hearingDeviceEventChangedReceiver, filter)
            localBluetoothManager.eventManager?.registerCallback(this)

            // Can't get connected hearing aids when hearing aids related profiles are not ready.
            // The profiles will be ready after the services are connected. Needs to add listener
            // and updates the information when all hearing aids related services are connected.
            if (!hearingAidHelper.isAllHearingAidRelatedProfilesReady) {
                profileManager.addServiceListener(this)
            }
        }
    }

    override fun onStop(context: PreferenceLifecycleContext) {
        super.onStop(context)
        if (isEntryPoint(context)) {
            context.unregisterReceiver(hearingDeviceEventChangedReceiver)
            localBluetoothManager.eventManager?.unregisterCallback(this)
            profileManager.removeServiceListener(this)
        }
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        super.onDestroy(context)
        lifecycleContext = null
    }

    override fun getMetricsCategory(): Int = SettingsEnums.ACCESSIBILITY_HEARING_AID_SETTINGS

    override fun isFlagEnabled(context: Context): Boolean = Flags.catalystHearingDevices()

    override fun fragmentClass(): Class<out Fragment>? =
        AccessibilityHearingAidsFragment::class.java

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?): Intent? =
        makeLaunchIntent(context, HearingDevicesActivity::class.java, metadata?.key)

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +HearingDevicesTopIntroPreference(context)
            +AvailableHearingDevicePreferenceCategory(context, metricsCategory)
            if (com.android.settings.flags.Flags.catalystMigration26q2()) {
                +PairHearingDeviceScreen.KEY
            } else {
                +AddDevicePreference(context)
            }
            +SavedHearingDevicePreferenceCategory(metricsCategory)
            if (SettingsLibFlags.hearingDevicesGranularOutputRouting()) {
                +HearingDeviceRoutingPreferenceCategory() += {
                    +HearingDeviceAudioRoutingSwitchPreference(
                        context,
                        "hearing_device_notification_routing",
                        Settings.Secure.HEARING_AID_NOTIFICATION_ROUTING,
                        R.string.accessibility_hearing_device_notification_routing_title,
                        R.string.accessibility_hearing_device_routing_hearing_device_summary,
                        R.string.accessibility_hearing_device_routing_device_speaker_summary,
                        R.string.hearing_device_notification_routing_purpose,
                        HearingAidAudioRoutingConstants.NOTIFICATION_ROUTING_ATTRIBUTES,
                    )
                    +HearingDeviceAudioRoutingSwitchPreference(
                        context,
                        "hearing_device_ringtone_alarm_routing",
                        Settings.Secure.HEARING_AID_RINGTONE_ROUTING,
                        R.string.accessibility_hearing_device_ringtone_alarm_routing_title,
                        R.string.accessibility_hearing_device_routing_duplicate_both_summary,
                        R.string.accessibility_hearing_device_routing_device_speaker_summary,
                        R.string.hearing_device_ringtone_alarm_routing_purpose,
                        HearingAidAudioRoutingConstants.RINGTONE_ROUTING_ATTRIBUTES,
                    )
                }
            }
            +HearingDeviceOptionsPreferenceCategory() += {
                +HearingDeviceShortcutPreference(context, metricsCategory)
                +HearingAidCompatibilitySwitchPreference(context)
            }
            +HearingDevicesFooterPreference(context)
            +HearingDevicesFeedbackButtonPreference { FeedbackManager(context, metricsCategory) }
        }

    override val availabilityDescription =
        "The device must support hearing devices (Hearing Aid or HAP Client Bluetooth Profile)."

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context): Boolean = hearingAidHelper.isHearingAidSupported

    override fun getSummary(context: Context): CharSequence? {
        val connectedDevice: CachedBluetoothDevice =
            hearingAidHelper.connectedHearingAidDevice
                ?: return context.getText(R.string.accessibility_hearingaid_not_connected_summary)

        val name: CharSequence? = connectedDevice.getName()
        if (hearingAidHelper.connectedHearingAidDeviceList.size > 1) {
            return context.getString(R.string.accessibility_hearingaid_more_device_summary, name)
        }

        val memberDevices = buildList {
            connectedDevice.subDevice?.let { add(it) }
            connectedDevice.memberDevice?.let { addAll(it) }
        }
        val connectedOtherSideDevice = memberDevices.firstOrNull { it.device.isConnected }
        if (connectedOtherSideDevice != null) {
            return context.getString(
                R.string.accessibility_hearingaid_left_and_right_side_device_summary,
                name,
            )
        }

        val stringResId =
            when (connectedDevice.deviceSide) {
                SIDE_LEFT -> R.string.accessibility_hearingaid_left_side_device_summary
                SIDE_RIGHT -> R.string.accessibility_hearingaid_right_side_device_summary
                else -> R.string.accessibility_hearingaid_active_device_summary
            }
        return context.getString(stringResId, name)
    }

    override fun onServiceConnected() {
        if (hearingAidHelper.isAllHearingAidRelatedProfilesReady) {
            lifecycleContext?.notifyPreferenceChange(KEY)
            profileManager.removeServiceListener(this)
        }
    }

    override fun onServiceDisconnected() {
        // Do nothing
    }

    override fun onActiveDeviceChanged(
        activeDevice: CachedBluetoothDevice?,
        bluetoothProfile: Int,
    ) {
        if (activeDevice == null || lifecycleContext == null) {
            return
        }

        if (bluetoothProfile == BluetoothProfile.HEARING_AID) {
            HearingAidUtils.launchHearingAidPairingDialog(
                lifecycleContext?.fragmentManager,
                activeDevice,
                metricsCategory,
            )
        }
    }

    class HearingDeviceRoutingPreferenceCategory(
        key: String = "hearing_device_routing_category",
        purpose: Int = R.string.hearing_device_audio_routing_purpose,
        title: Int = R.string.accessibility_hearing_device_routing_title,
    ) : PreferenceCategory(key, purpose, title), PreferenceAvailabilityProvider {
        override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)

        override val availabilityDescription = UI_ONLY_PREFERENCE

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

        override fun isAvailable(context: Context): Boolean =
            SettingsLibFlags.hearingDevicesGranularOutputRouting()
    }

    class HearingDeviceOptionsPreferenceCategory(
        key: String = "hearing_options_category",
        purpose: Int = R.string.hearing_options_category_purpose,
        title: Int = R.string.accessibility_screen_option,
    ) : PreferenceCategory(key, purpose, title)

    class HearingDeviceShortcutPreference(context: Context, metricsCategory: Int) :
        AccessibilityShortcutPreference(
            context = context,
            key = "hearing_aids_shortcut_preference",
            purpose = R.string.hearing_aids_shortcut_preference_purpose,
            title = R.string.accessibility_hearing_device_shortcut_title,
            componentName = ACCESSIBILITY_HEARING_AIDS_COMPONENT_NAME,
            featureName = R.string.accessibility_hearingaid_title,
            metricsCategory = metricsCategory,
        ) {
        override val sensitivityLevel = SensitivityLevel.DEEP_LINK_ONLY
    }

    class HearingDevicesScreenPreference(private val screenMetadata: HearingDevicesScreen) :
        PreferenceMetadata, PreferenceSummaryProvider, PreferenceAvailabilityProvider {
        override val key: String
            get() = "hearing_devices_preference"

        override val purpose: Int
            get() = screenMetadata.purpose

        override fun tags(context: Context) = arrayOf(METADATA_IN_UI)

        override val indexable = false

        override fun isEnabled(context: Context): Boolean = screenMetadata.isEnabled(context)

        override fun getSummary(context: Context): CharSequence? =
            screenMetadata.getSummary(context)

        override val availabilityDescription = screenMetadata.availabilityDescription

        override fun getAvailabilityStability() = screenMetadata.getAvailabilityStability()

        override fun isAvailable(context: Context): Boolean = screenMetadata.isAvailable(context)
    }

    companion object {
        const val KEY = "hearing_devices"
    }
}
