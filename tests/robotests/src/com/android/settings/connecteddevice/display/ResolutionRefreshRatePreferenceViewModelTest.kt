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
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.view.Display.Mode
import androidx.lifecycle.Observer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.graphics.surfaceflinger.flags.Flags.FLAG_FOLLOWER_ARBITRARY_REFRESH_RATE_SELECTION_PLATFORM
import com.android.graphics.surfaceflinger.flags.Flags.FLAG_FOLLOWER_DISPLAY_BACKPRESSURE_PLATFORM
import com.android.graphics.surfaceflinger.flags.Flags.FLAG_FORCE_SLOWER_FOLLOWER_GPU_COMPOSITION_PLATFORM
import com.android.graphics.surfaceflinger.flags.Flags.FLAG_SYNCED_RESOLUTION_SWITCH
import com.android.settings.connecteddevice.display.ResolutionRefreshRatePreferenceViewModel.RefreshRateItem
import com.android.settings.connecteddevice.display.ResolutionRefreshRatePreferenceViewModel.ResolutionItem
import com.android.settings.connecteddevice.display.ResolutionRefreshRatePreferenceViewModel.UiState
import com.android.settings.testutils.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Unit test for [ResolutionRefreshRatePreferenceViewModel] */
@RunWith(AndroidJUnit4::class)
@EnableFlags(
    FLAG_FOLLOWER_ARBITRARY_REFRESH_RATE_SELECTION_PLATFORM,
    FLAG_FOLLOWER_DISPLAY_BACKPRESSURE_PLATFORM,
    FLAG_FORCE_SLOWER_FOLLOWER_GPU_COMPOSITION_PLATFORM,
    FLAG_SYNCED_RESOLUTION_SWITCH,
)
class ResolutionRefreshRatePreferenceViewModelTest : ExternalDisplayTestBase() {

    // Rule to execute LiveData operations synchronously
    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    @get:Rule val setFlagsRule: SetFlagsRule = SetFlagsRule()

    @Mock private lateinit var uiStateObserver: Observer<UiState?>

    private lateinit var viewModel: ResolutionRefreshRatePreferenceViewModel
    private lateinit var application: Application

    private val externalDisplay by lazy { mDisplays.find { it.id == EXTERNAL_DISPLAY_ID }!! }

    @Before
    override fun setUp() {
        super.setUp()
        application = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        viewModel.uiState.removeObserver(uiStateObserver)
    }

    private fun setupViewModel(displayId: Int = EXTERNAL_DISPLAY_ID) {
        viewModel =
            ResolutionRefreshRatePreferenceViewModel(application, displayId, mMockedInjector)
        viewModel.uiState.observeForever(uiStateObserver)
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
    }

    @Test
    fun init_loadsCorrectInitialState() {
        setupViewModel()
        val state = viewModel.uiState.value!!
        val mode = externalDisplay.mode!!

        assertThat(state.currentActiveMode).isEqualTo(mode)
        assertThat(state.pendingMode).isEqualTo(mode)
        assertThat(viewModel.confirmationDialogEvent.value).isNull()
        assertThat(state.isApplying).isFalse()

        assertThat(state.topResolutionItems)
            .containsExactly(
                ResolutionItem(physicalWidth = 2048, physicalHeight = 1024),
                ResolutionItem(physicalWidth = 1920, physicalHeight = 1080),
                ResolutionItem(physicalWidth = 800, physicalHeight = 600),
            )
            .inOrder()

        assertThat(state.moreResolutionItems)
            .containsExactly(
                ResolutionItem(physicalWidth = 760, physicalHeight = 600),
                ResolutionItem(physicalWidth = 720, physicalHeight = 480),
                ResolutionItem(physicalWidth = 640, physicalHeight = 480),
            )
            .inOrder()

        assertThat(state.refreshRateItems).hasSize(1)
        assertThat(state.refreshRateItems.first().modeId).isEqualTo(mode.modeId)
        assertThat(state.refreshRateItems.first().refreshRate).isEqualTo(mode.refreshRate)
    }

