/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.connecteddevice.display

import android.app.settings.SettingsEnums
import android.os.Bundle
import android.window.DesktopExperienceFlags
import androidx.annotation.VisibleForTesting
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragmentBase
import com.android.settings.Utils.createAccessibleSequence
import com.android.settings.accessibility.TextReadingPreferenceFragment
import com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.EXTERNAL_DISPLAY_HELP_URL
import com.android.settings.core.SubSettingLauncher

/**
 * Fragment containing list of preferences for a single display, which gets updated dynamically,
 * based on the currently selected display
 */
open class SelectedDisplayPreferenceFragment : SettingsPreferenceFragmentBase() {

    private lateinit var viewModel: DisplayPreferenceViewModel

    private lateinit var rotationEntries: Array<String>

    override fun getMetricsCategory(): Int {
        return SettingsEnums.SETTINGS_CONNECTED_DEVICE_CATEGORY
    }

    override fun getHelpResource(): Int {
        return EXTERNAL_DISPLAY_HELP_URL
    }

    override fun getPreferenceScreenResId(): Int {
        return R.xml.external_display_settings
    }

    open fun launchResolutionSelector(displayId: Int) {
        val args =
            Bundle().apply {
                putInt(ExternalDisplaySettingsConfiguration.DISPLAY_ID_ARG, displayId)
            }
        SubSettingLauncher(requireContext())
            .setDestination(ResolutionPreferenceFragment::class.java.name)
            .setArguments(args)
            .setSourceMetricsCategory(metricsCategory)
            .launch()
    }

    open fun launchBuiltinDisplaySettings() {
        val args = Bundle()
        SubSettingLauncher(requireContext())
            .setDestination(TextReadingPreferenceFragment::class.java.getName())
            .setArguments(args)
            .setSourceMetricsCategory(metricsCategory)
            .launch()
    }

    override fun onActivityCreatedCallback(savedInstanceState: Bundle?) {
        // No-op, nothing specific to be done here
    }

    override fun onCreateCallback(icicle: Bundle?) {
        addPreferencesFromResource(R.xml.external_display_settings)
        // TODO(b/409354332): Setup viewmodel
        setup()
    }

    override fun onStartCallback() {
        // TODO(b/409354332): Observe viewmodel
    }

    override fun onStopCallback() {
        // No-op, viewModel observer should be managed by view lifecycle
    }

    private fun setup() {
        // TODO(b/409354332): Setup preference
    }

    private fun update(
        displayId: Int,
        enabledDisplays: Map<Int, DisplayDevice>,
        isMirroring: Boolean
    ) {
        // TODO(b/409354332): Update preferences
    }

    private fun mirroringPreference(): MirrorPreference {
        return MirrorPreference(
                requireContext(),
                DesktopExperienceFlags.ENABLE_DISPLAY_CONTENT_MODE_MANAGEMENT.isTrue(),
            )
            .apply {
                setTitle(PrefInfo.DISPLAY_MIRRORING.titleResource)
                key = PrefInfo.DISPLAY_MIRRORING.key
            }
    }

    private fun builtinDisplaySizePreference(): Preference {
        return Preference(requireContext()).apply {
            isPersistent = false
            setTitle(PrefInfo.DISPLAY_SIZE_AND_TEXT.titleResource)
            key = PrefInfo.DISPLAY_SIZE_AND_TEXT.key
            onPreferenceClickListener =
                object : Preference.OnPreferenceClickListener {
                    override fun onPreferenceClick(preference: Preference): Boolean {
                        launchBuiltinDisplaySettings()
                        return true
                    }
                }
        }
    }

    private fun externalDisplaySizePreference(): ExternalDisplaySizePreference {
        return ExternalDisplaySizePreference(requireContext(), /* attrs= */ null).apply {
            setTitle(PrefInfo.DISPLAY_SIZE.titleResource)
            key = PrefInfo.DISPLAY_SIZE.key
            setSummary(R.string.screen_zoom_short_summary)
        }
    }

    private fun updateExternalDisplaySizePreference(
        preference: ExternalDisplaySizePreference,
        display: DisplayDevice,
    ) {
        val displayMode = display.mode ?: return
        preference.setStateForPreference(
            displayMode.physicalWidth,
            displayMode.physicalHeight,
            display.id,
        )
    }

