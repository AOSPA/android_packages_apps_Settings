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
import android.view.Display.Mode
import androidx.lifecycle.Observer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
class ResolutionRefreshRatePreferenceViewModelTest : ExternalDisplayTestBase() {

    // Rule to execute LiveData operations synchronously
    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

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

    @Test
    fun init_loadsCorrectInitialState() {
        setupViewModel()
        val state = viewModel.uiState.value!!
        val mode = externalDisplay.mode!!

        assertThat(state.currentActiveMode).isEqualTo(mode)
        assertThat(state.pendingMode).isEqualTo(mode)
        assertThat(viewModel.confirmationDialogEvent.value).isNull()

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
                ResolutionItem(physicalWidth = 320, physicalHeight = 240),
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
    fun onApplyClicked_setsConfirmationEventAndStartsPreview() {
        setupViewModel()
        viewModel.onResolutionSelected(viewModel.uiState.value!!.moreResolutionItems.first())
        val uiState = viewModel.uiState.value!!
        val pendingMode = uiState.pendingMode
        val currentActiveMode = uiState.currentActiveMode

        viewModel.onApplyClicked()

        val confirmationDialogEvent = viewModel.confirmationDialogEvent.value
        assertThat(confirmationDialogEvent).isNotNull()
        assertThat(confirmationDialogEvent!!.newMode).isEqualTo(pendingMode)
        assertThat(confirmationDialogEvent.existingMode).isEqualTo(currentActiveMode)
        verify(mMockedInjector).setUserPreferredDisplayMode(EXTERNAL_DISPLAY_ID, pendingMode, false)
    }

    @Test
    fun onConfirmationResult_confirmed_setsModePermanentlyAndUpdatescurrentActiveMode() {
        setupViewModel()
        viewModel.onResolutionSelected(viewModel.uiState.value!!.moreResolutionItems.first())
        val uiState = viewModel.uiState.value!!
        val pendingMode = uiState.pendingMode
        viewModel.onApplyClicked()

        viewModel.onConfirmationResult(true)
        val updatedState = viewModel.uiState.value!!

        // The original mode should now be the new pending mode
        assertThat(updatedState.currentActiveMode).isEqualTo(pendingMode)
        // The pending mode should be synced to the new original mode
        assertThat(updatedState.pendingMode).isEqualTo(pendingMode)
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
        viewModel.onApplyClicked()

        viewModel.onConfirmationResult(false)
        val updatedState = viewModel.uiState.value!!

        // The original mode should remain unchanged
        assertThat(updatedState.currentActiveMode).isEqualTo(currentActiveMode)
        // The pending mode should be reverted back to the original mode
        assertThat(updatedState.pendingMode).isEqualTo(currentActiveMode)
        // The dialog event should be cleared
        assertThat(viewModel.confirmationDialogEvent.value).isNull()
        verify(mMockedInjector).resetUserPreferredDisplayMode(EXTERNAL_DISPLAY_ID)
    }
}
