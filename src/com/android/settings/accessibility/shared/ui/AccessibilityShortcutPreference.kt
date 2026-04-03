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

package com.android.settings.accessibility.shared.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.hardware.input.InputManager
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentManager
import androidx.preference.Preference
import com.android.internal.accessibility.common.ShortcutConstants
import com.android.internal.accessibility.util.ShortcutUtils
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityShortcutsTutorial
import com.android.settings.accessibility.AccessibilityUtil
import com.android.settings.accessibility.ShortcutPreference
import com.android.settings.accessibility.extensions.isInSetupWizard
import com.android.settings.accessibility.shared.data.AccessibilityShortcutDataStore
import com.android.settings.accessibility.shortcuts.EditShortcutsPreferenceFragment
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settingslib.core.instrumentation.Instrumentable.METRICS_CATEGORY_UNKNOWN
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSetWarningProvider
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.WarningInfo
import com.android.settingslib.preference.PreferenceBinding

/**
 * An interface for providing the name of the feature associated with the accessibility shortcut.
 * This is used, for example, to display the feature name in the accessibility shortcut tutorial.
 */
interface ShortcutFeatureNameProvider {
    fun getFeatureName(context: Context): CharSequence
}

/**
 * Metadata of Accessibility shortcuts.
 *
 * This class displays a [ShortcutPreference] on the screen, which has a toggle switch. It handles
 * the data storage for the shortcut's on/off state via an [AccessibilityShortcutDataStore] and
 * manages user interactions.
 */
