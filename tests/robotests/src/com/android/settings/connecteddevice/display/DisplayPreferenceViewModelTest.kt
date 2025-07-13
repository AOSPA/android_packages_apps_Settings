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
import android.hardware.display.DisplayTopology
import android.provider.Settings
import android.view.Display.DEFAULT_DISPLAY
import androidx.lifecycle.Observer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.testutils.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import java.util.function.Consumer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times

/** Unit test for [DisplayPreferenceViewModel] */
@RunWith(AndroidJUnit4::class)
class DisplayPreferenceViewModelTest : ExternalDisplayTestBase() {

    // Rule to execute LiveData operations synchronously
    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock private lateinit var displayTopology: DisplayTopology
    @Mock private lateinit var uiStateObserver: Observer<DisplayPreferenceViewModel.DisplayUiState>

    private val topologyListenerCaptor = argumentCaptor<Consumer<DisplayTopology>>()
    private lateinit var application: Application
    private lateinit var viewModel: DisplayPreferenceViewModel

    private var initialSelectedDisplayId = -1

    @Before
    override fun setUp() {
        super.setUp()
        application = ApplicationProvider.getApplicationContext()

        initialSelectedDisplayId = mDisplays.get(0).id
        doReturn(displayTopology).`when`(mMockedInjector).displayTopology
        doReturn(initialSelectedDisplayId).`when`(displayTopology).primaryDisplayId

        viewModel = DisplayPreferenceViewModel(application, mMockedInjector)
        viewModel.uiState.observeForever(uiStateObserver)

        verify(mMockedInjector).registerTopologyListener(topologyListenerCaptor.capture())
    }

    @After
    fun tearDown() {
        viewModel.uiState.removeObserver(uiStateObserver)
    }

    private fun setMirroringMode(enable: Boolean) {
        Settings.Secure.putInt(
            application.contentResolver,
            Settings.Secure.MIRROR_BUILT_IN_DISPLAY,
            if (enable) 1 else 0,
        )
        viewModel.mirrorModeObserver.onChange(/* selfChange= */ false)
    }

    @Test
    fun init_loadsEnabledDisplaysAndSetsDefaultDisplay() {
        val state = viewModel.uiState.value!!

        assertThat(state.enabledDisplays).hasSize(2)
        assertThat(state.enabledDisplays.keys)
            .containsExactly(EXTERNAL_DISPLAY_ID, OVERLAY_DISPLAY_ID)

        assertThat(state.selectedDisplayId).isEqualTo(initialSelectedDisplayId)
        assertThat(state.isMirroring).isFalse()
    }

    @Test
    fun mirrorModeSettingChanged_updatesUiState() {
        assertThat(viewModel.uiState.value!!.isMirroring).isFalse()

        setMirroringMode(true)

        assertThat(viewModel.uiState.value!!.isMirroring).isTrue()

        setMirroringMode(false)

        assertThat(viewModel.uiState.value!!.isMirroring).isFalse()
    }

    @Test
    fun updateSelectedDisplay_selectedDisplayUpdated() {
        viewModel.updateSelectedDisplay(123)

        val state = viewModel.uiState.value!!
        assertThat(state.selectedDisplayId).isEqualTo(123)
    }

    @Test
    fun updateEnabledDisplays_includeBuiltintDisplay_selectedDisplayIdKept_enabledDisplaysUpdated() {
        includeBuiltinDisplay()

        viewModel.updateEnabledDisplays()

        val state = viewModel.uiState.value!!
        assertThat(state.enabledDisplays).hasSize(3)
        assertThat(state.enabledDisplays.keys)
            .containsExactly(DEFAULT_DISPLAY, EXTERNAL_DISPLAY_ID, OVERLAY_DISPLAY_ID)
        assertThat(state.selectedDisplayId).isEqualTo(initialSelectedDisplayId)
    }

    @Test
    fun updateEnabledDisplays_removeSelectedDisplay_selectedDisplayUpdated() {
        val updatedEnabledDisplays = mDisplays.toMutableList()
        // Remove initially selected display
        updatedEnabledDisplays.removeIf { it.id == initialSelectedDisplayId }
        doReturn(updatedEnabledDisplays).`when`(mMockedInjector).getDisplays()
        doReturn(updatedEnabledDisplays[0].id).`when`(displayTopology).primaryDisplayId

        viewModel.updateEnabledDisplays()

        val state = viewModel.uiState.value!!
        assertThat(state.enabledDisplays).hasSize(1)
        assertThat(state.enabledDisplays.keys).containsExactly(updatedEnabledDisplays[0].id)
    }

    @Test
    fun inMirroringMode_displayListenerTriggersUpdate() {
        setMirroringMode(true)
        includeBuiltinDisplay()
        reset(uiStateObserver)

        mListener.update(DEFAULT_DISPLAY)

        verify(uiStateObserver, times(1)).onChanged(any())
        val state = viewModel.uiState.value!!
        assertThat(state.enabledDisplays).hasSize(3)
    }

    @Test
    fun inMirroringMode_topologyListenerDoesNotTriggerUpdate() {
        setMirroringMode(true)
        includeBuiltinDisplay()
        reset(uiStateObserver)

        topologyListenerCaptor.firstValue.accept(displayTopology)

        verify(uiStateObserver, never()).onChanged(any())
    }

    @Test
    fun inTopologyMode_topologyListenerTriggersUpdate() {
        setMirroringMode(false)
        includeBuiltinDisplay()
        reset(uiStateObserver)

        topologyListenerCaptor.firstValue.accept(displayTopology)

        verify(uiStateObserver, times(1)).onChanged(any())
        val state = viewModel.uiState.value!!
        assertThat(state.enabledDisplays).hasSize(3)
    }

    @Test
    fun inTopologyMode_displayListenerDoesNotTriggerUpdate() {
        setMirroringMode(false)
        includeBuiltinDisplay()
        reset(uiStateObserver)

        mListener.update(DEFAULT_DISPLAY)

        verify(uiStateObserver, never()).onChanged(any())
    }
}
