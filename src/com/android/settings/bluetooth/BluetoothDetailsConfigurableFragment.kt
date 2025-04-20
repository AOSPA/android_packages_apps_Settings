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

package com.android.settings.bluetooth

import android.app.ActivityOptions
import android.app.settings.SettingsEnums
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.UserManager
import android.util.Log
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat
import androidx.preference.TwoStatePreference
import com.android.settings.flags.Flags
import com.android.settings.R
import com.android.settings.bluetooth.ui.composable.MultiTogglePreference
import com.android.settings.bluetooth.ui.model.DeviceSettingPreferenceModel
import com.android.settings.bluetooth.ui.model.FragmentTypeModel
import com.android.settings.bluetooth.ui.view.DeviceDetailsMoreSettingsFragment
import com.android.settings.bluetooth.ui.viewmodel.BluetoothDeviceDetailsViewModel
import com.android.settings.core.SubSettingLauncher
import com.android.settings.dashboard.RestrictedDashboardFragment
import com.android.settings.overlay.FeatureFactory
import com.android.settings.spa.preference.ComposePreference
import com.android.settingslib.PrimarySwitchPreference
import com.android.settingslib.bluetooth.CachedBluetoothDevice
import com.android.settingslib.bluetooth.LocalBluetoothManager
import com.android.settingslib.bluetooth.devicesettings.DeviceSettingId
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingActionModel
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingConfigItemModel
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingIcon
import com.android.settingslib.spa.widget.ui.LinearLoadingBar
import com.android.settingslib.widget.CardPreference
import com.android.settingslib.widget.FooterPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/** Base class for bluetooth settings which makes the preference visibility/order configurable. */
abstract class BluetoothDetailsConfigurableFragment : RestrictedDashboardFragment(UserManager.DISALLOW_CONFIG_BLUETOOTH) {
    protected lateinit var localBluetoothManager: LocalBluetoothManager
    protected lateinit var deviceAddress: String
    protected lateinit var cachedDevice: CachedBluetoothDevice
    private var displayOrder: List<String>? = null
    private lateinit var originalDisplayOrder: List<String>
    private val metricsFeatureProvider = FeatureFactory.featureFactory.metricsFeatureProvider
    private val prefVisibility = mutableMapOf<String, MutableStateFlow<Boolean>>()
    private val uiJobs = mutableListOf<Job>()

    private lateinit var viewModel: BluetoothDeviceDetailsViewModel

    private val invisiblePrefCategory: PreferenceGroup by lazy {
        preferenceScreen.findPreference<PreferenceGroup>(INVISIBLE_CATEGORY) ?: run {
            PreferenceCategory(requireContext()).apply {
                key = INVISIBLE_CATEGORY
                isVisible = false
                isOrderingAsAdded = true
            }.also {
                preferenceScreen.addPreference(it)
                it.addPreference(ComposePreference(requireContext()).apply {
                    key = LOADING_PREF
                    setContent {
                        LinearLoadingBar(isLoading = true)
                    }
                })
            }
        }
    }

