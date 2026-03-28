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

package com.android.settings.accessibility.textreading.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import androidx.preference.Preference
import com.android.internal.accessibility.AccessibilityShortcutController
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityQuickSettingUtils
import com.android.settings.accessibility.AccessibilityQuickSettingsTooltipWindow
import com.android.settings.accessibility.TextReadingPreferenceFragment.EntryPoint
import com.android.settings.accessibility.TooltipSliderPreference
import com.android.settings.accessibility.extensions.isInSetupWizard
import com.android.settings.accessibility.shared.utils.shouldShowFocusRingsInSuw
import com.android.settings.accessibility.textreading.data.FontSizeDataStore
import com.android.settingslib.R as SettingsLibR
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.metadata.IntRangeValuePreference
import com.android.settingslib.metadata.MUSTPASS_SET
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.widget.SliderPreference
import com.android.settingslib.widget.SliderPreferenceBinding
import com.google.android.material.slider.Slider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class FontSizePreference(
    context: Context,
    @EntryPoint private val entryPoint: Int,
    val isUiOnly: Boolean,
) : IntRangeValuePreference, SliderPreferenceBinding, PreferenceLifecycleProvider {
    private val fontSizeDataStore by lazy {
        FontSizeDataStore(context = context, entryPoint = entryPoint)
    }
    private val fontSizes by lazy { fontSizeDataStore.fontSizeData.value.values }
    private val fontSizesLabel by lazy {
        fontSizes
            .map { value ->
                context.getString(SettingsLibR.string.font_scale_percentage, (value * 100).toInt())
            }
            .toTypedArray()
    }

    private val delegate by lazy {
        FontSizeDelegate(fontSizeDataStore = fontSizeDataStore, dataStoreKey = KEY)
    }

    val fontSizePreview
        get() = delegate.sizePreview

    override fun tags(context: Context): Array<String> {
        if (isUiOnly) {
            return arrayOf(UI_ONLY_PREFERENCE)
        }
        return arrayOf(MUSTPASS_SET)
    }

    override fun getReadPermissions(context: Context) = Permissions.EMPTY

    override fun getWritePermissions(context: Context) =
        Permissions.allOf(
            Manifest.permission.WRITE_SETTINGS, // required to write System settings
            Manifest.permission.WRITE_SECURE_SETTINGS, // required to write Secure settings
        )

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(
        context: Context,
        callingPid: Int,
        callingUid: Int,
    ): @ReadWritePermit Int {
        return ReadWritePermit.ALLOW
    }

    override val supportsWrite = true

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.font_size_purpose

    override val title: Int
        get() = R.string.title_font_size

    override val summary: Int
        get() = R.string.short_summary_font_size

    override val keywords: Int
        get() = R.string.keywords_font_size

    override fun createWidget(context: Context) =
        TooltipSliderPreference(context).apply {
            setIconStart(R.drawable.ic_remove_24dp)
            setIconStartContentDescription(R.string.font_size_make_smaller_desc)
            setIconEnd(R.drawable.ic_add_24dp)
            setIconEndContentDescription(R.string.font_size_make_larger_desc)
            setTickVisible(true)
            setDefaultValue(delegate.sizePreview.value.currentIndex)
            val onCommitAction: (Int) -> Unit = { committedIndex ->
                if (committedIndex != fontSizeDataStore.getInt(KEY)) {
                    showQuickSettingsTooltipIfNeeded(this)
                }
            }
            val onUpdateUi: (Int) -> Unit = { index ->
                // Updates the 'Percentage' text description for screen readers
                setSliderStateDescription(fontSizesLabel[index])
            }
            setExtraChangeListener { _, value, _ ->
                val index = value.toInt()
                delegate.onValueChange(
                    index = index,
                    onUpdateUi = onUpdateUi,
                    onCommitAction = onCommitAction,
                )
            }
            setExtraTouchListener(
                object : Slider.OnSliderTouchListener {
                    override fun onStartTrackingTouch(slider: Slider) {
                        delegate.onStartTrackingTouch()
                    }

                    override fun onStopTrackingTouch(slider: Slider) {
                        val index = slider.value.toInt()
                        delegate.onStopTrackingTouch(index, onCommitAction)
                    }
                }
            )
        }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        // The PreferenceMetadata is persistent to allow GET/SET api to access the storage.
        // Set the preference widget to non-persistent to prevent it trying to save the value to
        // datastore while the user is dragging, or when we want to have some delay to show the
        // preview before committing the changes.
        preference as SliderPreference
        preference.run {
            value = delegate.sizePreview.value.currentIndex
            setSliderStateDescription(fontSizesLabel[value])
            isPersistent = false
            // This change makes the row that contains the "Font size" slider unable to be focused,
            // but allows the slider and its buttons to be focusable.
            if (shouldShowFocusRingsInSuw(context)) {
                isSelectable = false
            }
        }
    }

    override fun onStart(context: PreferenceLifecycleContext) {
        super.onStart(context)
        context.findPreference<TooltipSliderPreference>(KEY)?.run {
            // This is needed to prevent slider value gets overwrites by
            // [View#onRestoreInstanceState].
            // When the display size changed, it triggers the configuration changes. The
            // SliderPreference widget is not a persistent preference, hence when the data is
            // changed outside of Settings app while the display size slider is visible, the Slider
            // widget won't save the correct index when
            // [View#onSaveInstanceState] is called.

            value = delegate.sizePreview.value.currentIndex

            if (needsQSTooltipReshow) {
                context.lifecycleScope.launch(Dispatchers.Main) {
                    showQuickSettingsTooltipIfNeeded(preference = this@run)
                }
            }
        }
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        super.onDestroy(context)
        context.findPreference<TooltipSliderPreference>(KEY)?.run { dismissTooltip() }
    }

    override fun getIncrementStep(context: Context): Int {
        return 1
    }

    override fun getMinValue(context: Context): Int {
        return 0
    }

    override fun getMaxValue(context: Context): Int {
        return fontSizes.size - 1
    }

    override fun storage(context: Context): KeyValueStore {
        return fontSizeDataStore
    }

    private fun showQuickSettingsTooltipIfNeeded(preference: TooltipSliderPreference) {
        val context = preference.context
        if (context.isInSetupWizard()) {
            // Don't show Quick Settings tooltip in Setup Wizard
            return
        }

        val tileComponentName = AccessibilityShortcutController.FONT_SIZE_COMPONENT_NAME
        val shouldSkipShowingTooltip =
            !preference.needsQSTooltipReshow &&
                AccessibilityQuickSettingUtils.hasValueInSharedPreferences(
                    context,
                    tileComponentName,
                )

        if (shouldSkipShowingTooltip) {
            return
        }

        // TODO (b/287728819): Move tooltip showing to SystemUI
        val decorView = (context as? Activity)?.window?.peekDecorView()
        if (decorView != null) {
            val tooltipContent =
                context.getText(R.string.accessibility_font_scaling_auto_added_qs_tooltip_content)
            val tooltipWindow: AccessibilityQuickSettingsTooltipWindow =
                preference.createTooltipWindow()
            tooltipWindow.setup(
                tooltipContent,
                R.drawable.accessibility_auto_added_qs_tooltip_illustration,
            )
            tooltipWindow.showAtTopCenter(decorView)
        }
        AccessibilityQuickSettingUtils.optInValueToSharedPreferences(context, tileComponentName)
        preference.needsQSTooltipReshow = false
    }

    companion object {
        const val KEY = "font_size"
    }
}
