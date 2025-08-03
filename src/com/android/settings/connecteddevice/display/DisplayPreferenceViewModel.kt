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
import android.database.ContentObserver
import android.provider.Settings
import android.provider.Settings.Secure.INCLUDE_DEFAULT_DISPLAY_IN_TOPOLOGY
import android.view.Display.DEFAULT_DISPLAY
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData

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
        val isMirroring: Boolean,
        val includeDefaultDisplayInTopology: Boolean,
    )

    private val appContext = application.applicationContext
    private val selectedDisplayId = MutableLiveData<Int>()
    private val enabledDisplays = MutableLiveData<Map<Int, DisplayDevice>>()
    private val isMirroring = MutableLiveData<Boolean>()
    private val includeDefaultDisplayInTopology = MutableLiveData<Boolean>()

    private val uiStateMediator = MediatorLiveData<DisplayUiState>()
    val uiState: LiveData<DisplayUiState> = uiStateMediator

    private val displayListener =
        object : ExternalDisplaySettingsConfiguration.DisplayListener() {
            override fun update(displayId: Int) {
                updateEnabledDisplays()
            }
        }

    @VisibleForTesting
    val mirrorModeObserver =
        object : ContentObserver(injector.handler) {
            override fun onChange(selfChange: Boolean) {
                updateMirroringState()
            }
        }

    @VisibleForTesting
    val includeDefaultDisplayInTopologyObserver =
        object : ContentObserver(injector.handler) {
            override fun onChange(selfChange: Boolean) {
                updateIncludeDefaultDisplayInTopology()
            }
        }

    init {
        val updateMediator = {
            val displays = enabledDisplays.value ?: emptyMap()
            val selectedId = selectedDisplayId.value ?: getDefaultDisplayId()
            val mirroring = isMirroring.value ?: isDisplayInMirroringMode(appContext)
            val includeDefaultDisplayInTopology =
                includeDefaultDisplayInTopology.value ?: isIncludeDefaultDisplayInTopology()
            uiStateMediator.value =
                DisplayUiState(displays, selectedId, mirroring, includeDefaultDisplayInTopology)
        }
        uiStateMediator.addSource(enabledDisplays) { updateMediator() }
        uiStateMediator.addSource(selectedDisplayId) { updateMediator() }
        uiStateMediator.addSource(isMirroring) { updateMediator() }
        uiStateMediator.addSource(includeDefaultDisplayInTopology) { updateMediator() }

        injector.registerDisplayListener(displayListener)
        registerMirrorModeObserver()
        registerIncludeDefaultDisplayInTopologyObserver()

        updateSelectedDisplay(getDefaultDisplayId())
        updateEnabledDisplays()
        updateMirroringState()
        updateIncludeDefaultDisplayInTopology()
    }

    override fun onCleared() {
        super.onCleared()
        injector.unregisterDisplayListener(displayListener)
        appContext.contentResolver.unregisterContentObserver(mirrorModeObserver)
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
                .filter {
                    it.isEnabled == DisplayIsEnabled.YES &&
                        (it.id == DEFAULT_DISPLAY || it.isConnectedDisplay)
                }
                .associateBy { it.id }
        enabledDisplays.value = enabledDisplaysMap

        val currentSelectedId = selectedDisplayId.value
        if (currentSelectedId == null || !enabledDisplaysMap.contains(currentSelectedId)) {
            updateSelectedDisplay(getDefaultDisplayId())
        }
    }

    private fun updateMirroringState() {
        // This doesn't need to trigger manual viewmodel updates for enabled displays as Display
        // and/or DisplayTopology callback will eventually be called following mirroring update
        val newMirroringState = isDisplayInMirroringMode(appContext)
        if (isMirroring.value != newMirroringState) {
            isMirroring.value = newMirroringState
        }
    }

    private fun registerMirrorModeObserver() {
        appContext.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.MIRROR_BUILT_IN_DISPLAY),
            /* notifyForDescendants= */ false,
            mirrorModeObserver,
        )
    }

    private fun updateIncludeDefaultDisplayInTopology() {
        val newState = isIncludeDefaultDisplayInTopology()
        if (includeDefaultDisplayInTopology.value != newState) {
            includeDefaultDisplayInTopology.value = newState
        }
    }

    private fun registerIncludeDefaultDisplayInTopologyObserver() {
        appContext.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(INCLUDE_DEFAULT_DISPLAY_IN_TOPOLOGY),
            /* notifyForDescendants= */ false,
            includeDefaultDisplayInTopologyObserver,
        )
    }

    private fun isIncludeDefaultDisplayInTopology() =
        Settings.Secure.getInt(
            appContext.getContentResolver(),
            INCLUDE_DEFAULT_DISPLAY_IN_TOPOLOGY,
            0,
        ) != 0

    private fun getDefaultDisplayId(): Int {
        return injector.displayTopology?.primaryDisplayId ?: DEFAULT_DISPLAY
    }
}
