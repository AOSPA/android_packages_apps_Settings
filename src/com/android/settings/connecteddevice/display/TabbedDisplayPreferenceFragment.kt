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

import android.content.Context
import android.os.Bundle
import android.view.Display
import android.view.InputDevice
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.android.internal.annotations.VisibleForTesting
import com.android.settings.R
import com.android.settings.core.SettingsBaseActivity
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settings.utils.DesktopSettingsUtils
import com.android.settingslib.collapsingtoolbar.widget.ScrollableToolbarItemLayout
import com.android.settingslib.search.SearchIndexable
import com.android.settingslib.search.SearchIndexableRaw
import com.google.android.material.appbar.AppBarLayout
import com.google.common.collect.HashBiMap

/**
 * The main fragment that holds both the DisplayTopologyPreferenceView and the
 * SelectedDisplayPreferenceFragment, isolating them from each other to prevent redraw issues.
 */
// LINT.IfChange
@SearchIndexable
open class TabbedDisplayPreferenceFragment(
    private val testViewModel: DisplayPreferenceViewModel? = null
) : Fragment() {

    @VisibleForTesting internal lateinit var appBarLayout: AppBarLayout

    @VisibleForTesting
    internal lateinit var displayTopologyPreferenceView: DisplayTopologyPreferenceView

    @VisibleForTesting internal lateinit var selectedDisplayPrefContainer: FocusAwareFrameLayout

    @VisibleForTesting internal lateinit var noDisplayConnectedLayout: View

    private lateinit var viewModel: DisplayPreferenceViewModel

    // Map toolbar indices to display IDs
    private val toolbarIdxToDisplayIdMapping: HashBiMap<Int, Int> = HashBiMap.create()

    private var floatingToolbar: View? = null
    private val toolbarLayoutListener =
        View.OnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
            val params = v.layoutParams as? ViewGroup.MarginLayoutParams
            val bottomMargin = params?.bottomMargin ?: 0
            val totalPadding = v.height + bottomMargin * 2
            // Font / display size might be set to be big enough to start obscuring the content
            // behind. Add toolbar height as pref padding to let scrolling beyond the item list to
            // show prefs behind the toolbar.
            selectedDisplayPrefContainer.setPadding(
                selectedDisplayPrefContainer.paddingLeft,
                selectedDisplayPrefContainer.paddingTop,
                selectedDisplayPrefContainer.paddingRight,
                totalPadding,
            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (testViewModel != null) {
            // Test-only path
            viewModel = testViewModel
        } else {
            viewModel = ViewModelProvider(this).get(DisplayPreferenceViewModel::class.java)
        }

        arguments?.let { args ->
            if (args.containsKey(ExternalDisplaySettingsConfiguration.DISPLAY_ID_ARG)) {
                val displayId = args.getInt(ExternalDisplaySettingsConfiguration.DISPLAY_ID_ARG)

                // Ensure the display is valid and enabled before selecting it to avoid invalid
                // state
                val isDisplayEnabled =
                    viewModel.injector.getDisplays().any {
                        it.id == displayId &&
                            it.isEnabled == DisplayIsEnabled.YES &&
                            (it.id == Display.DEFAULT_DISPLAY || it.isConnectedDisplay)
                    }

                if (isDisplayEnabled) {
                    viewModel.updateSelectedDisplay(displayId)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.tabbed_display_settings_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = getCurrentActivity() ?: return
        activity.setTitle(R.string.external_display_settings_title)
        appBarLayout = view.findViewById(R.id.app_bar_layout)
        selectedDisplayPrefContainer =
            view.findViewById(R.id.selected_display_preference_container) as FocusAwareFrameLayout
        noDisplayConnectedLayout = view.findViewById(R.id.no_display_connected_layout)

        floatingToolbar =
            activity.findViewById<View?>(
                com.android.settingslib.collapsingtoolbar.R.id.floating_toolbar
            )
        floatingToolbar?.addOnLayoutChangeListener(toolbarLayoutListener)

        setupAppBarLayout()
        setupDisplayTopologyPreferenceView(view)
        setupSelectedDisplayPreferenceFragment(savedInstanceState)
        setupInputFocus()
        startListeningForUpdates()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::displayTopologyPreferenceView.isInitialized) {
            displayTopologyPreferenceView.removeOnDisplayBlockSelectedListener()
            displayTopologyPreferenceView.setNextFocusFinder(null)
        }
        if (::selectedDisplayPrefContainer.isInitialized) {
            selectedDisplayPrefContainer.setNextFocusFinder(null)
        }
        if (::appBarLayout.isInitialized) {
            appBarLayout.setOnGenericMotionListener(null)
        }
        getCurrentActivity()?.removeOnItemSelectedListener()
        floatingToolbar?.removeOnLayoutChangeListener(toolbarLayoutListener)
    }

    @VisibleForTesting
    internal open fun createDisplayTopologyPreferenceView(): DisplayTopologyPreferenceView {
        val uiContext = requireContext()
        val selectedId = viewModel.uiState.value?.selectedDisplayId
        return if (selectedId != null && selectedId != -1) {
            DisplayTopologyPreferenceView(uiContext, viewModel.injector, selectedId)
        } else {
            DisplayTopologyPreferenceView(uiContext, viewModel.injector)
        }
    }

    @VisibleForTesting
    internal open fun getCurrentActivity(): SettingsBaseActivity? {
        return getActivity() as? SettingsBaseActivity
    }

    private fun startListeningForUpdates() {
        displayTopologyPreferenceView.setOnDisplayBlockSelectedListener(
            object : DisplayTopologyPreferenceController.OnDisplayBlockSelectedListener {
                override fun onSelected(displayId: Int) {
                    viewModel.updateSelectedDisplay(displayId)
                }
            }
        )
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            val enabledDisplays = state.enabledDisplays
            val selectedDisplayId = state.selectedDisplayId
            val isMirroring = state.isMirroring

            // Determine if there are any connected external displays to show the main UI
            val hasConnectedExternalDisplay = enabledDisplays.values.any { it.isConnectedDisplay }

            if (hasConnectedExternalDisplay) {
                showMainViews()
                val highlightedDisplayId = if (isMirroring) -1 else selectedDisplayId
                displayTopologyPreferenceView.selectDisplay(highlightedDisplayId)
                updateToolbarDisplays(enabledDisplays, selectedDisplayId)
            } else {
                hideMainViews()
                clearToolbar()
            }
        }
    }

    private fun showMainViews() {
        appBarLayout.visibility = View.VISIBLE
        selectedDisplayPrefContainer.visibility = View.VISIBLE
        noDisplayConnectedLayout.visibility = View.GONE
    }

    private fun hideMainViews() {
        appBarLayout.visibility = View.GONE
        selectedDisplayPrefContainer.visibility = View.GONE
        noDisplayConnectedLayout.visibility = View.VISIBLE
    }

    private fun setupAppBarLayout() {
        // TODO(b/428853467): AppBarLayout doesn't support mouse scrolling
        appBarLayout.setOnGenericMotionListener { view, event ->
            if (
                event.action != MotionEvent.ACTION_SCROLL ||
                    !event.isFromSource(InputDevice.SOURCE_MOUSE)
            ) {
                return@setOnGenericMotionListener false
            }
            // Propagate scroll events to selectedDisplayPrefContainer to match the
            // `appbar_scrolling_view_behavior` property
            val newEvent = MotionEvent.obtain(event)
            // When settings window is resized to minimum, and container is not visible, the
            // rendered bounds of selectedDisplayPrefContainer might be smaller than the fetched
            // width/height. So height/2 could overshoot the set location. Setting 0 for y would be
            // safer to ensure that events are sent. Meanwhile, for x, setting it as 0 might hit
            // padding / margins, as the width is not affected by scrolling, it's safe to set
            // width/2 here.
            newEvent.setLocation((selectedDisplayPrefContainer.width / 2).toFloat(), 0f)
            selectedDisplayPrefContainer.dispatchGenericMotionEvent(newEvent)
            newEvent.recycle()
            return@setOnGenericMotionListener true
        }
    }

    private fun setupDisplayTopologyPreferenceView(layoutView: View) {
        val topologyContainer =
            layoutView.findViewById<ViewGroup>(R.id.display_topology_preference_container)
        displayTopologyPreferenceView = createDisplayTopologyPreferenceView()
        topologyContainer.addView(displayTopologyPreferenceView)
    }

    private fun setupSelectedDisplayPreferenceFragment(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            // Fragment is restored from fragment manager
            return
        }
        childFragmentManager
            .beginTransaction()
            .replace(
                R.id.selected_display_preference_container,
                SelectedDisplayPreferenceFragment(viewModel),
            )
            .commit()
    }

    private fun clearToolbar() {
        val activity = getCurrentActivity() ?: return
        // Remove existing listener to not get updated when toolbar items are re-added
        activity.removeOnItemSelectedListener()
        toolbarIdxToDisplayIdMapping.clear()
        activity.setFloatingToolbarVisibility(false)
        activity.setToolbarItems(emptyList())
    }

    private fun updateToolbarDisplays(
        enabledDisplays: Map<Int, DisplayDevice>,
        selectedDisplayId: Int,
    ) {
        val activity = getCurrentActivity() ?: return
        clearToolbar()

        val toolbarItems = mutableListOf<ScrollableToolbarItemLayout.ToolbarItem>()
        enabledDisplays.values.forEachIndexed { i, display ->
            val displayId = display.id
            if (!display.isConnectedDisplay) {
                toolbarItems.add(
                    ScrollableToolbarItemLayout.ToolbarItem(
                        R.drawable.display_category_builtin_display_icon,
                        requireContext().getString(R.string.builtin_display_settings_category),
                    )
                )
            } else {
                toolbarItems.add(
                    ScrollableToolbarItemLayout.ToolbarItem(
                        R.drawable.display_category_connected_display_icon,
                        display.name,
                    )
                )
            }
            toolbarIdxToDisplayIdMapping[i] = displayId
        }

        activity.setToolbarItems(toolbarItems)
        activity.setFloatingToolbarVisibility(true)
        toolbarIdxToDisplayIdMapping.inverse().get(selectedDisplayId)?.let { toolbarPosition ->
            activity.setToolbarSelectedItem(toolbarPosition)
        }
        activity.setOnItemSelectedListener(
            object : ScrollableToolbarItemLayout.OnItemSelectedListener {
                override fun onItemSelected(
                    position: Int,
                    toolbarItem: ScrollableToolbarItemLayout.ToolbarItem,
                ) {
                    toolbarIdxToDisplayIdMapping[position]?.let { displayId ->
                        // Scrolls back up to display block panel
                        appBarLayout.setExpanded(/* expanded= */ true, /* animate= */ true)
                        viewModel.updateSelectedDisplay(displayId)
                    }
                }
            }
        )
    }

    /**
     * Sets up the children forward and backward focus target when focus leaves the child container
     *
     * For display blocks, moving backward should move back to the Activity root (this refers to
     * back button of Settings page). Moving forward should move the focus to selected display
     * options
     *
     * For selected display options, moving backward should move back focus to display block panel.
     * Moving forward should move it to toolbar
     */
    private fun setupInputFocus() {
        displayTopologyPreferenceView.setNextFocusFinder(
            object : FocusAwareFrameLayout.NextFocusFinder {
                override fun findNextFocus(focused: View?, direction: Int): View? {
                    if (FocusAwareFrameLayout.isMovingForward(direction)) {
                        return selectedDisplayPrefContainer
                    } else {
                        // When moving backward from the display topology, move focus to the root
                        // decor view of the activity, typically allowing focus to go to the
                        // back button or other elements in the SettingsBaseActivity.
                        return getCurrentActivity()?.window?.decorView as? ViewGroup
                    }
                }
            }
        )
        selectedDisplayPrefContainer.setNextFocusFinder(
            object : FocusAwareFrameLayout.NextFocusFinder {
                override fun findNextFocus(focused: View?, direction: Int): View? {
                    if (FocusAwareFrameLayout.isMovingBackward(direction)) {
                        // Scrolls back up to display block panel
                        appBarLayout.setExpanded(/* expanded= */ true, /* animate= */ true)
                        return displayTopologyPreferenceView
                    }
                    // Focus forward already moves naturally to Toolbar items
                    return null
                }
            }
        )
    }

    companion object {

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER: BaseSearchIndexProvider =
            object : BaseSearchIndexProvider() {
                override fun getRawDataToIndex(
                    context: Context,
                    enabled: Boolean,
                ): MutableList<SearchIndexableRaw?> {
                    val rawData: MutableList<SearchIndexableRaw?> = mutableListOf()
                    val indexInfo = SearchIndexableRaw(context)
                    indexInfo.key = "external_display_screen_title"
                    indexInfo.title = context.getString(R.string.external_display_settings_title)
                    indexInfo.keywords =
                        context.getString(R.string.keywords_external_display_settings)
                    if (DesktopSettingsUtils.shouldShowTopLevelDeviceCategory(context)) {
                        indexInfo.screenTitle = context.getString(R.string.display_settings)
                    } else {
                        indexInfo.screenTitle =
                            context.getString(R.string.connected_devices_dashboard_title)
                    }
                    rawData.add(indexInfo)
                    return rawData
                }
            }
    }
}
// LINT.ThenChange(TabbedDisplayApiScreen.kt)
