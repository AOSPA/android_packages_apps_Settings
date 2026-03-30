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

import android.app.Application
import android.os.Bundle
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.Settings
import com.android.settings.core.SettingsBaseActivity
import com.android.settings.testutils.InstantTaskExecutorRule
import com.android.settings.testutils.shadow.ShadowDesktopSettingsUtils
import com.android.settingslib.collapsingtoolbar.widget.ScrollableToolbarItemLayout
import com.android.settingslib.search.Indexable
import com.google.android.material.appbar.AppBarLayout
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config

/** Unit tests for [TabbedDisplayPreferenceFragment]. */
@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowDesktopSettingsUtils::class])
class TabbedDisplayPreferenceFragmentTest : ExternalDisplayTestBase() {
    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(Settings.TestingSettingsActivity::class.java)

    @Captor
    private lateinit var toolbarListener:
        ArgumentCaptor<ScrollableToolbarItemLayout.OnItemSelectedListener>
    @Captor
    private lateinit var toolbarItemsCaptor:
        ArgumentCaptor<List<ScrollableToolbarItemLayout.ToolbarItem>>
    @Captor private lateinit var motionEventCaptor: ArgumentCaptor<MotionEvent>
    @Captor
    private lateinit var layoutChangeListenerCaptor: ArgumentCaptor<View.OnLayoutChangeListener>

    private lateinit var viewModel: DisplayPreferenceViewModel
    private lateinit var fragment: TestableTabbedDisplayPreferenceFragment
    private lateinit var settingsActivity: SettingsBaseActivity
    private lateinit var topologyView: FakeDisplayTopologyPreferenceView
    private lateinit var appBarLayoutSpy: AppBarLayout
    private lateinit var selectedDisplayPrefContainerSpy: FocusAwareFrameLayout
    private lateinit var floatingToolbar: View

    @Before
    override fun setUp() {
        super.setUp()
        val application = ApplicationProvider.getApplicationContext() as Application

        includeBuiltinDisplay()

        viewModel =
            DisplayPreferenceViewModel(
                application,
                mMockedInjector,
                mActivityManager,
                mActivityTaskManager,
                mDevicePolicyManager,
            )
        topologyView = FakeDisplayTopologyPreferenceView(mMockedInjector)
        floatingToolbar = spy(View(application))
        initFragment()

        // Spy on the container to verify propagated events
        appBarLayoutSpy = spy(fragment.appBarLayout)
        fragment.appBarLayout = appBarLayoutSpy
        selectedDisplayPrefContainerSpy = spy(fragment.selectedDisplayPrefContainer)
        fragment.selectedDisplayPrefContainer = selectedDisplayPrefContainerSpy

        verify(settingsActivity, atLeastOnce()).setOnItemSelectedListener(toolbarListener.capture())
        reset(settingsActivity)
    }

    @Test
    fun onCreate_withExistentDisplayIdArg_updatesSelectedDisplay() {
        val selectedDisplayId = mDisplays[1].id
        val bundle = Bundle()
        bundle.putInt(ExternalDisplaySettingsConfiguration.DISPLAY_ID_ARG, selectedDisplayId)

        val newTopologyView = FakeDisplayTopologyPreferenceView(mMockedInjector)
        val newFragment =
            TestableTabbedDisplayPreferenceFragment(newTopologyView, settingsActivity, viewModel)
        newFragment.arguments = bundle

        activityScenarioRule.scenario.onActivity { activity ->
            activity.supportFragmentManager
                .beginTransaction()
                .add(newFragment, "testTagWithArgs")
                .commitNow()
        }

        val updatedState = viewModel.uiState.value
        assertThat(updatedState?.selectedDisplayId).isEqualTo(selectedDisplayId)
    }

    @Test
    fun onCreate_withNonExistentDisplayIdArg_doesNotUpdateSelectedDisplay() {
        val selectedDisplayId = 123456789
        val bundle = Bundle()
        bundle.putInt(ExternalDisplaySettingsConfiguration.DISPLAY_ID_ARG, selectedDisplayId)

        val newTopologyView = FakeDisplayTopologyPreferenceView(mMockedInjector)
        val newFragment =
            TestableTabbedDisplayPreferenceFragment(newTopologyView, settingsActivity, viewModel)
        newFragment.arguments = bundle

        activityScenarioRule.scenario.onActivity { activity ->
            activity.supportFragmentManager
                .beginTransaction()
                .add(newFragment, "testTagWithArgs")
                .commitNow()
        }

        val updatedState = viewModel.uiState.value
        assertThat(updatedState?.selectedDisplayId).isNotEqualTo(selectedDisplayId)
    }

