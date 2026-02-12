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
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat
import androidx.preference.TwoStatePreference
import com.android.settings.R
import com.android.settings.bluetooth.Utils.INVISIBLE_CATEGORY
import com.android.settings.bluetooth.ui.model.DeviceSettingPreferenceModel
import com.android.settings.bluetooth.ui.model.FragmentTypeModel
import com.android.settings.bluetooth.ui.view.DeviceDetailsMoreSettingsFragment
import com.android.settings.bluetooth.ui.viewmodel.BluetoothDeviceDetailsViewModel
import com.android.settings.connecteddevice.ConnectedDeviceDashboardFragment
import com.android.settings.core.SubSettingLauncher
import com.android.settings.dashboard.RestrictedDashboardFragment
import com.android.settings.flags.Flags
import com.android.settings.overlay.FeatureFactory
import com.android.settings.spa.preference.ComposePreference
import com.android.settingslib.PrimarySwitchPreference
import com.android.settingslib.bluetooth.CachedBluetoothDevice
import com.android.settingslib.bluetooth.LocalBluetoothManager
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingActionModel
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingConfigNodeModel
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingIcon
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingLayout
import com.android.settingslib.spa.widget.ui.LinearLoadingBar
import com.android.settingslib.widget.BannerMessagePreference
import com.android.settingslib.widget.CardPreference
import com.android.settingslib.widget.ChainedPreferenceGroup
import com.android.settingslib.widget.FooterPreference
import com.android.settingslib.widget.SegmentedButtonPreference
import com.android.settingslib.widget.UntitledPreferenceCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch

