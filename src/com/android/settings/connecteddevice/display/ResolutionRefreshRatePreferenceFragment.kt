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
import android.icu.text.NumberFormat
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.Utils
import com.android.settings.connecteddevice.display.ResolutionRefreshRatePreferenceViewModel.RefreshRateItem
import com.android.settings.connecteddevice.display.ResolutionRefreshRatePreferenceViewModel.ResolutionItem
import com.android.settings.connecteddevice.display.ResolutionRefreshRatePreferenceViewModel.UiState
import com.android.settingslib.widget.SelectorWithWidgetPreference
import java.util.Locale

class ResolutionRefreshRatePreferenceFragment(
    private val testViewModel: ResolutionRefreshRatePreferenceViewModel? = null
) : SettingsPreferenceFragment() {

    private lateinit var viewModel: ResolutionRefreshRatePreferenceViewModel
    private lateinit var topOptionsPreference: PreferenceCategory
    private lateinit var moreOptionsPreference: PreferenceCategory
    private lateinit var refreshRatePreference: PreferenceCategory

    private val resolutionFormatter =
        NumberFormat.getNumberInstance(Locale.getDefault()).apply { isGroupingUsed = false }
    private val refreshRateFormatter =
        NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            isGroupingUsed = false
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }

    override fun getMetricsCategory(): Int {
        return SettingsEnums.SETTINGS_EXTERNAL_DISPLAY_CATEGORY
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val displayId = arguments?.getInt(DISPLAY_ID_ARG, INVALID_DISPLAY) ?: INVALID_DISPLAY
        if (displayId == INVALID_DISPLAY) {
            finish()
            return
        }
        if (testViewModel != null) {
            // Test-only path
            viewModel = testViewModel
        } else {
            val factory =
                object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(
                        modelClass: Class<T>,
                        extras: CreationExtras,
                    ): T {
                        @Suppress("UNCHECKED_CAST")
                        return ResolutionRefreshRatePreferenceViewModel(
                            requireActivity().application,
                            displayId,
                        )
                            as T
                    }
                }
            viewModel =
                ViewModelProvider(this, factory)
                    .get(ResolutionRefreshRatePreferenceViewModel::class.java)
        }
        addPreferencesFromResource(R.xml.external_display_resolution_refresh_rate_settings)
        setupPreferences()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            if (state == null) {
                finishFragment()
                return@observe
            }
            update(state)
        }
    }

    private fun setupPreferences() {
        val screen = preferenceScreen ?: return
        topOptionsPreference =
            setupCategory(screen, TOP_OPTIONS_KEY, R.string.external_display_resolution_title)
        moreOptionsPreference =
            setupCategory(screen, MORE_OPTIONS_KEY, R.string.external_display_more_options_title)
        refreshRatePreference =
            setupCategory(
                screen,
                REFRESH_RATE_OPTIONS_KEY,
                R.string.external_display_refresh_rate_title,
            )
    }

    private fun update(state: UiState) {
        // Render resolution preferences
        updateResolutionCategory(topOptionsPreference, state, state.topResolutionItems)
        topOptionsPreference.isEnabled = state.topResolutionItems.size > 1
        updateResolutionCategory(moreOptionsPreference, state, state.moreResolutionItems)

        moreOptionsPreference.apply {
            initialExpandedChildrenCount =
                if (state.areMoreOptionsExpanded) {
                    Integer.MAX_VALUE
                } else {
                    0
                }
            setOnExpandButtonClickListener { viewModel.onMoreOptionsExpanded() }
        }

        // Render refresh rate preferences
        refreshRatePreference.apply {
            removeAll()
            state.refreshRateItems.forEach { item ->
                val pref =
                    SelectorWithWidgetPreference(prefContext).apply {
                        key = item.modeId.toString()
                        title =
                            Utils.createAccessibleSequence(
                                getString(
                                    R.string.screen_refresh_rate_displayed_text,
                                    item.toReadableString(),
                                ),
                                getString(
                                    R.string.screen_refresh_rate_a11y,
                                    item.toReadableString(),
                                ),
                            )
                        isChecked = item.modeId == state.pendingMode.modeId
                        setOnPreferenceClickListener {
                            viewModel.onRefreshRateSelected(item.modeId)
                            true
                        }
                    }
                addPreference(pref)
            }
            isEnabled = state.refreshRateItems.size > 1
        }

        activity?.invalidateMenu()
    }

    private fun updateResolutionCategory(
        category: PreferenceCategory,
        state: UiState,
        items: List<ResolutionItem>,
    ) {
        category.removeAll()
        items.forEach { item ->
            val pref =
                SelectorWithWidgetPreference(prefContext).apply {
                    val width = item.physicalWidth
                    val height = item.physicalHeight
                    key = "${width}x$height"
                    title =
                        Utils.createAccessibleSequence(
                            item.toReadableString(),
                            getString(R.string.screen_resolution_delimiter_a11y, width, height),
                        )
                    isSingleLineTitle = true
                    isChecked =
                        width == state.pendingMode.physicalWidth &&
                            height == state.pendingMode.physicalHeight
                    setOnPreferenceClickListener {
                        viewModel.onResolutionSelected(item)
                        true
                    }
                }
            category.addPreference(pref)
        }
        category.isVisible = items.isNotEmpty()
    }

    private fun setupCategory(
        screen: PreferenceScreen,
        key: String,
        titleRes: Int,
    ): PreferenceCategory {
        return PreferenceCategory(prefContext).also {
            it.key = key
            it.setTitle(titleRes)
            it.isPersistent = false
            screen.addPreference(it)
        }
    }

    private fun ResolutionItem.toReadableString(): String {
        val formattedWidth = resolutionFormatter.format(this.physicalWidth)
        val formattedHeight = resolutionFormatter.format(this.physicalHeight)
        return "$formattedWidth x $formattedHeight"
    }

    private fun RefreshRateItem.toReadableString(): String {
        return refreshRateFormatter.format(this.refreshRate)
    }

    companion object {

        const val MORE_OPTIONS_KEY = "more_options"
        const val TOP_OPTIONS_KEY = "top_options"
        const val REFRESH_RATE_OPTIONS_KEY = "refresh_rate_options"
        const val DISPLAY_ID_ARG = "display_id"
        const val INVALID_DISPLAY = -1
    }
}