    @Test
    fun onCreate_withDisabledDisplayIdArg_doesNotUpdateSelectedDisplay() {
        val disabledDisplay = createExternalDisplay(DisplayIsEnabled.NO)
        val displays = listOf(disabledDisplay, createOverlayDisplay(DisplayIsEnabled.YES))
        updateDisplaysAndTopology(displays)
        val selectedDisplayId = disabledDisplay.id
        val bundle = Bundle()
        bundle.putInt(ExternalDisplaySettingsConfiguration.DISPLAY_ID_ARG, selectedDisplayId)

        val newTopologyView = FakeDisplayTopologyPreferenceView(mMockedInjector)
        val newFragment =
            TestableTabbedDisplayPreferenceFragment(newTopologyView, settingsActivity, viewModel)
        newFragment.arguments = bundle

        activityScenarioRule.scenario.onActivity { activity ->
            activity.supportFragmentManager
                .beginTransaction()
                .add(newFragment, "testTagWithArgsDisabled")
                .commitNow()
        }

        val updatedState = viewModel.uiState.value
        assertThat(updatedState?.selectedDisplayId).isNotEqualTo(selectedDisplayId)
    }

    @Test
    fun onCreate_withDefaultDisplayIdArg_updatesSelectedDisplay() {
        // Built-in display is not an external connected display (isConnectedDisplay=false),
        // but it is the default display, so it should be allowed.
        val builtinDisplay = includeBuiltinDisplay()
        val selectedDisplayId = builtinDisplay.id
        val bundle = Bundle()
        bundle.putInt(ExternalDisplaySettingsConfiguration.DISPLAY_ID_ARG, selectedDisplayId)

        val newTopologyView = FakeDisplayTopologyPreferenceView(mMockedInjector)
        val newFragment =
            TestableTabbedDisplayPreferenceFragment(newTopologyView, settingsActivity, viewModel)
        newFragment.arguments = bundle

        activityScenarioRule.scenario.onActivity { activity ->
            activity.supportFragmentManager
                .beginTransaction()
                .add(newFragment, "testTagWithArgsDefaultDisplay")
                .commitNow()
        }

        val updatedState = viewModel.uiState.value
        assertThat(updatedState?.selectedDisplayId).isEqualTo(selectedDisplayId)
    }

    @Test
    fun noExternalDisplay_showsNoDisplayConnected() {
        // Remove other external displays
        mDisplays = emptyList()
        includeBuiltinDisplay()
        updateDisplaysAndTopology(mDisplays)

        viewModel.updateEnabledDisplays()

        assertThat(fragment.appBarLayout.visibility).isEqualTo(View.GONE)
        assertThat(fragment.selectedDisplayPrefContainer.visibility).isEqualTo(View.GONE)
        assertThat(fragment.noDisplayConnectedLayout.visibility).isEqualTo(View.VISIBLE)
        verify(settingsActivity).setFloatingToolbarVisibility(false)
        verify(settingsActivity).removeOnItemSelectedListener()
        verify(settingsActivity).setToolbarItems(toolbarItemsCaptor.capture())
        assertThat(toolbarItemsCaptor.value.size).isEqualTo(0)
    }

    @Test
    fun withExternalDisplay_showsMainContent() {
        viewModel.updateEnabledDisplays()

        assertThat(fragment.appBarLayout.visibility).isEqualTo(View.VISIBLE)
        assertThat(fragment.selectedDisplayPrefContainer.visibility).isEqualTo(View.VISIBLE)
        assertThat(fragment.noDisplayConnectedLayout.visibility).isEqualTo(View.GONE)
        verify(settingsActivity).setToolbarSelectedItem(/* position= */ 0)
        verify(settingsActivity).setFloatingToolbarVisibility(true)
        verify(settingsActivity, atLeastOnce()).setToolbarItems(toolbarItemsCaptor.capture())
        assertThat(toolbarItemsCaptor.value.size).isEqualTo(3)
    }

    @Test
    fun onDisplayBlockSelected_updatesViewModel() {
        val selectedDisplayId = mDisplays.first { it.isConnectedDisplay }.id
        val listener = topologyView.selectedListener

        listener?.onSelected(selectedDisplayId)

        val updatedState = viewModel.uiState.value
        assertThat(updatedState?.selectedDisplayId).isEqualTo(selectedDisplayId)
    }