/** Base class for bluetooth settings which makes the preference visibility/order configurable. */
abstract class BluetoothDetailsConfigurableFragment :
    RestrictedDashboardFragment(UserManager.DISALLOW_CONFIG_BLUETOOTH) {
    protected lateinit var localBluetoothManager: LocalBluetoothManager
    protected lateinit var deviceAddress: String
    protected lateinit var cachedDevice: CachedBluetoothDevice
    private lateinit var displayOrder: DeviceSettingLayout
    private lateinit var originalDisplayOrder: DeviceSettingLayout
    private val metricsFeatureProvider = FeatureFactory.featureFactory.metricsFeatureProvider
    private val prefVisibility = mutableMapOf<String, MutableStateFlow<Boolean>>()
    private val uiJobs = mutableListOf<Job>()

    private lateinit var viewModel: BluetoothDeviceDetailsViewModel

    private val invisiblePrefCategory: PreferenceGroup by lazy {
        preferenceScreen.findPreference<PreferenceGroup>(INVISIBLE_CATEGORY)
            ?: run {
                PreferenceCategory(requireContext())
                    .apply {
                        key = INVISIBLE_CATEGORY
                        isVisible = false
                        isOrderingAsAdded = true
                    }
                    .also { preferenceScreen.addPreference(it) }
            }
    }

    override fun onAttach(context: Context) {
        localBluetoothManager = Utils.getLocalBtManager(context)
        deviceAddress =
            arguments?.getString(KEY_DEVICE_ADDRESS)
                ?: run {
                    Log.w(TAG, "onAttach() address is null!")
                    super.onAttach(context)
                    // Go to connected devices screen if address is null.
                    launchConnectedDevicesScreen()
                    finish()
                    return
                }
        cachedDevice =
            getCachedDevice(deviceAddress)
                ?: run {
                    Log.w(TAG, "onAttach() CachedDevice is null!")
                    super.onAttach(context)
                    // Go to connected devices screen if the device is not found.
                    launchConnectedDevicesScreen()
                    finish()
                    return
                }
        super.onAttach(context)
        viewModel =
            ViewModelProvider(
                    this,
                    BluetoothDeviceDetailsViewModel.Factory(
                        requireActivity().application,
                        cachedDevice,
                        Dispatchers.IO,
                    ),
                )
                .get(BluetoothDeviceDetailsViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var fakeId = 0
        originalDisplayOrder =
            DeviceSettingLayout(
                preferenceScreen.getAllPreferences().map {
                    DeviceSettingConfigNodeModel.Item.BuiltinItem.CommonBuiltinItem(
                        fakeId++,
                        false,
                        it.key,
                    )
                }
            )
        displayOrder = originalDisplayOrder
    }

    fun requestUpdateLayout(prefKeyOrder: List<String>?) {
        if (prefKeyOrder == null) {
            return
        }
        val order =
            DeviceSettingLayout(
                prefKeyOrder.mapIndexed { idx, prefKey ->
                    DeviceSettingConfigNodeModel.Item.BuiltinItem.CommonBuiltinItem(
                        idx,
                        false,
                        prefKey,
                    )
                }
            )
        constructLayout(order)
    }

    fun requestUpdateLayout(fragmentType: FragmentTypeModel) {
        lifecycleScope.launch { updateLayoutInternal(fragmentType) }
    }

    private fun DeviceSettingLayout.traverse(
        runnable: (DeviceSettingConfigNodeModel.Group?, DeviceSettingConfigNodeModel.Item) -> Unit
    ) {
        for (node in nodes) {
            when (node) {
                is DeviceSettingConfigNodeModel.Group -> {
                    for (item in node.children) {
                        runnable(node, item)
                    }
                }

                is DeviceSettingConfigNodeModel.Item -> {
                    runnable(null, node)
                }
            }
        }
    }

    private suspend fun updateLayoutInternal(fragmentType: FragmentTypeModel) {
        val pageLayout =
            viewModel.getNodes(fragmentType)
                ?: run {
                    constructLayout(originalDisplayOrder)
                    return
                }
        pageLayout.traverse { parent, settingItem ->
            if (settingItem is DeviceSettingConfigNodeModel.Item.BuiltinItem) {
                findPreference<Preference>(settingItem.preferenceKey)?.let { pref ->
                    if (
                        settingItem
                            is DeviceSettingConfigNodeModel.Item.BuiltinItem.BluetoothProfilesItem
                    ) {
                        use(BluetoothDetailsProfilesController::class.java)?.run {
                            setInvisibleProfiles(settingItem.invisibleProfiles)
                        }
                    }
                    logItemShown(pref.key, pref.isVisible)
                }
            }
        }
        constructLayout(pageLayout)
    }

    private fun listenToAppProvidedSettingChanges(
        container: PreferenceGroup,
        settingId: Int,
        prefOrder: Int,
        highlight: Boolean,
    ) {
        val prefKey = getPreferenceKey(settingId)
        val deviceSetting =
            viewModel.getDeviceSetting(cachedDevice, settingId).dropWhile { it == null }

        deviceSetting
            .onEach { logItemShown(prefKey, it != null) }
            .launchIn(lifecycleScope)
            .also { uiJobs.add(it) }
        deviceSetting
            .withIndex()
            .debounce {
                // Debounce here otherwise ANC toggle may flicker after user clicks.
                if (
                    it.index > 0 && it.value is DeviceSettingPreferenceModel.MultiTogglePreference
                ) {
                    200
                } else {
                    0
                }
            }
            .map { it.value }
            .onEach { refreshAppProvidedPreference(container, prefOrder, it, settingId, highlight) }
            .launchIn(lifecycleScope)
            .also { uiJobs.add(it) }
    }

    private fun refreshAppProvidedPreference(
        container: PreferenceGroup,
        prefOrder: Int,
        model: DeviceSettingPreferenceModel?,
        settingId: Int,
        highlighted: Boolean,
    ) {
        // Each app-provided preference is wrapped by a ChainedPreferenceGroup so the preference
        // corner style will be shown correctly.
        val prefWrapperKey = getPreferenceChainedWrapperKey(settingId)
        val existedPrefWrapper = container.findPreference<ChainedPreferenceGroup>(prefWrapperKey)
        if (model == null) {
            existedPrefWrapper?.let { container.removePreference(it) }
            Utils.updateVisibilityAccordingToChildren(container)
            return
        }
        val prefKey = getPreferenceKey(settingId)
        val existedPref = existedPrefWrapper?.getPreference(0)
        val generatedPref =
            when (model) {
                is DeviceSettingPreferenceModel.PlainPreference -> {
                    val pref =
                        existedPref
                            ?: run {
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
                        onPreferenceClickListener =
                            Preference.OnPreferenceClickListener {
                                logItemClick(prefKey, EVENT_CLICK_PRIMARY)
                                model.action?.let { triggerAction(it) }
                                true
                            }
                    }
                }

                is DeviceSettingPreferenceModel.SwitchPreference ->
                    if (model.action == null) {
                        val pref =
                            existedPref as? SwitchPreferenceCompat
                                ?: SwitchPreferenceCompat(requireContext())
                        pref.apply {
                            title = model.title
                            summary = model.summary
                            icon = getDrawable(model.icon)
                            isChecked = model.checked
                            isEnabled = !model.disabled
                            onPreferenceChangeListener =
                                object : Preference.OnPreferenceChangeListener {
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
                            existedPref as? PrimarySwitchPreference
                                ?: PrimarySwitchPreference(requireContext())
                        pref.apply {
                            title = model.title
                            summary = model.summary
                            icon = getDrawable(model.icon)
                            isChecked = model.checked
                            isEnabled = !model.disabled
                            isSwitchEnabled = !model.disabled
                            onPreferenceClickListener =
                                Preference.OnPreferenceClickListener {
                                    logItemClick(prefKey, EVENT_CLICK_PRIMARY)
                                    triggerAction(model.action)
                                    true
                                }
                            onPreferenceChangeListener =
                                object : Preference.OnPreferenceChangeListener {
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
                    val pref =
                        existedPref as? SegmentedButtonPreference
                            ?: SegmentedButtonPreference(requireContext())
                    pref.apply {
                        for (idx in 0..3) {
                            if (idx >= model.toggles.size) {
                                pref.setButtonVisibility(idx, false)
                                continue
                            }
                            pref.setButtonVisibility(idx, true)
                            pref.setButtonEnabled(idx, model.isAllowedChangingState)
                            getDrawable(model.toggles[idx].icon)?.let {
                                pref.setUpButton(idx, model.toggles[idx].label, it)
                            }
                            pref.setCheckedIndex(
                                if (model.isAllowedChangingState) model.selectedIndex else -1
                            )
                            pref.setOnButtonClickListener { _, checkedId, isChecked ->
                                val checkedIndex =
                                    when (checkedId) {
                                        com.android.settingslib.widget.preference.segmentedbutton.R
                                            .id
                                            .button_1 -> {
                                            0
                                        }

                                        com.android.settingslib.widget.preference.segmentedbutton.R
                                            .id
                                            .button_2 -> {
                                            1
                                        }

                                        com.android.settingslib.widget.preference.segmentedbutton.R
                                            .id
                                            .button_3 -> {
                                            2
                                        }

                                        com.android.settingslib.widget.preference.segmentedbutton.R
                                            .id
                                            .button_4 -> {
                                            3
                                        }

                                        else -> {
                                            return@setOnButtonClickListener
                                        }
                                    }
                                if (isChecked) {
                                    for (idx in 0..3) {
                                        pref.setButtonEnabled(idx, false)
                                    }
                                    model.onSelectedChange(checkedIndex)
                                }
                            }
                        }
                    }
                }

                is DeviceSettingPreferenceModel.FooterPreference -> {
                    val pref =
                        existedPref as? FooterPreference ?: FooterPreference(requireContext())
                    pref.apply { title = model.footerText }
                }

                is DeviceSettingPreferenceModel.MoreSettingsPreference -> {
                    val pref = existedPref ?: Preference(requireContext())
                    pref.apply {
                        title =
                            context.getString(
                                R.string.bluetooth_device_more_settings_preference_title
                            )
                        summary =
                            context.getString(
                                R.string.bluetooth_device_more_settings_preference_summary
                            )
                        icon =
                            context.getDrawable(
                                if (Flags.enableBluetoothSettingsExpressiveDesign()) {
                                    R.drawable.ic_bluetooth_more_vert
                                } else {
                                    R.drawable.ic_chevron_right_24dp
                                }
                            )
                        onPreferenceClickListener =
                            Preference.OnPreferenceClickListener {
                                logItemClick(prefKey, EVENT_CLICK_PRIMARY)
                                SubSettingLauncher(context)
                                    .setDestination(
                                        DeviceDetailsMoreSettingsFragment::class.java.name
                                    )
                                    .setSourceMetricsCategory(getMetricsCategory())
                                    .setArguments(
                                        Bundle().apply {
                                            putString(KEY_DEVICE_ADDRESS, cachedDevice.address)
                                        }
                                    )
                                    .launch()
                                true
                            }
                    }
                }

                is DeviceSettingPreferenceModel.BannerPreference -> {
                    val pref =
                        existedPref as? BannerMessagePreference
                            ?: BannerMessagePreference(requireContext())
                    pref.apply {
                        setAttentionLevel(BannerMessagePreference.AttentionLevel.NORMAL)
                        key = prefKey
                        order = prefOrder
                        title = model.title
                        summary = model.message
                        icon = getDrawable(model.icon, false)
                        if (model.positiveButton != null && model.positiveButton.action != null) {
                            setPositiveButtonText(model.positiveButton.label)
                            setPositiveButtonOnClickListener {
                                model.positiveButton.action?.let { triggerAction(it) }
                            }
                            setPositiveButtonEnabled(true)
                            setPositiveButtonVisible(true)
                        }
                        if (model.negativeButton != null && model.negativeButton.action != null) {
                            setNegativeButtonText(model.negativeButton.label)
                            setNegativeButtonOnClickListener {
                                model.negativeButton.action?.let { triggerAction(it) }
                            }
                            setNegativeButtonEnabled(true)
                            setNegativeButtonVisible(true)
                        }
                    }
                }

                else -> null
            }
        if (existedPref == null && generatedPref != null) {
            val generatedPrefWrapper =
                ChainedPreferenceGroup(requireContext()).apply {
                    key = prefWrapperKey
                    order = prefOrder
                }
            container.addPreference(generatedPrefWrapper)
            generatedPref.key = prefKey
            generatedPrefWrapper.addPreference(generatedPref)
            Utils.updateVisibilityAccordingToChildren(container)
        }
    }

    private fun getDrawable(
        deviceSettingIcon: DeviceSettingIcon?,
        applyTint: Boolean = true,
    ): Drawable? =
        when (deviceSettingIcon) {
            is DeviceSettingIcon.BitmapIcon ->
                deviceSettingIcon.bitmap.toDrawable(requireContext().resources)

            is DeviceSettingIcon.ResourceIcon -> context?.getDrawable(deviceSettingIcon.resId)
            null -> null
        }?.apply {
            if (applyTint) {
                setTint(
                    requireContext()
                        .getColor(
                            com.android.settingslib.widget.theme.R.color
                                .settingslib_materialColorOnSurfaceVariant
                        )
                )
            }
        }

    private fun logItemClick(preferenceKey: String, value: Int = 0) {
        logAction(preferenceKey, SettingsEnums.ACTION_BLUETOOTH_DEVICE_DETAILS_ITEM_CLICKED, value)
    }

    private fun logItemShown(preferenceKey: String, visible: Boolean) {
        if (!visible && !prefVisibility.containsKey(preferenceKey)) {
            return
        }
        prefVisibility
            .computeIfAbsent(preferenceKey) {
                MutableStateFlow(true).also { visibilityFlow ->
                    visibilityFlow
                        .onEach {
                            logAction(
                                preferenceKey,
                                SettingsEnums.ACTION_BLUETOOTH_DEVICE_DETAILS_ITEM_SHOWN,
                                if (it) EVENT_VISIBLE else EVENT_INVISIBLE,
                            )
                        }
                        .launchIn(lifecycleScope)
                }
            }
            .value = visible
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
                    ActivityOptions.makeBasic()
                        .setPendingIntentBackgroundActivityStartMode(
                            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
                        )
                action.pendingIntent.send(options.toBundle())
            }
        }
    }

    private fun constructLayout(layout: DeviceSettingLayout) {
        if (layout == displayOrder) {
            return
        }
        displayOrder = layout
        for (job in uiJobs) {
            job.cancel()
        }
        uiJobs.clear()
        val existedBuiltinPrefs =
            preferenceScreen
                .getAllPreferences(remove = true)
                .filter { !isGeneratedPreference(it.key) }
                .associateBy { it.key }
                .toMutableMap()
        existedBuiltinPrefs.put(
            LOADING_PREF,
            ComposePreference(requireContext()).apply {
                key = LOADING_PREF
                setContent { LinearLoadingBar(isLoading = true) }
            },
        )

        var orderCount = 0
        var previousContainer: PreferenceCategory? = null
        layout.traverse { parent, nodeItem ->
            orderCount += 1
            val prefOrder = orderCount
            var parentContainer =
                if (parent == null) {
                    preferenceScreen
                } else {
                    val parentKey = getPreferenceCategoryKey(parent)
                    findPreference(parentKey)
                        ?: if (parent.preferenceCategoryTitle.isNullOrEmpty()) {
                            UntitledPreferenceCategory(requireContext())
                                .apply {
                                    key = parentKey
                                    order = prefOrder
                                    isVisible = false
                                }
                                .also { preferenceScreen.addPreference(it) }
                        } else {
                            PreferenceCategory(requireContext())
                                .apply {
                                    key = parentKey
                                    title = parent.preferenceCategoryTitle
                                    order = prefOrder
                                    isVisible = false
                                }
                                .also { preferenceScreen.addPreference(it) }
                        }
                }
            when (nodeItem) {
                is DeviceSettingConfigNodeModel.Item.BuiltinItem -> {
                    existedBuiltinPrefs.remove(nodeItem.preferenceKey)?.let {
                        it.order = prefOrder
                        parentContainer.isVisible = true
                        parentContainer.addPreference(it)
                    }
                }

                is DeviceSettingConfigNodeModel.Item.AppProvidedItem -> {
                    listenToAppProvidedSettingChanges(
                        parentContainer,
                        nodeItem.settingId,
                        prefOrder,
                        nodeItem.highlighted,
                    )
                }
            }
        }
        preferenceScreen.addPreference(invisiblePrefCategory)
        for (invisiblePref in existedBuiltinPrefs.values) {
            invisiblePrefCategory.addPreference(invisiblePref)
        }
    }

    private fun PreferenceGroup.getAllPreferences(remove: Boolean = false): List<Preference> {
        val prefs = mutableListOf<Preference>()
        for (i in 0 until preferenceCount) {
            val pref = getPreference(i)
            if (pref is PreferenceCategory && isGeneratedPreference(pref.key)) {
                prefs.addAll(pref.getAllPreferences(remove))
            } else {
                prefs.add(pref)
            }
        }
        if (remove) {
            removeAll()
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
        if (remoteDevice.bondState != BluetoothDevice.BOND_BONDED) {
            return null
        }
        Log.i(TAG, "Add device to cached device manager: " + remoteDevice.anonymizedAddress)
        return localBluetoothManager.cachedDeviceManager.addDevice(remoteDevice)
    }

    protected fun isCachedDeviceInitialized() = ::cachedDevice.isInitialized

    private fun launchConnectedDevicesScreen() {
        SubSettingLauncher(context)
            .setDestination(ConnectedDeviceDashboardFragment::class.java.getName())
            .setTitleRes(R.string.connected_devices_dashboard_title)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .setSourceMetricsCategory(SettingsEnums.BLUETOOTH_DEVICE_DETAILS)
            .launch()
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
        private const val EVENT_SWITCH_OFF = 0
        private const val EVENT_SWITCH_ON = 1
        private const val EVENT_CLICK_PRIMARY = 2
        private const val EVENT_INVISIBLE = 0
        private const val EVENT_VISIBLE = 1

        private fun getPreferenceKey(settingId: Int) = "DEVICE_SETTING_$settingId"

        private fun getPreferenceChainedWrapperKey(settingId: Int) =
            "DEVICE_SETTING_CHAINED_WRAPPER_$settingId"

        private fun getPreferenceCategoryKey(group: DeviceSettingConfigNodeModel.Group) =
            "GENERATED_CATEGORY_${group.children.firstOrNull()?.settingId}"

        private fun isGeneratedPreference(key: String) =
            key.startsWith("DEVICE_SETTING_") ||
                key.startsWith("DEVICE_SETTING_CHAINED_WRAPPER_") ||
                key.startsWith("GENERATED_CATEGORY_") ||
                key == INVISIBLE_CATEGORY
    }
}