    private fun resolutionPreference(): Preference {
        return Preference(requireContext()).apply {
            setTitle(PrefInfo.DISPLAY_RESOLUTION.titleResource)
            key = PrefInfo.DISPLAY_RESOLUTION.key
            onPreferenceClickListener =
                object : Preference.OnPreferenceClickListener {
                    override fun onPreferenceClick(preference: Preference): Boolean {
                        writePreferenceClickMetric(preference)
                        val displayId = viewModel.uiState.value?.selectedDisplayId ?: return false
                        launchResolutionSelector(displayId)
                        return true
                    }
                }
        }
    }

    private fun updateResolutionPreference(preference: Preference, display: DisplayDevice) {
        val displayMode = display.mode ?: return
        val width = displayMode.getPhysicalWidth()
        val height = displayMode.getPhysicalHeight()
        preference.setSummary(
            createAccessibleSequence(
                "$width x $height",
                getResources().getString(R.string.screen_resolution_delimiter_a11y, width, height),
            )
        )
    }

    private fun rotationPreference(): ListPreference {
        rotationEntries =
            arrayOf(
                requireContext().getString(R.string.external_display_standard_rotation),
                requireContext().getString(R.string.external_display_rotation_90),
                requireContext().getString(R.string.external_display_rotation_180),
                requireContext().getString(R.string.external_display_rotation_270),
            )
        val rotationEntryValues = arrayOf("0", "1", "2", "3")
        return ListPreference(requireContext()).apply {
            setTitle(PrefInfo.DISPLAY_ROTATION.titleResource)
            key = PrefInfo.DISPLAY_ROTATION.key
            setEntries(rotationEntries)
            setEntryValues(rotationEntryValues)
            onPreferenceChangeListener =
                object : Preference.OnPreferenceChangeListener {
                    override fun onPreferenceChange(
                        preference: Preference,
                        newValue: Any?,
                    ): Boolean {
                        writePreferenceClickMetric(preference)
                        val displayId = viewModel.uiState.value?.selectedDisplayId ?: return false
                        val rotation = Integer.parseInt(newValue as String)
                        if (!viewModel.injector.freezeDisplayRotation(displayId, rotation)) {
                            return false
                        }
                        setValueIndex(rotation)
                        return true
                    }
                }
        }
    }

    private fun updateRotationPreference(preference: ListPreference, display: DisplayDevice) {
        val rotation = viewModel.injector.getDisplayUserRotation(display.id)
        preference.apply {
            setValueIndex(rotation)
            setSummary(rotationEntries[rotation])
        }
    }

    @VisibleForTesting
    internal enum class DisplayType {
        BUILTIN_DISPLAY,
        EXTERNAL_DISPLAY,
        UNKNOWN,
    }

    @VisibleForTesting
    internal enum class PrefInfo(
        val titleResource: Int,
        val key: String,
        val displayType: DisplayType,
    ) {
        DISPLAY_MIRRORING(
            R.string.external_display_mirroring_title,
            "pref_key_builtin_display_mirroring",
            DisplayType.BUILTIN_DISPLAY,
        ),
        DISPLAY_SIZE_AND_TEXT(
            R.string.accessibility_text_reading_options_title,
            "pref_key_builtin_display_size_and_text",
            DisplayType.BUILTIN_DISPLAY,
        ),
        DISPLAY_SIZE(
            R.string.screen_zoom_title,
            "pref_key_external_display_size",
            DisplayType.EXTERNAL_DISPLAY,
        ),
        DISPLAY_RESOLUTION(
            R.string.external_display_resolution_settings_title,
            "pref_key_external_display_resolution",
            DisplayType.EXTERNAL_DISPLAY,
        ),
        DISPLAY_ROTATION(
            R.string.external_display_rotation,
            "pref_key_external_display_rotation",
            DisplayType.EXTERNAL_DISPLAY,
        ),
        EMPTY_DISPLAY(
            R.string.external_display_not_found_footer_title,
            "pref_key_empty_display",
            DisplayType.UNKNOWN,
        ),
    }
}