    @Test
    fun init_withUnsupportedActiveMode_includesMode() {
        val displayId = 123
        val width = 765
        val height = 432
        val unsupportedMode = Mode(234, width, height, 240f)
        val updatedEnabledDisplays = mDisplays.toMutableList()
        updatedEnabledDisplays.add(
            DisplayDevice(
                displayId,
                "local:1111111111",
                "test",
                unsupportedMode,
                externalDisplay.supportedModes,
                /* isEnabled= */ DisplayIsEnabled.YES,
                /* isConnectedDisplay= */ true,
                /* rotation= */ 0,
                isHdrSupported = externalDisplay.isHdrSupported,
            )
        )
        updateDisplaysAndTopology(updatedEnabledDisplays)

        setupViewModel(displayId)
        val state = viewModel.uiState.value!!

        assertThat(state.currentActiveMode).isEqualTo(unsupportedMode)
        assertThat(state.pendingMode).isEqualTo(unsupportedMode)

        val allResolutions = state.topResolutionItems + state.moreResolutionItems
        assertThat(allResolutions).contains(ResolutionItem(width, height))
    }

    @Test
    fun displayDisconnected_emitsEmptyState() {
        setupViewModel()
        whenever(mMockedInjector.getDisplay(EXTERNAL_DISPLAY_ID)).thenReturn(null)

        mListener.update(EXTERNAL_DISPLAY_ID)
        assertThat(viewModel.uiState.value).isNull()
    }

    @Test
    fun displayDisconnected_duringConfirmationDialog_clearsUiState() {
        // Setup ViewModel and enter "Applying" state
        setupViewModel()
        viewModel.onResolutionSelected(viewModel.uiState.value!!.moreResolutionItems.first())
        val pendingMode = viewModel.uiState.value!!.pendingMode

        // Trigger the confirmation dialog state
        viewModel.onApplyClicked()
        simulateModeApplied(pendingMode)

        // Verify confirmation state
        assertThat(viewModel.confirmationDialogEvent.value).isNotNull()

        // Simulate display disconnection
        whenever(mMockedInjector.getDisplay(EXTERNAL_DISPLAY_ID)).thenReturn(null)
        mListener.update(EXTERNAL_DISPLAY_ID)

        // Assert UI state is cleared despite the active dialog
        assertThat(viewModel.uiState.value).isNull()
        assertThat(viewModel.confirmationDialogEvent.value).isNull()
    }

    @Test
    fun displayDisconnected_whileApplying_clearsUiState() {
        setupViewModel()
        viewModel.onResolutionSelected(viewModel.uiState.value!!.moreResolutionItems.first())

        // Start the mode change (isApplying = true)
        viewModel.onApplyClicked()

        // Simulate disconnect before activeMode is updated
        whenever(mMockedInjector.getDisplay(EXTERNAL_DISPLAY_ID)).thenReturn(null)
        mListener.update(EXTERNAL_DISPLAY_ID)

        // Assert UI state is cleared
        assertThat(viewModel.uiState.value).isNull()
        assertThat(viewModel.confirmationDialogEvent.value).isNull()
    }

    @Test
    fun onResolutionSelected_newResolution_selectsHighestRefreshRateAndUpdatesState() {
        setupViewModel()
        // This resolution has 50Hz and 60Hz refresh rate variants
        val newResolutionItem = ResolutionItem(physicalWidth = 640, physicalHeight = 480)

        viewModel.onResolutionSelected(newResolutionItem)
        val state = viewModel.uiState.value!!

        val expectedHighestRefreshRateMode = externalDisplay.supportedModes.find { it.modeId == 3 }
        assertThat(expectedHighestRefreshRateMode).isNotNull()
        assertThat(state.pendingMode).isEqualTo(expectedHighestRefreshRateMode)
        assertThat(state.refreshRateItems).hasSize(2)
        assertThat(state.refreshRateItems)
            .containsExactly(
                RefreshRateItem(modeId = 3, refreshRate = 60f),
                RefreshRateItem(modeId = 4, refreshRate = 50f),
            )
            .inOrder()
    }

