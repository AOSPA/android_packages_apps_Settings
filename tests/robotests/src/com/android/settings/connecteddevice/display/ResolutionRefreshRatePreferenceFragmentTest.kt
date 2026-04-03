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
import android.os.Looper
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.view.Display.Mode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.preference.PreferenceCategory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.graphics.surfaceflinger.flags.Flags.FLAG_FOLLOWER_ARBITRARY_REFRESH_RATE_SELECTION_PLATFORM
import com.android.graphics.surfaceflinger.flags.Flags.FLAG_FOLLOWER_DISPLAY_BACKPRESSURE_PLATFORM
import com.android.graphics.surfaceflinger.flags.Flags.FLAG_FORCE_SLOWER_FOLLOWER_GPU_COMPOSITION_PLATFORM
import com.android.graphics.surfaceflinger.flags.Flags.FLAG_SYNCED_RESOLUTION_SWITCH
import com.android.settings.R
import com.android.settings.connecteddevice.display.ResolutionRefreshRatePreferenceFragment.Companion.DISPLAY_ID_ARG
import com.android.settings.connecteddevice.display.ResolutionRefreshRatePreferenceFragment.Companion.MORE_OPTIONS_KEY
import com.android.settings.connecteddevice.display.ResolutionRefreshRatePreferenceFragment.Companion.REFRESH_RATE_OPTIONS_KEY
import com.android.settings.connecteddevice.display.ResolutionRefreshRatePreferenceFragment.Companion.TOP_OPTIONS_KEY
import com.android.settings.testutils.InstantTaskExecutorRule
import com.android.settingslib.widget.SelectorWithWidgetPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Shadows.shadowOf

/** Unit tests for [ResolutionRefreshRatePreferenceFragment] */
@RunWith(AndroidJUnit4::class)
@EnableFlags(
    FLAG_FOLLOWER_ARBITRARY_REFRESH_RATE_SELECTION_PLATFORM,
    FLAG_FOLLOWER_DISPLAY_BACKPRESSURE_PLATFORM,
    FLAG_FORCE_SLOWER_FOLLOWER_GPU_COMPOSITION_PLATFORM,
    FLAG_SYNCED_RESOLUTION_SWITCH,
)
class ResolutionRefreshRatePreferenceFragmentTest : ExternalDisplayTestBase() {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    @get:Rule val setFlagsRule: SetFlagsRule = SetFlagsRule()

    private lateinit var scenario: FragmentScenario<ResolutionRefreshRatePreferenceFragment>
    private lateinit var viewModel: ResolutionRefreshRatePreferenceViewModel
    private lateinit var application: Application

    private val externalDisplay by lazy { mDisplays.find { it.id == EXTERNAL_DISPLAY_ID }!! }

    @Before
    override fun setUp() {
        super.setUp()
        application = ApplicationProvider.getApplicationContext()

        viewModel =
            ResolutionRefreshRatePreferenceViewModel(
                application,
                EXTERNAL_DISPLAY_ID,
                mMockedInjector,
            )
    }