    override fun onAttach(context: Context) {
        localBluetoothManager = Utils.getLocalBtManager(context)
        deviceAddress =
            arguments?.getString(KEY_DEVICE_ADDRESS) ?: run {
                Log.w(TAG, "onAttach() address is null!")
                finish()
                return
            }
        cachedDevice = getCachedDevice(deviceAddress) ?: run {
            Log.w(TAG, "onAttach() CachedDevice is null!")
            finish()
            return
        }
        super.onAttach(context)
        viewModel = ViewModelProvider(
            this,
            BluetoothDeviceDetailsViewModel.Factory(
                requireActivity().application,
                cachedDevice,
                Dispatchers.IO,
            ),
        ).get(BluetoothDeviceDetailsViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        originalDisplayOrder = preferenceScreen.getAllChildren().map {
            it.key
        }
        updatePreferenceOrder()
    }

    fun requestUpdateLayout(prefKeyOrder: List<String>?) {
        if (displayOrder == prefKeyOrder) {
            return
        }
        displayOrder = prefKeyOrder
        updatePreferenceOrder()
    }

    fun requestUpdateLayout(fragmentType: FragmentTypeModel) {
        lifecycleScope.launch { updateLayoutInternal(fragmentType) }
    }

    private suspend fun updateLayoutInternal(fragmentType: FragmentTypeModel) {
        val items = viewModel.getItems(fragmentType) ?: run {
            displayOrder = originalDisplayOrder
            updatePreferenceOrder()
            return
        }

        val prefKeyToSettingId =
            items.filterIsInstance<DeviceSettingConfigItemModel.BuiltinItem>()
                .associateBy({ it.preferenceKey }, { it.settingId })

        val settingIdToPreferences: MutableMap<Int, Preference> = HashMap()
        for (pref in getAllPreferences()) {
            prefKeyToSettingId[pref.key]?.let { id -> settingIdToPreferences[id] = pref }
        }
        for (job in uiJobs) {
            job.cancel()
        }
        uiJobs.clear()
        val configDisplayOrder = mutableListOf<String>()
        for (row in items.indices) {
            val settingItem = items[row]
            val settingId = settingItem.settingId

            val existingPrefKey = settingIdToPreferences[settingId]?.key
            configDisplayOrder.add(
                existingPrefKey ?: getPreferenceKey(
                    settingId
                )
            )
            if (existingPrefKey != null) {
                continue
            }

            val prefKey = getPreferenceKey(settingId)

            viewModel.getDeviceSetting(cachedDevice, settingId)
                .onEach { logItemShown(prefKey, it != null) }
                .launchIn(lifecycleScope)
                .also { uiJobs.add(it) }
            if (settingId == DeviceSettingId.DEVICE_SETTING_ID_ANC) {
                // TODO(b/399316980): replace it with SegmentedButtonPreference once it's ready.
                val pref = ComposePreference(requireContext()).apply {
                    key = prefKey
                    order = row
                }.also { pref ->
                    pref.setContent {
                        buildComposePreference(cachedDevice, settingId, prefKey)
                    }
                }
                preferenceScreen.addPreference(pref)
            } else {
                viewModel.getDeviceSetting(cachedDevice, settingId).onEach {
                    val existedPref = preferenceScreen.findPreference<Preference>(
                        prefKey
                    )
                    val item = it ?: run {
                        existedPref?.let {
                            preferenceScreen.removePreference(
                                existedPref
                            )
                        }
                        return@onEach
                    }
                    buildPreference(
                        existedPref, item, prefKey, settingItem.highlighted
                    )?.apply {
                        key = prefKey
                        order = row
                    }?.also { preferenceScreen.addPreference(it) }
                }.launchIn(lifecycleScope).also { uiJobs.add(it) }
            }
        }

        for (row in items.indices) {
            val settingItem = items[row]
            val settingId = settingItem.settingId
            settingIdToPreferences[settingId]?.let { pref ->
                if (settingId == DeviceSettingId.DEVICE_SETTING_ID_BLUETOOTH_PROFILES) {
                    use(BluetoothDetailsProfilesController::class.java)?.run {
                        if (settingItem is DeviceSettingConfigItemModel.BuiltinItem.BluetoothProfilesItem) {
                            setInvisibleProfiles(settingItem.invisibleProfiles)
                        }
                    }
                }
                logItemShown(pref.key, pref.isVisible)
            }
        }
        displayOrder = configDisplayOrder
        updatePreferenceOrder()
    }

    private fun buildPreference(
        existedPref: Preference?,
        model: DeviceSettingPreferenceModel,
        prefKey: String,
        highlighted: Boolean,
    ): Preference? = when (model) {
        is DeviceSettingPreferenceModel.PlainPreference -> {
            val pref = existedPref ?: run {
                if (highlighted) {
                    if (Flags.enableBluetoothSettingsExpressiveDesign()) {
                        CardPreference(requireContext())
                    } else {
                        SpotlightPreference(requireContext())
                    }
                } else {
                    Preference(requireContext())
                }
            }
            pref.apply {
                title = model.title
                summary = model.summary
                icon = getDrawable(model.icon)
                onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    logItemClick(prefKey, EVENT_CLICK_PRIMARY)
                    model.action?.let { triggerAction(it) }
                    true
                }
            }
        }

        is DeviceSettingPreferenceModel.SwitchPreference -> if (model.action == null) {
            val pref =
                existedPref as? SwitchPreferenceCompat ?: SwitchPreferenceCompat(requireContext())
            pref.apply {
                title = model.title
                summary = model.summary
                icon = getDrawable(model.icon)
                isChecked = model.checked
                isEnabled = !model.disabled
                onPreferenceChangeListener = object : Preference.OnPreferenceChangeListener {
                    override fun onPreferenceChange(
                        p: Preference,
                        value: Any?,
                    ): Boolean {
                        (p as? TwoStatePreference)?.let { newState ->
                            val newState = value as? Boolean ?: return false
                            logItemClick(
                                prefKey,
                                if (newState) EVENT_SWITCH_ON else EVENT_SWITCH_OFF,
                            )
                            model.onCheckedChange.invoke(newState)
                        }
                        return false
                    }
                }
            }
        } else {
            val pref =
                existedPref as? PrimarySwitchPreference ?: PrimarySwitchPreference(requireContext())
            pref.apply {
                title = model.title
                summary = model.summary
                icon = getDrawable(model.icon)
                isChecked = model.checked
                isEnabled = !model.disabled
                isSwitchEnabled = !model.disabled
                onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    logItemClick(prefKey, EVENT_CLICK_PRIMARY)
                    triggerAction(model.action)
                    true
                }
                onPreferenceChangeListener = object : Preference.OnPreferenceChangeListener {
                    override fun onPreferenceChange(
                        p: Preference,
                        value: Any?,
                    ): Boolean {
                        val newState = value as? Boolean ?: return false
                        logItemClick(
                            prefKey,
                            if (newState) EVENT_SWITCH_ON else EVENT_SWITCH_OFF,
                        )
                        model.onCheckedChange.invoke(newState)
                        return false
                    }
                }
            }
        }