    @Test
    fun onRefreshRateSelected_updatesPendingMode() {
        setupViewModel()
        val currentActiveMode = viewModel.uiState.value!!.currentActiveMode

        val resolutionToSelect = ResolutionItem(physicalWidth = 640, physicalHeight = 480)
        viewModel.onResolutionSelected(resolutionToSelect)

        val targetModeAt50Hz = externalDisplay.supportedModes.find { it.modeId == 4 }
        assertThat(targetModeAt50Hz).isNotNull()

        viewModel.onRefreshRateSelected(targetModeAt50Hz!!.modeId)
        val state = viewModel.uiState.value!!

        assertThat(state.pendingMode).isEqualTo(targetModeAt50Hz)
        assertThat(state.currentActiveMode).isEqualTo(currentActiveMode)
    }

    @Test
    fun onApplyClicked_setsApplyingStateAndCallsInjector() {
        setupViewModel()
        viewModel.onResolutionSelected(viewModel.uiState.value!!.moreResolutionItems.first())
        val uiState = viewModel.uiState.value!!
        val pendingMode = uiState.pendingMode

        viewModel.onApplyClicked()

        val updatedState = viewModel.uiState.value!!
        assertThat(updatedState.isApplying).isTrue()
        assertThat(viewModel.confirmationDialogEvent.value).isNull()
        verify(mMockedInjector).setUserPreferredDisplayMode(EXTERNAL_DISPLAY_ID, pendingMode, false)
    }

    @Test
    fun displayUpdate_afterApply_showsConfirmationDialog() {
        setupViewModel()
        val originalMode = viewModel.uiState.value!!.currentActiveMode
        viewModel.onResolutionSelected(viewModel.uiState.value!!.moreResolutionItems.first())
        val pendingMode = viewModel.uiState.value!!.pendingMode
        viewModel.onApplyClicked()

        simulateModeApplied(pendingMode)

        val confirmationDialogEvent = viewModel.confirmationDialogEvent.value
        assertThat(confirmationDialogEvent).isNotNull()
        assertThat(confirmationDialogEvent!!.newMode).isEqualTo(pendingMode)
        assertThat(confirmationDialogEvent.previousMode).isEqualTo(originalMode)
    }

    @Test
    fun onConfirmationResult_confirmed_setsModePermanentlyAndUpdatescurrentActiveMode() {
        setupViewModel()
        viewModel.onResolutionSelected(viewModel.uiState.value!!.moreResolutionItems.first())
        val uiState = viewModel.uiState.value!!
        val pendingMode = uiState.pendingMode
        viewModel.onApplyClicked()

        simulateModeApplied(pendingMode)

        viewModel.onConfirmationResult(true)
        val updatedState = viewModel.uiState.value!!

        // The original mode should now be the new pending mode
        assertThat(updatedState.currentActiveMode).isEqualTo(pendingMode)
        // The pending mode should be synced to the new original mode
        assertThat(updatedState.pendingMode).isEqualTo(pendingMode)
        // Applying state is concluded
        assertThat(updatedState.isApplying).isFalse()
        // The dialog event should be cleared
        assertThat(viewModel.confirmationDialogEvent.value).isNull()
        verify(mMockedInjector).setUserPreferredDisplayMode(EXTERNAL_DISPLAY_ID, pendingMode, true)
    }

