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
import android.view.Display.DEFAULT_DISPLAY
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import java.util.function.Consumer

/** Centralized data source to provide display updates for display preference fragments */
class DisplayPreferenceViewModel
@JvmOverloads
constructor(
    application: Application,
    val injector: ConnectedDisplayInjector =
        ConnectedDisplayInjector(application.applicationContext),
) : AndroidViewModel(application) {

    data class DisplayUiState(
        val enabledDisplays: Map<Int, DisplayDevice>,
        val selectedDisplayId: Int,
    )

    private val appContext = application.applicationContext
    private val selectedDisplayId = MutableLiveData<Int>()
    private val enabledDisplays = MutableLiveData<Map<Int, DisplayDevice>>()

    private val uiStateMediator = MediatorLiveData<DisplayUiState>()
    val uiState: LiveData<DisplayUiState> = uiStateMediator

    private val displayListener =
        object : ExternalDisplaySettingsConfiguration.DisplayListener() {
            override fun update(displayId: Int) {
                // This listens to updates while in mirroring mode because topology update doesn't
                // happen. In non-mirroring mode, both display update and topology update might
                // happen, to prevent double update causing flickering, only listen to one at a
                // time, depending on the mirroring mode
                if (isDisplayInMirroringMode(appContext)) {
                    updateEnabledDisplays()
                }
            }
        }

    private val topologyListener =
        Consumer<DisplayTopology> {
            if (!isDisplayInMirroringMode(appContext)) {
                updateEnabledDisplays()
            }
        }

    init {
        val updateMediator = {
            val displays = enabledDisplays.value ?: emptyMap()
            val selectedId = selectedDisplayId.value ?: getDefaultDisplayId()
            uiStateMediator.value = DisplayUiState(displays, selectedId)
        }
        uiStateMediator.addSource(enabledDisplays) { updateMediator() }
        uiStateMediator.addSource(selectedDisplayId) { updateMediator() }

        injector.registerDisplayListener(displayListener)
        injector.registerTopologyListener(topologyListener)

        updateSelectedDisplay(getDefaultDisplayId())
        updateEnabledDisplays()
    }

    override fun onCleared() {
        super.onCleared()
        injector.unregisterTopologyListener(topologyListener)
        injector.unregisterDisplayListener(displayListener)
    }

    fun updateSelectedDisplay(newDisplayId: Int) {
        if (selectedDisplayId.value != newDisplayId) {
            selectedDisplayId.value = newDisplayId
        }
    }

    fun updateEnabledDisplays() {
        val enabledDisplaysMap =
            injector
                .getDisplays()
                .filter { it.isEnabled == DisplayIsEnabled.YES }
                .associateBy { it.id }
        enabledDisplays.value = enabledDisplaysMap

        val currentSelectedId = selectedDisplayId.value
        if (currentSelectedId == null || !enabledDisplaysMap.contains(currentSelectedId)) {
            updateSelectedDisplay(getDefaultDisplayId())
        }
    }

    private fun getDefaultDisplayId(): Int {
        return injector.displayTopology?.primaryDisplayId ?: DEFAULT_DISPLAY
    }
}
