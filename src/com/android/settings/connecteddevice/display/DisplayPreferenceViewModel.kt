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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Centralized data source to provide display updates for display preference fragments */
class DisplayPreferenceViewModel
@JvmOverloads
constructor(
    application: Application,
    val injector: ConnectedDisplayInjector =
        ConnectedDisplayInjector(application.applicationContext),
) : AndroidViewModel(application) {

    data class DisplayUiState(
        val enabledDisplays: Map<Int, DisplayDeviceAdditionalInfo> = emptyMap(),
        val selectedDisplayId: Int = -1,
        val isMirroring: Boolean = false,
        val includeDefaultDisplayInTopology: Boolean = false,
        val showIncludeDefaultDisplayInTopologyPref: Boolean = false,
    )

    private val appContext = application.applicationContext
    private val _uiState = MutableLiveData(DisplayUiState())
    val uiState: LiveData<DisplayUiState> = _uiState

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
        injector.registerDisplayListener(displayListener)
        registerMirrorModeObserver()
        registerIncludeDefaultDisplayInTopologyObserver()

        // Wait synchronously for the first load
        viewModelScope.launch { updateEnabledDisplays().join() }
        updateMirroringState()
        updateIncludeDefaultDisplayInTopology()
    }

    override fun onCleared() {
        super.onCleared()
        appContext.contentResolver.unregisterContentObserver(
            includeDefaultDisplayInTopologyObserver
        )
        appContext.contentResolver.unregisterContentObserver(mirrorModeObserver)
        injector.unregisterDisplayListener(displayListener)
    }

    fun updateSelectedDisplay(newDisplayId: Int) {
        if (_uiState.value?.selectedDisplayId != newDisplayId) {
            updateState { it.copy(selectedDisplayId = newDisplayId) }
        }
    }

    fun updateEnabledDisplays(): Job {
        return viewModelScope.launch {
            // getDisplaysWithAdditionalInfo() runs on bg thread as it will do multiple binder calls
            val enabledDisplaysMap =
                injector
                    .getDisplaysWithAdditionalInfo()
                    .filter {
                        it.isEnabled == DisplayIsEnabled.YES &&
                            (it.id == DEFAULT_DISPLAY || it.isConnectedDisplay)
                    }
                    .associateBy { it.id }

            updateState { currentState ->
                val selectedId =
                    if (enabledDisplaysMap.contains(currentState.selectedDisplayId)) {
                        currentState.selectedDisplayId
                    } else {
                        // If the currently selected display is no longer available, reset to
                        // default.
                        getDefaultDisplayId()
                    }
                currentState.copy(
                    enabledDisplays = enabledDisplaysMap,
                    selectedDisplayId = selectedId,
                )
            }
        }
    }

    private fun updateMirroringState() {
        // This doesn't need to trigger manual viewmodel updates for enabled displays as Display
        // callback will eventually be called following mirroring update
        val newMirroringState = isDisplayInMirroringMode(appContext)
        updateState {
            it.copy(
                isMirroring = newMirroringState,
                showIncludeDefaultDisplayInTopologyPref =
                    isIncludeDefaultDisplayInTopologyPrefAllowed(newMirroringState),
            )
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
        if (_uiState.value?.includeDefaultDisplayInTopology != newState) {
            updateState { it.copy(includeDefaultDisplayInTopology = newState) }
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

    private fun isIncludeDefaultDisplayInTopologyPrefAllowed(isMirroring: Boolean) =
        !isMirroring &&
            injector.isDefaultDisplayInTopologyFlagEnabled() &&
            injector.isProjectedModeEnabled()

    private fun getDefaultDisplayId(): Int {
        return injector.displayTopology?.primaryDisplayId ?: DEFAULT_DISPLAY
    }

    private fun updateState(updateAction: (DisplayUiState) -> DisplayUiState) {
        val currentState = _uiState.value ?: DisplayUiState()
        _uiState.value = updateAction(currentState)
    }
}