    @Test
    fun onConfirmationResult_canceled_resetsHardwareAndRevertsPendingMode() {
        setupViewModel()
        val uiState = viewModel.uiState.value!!
        val currentActiveMode = uiState.currentActiveMode
        viewModel.onResolutionSelected(uiState.moreResolutionItems.first())
        val pendingMode = viewModel.uiState.value!!.pendingMode
        viewModel.onApplyClicked()

        simulateModeApplied(pendingMode)

        viewModel.onConfirmationResult(false)
        val updatedState = viewModel.uiState.value!!

        // The original mode should remain unchanged
        assertThat(updatedState.currentActiveMode).isEqualTo(currentActiveMode)
        // The pending mode should be reverted back to the original mode
        assertThat(updatedState.pendingMode).isEqualTo(currentActiveMode)
        // Applying state is concluded
        assertThat(updatedState.isApplying).isFalse()
        // The dialog event should be cleared
        assertThat(viewModel.confirmationDialogEvent.value).isNull()
        verify(mMockedInjector).resetUserPreferredDisplayMode(EXTERNAL_DISPLAY_ID)
    }

    @Test
    fun onResolutionSelected_withDuplicateRefreshRates_deduplicatesRefreshRateItems() {
        // Setup modes with duplicate refresh rates for the same resolution.
        // This simulates a scenario where a display might report multiple modes (e.g., HDR and
        // non-HDR) for the same resolution and refresh rate.
        val displayId = 456
        val width = 640
        val height = 480
        val activeMode = Mode(99, 1920, 1080, 60f)
        val supportedModes =
            listOf(
                activeMode,
                Mode(100, width, height, 60f), // 60Hz variant 1
                Mode(101, width, height, 60f), // 60Hz variant 2 (duplicate refresh rate)
                Mode(102, width, height, 50f), // 50Hz
            )

        // Create a new display device with these modes.
        val displayWithDuplicates =
            DisplayDevice(
                displayId,
                "local:2222222222",
                "test_duplicates",
                activeMode,
                supportedModes,
                isEnabled = DisplayIsEnabled.YES,
                isConnectedDisplay = true,
                rotation = 0,
                isHdrSupported = externalDisplay.isHdrSupported,
            )
        updateDisplaysAndTopology(listOf(displayWithDuplicates))

        // Setup ViewModel and select the resolution with duplicate refresh rates.
        setupViewModel(displayId)
        viewModel.onResolutionSelected(ResolutionItem(width, height))

        // Assert that the refresh rate list is correctly deduplicated.
        val state = viewModel.uiState.value!!
        assertThat(state.refreshRateItems).hasSize(2)
        assertThat(state.refreshRateItems)
            .containsExactly(
                RefreshRateItem(modeId = 100, refreshRate = 60f),
                RefreshRateItem(modeId = 102, refreshRate = 50f),
            )
            .inOrder()
    }

    @Test
    fun init_withHighDpi_filtersOutLowDpResolutions() {
        // Mocking physical DPI so that calculateBaseDensity results in 320 DPI.
        // 320 DPI means 1 dp = 2 pixels.
        // 600 dp = 1200 pixels.
        whenever(mMockedInjector.getPhysicalDpi(EXTERNAL_DISPLAY_ID)).thenReturn(Pair(320f, 320f))

        setupViewModel()
        val state = viewModel.uiState.value!!

        // Resolutions in ExternalDisplayTestBase:
        // (0) 1920x1080 -> 1920*160/320 = 960 dp (Allowed)
        // (5) 2048x1024 -> 2048*160/320 = 1024 dp (Allowed)
        // (1) 800x600 -> 800*160/320 = 400 dp (Filtered)
        // (2) 320x240 -> 320*160/320 = 160 dp (Filtered)
        // (3,4) 640x480 -> 640*160/320 = 320 dp (Filtered)
        // (6) 720x480 -> 720*160/320 = 360 dp (Filtered)
        // (7) 760x600 -> 760*160/320 = 380 dp (Filtered)
        // (8) 720x480 -> 720*160/320 = 360 dp (Filtered)

        assertThat(state.topResolutionItems)
            .containsExactly(
                ResolutionItem(physicalWidth = 2048, physicalHeight = 1024),
                ResolutionItem(physicalWidth = 1920, physicalHeight = 1080),
                ResolutionItem(physicalWidth = 720, physicalHeight = 480),
            )
            .inOrder()

        assertThat(state.moreResolutionItems).isEmpty()
    }