    @Test
    fun onToolbarItemSelected_updatesViewModel() {
        val listener = toolbarListener.value
        val secondDisplayId = mDisplays[1].id

        listener.onItemSelected(
            /* position= */ 1,
            ScrollableToolbarItemLayout.ToolbarItem(/* iconResId= */ null, ""),
        )

        val updatedState = viewModel.uiState.value
        assertThat(updatedState?.selectedDisplayId).isEqualTo(secondDisplayId)
    }

    @Test
    fun onStateUpdate_selectsCorrectDisplayBlockAndToolbarItem() {
        val secondDisplayId = mDisplays[1].id

        viewModel.updateSelectedDisplay(secondDisplayId)

        assertThat(topologyView.selectedDisplay).isEqualTo(secondDisplayId)
        // Verify the toolbar is updated with the correct selected item.
        verify(settingsActivity).setToolbarSelectedItem(/* position= */ 1)
        // Verify a listener is re-attached to handle user interaction on the toolbar.
        verify(settingsActivity).setOnItemSelectedListener(any())
    }

    @Test
    fun onStateUpdate_withInvalidSelectedId_doesNotSetToolbarItem() {
        val invalidDisplayId = 999

        viewModel.updateSelectedDisplay(invalidDisplayId)

        // The topology view is still updated with the invalid ID.
        assertThat(topologyView.selectedDisplay).isEqualTo(invalidDisplayId)
        // Verify that setToolbarSelectedItem is not called, as the ID is not in the map.
        verify(settingsActivity, never()).setToolbarSelectedItem(any())
    }

    @Test
    fun inMirroringMode_selectsNoDisplayInTopologyView() {
        android.provider.Settings.Secure.putInt(
            (ApplicationProvider.getApplicationContext() as Application).contentResolver,
            android.provider.Settings.Secure.MIRROR_BUILT_IN_DISPLAY,
            1,
        )
        viewModel.mirrorModeObserver.onChange(/* selfChange= */ false)

        assertThat(topologyView.selectedDisplay).isEqualTo(-1)
    }

    @Test
    fun setupAppBarLayout_onMouseScroll_propagatesEvent() {
        val containerSpy = fragment.selectedDisplayPrefContainer
        val containerWidth = 100
        val containerHeight = 100
        whenever(containerSpy.width).thenReturn(containerWidth)
        whenever(containerSpy.height).thenReturn(containerHeight)
        val uptime = SystemClock.uptimeMillis()
        val motionEvent =
            MotionEvent.obtain(
                uptime,
                uptime,
                MotionEvent.ACTION_SCROLL,
                /* x= */ 50f,
                /* y= */ 150f,
                /* metaState= */ 0,
            )
        motionEvent.source = InputDevice.SOURCE_MOUSE

        val result = fragment.appBarLayout.dispatchGenericMotionEvent(motionEvent)

        assertThat(result).isTrue()
        verify(containerSpy).dispatchGenericMotionEvent(motionEventCaptor.capture())
        val capturedEvent = motionEventCaptor.value
        assertThat(capturedEvent.x).isEqualTo(50.0f)
        assertThat(capturedEvent.y).isEqualTo(0.0f)
        motionEvent.recycle()
    }

    @Test
    fun setupAppBarLayout_onNonMouseScrollAction_doesNotPropagateEvent() {
        // Verifies that a non-scroll motion event is not propagated.
        val containerSpy = fragment.selectedDisplayPrefContainer
        val uptime = SystemClock.uptimeMillis()
        val motionEvent =
            MotionEvent.obtain(
                uptime,
                uptime,
                MotionEvent.ACTION_MOVE, // Not a scroll action
                /* x= */ 50f,
                /* y= */ 50f,
                /* metaState= */ 0,
            )
        motionEvent.source = InputDevice.SOURCE_MOUSE

        val result = fragment.appBarLayout.dispatchGenericMotionEvent(motionEvent)

        assertThat(result).isFalse()
        verify(containerSpy, never()).dispatchGenericMotionEvent(any())
        motionEvent.recycle()
    }