        is DeviceSettingPreferenceModel.MultiTogglePreference -> {
            // TODO(b/399316980): implement it with SegmentedButtonPreference once it's ready.
            null
        }

        is DeviceSettingPreferenceModel.FooterPreference -> {
            val pref = existedPref as? FooterPreference ?: FooterPreference(requireContext())
            pref.apply { title = model.footerText }
        }

        is DeviceSettingPreferenceModel.MoreSettingsPreference -> {
            val pref = existedPref ?: Preference(requireContext())
            pref.apply {
                title = context.getString(R.string.bluetooth_device_more_settings_preference_title)
                summary = context.getString(
                    R.string.bluetooth_device_more_settings_preference_summary
                )
                icon = context.getDrawable(R.drawable.ic_chevron_right_24dp)
                onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    logItemClick(prefKey, EVENT_CLICK_PRIMARY)
                    SubSettingLauncher(context).setDestination(
                        DeviceDetailsMoreSettingsFragment::class.java.name
                    ).setSourceMetricsCategory(
                        getMetricsCategory()
                    ).setArguments(
                        Bundle().apply {
                            putString(KEY_DEVICE_ADDRESS, cachedDevice.address)
                        }).launch()
                    true
                }
            }
        }

        is DeviceSettingPreferenceModel.HelpPreference -> {
            null
        }
    }

    private fun getPreferenceKey(settingId: Int) = "DEVICE_SETTING_${settingId}"

    private fun getDrawable(deviceSettingIcon: DeviceSettingIcon?): Drawable? =
        when (deviceSettingIcon) {
            is DeviceSettingIcon.BitmapIcon -> deviceSettingIcon.bitmap.toDrawable(requireContext().resources)

            is DeviceSettingIcon.ResourceIcon -> context?.getDrawable(deviceSettingIcon.resId)
            null -> null
        }?.apply {
            setTint(
                requireContext().getColor(
                    com.android.settingslib.widget.theme.R.color.settingslib_materialColorOnSurfaceVariant
                )
            )
        }

    @Composable
    private fun buildComposePreference(
        cachedDevice: CachedBluetoothDevice,
        settingId: Int,
        prefKey: String,
    ) {
        val contents by remember(settingId) {
            viewModel.getDeviceSetting(
                cachedDevice, settingId
            )
        }.collectAsStateWithLifecycle(initialValue = null)

        val settings = contents
        AnimatedVisibility(visible = settings != null, enter = fadeIn(), exit = fadeOut()) {
            (settings as? DeviceSettingPreferenceModel.MultiTogglePreference)?.let {
                buildMultiTogglePreference(it, prefKey)
            }
        }
    }

    @Composable
    private fun buildMultiTogglePreference(
        pref: DeviceSettingPreferenceModel.MultiTogglePreference,
        prefKey: String,
    ) {
        MultiTogglePreference(
            pref.copy(
                onSelectedChange = { newState ->
                    logItemClick(prefKey, newState)
                    pref.onSelectedChange(newState)
                })
        )
    }

    private fun logItemClick(preferenceKey: String, value: Int = 0) {
        logAction(preferenceKey, SettingsEnums.ACTION_BLUETOOTH_DEVICE_DETAILS_ITEM_CLICKED, value)
    }

    private fun logItemShown(preferenceKey: String, visible: Boolean) {
        if (!visible && !prefVisibility.containsKey(preferenceKey)) {
            return
        }
        prefVisibility.computeIfAbsent(preferenceKey) {
            MutableStateFlow(true).also { visibilityFlow ->
                visibilityFlow.onEach {
                    logAction(
                        preferenceKey,
                        SettingsEnums.ACTION_BLUETOOTH_DEVICE_DETAILS_ITEM_SHOWN,
                        if (it) EVENT_VISIBLE else EVENT_INVISIBLE,
                    )
                }.launchIn(lifecycleScope)
            }
        }.value = visible
    }

    private fun logAction(preferenceKey: String, action: Int, value: Int) {
        metricsFeatureProvider.action(SettingsEnums.PAGE_UNKNOWN, action, 0, preferenceKey, value)
    }

    private fun triggerAction(action: DeviceSettingActionModel) {
        when (action) {
            is DeviceSettingActionModel.IntentAction -> {
                action.intent.removeFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(action.intent)
            }

            is DeviceSettingActionModel.PendingIntentAction -> {
                val options =
                    ActivityOptions.makeBasic().setPendingIntentBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
                    )
                action.pendingIntent.send(options.toBundle())
            }
        }
    }

    private fun updatePreferenceOrder() {
        val order = displayOrder ?: return
        if (preferenceScreen == null) {
            return
        }
        val allPrefs =
            (invisiblePrefCategory.getAndRemoveAllChildren() + preferenceScreen.getAndRemoveAllChildren()).filter {
                it != invisiblePrefCategory
            }
        val visiblePrefs = allPrefs.filter { order.contains(it.key) }
        visiblePrefs.forEach { it.order = order.indexOf(it.key) }
        val invisiblePrefs = allPrefs.filter { !order.contains(it.key) }
        preferenceScreen.addPreferences(visiblePrefs)
        preferenceScreen.addPreference(invisiblePrefCategory)
        invisiblePrefCategory.addPreferences(invisiblePrefs)
    }

    private fun PreferenceGroup.getAllChildren(): List<Preference> {
        val prefs = mutableListOf<Preference>()
        for (i in 0..<preferenceCount) {
            prefs.add(getPreference(i))
        }
        return prefs
    }

    private fun PreferenceGroup.getAndRemoveAllChildren(): List<Preference> {
        val prefs = getAllChildren()
        removeAll()
        return prefs
    }

    private fun PreferenceGroup.addPreferences(prefs: List<Preference>) {
        for (pref in prefs) {
            addPreference(pref)
        }
    }

    private fun getAllPreferences(): List<Preference> {
        val prefs = mutableListOf<Preference>()
        for (i in 0 until preferenceScreen.preferenceCount) {
            val pref = preferenceScreen.getPreference(i)
            if (pref.key == INVISIBLE_CATEGORY && pref is PreferenceCategory) {
                prefs.addAll(pref.getAllChildren())
            } else {
                prefs.add(pref)
            }
        }
        return prefs
    }

    fun getCachedDevice(deviceAddress: String): CachedBluetoothDevice? {
        val remoteDevice: BluetoothDevice =
            localBluetoothManager.bluetoothAdapter.getRemoteDevice(deviceAddress) ?: return null
        val cachedDevice: CachedBluetoothDevice? =
            localBluetoothManager.cachedDeviceManager.findDevice(remoteDevice)
        if (cachedDevice != null) {
            return cachedDevice
        }
        Log.i(
            TAG, "Add device to cached device manager: " + remoteDevice.anonymizedAddress
        )
        return localBluetoothManager.cachedDeviceManager.addDevice(remoteDevice)
    }

    private class SpotlightPreference(context: Context) : Preference(context) {
        init {
            layoutResource = R.layout.bluetooth_device_spotlight_preference
        }

        override fun onBindViewHolder(holder: PreferenceViewHolder) {
            super.onBindViewHolder(holder)
            holder.isDividerAllowedBelow = false
            holder.isDividerAllowedAbove = false
        }
    }

    companion object {
        const val KEY_DEVICE_ADDRESS = "device_address"
        const val LOADING_PREF = "loading_pref"

        private const val TAG = "BtDetailsConfigFrg"
        private const val INVISIBLE_CATEGORY = "invisible_profile_category"
        private const val EVENT_SWITCH_OFF = 0
        private const val EVENT_SWITCH_ON = 1
        private const val EVENT_CLICK_PRIMARY = 2
        private const val EVENT_INVISIBLE = 0
        private const val EVENT_VISIBLE = 1
    }
}