open class AccessibilityShortcutPreference(
    val context: Context,
    override val key: String,
    @StringRes override val purpose: Int,
    @StringRes override val title: Int = 0,
    val componentName: ComponentName,
    @StringRes val featureName: Int = 0,
    val metricsCategory: Int = METRICS_CATEGORY_UNKNOWN,
) :
    BooleanValuePreference,
    PreferenceBinding,
    PreferenceSummaryProvider,
    PreferenceSetWarningProvider,
    PreferenceLifecycleProvider {

    private lateinit var lifecycleContext: PreferenceLifecycleContext

    protected open val dataStore: AccessibilityShortcutDataStore by lazy {
        AccessibilityShortcutDataStore(context, componentName)
    }

    override fun createWidget(context: Context): Preference = ShortcutPreference(context, null)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        if (preference is ShortcutPreference) {
            preference.apply {
                isChecked = dataStore.getBoolean(key) ?: false
                isSettingsEditable = getSettingsEditable(context)
            }
        }
    }

    override fun storage(context: Context): KeyValueStore = dataStore

    override fun getReadPermissions(context: Context) = SettingsSecureStore.getReadPermissions()

    override fun getWritePermissions(context: Context) = SettingsSecureStore.getWritePermissions()

    override fun getEnabledDescription(): String = "Clients can only set the value to 'false'"

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(
        context: Context,
        value: Boolean?,
        callingPid: Int,
        callingUid: Int,
    ) =
        when (value) {
            true -> ReadWritePermit.DISALLOW
            else -> ReadWritePermit.ALLOW
        }

    private fun getWarningMessageBasedOnShortcutsTutorial(): String? {
        if (featureName == 0) { // return if resource is zero
            return null
        }

        val instructions = ArrayList<CharSequence>()
        val buttonMode = ShortcutUtils.getButtonMode(context, context.userId)
        val shortcutTypes = dataStore.getUserShortcutTypes()

        for (shortcutType in AccessibilityUtil.SHORTCUTS_ORDER_IN_UI) {
            if ((shortcutTypes and shortcutType) == 0) {
                continue
            }

            val instruction = AccessibilityShortcutsTutorial.getShortcutInstruction(
                context,
                shortcutType,
                buttonMode,
                context.getString(featureName),
                context.isInSetupWizard()
            )

            // Filter out any blank instructions
            if (instruction.isNotBlank()) {
                instructions.add(instruction)
            }
        }

        if (instructions.isNotEmpty()) {
            // Create warning message based on the instructions list
            return instructions.joinToString(separator = "\n")
        }

        return null
    }

    override val setWarning = getWarningMessageBasedOnShortcutsTutorial()?.let { warningMessage ->
        WarningInfo(warningMessage = warningMessage)
    }

    override val supportsWrite = true
    private val inputDeviceListener by lazy {
        object : InputManager.InputDeviceListener {
            override fun onInputDeviceAdded(deviceId: Int) {
                if (::lifecycleContext.isInitialized) {
                    lifecycleContext.notifyPreferenceChange(key)
                }
            }

            override fun onInputDeviceRemoved(deviceId: Int) {
                if (::lifecycleContext.isInitialized) {
                    lifecycleContext.notifyPreferenceChange(key)
                }
            }

            override fun onInputDeviceChanged(deviceId: Int) {
                // No-op
            }
        }
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        lifecycleContext = context
        context
            .getSystemService(InputManager::class.java)
            ?.registerInputDeviceListener(inputDeviceListener, null)
        val shortcutPreference = context.requirePreference<ShortcutPreference>(key)
        shortcutPreference.setOnClickCallback(
            object : ShortcutPreference.OnClickCallback {

                override fun onSettingsClicked(preference: ShortcutPreference?) {
                    if (preference == null) return
                    onSettingsClicked(preference, context)
                }

                override fun onToggleClicked(preference: ShortcutPreference?) {
                    if (preference == null) return
                    onToggleClicked(preference, context)
                }
            }
        )
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        super.onDestroy(context)
        context
            .getSystemService(InputManager::class.java)
            ?.unregisterInputDeviceListener(inputDeviceListener)
    }

    protected open fun onSettingsClicked(
        preference: ShortcutPreference,
        context: PreferenceLifecycleContext,
    ) {
        showEditShortcutsScreen(preference.context, preference.title ?: "")
        featureFactory.metricsFeatureProvider.logClickedPreference(preference, metricsCategory)
    }

    protected open fun onToggleClicked(
        preference: ShortcutPreference,
        context: PreferenceLifecycleContext,
    ) {
        if (preference.isChecked) {
            showShortcutsTutorial(
                context,
                context.childFragmentManager,
                dataStore.getUserShortcutTypes(),
                preference.context.isInSetupWizard(),
            )
        }
        dataStore.setBoolean(key, preference.isChecked)
    }

    override fun getSummary(context: Context): CharSequence? {
        if (!getSettingsEditable(context)) {
            return context.getText(R.string.accessibility_shortcut_edit_dialog_title_hardware)
        }

        if (dataStore.getBoolean(key) != true) {
            return context.getText(R.string.accessibility_shortcut_state_off)
        }

        return AccessibilityUtil.getShortcutSummaryList(context, dataStore.getUserShortcutTypes())
    }

    protected open fun getSettingsEditable(context: Context): Boolean = true

    protected fun showShortcutsTutorial(
        context: Context,
        fragmentManager: FragmentManager,
        shortcutTypes: Int,
        isInSetupWizard: Boolean,
    ) {
        // There is no tutorial for the key gesture shortcut, so do not try to show the
        // shortcuts tutorial for the key gesture.
        val supportedShortcutTypes =
            AccessibilityUtil.removeTypeFromShortcutTypes(
                shortcutTypes,
                ShortcutConstants.UserShortcutType.KEY_GESTURE,
            )

        if (supportedShortcutTypes == ShortcutConstants.UserShortcutType.DEFAULT) {
            return
        }

        val featureLabel: CharSequence =
            when {
                title != 0 -> context.getText(title)
                this is ShortcutFeatureNameProvider -> getFeatureName(context)
                else -> ""
            }

        AccessibilityShortcutsTutorial.DialogFragment.showDialog(
            fragmentManager,
            supportedShortcutTypes,
            featureLabel,
            isInSetupWizard,
        )
    }

    protected fun showEditShortcutsScreen(context: Context, screenTitle: CharSequence) {
        EditShortcutsPreferenceFragment.showEditShortcutScreen(
            context,
            metricsCategory,
            screenTitle,
            componentName,
            (context as? Activity)?.intent,
        )
    }
}