    @Test
    fun toolbarLayoutListener_onLayoutChange_updatesPaddingWithMargin() {
        // Verifies that the toolbar layout listener correctly updates the bottom padding of the
        // preference container when the toolbar has margins.
        verify(floatingToolbar).addOnLayoutChangeListener(layoutChangeListenerCaptor.capture())
        val listener = layoutChangeListenerCaptor.value

        // Define toolbar dimensions
        val toolbarHeight = 50
        val bottomMargin = 10
        val expectedPadding = toolbarHeight + bottomMargin * 2
        val layoutParams =
            ViewGroup.MarginLayoutParams(0, 0).apply { this.bottomMargin = bottomMargin }
        doReturn(toolbarHeight).whenever(floatingToolbar).height
        doReturn(layoutParams).whenever(floatingToolbar).layoutParams

        // Trigger the layout change
        listener.onLayoutChange(
            floatingToolbar,
            /* left= */ 0,
            /* top= */ 0,
            /* right= */ 0,
            /* bottom= */ 0,
            /* oldLeft= */ 0,
            /* oldTop= */ 0,
            /* oldRight= */ 0,
            /* oldBottom= */ 0,
        )

        // Verify the padding state was set correctly
        assertThat(selectedDisplayPrefContainerSpy.paddingBottom).isEqualTo(expectedPadding)
    }

    @Test
    fun setupAppBarLayout_onNonMouseScrollSource_doesNotPropagateEvent() {
        // Verifies that a scroll event from a non-mouse source is not propagated.
        val containerSpy = fragment.selectedDisplayPrefContainer
        val uptime = SystemClock.uptimeMillis()
        val motionEvent =
            MotionEvent.obtain(
                uptime,
                uptime,
                MotionEvent.ACTION_SCROLL,
                /* x= */ 50f,
                /* y= */ 50f,
                /* metaState= */ 0,
            )
        motionEvent.source = InputDevice.SOURCE_TOUCHSCREEN // Not a mouse source

        val result = fragment.appBarLayout.dispatchGenericMotionEvent(motionEvent)

        assertThat(result).isFalse()
        verify(containerSpy, never()).dispatchGenericMotionEvent(any())
        motionEvent.recycle()
    }

    @Test
    fun onDestroyView_removesListeners() {
        // Verifies that listeners on the app bar and floating toolbar are removed when the view is
        // destroyed.
        verify(floatingToolbar).addOnLayoutChangeListener(layoutChangeListenerCaptor.capture())
        val listener = layoutChangeListenerCaptor.value

        fragment.onDestroyView()

        verify(appBarLayoutSpy).setOnGenericMotionListener(null)
        verify(floatingToolbar).removeOnLayoutChangeListener(listener)
    }

    @Test
    fun focus_forwardFromTopologyView_movesToContainer() {
        val direction = View.FOCUS_DOWN
        val v =
            View(mContext).apply {
                isFocusable = true
                isFocusableInTouchMode = true
            }
        topologyView.addView(v)

        val nextFocus = topologyView.focusSearch(v, direction)
        assertThat(nextFocus).isEqualTo(selectedDisplayPrefContainerSpy)
    }

    @Test
    fun focus_backwardFromTopologyView_movesToParentActivity() {
        val direction = View.FOCUS_UP
        val v =
            View(mContext).apply {
                isFocusable = true
                isFocusableInTouchMode = true
            }
        topologyView.addView(v)

        val nextFocus = topologyView.focusSearch(v, direction)
        assertThat(nextFocus).isEqualTo(settingsActivity.window?.decorView)
    }

    @Test
    fun focus_backwardFromSelectedDisplayContainer_movesToTopologyViewAndExpandsAppBar() {
        viewModel.updateEnabledDisplays()
        val direction = View.FOCUS_UP
        val container = selectedDisplayPrefContainerSpy

        val v =
            android.widget.Button(mContext).apply {
                isFocusable = true
                visibility = View.VISIBLE
            }
        container.addView(v)

        // Mock to return dummy view v to simulate it's at the boundary
        doAnswer { invocation ->
                val list = invocation.getArgument<ArrayList<View>>(0)
                list.add(v)
                null
            }
            .whenever(container)
            .addFocusables(any(), any())

        val nextFocus = container.focusSearch(v, direction)
        assertThat(nextFocus).isEqualTo(topologyView)
        verify(appBarLayoutSpy, atLeastOnce())
            .setExpanded(/* expanded= */ true, /* animate= */ true)
    }

    @Test
    fun focus_forwardFromSelectedDisplayContainer_returnNull() {
        viewModel.updateEnabledDisplays()
        val direction = View.FOCUS_DOWN
        val container = selectedDisplayPrefContainerSpy

        val v =
            android.widget.Button(mContext).apply {
                isFocusable = true
                visibility = View.VISIBLE
            }
        container.addView(v)

        // Mock to return dummy view v to simulate it's at the boundary
        doAnswer { invocation ->
                val list = invocation.getArgument<ArrayList<View>>(0)
                list.add(v)
                null
            }
            .whenever(container)
            .addFocusables(any(), any())

        val nextFocus = container.focusSearch(v, direction)
        // It's expected to be null here to let parent view to handle the next focus (which will
        // naturally be moved to Toolbar items
        assertNull(nextFocus)
    }