    @Test
    fun init_withHighDpi_includesActiveModeEvenIfBelowThreshold() {
        val displayId = 789
        val lowResMode = Mode(10, 800, 600, 60f)
        val highResMode = Mode(11, 1920, 1080, 60f)
        val supportedModes = listOf(lowResMode, highResMode)

        val display =
            DisplayDevice(
                displayId,
                "local:789",
                "test",
                lowResMode,
                supportedModes,
                isEnabled = DisplayIsEnabled.YES,
                isConnectedDisplay = true,
                rotation = 0,
                isHdrSupported = false,
            )
        updateDisplaysAndTopology(listOf(display))

        whenever(mMockedInjector.getPhysicalDpi(displayId)).thenReturn(Pair(320f, 320f))

        setupViewModel(displayId)
        val state = viewModel.uiState.value!!

        // lowResMode is 800x600 -> 400dp < 600dp, but it's active so it should be included.
        // highResMode is 1920x1080 -> 293dp < 600dp, should be filtered.

        val allResolutions = state.topResolutionItems + state.moreResolutionItems
        assertThat(allResolutions).containsExactly(ResolutionItem(800, 600))
    }

    @Test
    fun isAllowed_withHighResActiveMode2560x1440_doesNotFilter1024x768_bug_496815734() {
        // This test mimics Case 1 from b/496815734.
        val displayId = 101
        val activeMode = Mode(1, 2560, 1440, 60f)
        val lowResMode = Mode(2, 1024, 768, 60f)
        val supportedModes = listOf(activeMode, lowResMode)

        val display =
            DisplayDevice(
                displayId,
                "local:101",
                "test_bug_case1",
                activeMode,
                supportedModes,
                isEnabled = DisplayIsEnabled.YES,
                isConnectedDisplay = true,
                rotation = 0,
                isHdrSupported = false,
            )
        updateDisplaysAndTopology(listOf(display))

        // Case 1: Native 2560x1440 is active
        // currentActiveWidth = 2560, physicalXDpi = 108.92
        // checkedModeWidth = 1024
        // Step 1: adjXDpi = (1024 * 108.92) / 2560 = 43.569
        // Step 2: calculateBaseDensity results in density >= 100
        // Result: widthDp > 600 -> VISIBLE
        whenever(mMockedInjector.getPhysicalDpi(displayId)).thenReturn(Pair(108.92f, 108.86f))

        setupViewModel(displayId)
        val state = viewModel.uiState.value!!

        val allResolutions = state.topResolutionItems + state.moreResolutionItems
        assertThat(allResolutions).contains(ResolutionItem(1024, 768))
    }

    @Test
    fun isAllowed_withHighResActiveMode1920x1200_doesNotFilter1024x768_bug_496815734() {
        // This test mimics Case 2 from b/496815734.
        val displayId = 102
        val activeMode = Mode(1, 1920, 1200, 60f)
        val lowResMode = Mode(2, 1024, 768, 60f)
        val supportedModes = listOf(activeMode, lowResMode)

        val display =
            DisplayDevice(
                displayId,
                "local:102",
                "test_bug_case2",
                activeMode,
                supportedModes,
                isEnabled = DisplayIsEnabled.YES,
                isConnectedDisplay = true,
                rotation = 0,
                isHdrSupported = false,
            )
        updateDisplaysAndTopology(listOf(display))

        // Case 2: 1920x1200 is active
        // currentActiveWidth = 1920, physicalXDpi = 81.28
        // checkedModeWidth = 1024
        // Step 1: adjXDpi = (1024 * 81.28) / 1920 = 43.349
        // Result: widthDp > 600 -> VISIBLE
        whenever(mMockedInjector.getPhysicalDpi(displayId)).thenReturn(Pair(81.28f, 89.65f))

        setupViewModel(displayId)
        val state = viewModel.uiState.value!!

        val allResolutions = state.topResolutionItems + state.moreResolutionItems
        assertThat(allResolutions).contains(ResolutionItem(1024, 768))
    }
}
