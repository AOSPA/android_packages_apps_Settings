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
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.Utils
import com.android.settings.connecteddevice.display.ResolutionRefreshRatePreferenceViewModel.ConfirmationDialogEvent
import com.android.settings.connecteddevice.display.ResolutionRefreshRatePreferenceViewModel.RefreshRateItem
import com.android.settings.connecteddevice.display.ResolutionRefreshRatePreferenceViewModel.ResolutionItem
import com.android.settings.connecteddevice.display.ResolutionRefreshRatePreferenceViewModel.UiState
import com.android.settingslib.widget.SelectorWithWidgetPreference
import java.util.Locale
import kotlin.properties.Delegates

// LINT.IfChange
class ResolutionRefreshRatePreferenceFragment(
    private val testViewModel: ResolutionRefreshRatePreferenceViewModel? = null
) : SettingsPreferenceFragment(), MenuProvider {

    private lateinit var viewModel: ResolutionRefreshRatePreferenceViewModel
    private lateinit var topOptionsPreference: PreferenceCategory
    private lateinit var moreOptionsPreference: PreferenceCategory
    private lateinit var refreshRatePreference: PreferenceCategory
    private var displayId: Int by Delegates.notNull()

    private var applyActionView: View? = null
    private var spinnerActionView: View? = null

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
        displayId = arguments?.getInt(DISPLAY_ID_ARG, INVALID_DISPLAY) ?: INVALID_DISPLAY
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
        setupConfirmationResultListener()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        viewModel.uiState.distinctUntilChanged().observe(viewLifecycleOwner) { state ->
            if (state == null) {
                finishFragment()
                return@observe
            }
            update(state)
        }
        viewModel.confirmationDialogEvent.distinctUntilChanged().observe(viewLifecycleOwner) {
            confirmationDialogEvent ->
            if (confirmationDialogEvent != null) {
                showDialog(confirmationDialogEvent)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        applyActionView = null
        spinnerActionView = null
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
        topOptionsPreference.isEnabled =
            enablePrefSelection(state) && state.topResolutionItems.size > 1
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
        moreOptionsPreference.isEnabled = enablePrefSelection(state)

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
            isEnabled = enablePrefSelection(state) && state.refreshRateItems.size > 1
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
                    val formattedWidth = resolutionFormatter.format(width)
                    val formattedHeight = resolutionFormatter.format(height)
                    key = "${width}x$height"
                    title =
                        Utils.createAccessibleSequence(
                            getString(
                                R.string.screen_resolution_displayed_text,
                                formattedWidth,
                                formattedHeight,
                            ),
                            getString(
                                R.string.screen_resolution_delimiter_a11y,
                                formattedWidth,
                                formattedHeight,
                            ),
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

    private fun showDialog(dialogInfo: ConfirmationDialogEvent) {
        if (
            (getParentFragmentManager().findFragmentByTag(ResolutionChangeDialogFragment.TAG) !=
                null)
        ) {
            return
        }
        logInfo("Showing dialog to confirm resolution/refresh rate change")
        val dialog =
            ResolutionChangeDialogFragment.newInstance(dialogInfo.newMode, dialogInfo.previousMode)
        dialog.show(parentFragmentManager, ResolutionChangeDialogFragment.TAG)
    }

    private fun setupConfirmationResultListener() {
        parentFragmentManager.setFragmentResultListener(
            ResolutionChangeDialogFragment.KEY_RESULT,
            this,
        ) { _, bundle ->
            val confirmed =
                bundle.getParcelable(
                    ResolutionChangeDialogFragment.KEY_CONFIRMED,
                    ResolutionChangeConfirmationState::class.java,
                )
            when (confirmed) {
                ResolutionChangeConfirmationState.ACCEPT -> viewModel.onConfirmationResult(true)
                ResolutionChangeConfirmationState.REVERT -> viewModel.onConfirmationResult(false)
                ResolutionChangeConfirmationState.NO_ACTION -> {
                    // Expected behavior during resolution change, if this view is located on the
                    // display being updated.
                    // Do nothing, ViewModel will retain state and reshow the dialog.
                    logInfo("Dialog dismissed due to configuration change. Waiting for recreation.")
                }
                else -> {
                    logError("Unexpected confirmed state $confirmed, this should never happen")
                }
            }
        }
    }

    private fun enablePrefSelection(uiState: UiState): Boolean = !uiState.isApplying

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        val applyItem = menu.add(Menu.NONE, Menu.FIRST, 0, R.string.apply)
        applyItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

        val inflater = LayoutInflater.from(requireContext())

        applyActionView =
            inflater.inflate(R.layout.resolution_change_apply_button, null).apply {
                findViewById<Button>(R.id.resolution_change_apply_button_view).setOnClickListener {
                    viewModel.onApplyClicked()
                }
            }

        spinnerActionView =
            ProgressBar(requireContext(), null, android.R.attr.progressBarStyleSmall).apply {
                val padding = (16 * resources.displayMetrics.density).toInt()
                setPadding(padding, 0, padding, 0)
            }

        applyItem.actionView = applyActionView
    }

    override fun onPrepareMenu(menu: Menu) {
        val uiState = viewModel.uiState.value
        if (uiState == null) {
            // If state is null, fragment will exit on ViewModel#observe
            logWarn("Missing UiState, fragment will exit")
            return
        }
        val applyItem = menu.findItem(Menu.FIRST) ?: return

        if (uiState.isApplying) {
            if (applyItem.actionView !== spinnerActionView) {
                applyItem.actionView = spinnerActionView
            }
        } else {
            if (applyItem.actionView !== applyActionView) {
                applyItem.actionView = applyActionView
            }

            val isVisible = uiState.pendingMode.modeId != uiState.currentActiveMode.modeId
            val applyButton: Button? =
                applyActionView?.findViewById(R.id.resolution_change_apply_button_view)
            applyButton?.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false

    private fun RefreshRateItem.toReadableString(): String {
        return refreshRateFormatter.format(this.refreshRate)
    }

    private fun logInfo(message: String) {
        Log.i(TAG, "[Display#$displayId] $message")
    }

    private fun logWarn(message: String) {
        Log.d(TAG, "[Display#$displayId] $message")
    }

    private fun logError(message: String) {
        Log.e(TAG, "[Display#$displayId] $message")
    }

    companion object {

        const val MORE_OPTIONS_KEY = "more_options"
        const val TOP_OPTIONS_KEY = "top_options"
        const val REFRESH_RATE_OPTIONS_KEY = "refresh_rate_options"
        const val DISPLAY_ID_ARG = "display_id"
        const val INVALID_DISPLAY = -1
        private const val TAG = "ResRefreshRatePref"
    }
}
// LINT.ThenChange(ResolutionRefreshRateApiScreen.kt)