    @Test
    fun searchIndexProvider_getRawIndexData() {
        ShadowDesktopSettingsUtils.setShouldShow(false)
        val provider: Indexable.SearchIndexProvider =
            TabbedDisplayPreferenceFragment.SEARCH_INDEX_DATA_PROVIDER

        val indexData = provider.getRawDataToIndex(mContext, /* enabled= */ true)
        assertThat(indexData).hasSize(1)
        val resource = indexData.first()
        assertThat(resource).isNotNull()
        assertThat(resource.screenTitle)
            .contains(mContext.getString(R.string.connected_devices_dashboard_title))
        assertThat(resource.keywords)
            .isEqualTo(mContext.getString(R.string.keywords_external_display_settings))
        assertThat(resource.title)
            .isEqualTo(mContext.getString(R.string.external_display_settings_title))
    }

    @Test
    fun searchIndexProvider_getRawIndexData_topLevelDeviceEnabled() {
        ShadowDesktopSettingsUtils.setShouldShow(true)
        val provider: Indexable.SearchIndexProvider =
            TabbedDisplayPreferenceFragment.SEARCH_INDEX_DATA_PROVIDER

        val indexData = provider.getRawDataToIndex(mContext, /* enabled= */ true)
        assertThat(indexData).hasSize(1)
        val resource = indexData.first()
        assertThat(resource).isNotNull()
        assertThat(resource.screenTitle).contains(mContext.getString(R.string.display_settings))
        assertThat(resource.keywords)
            .isEqualTo(mContext.getString(R.string.keywords_external_display_settings))
        assertThat(resource.title)
            .isEqualTo(mContext.getString(R.string.external_display_settings_title))
    }

    private fun initFragment(): TestableTabbedDisplayPreferenceFragment {
        activityScenarioRule.scenario.onActivity { activity ->
            settingsActivity = spy(activity)
            whenever(
                    settingsActivity.findViewById<View?>(
                        com.android.settingslib.collapsingtoolbar.R.id.floating_toolbar
                    )
                )
                .thenReturn(floatingToolbar)
            fragment =
                TestableTabbedDisplayPreferenceFragment(topologyView, settingsActivity, viewModel)
            activity.supportFragmentManager.beginTransaction().add(fragment, "testTag").commitNow()
        }
        return fragment
    }

    class TestableTabbedDisplayPreferenceFragment(
        val topologyView: DisplayTopologyPreferenceView,
        val settingsActivity: SettingsBaseActivity,
        viewModel: DisplayPreferenceViewModel,
    ) : TabbedDisplayPreferenceFragment(viewModel) {

        private val mockViewLifecycleOwner: LifecycleOwner = mock()
        val lifecycleRegistry = LifecycleRegistry(this)

        init {
            whenever(mockViewLifecycleOwner.lifecycle).thenReturn(lifecycleRegistry)
            // Required to allow observer to start observing data
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        override fun createDisplayTopologyPreferenceView(): DisplayTopologyPreferenceView {
            return topologyView
        }

        override fun getCurrentActivity(): SettingsBaseActivity? {
            return settingsActivity
        }

        override fun getViewLifecycleOwner(): LifecycleOwner {
            return mockViewLifecycleOwner
        }
    }

    class FakeDisplayTopologyPreferenceView(
        injector: ConnectedDisplayInjector,
        initialSelectedDisplayId: Int? = null,
    ) : DisplayTopologyPreferenceView(injector.context, injector, initialSelectedDisplayId) {

        var selectedListener: DisplayTopologyPreferenceController.OnDisplayBlockSelectedListener? =
            null
        var selectedDisplay: Int = -1

        override fun setOnDisplayBlockSelectedListener(
            listener: DisplayTopologyPreferenceController.OnDisplayBlockSelectedListener
        ) {
            super.setOnDisplayBlockSelectedListener(listener)
            selectedListener = listener
        }

        override fun removeOnDisplayBlockSelectedListener() {
            super.removeOnDisplayBlockSelectedListener()
            selectedListener = null
        }

        override fun selectDisplay(displayId: Int) {
            super.selectDisplay(displayId)
            selectedDisplay = displayId
        }
    }
}