    private fun launchFragment() {
        val args = Bundle().apply { putInt(DISPLAY_ID_ARG, EXTERNAL_DISPLAY_ID) }
        scenario =
            launchFragmentInContainer(
                fragmentArgs = args,
                themeResId = R.style.Theme_Settings_Home,
            ) {
                ResolutionRefreshRatePreferenceFragment(viewModel)
            }
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun simulateModeApplied(mode: Mode) {
        val updatedEnabledDisplays = mDisplays.toMutableList()
        val displayIndex = updatedEnabledDisplays.indexOfFirst { it.id == EXTERNAL_DISPLAY_ID }
        updatedEnabledDisplays[displayIndex] =
            DisplayDevice(
                EXTERNAL_DISPLAY_ID,
                externalDisplay.uniqueId,
                externalDisplay.name,
                mode,
                externalDisplay.supportedModes,
                DisplayIsEnabled.YES,
                true,
                0,
                isHdrSupported = externalDisplay.isHdrSupported,
            )
        updateDisplaysAndTopology(updatedEnabledDisplays)
        mListener.update(EXTERNAL_DISPLAY_ID)
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun initialState_rendersCorrectPreferences() {
        launchFragment()

        scenario.onFragment { fragment ->
            val topCategory =
                fragment.preferenceScreen.findPreference<PreferenceCategory>(TOP_OPTIONS_KEY)!!
            val moreCategory =
                fragment.preferenceScreen.findPreference<PreferenceCategory>(MORE_OPTIONS_KEY)!!
            val refreshRateCategory =
                fragment.preferenceScreen.findPreference<PreferenceCategory>(
                    REFRESH_RATE_OPTIONS_KEY
                )!!

            assertThat(topCategory.preferenceCount).isEqualTo(3)
            assertThat(moreCategory.preferenceCount).isEqualTo(3)

            val initialResPref = topCategory.getPreference(1) as SelectorWithWidgetPreference
            assertThat(initialResPref.key).isEqualTo("1920x1080")
            assertThat(initialResPref.isChecked).isTrue()
            assertThat(initialResPref.isEnabled).isTrue()

            // Verify resolution preference title is correctly formatted
            val expectedTitle =
                fragment.getString(R.string.screen_resolution_displayed_text, "1920", "1080")
            assertThat(initialResPref.title.toString()).isEqualTo(expectedTitle)

            assertThat(refreshRateCategory.preferenceCount).isEqualTo(1)
            val initialRefreshPref =
                refreshRateCategory.getPreference(0) as SelectorWithWidgetPreference
            assertThat(initialRefreshPref.title.toString()).contains("60.00 Hz")
            assertThat(initialRefreshPref.isChecked).isTrue()
        }
    }

    @Test
    fun initialState_whenSelectedModeIsInMoreList_expandsMoreOptionsByDefault() {
        // Choose resolution that is not in the top 3 resolutions
        val width = 320
        val height = 240
        val mode =
            externalDisplay.supportedModes.find {
                it.physicalWidth == width && it.physicalHeight == height
            }!!
        val displayWithDifferentInitialMode =
            DisplayDevice(
                id = externalDisplay.id,
                uniqueId = externalDisplay.uniqueId,
                name = externalDisplay.name,
                mode = mode,
                supportedModes = externalDisplay.supportedModes,
                isEnabled = externalDisplay.isEnabled,
                isConnectedDisplay = externalDisplay.isConnectedDisplay,
                rotation = externalDisplay.rotation,
                isHdrSupported = externalDisplay.isHdrSupported,
            )
        whenever(mMockedInjector.getDisplay(EXTERNAL_DISPLAY_ID))
            .thenReturn(displayWithDifferentInitialMode)
        mListener.update(EXTERNAL_DISPLAY_ID)

        launchFragment()

        // Verify it's initially expanded
        assertThat(viewModel.uiState.value?.areMoreOptionsExpanded).isTrue()
        scenario.onFragment { fragment ->
            val moreCategory =
                fragment.preferenceScreen.findPreference<PreferenceCategory>(MORE_OPTIONS_KEY)!!
            assertThat(moreCategory.initialExpandedChildrenCount).isEqualTo(Integer.MAX_VALUE)
        }
    }

    @Test
    fun displayDisconnected_finishesActivity() {
        launchFragment()
        whenever(mMockedInjector.getDisplay(EXTERNAL_DISPLAY_ID)).thenReturn(null)
        mListener.update(EXTERNAL_DISPLAY_ID)
        shadowOf(Looper.getMainLooper()).idle()

        scenario.onFragment { fragment ->
            assertThat(fragment.requireActivity().isFinishing).isTrue()
        }
    }

    @Test
    fun displayDisconnected_duringConfirmationDialog_finishesActivity() {
        launchFragment()

        // Select a new resolution
        scenario.onFragment { fragment ->
            val moreCategory =
                fragment.preferenceScreen.findPreference<PreferenceCategory>(MORE_OPTIONS_KEY)!!
            val prefToClick = moreCategory.findPreference<SelectorWithWidgetPreference>("640x480")!!
            prefToClick.performClick()
        }
        shadowOf(Looper.getMainLooper()).idle()

        // Click Apply Button
        scenario.onFragment { fragment ->
            val (menuItem, menu) = getApplyMenuItemAndMenuFromFragment(fragment)
            fragment.onPrepareMenu(menu)
            val applyButton =
                menuItem.actionView!!.findViewById<Button>(R.id.resolution_change_apply_button_view)
            applyButton.performClick()
        }
        shadowOf(Looper.getMainLooper()).idle()

        // Simulate successful mode set to trigger the Dialog
        val pendingMode = externalDisplay.supportedModes.find { it.modeId == 3 }!! // 640x480
        simulateModeApplied(pendingMode)

        // Verify Dialog is showing
        scenario.onFragment { fragment ->
            assertThat(
                    fragment.parentFragmentManager.findFragmentByTag(
                        ResolutionChangeDialogFragment.TAG
                    )
                )
                .isNotNull()
        }

        // Disconnect the display
        whenever(mMockedInjector.getDisplay(EXTERNAL_DISPLAY_ID)).thenReturn(null)
        mListener.update(EXTERNAL_DISPLAY_ID)
        shadowOf(Looper.getMainLooper()).idle()

        // Verify fragment exits even though the dialog was active
        scenario.onFragment { fragment ->
            assertThat(fragment.requireActivity().isFinishing).isTrue()
        }
    }

    @Test
    fun onResolutionClick_updatesStateAndRendersNewSelection() {
        launchFragment()

        // Selecting resolution with multiple refresh rates
        scenario.onFragment { fragment ->
            val moreCategory =
                fragment.preferenceScreen.findPreference<PreferenceCategory>(MORE_OPTIONS_KEY)!!
            val prefToClick = moreCategory.findPreference<SelectorWithWidgetPreference>("640x480")!!
            prefToClick.performClick()
        }
        shadowOf(Looper.getMainLooper()).idle()

        // Highest refresh rate for 640x480 should be selected (60Hz)
        val highestRefreshRateMode = externalDisplay.supportedModes.find { it.modeId == 3 }
        assertThat(viewModel.uiState.value?.pendingMode).isEqualTo(highestRefreshRateMode)

        scenario.onFragment { fragment ->
            val refreshRateCategory =
                fragment.preferenceScreen.findPreference<PreferenceCategory>(
                    REFRESH_RATE_OPTIONS_KEY
                )!!
            assertThat(refreshRateCategory.preferenceCount).isEqualTo(2)
            val selectedRefreshPref =
                refreshRateCategory.getPreference(0) as SelectorWithWidgetPreference
            assertThat(selectedRefreshPref.title.toString()).contains("60.00 Hz")
            assertThat(selectedRefreshPref.isChecked).isTrue()
        }
    }

    @Test
    fun onRefreshRateClick_updatesStateAndRendersNewSelection() {
        launchFragment()
        // Selecting resolution with multiple refresh rates
        scenario.onFragment { f ->
            f.preferenceScreen
                .findPreference<SelectorWithWidgetPreference>("640x480")!!
                .performClick()
        }
        shadowOf(Looper.getMainLooper()).idle()

        // Select other refresh rate (640x480, 50Hz)
        val refreshRateModeId = 4
        scenario.onFragment { fragment ->
            val refreshRateCategory =
                fragment.preferenceScreen.findPreference<PreferenceCategory>(
                    REFRESH_RATE_OPTIONS_KEY
                )!!
            val prefToClick =
                refreshRateCategory.findPreference<SelectorWithWidgetPreference>(
                    refreshRateModeId.toString()
                )!!
            prefToClick.performClick()
        }
        shadowOf(Looper.getMainLooper()).idle()

        val modeAt50Hz = externalDisplay.supportedModes.find { it.modeId == refreshRateModeId }
        assertThat(viewModel.uiState.value?.pendingMode).isEqualTo(modeAt50Hz)
    }

    @Test
    fun applyButton_initialState_isHidden() {
        launchFragment()

        scenario.onFragment { fragment ->
            val (menuItem, menu) = getApplyMenuItemAndMenuFromFragment(fragment)
            fragment.onPrepareMenu(menu)
            val actionView = menuItem.actionView
            assertThat(actionView).isNotInstanceOf(ProgressBar::class.java)

            val applyButton =
                actionView!!.findViewById<Button>(R.id.resolution_change_apply_button_view)
            assertThat(applyButton.visibility).isEqualTo(View.INVISIBLE)
        }
    }

    @Test
    fun applyButton_whenStateIsPending_isVisible() {
        launchFragment()
        val newResolutionItem =
            ResolutionRefreshRatePreferenceViewModel.ResolutionItem(
                physicalWidth = 800,
                physicalHeight = 600,
            )
        viewModel.onResolutionSelected(newResolutionItem)
        shadowOf(Looper.getMainLooper()).idle()

        scenario.onFragment { fragment ->
            val (menuItem, menu) = getApplyMenuItemAndMenuFromFragment(fragment)
            fragment.onPrepareMenu(menu)
            val actionView = menuItem.actionView
            assertThat(actionView).isNotInstanceOf(ProgressBar::class.java)

            val applyButton =
                actionView!!.findViewById<Button>(R.id.resolution_change_apply_button_view)
            assertThat(applyButton.visibility).isEqualTo(View.VISIBLE)
        }
    }

    @Test
    fun applyButton_onClick_callsViewModelOnApplyClickedAndShowsSpinner() {
        launchFragment()
        val width = 800
        val height = 600
        val pendingMode =
            externalDisplay.supportedModes.find {
                it.physicalWidth == width && it.physicalHeight == height
            }!!
        val newResolutionItem =
            ResolutionRefreshRatePreferenceViewModel.ResolutionItem(width, height)
        viewModel.onResolutionSelected(newResolutionItem)
        shadowOf(Looper.getMainLooper()).idle()

        scenario.onFragment { fragment ->
            val (menuItem, menu) = getApplyMenuItemAndMenuFromFragment(fragment)
            fragment.onPrepareMenu(menu)
            val applyButton =
                menuItem.actionView!!.findViewById<Button>(R.id.resolution_change_apply_button_view)
            applyButton.performClick()
        }
        shadowOf(Looper.getMainLooper()).idle()

        // Validate isApplying is true
        assertThat(viewModel.uiState.value?.isApplying).isTrue()
        verify(mMockedInjector)
            .setUserPreferredDisplayMode(EXTERNAL_DISPLAY_ID, pendingMode, /* storeMode= */ false)

        // Verifying UI responds to isApplying = true
        scenario.onFragment { fragment ->
            val (menuItem, menu) = getApplyMenuItemAndMenuFromFragment(fragment)
            fragment.onPrepareMenu(menu)
            // Should swap to ProgressBar
            assertThat(menuItem.actionView).isInstanceOf(ProgressBar::class.java)

            // Other preferences should be disabled
            val topCategory =
                fragment.preferenceScreen.findPreference<PreferenceCategory>(TOP_OPTIONS_KEY)!!
            val moreCategory =
                fragment.preferenceScreen.findPreference<PreferenceCategory>(MORE_OPTIONS_KEY)!!
            val refreshRateCategory =
                fragment.preferenceScreen.findPreference<PreferenceCategory>(
                    REFRESH_RATE_OPTIONS_KEY
                )!!
            assertThat(topCategory.isEnabled).isFalse()
            assertThat(moreCategory.isEnabled).isFalse()
            assertThat(refreshRateCategory.isEnabled).isFalse()
        }
    }

    @Test
    fun onConfirmationResult_accept_updatesViewModelAndInjector() {
        launchFragment()
        val width = 640
        val height = 480
        val pendingMode =
            externalDisplay.supportedModes.find {
                it.physicalWidth == width && it.physicalHeight == height
            }!!
        scenario.onFragment { f ->
            f.preferenceScreen
                .findPreference<SelectorWithWidgetPreference>("${width}x$height")!!
                .performClick()
        }
        shadowOf(Looper.getMainLooper()).idle()
        scenario.onFragment { fragment ->
            val (menuItem, menu) = getApplyMenuItemAndMenuFromFragment(fragment)
            fragment.onPrepareMenu(menu)
            val applyButton =
                menuItem.actionView!!.findViewById<Button>(R.id.resolution_change_apply_button_view)
            applyButton.performClick()
        }
        shadowOf(Looper.getMainLooper()).idle()

        // Simulate successful mode set
        simulateModeApplied(pendingMode)

        // Dialog should now be opened
        scenario.onFragment { fragment ->
            assertThat(
                    fragment.parentFragmentManager.findFragmentByTag(
                        ResolutionChangeDialogFragment.TAG
                    )
                )
                .isNotNull()
        }

        val bundle =
            Bundle().apply {
                putParcelable(
                    ResolutionChangeDialogFragment.KEY_CONFIRMED,
                    ResolutionChangeConfirmationState.ACCEPT,
                )
            }

        scenario.onFragment { fragment ->
            fragment.setFragmentResult(ResolutionChangeDialogFragment.KEY_RESULT, bundle)
        }
        shadowOf(Looper.getMainLooper()).idle()

        // currentActiveMode is updated to match the pendingMode that has just been set
        assertThat(viewModel.uiState.value?.currentActiveMode).isEqualTo(pendingMode)
        assertThat(viewModel.uiState.value?.isApplying).isFalse()
        verify(mMockedInjector)
            .setUserPreferredDisplayMode(EXTERNAL_DISPLAY_ID, pendingMode, /* storeMode= */ true)
    }

    @Test
    fun onConfirmationResult_revert_updatesViewModelAndInjector() {
        launchFragment()
        val currentActiveMode = viewModel.uiState.value!!.currentActiveMode
        val width = 640
        val height = 480
        val pendingMode =
            externalDisplay.supportedModes.find {
                it.physicalWidth == width && it.physicalHeight == height
            }!!

        scenario.onFragment { f ->
            f.preferenceScreen
                .findPreference<SelectorWithWidgetPreference>("${width}x$height")!!
                .performClick()
        }
        shadowOf(Looper.getMainLooper()).idle()
        scenario.onFragment { fragment ->
            val (menuItem, menu) = getApplyMenuItemAndMenuFromFragment(fragment)
            fragment.onPrepareMenu(menu)
            val applyButton =
                menuItem.actionView!!.findViewById<Button>(R.id.resolution_change_apply_button_view)
            applyButton.performClick()
        }
        shadowOf(Looper.getMainLooper()).idle()

        // Simulate successful mode set
        simulateModeApplied(pendingMode)

        val bundle =
            Bundle().apply {
                putParcelable(
                    ResolutionChangeDialogFragment.KEY_CONFIRMED,
                    ResolutionChangeConfirmationState.REVERT,
                )
            }

        scenario.onFragment { fragment ->
            fragment.setFragmentResult(ResolutionChangeDialogFragment.KEY_RESULT, bundle)
        }
        shadowOf(Looper.getMainLooper()).idle()

        // pendingMode is reverted back to currentActiveMode
        assertThat(viewModel.uiState.value?.pendingMode).isEqualTo(currentActiveMode)
        assertThat(viewModel.uiState.value?.isApplying).isFalse()
        verify(mMockedInjector).resetUserPreferredDisplayMode(EXTERNAL_DISPLAY_ID)
    }

    private fun getApplyMenuItemAndMenuFromFragment(
        fragment: ResolutionRefreshRatePreferenceFragment
    ): Pair<MenuItem, Menu> {
        val menu = mock<Menu>()
        val mockMenuItem = mock<MenuItem>()
        var actionView: View? = null
        whenever(mockMenuItem.setActionView(any<View>())).thenAnswer {
            actionView = it.arguments[0] as View
            mockMenuItem
        }
        whenever(mockMenuItem.actionView).thenAnswer { actionView }
        whenever(menu.add(Menu.NONE, Menu.FIRST, 0, R.string.apply)).thenReturn(mockMenuItem)
        whenever(menu.findItem(Menu.FIRST)).thenReturn(mockMenuItem)
        fragment.onCreateMenu(menu, mock())
        return Pair(mockMenuItem, menu)
    }
}
