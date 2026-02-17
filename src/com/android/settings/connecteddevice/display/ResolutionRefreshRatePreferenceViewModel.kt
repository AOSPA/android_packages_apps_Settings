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
import android.graphics.Point
import android.os.Build
import android.os.SystemProperties
import android.util.Log
import android.view.Display.Mode
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.settings.R
import com.android.settings.core.instrumentation.SettingsStatsLog

class ResolutionRefreshRatePreferenceViewModel
@JvmOverloads
constructor(
    application: Application,
    private val displayId: Int,
    val injector: ConnectedDisplayInjector =
        ConnectedDisplayInjector(application.applicationContext),
) : AndroidViewModel(application) {

    data class UiState(
        val currentActiveMode: Mode,
        val pendingMode: Mode,
        val topResolutionItems: List<ResolutionItem> = emptyList(),
        val moreResolutionItems: List<ResolutionItem> = emptyList(),
        val refreshRateItems: List<RefreshRateItem> = emptyList(),
        val areMoreOptionsExpanded: Boolean = false,
    )

    data class ResolutionItem(val physicalWidth: Int, val physicalHeight: Int)

    data class RefreshRateItem(val modeId: Int, val refreshRate: Float)

    data class ConfirmationDialogEvent(val newMode: Mode, val previousMode: Mode)

    private var allowedModes: Map<Int, Mode> = emptyMap()

    private var peakWidth: Int = 0
    private var peakHeight: Int = 0
    private var peakRefreshRate: Int = 0
    private var isRefreshRateSyncEnabled: Boolean = false
    private var allowedResolutions: Set<Point> = emptySet()

    private val _uiState = MutableLiveData<UiState?>(null)
    val uiState: LiveData<UiState?> = _uiState
    private val _confirmationDialogEvent = MutableLiveData<ConfirmationDialogEvent?>(null)
    val confirmationDialogEvent: LiveData<ConfirmationDialogEvent?> = _confirmationDialogEvent

    private val displayListener =
        object : ExternalDisplaySettingsConfiguration.DisplayListener() {
            override fun update(updatedDisplayId: Int) {
                if (displayId != updatedDisplayId) {
                    return
                }
                reloadData()
            }
        }

    init {
        loadFilterConfiguration()
        reloadData()
        injector.registerDisplayListener(displayListener)
    }

    override fun onCleared() {
        super.onCleared()
        injector.unregisterDisplayListener(displayListener)
    }

    private fun reloadData() {
        val display = injector.getDisplay(displayId)
        // Display is disconnected, reset state
        if (display == null || display.mode == null) {
            // Fragment should close itself if UiState is null
            _uiState.value = null
            return
        }
        val currentActiveMode = display.mode
        val supportedModes = display.supportedModes
        allowedModes =
            supportedModes.filter { isAllowed(it, supportedModes) }.associateBy { it.modeId }
        // Active mode might not be part of supported modes, but might have previously been set by
        // the system and should still be displayed
        allowedModes += (currentActiveMode.modeId to currentActiveMode)

        logInfo("Currently selected display mode: $currentActiveMode")
        val resolutionGroups =
            allowedModes.values
                .distinctBy { Point(it.physicalWidth, it.physicalHeight) }
                .map { it.toResolutionItem() }
                .sortedWith(
                    compareByDescending<ResolutionItem> { it.physicalWidth }
                        .thenByDescending { it.physicalHeight }
                )

        val moreItems = resolutionGroups.drop(TOP_MODE_RES_MAX_COUNT)
        val previouslyExpanded = _uiState.value?.areMoreOptionsExpanded ?: false
        val shouldExpand =
            previouslyExpanded || moreItems.any { currentActiveMode.hasSameResolutionAs(it) }
        _uiState.value =
            UiState(
                currentActiveMode = currentActiveMode,
                pendingMode = currentActiveMode,
                topResolutionItems = resolutionGroups.take(TOP_MODE_RES_MAX_COUNT),
                moreResolutionItems = moreItems,
                refreshRateItems = buildRefreshRateItems(currentActiveMode),
                areMoreOptionsExpanded = shouldExpand,
            )
    }

    private fun isAllowed(mode: Mode, displaySupportedModes: List<Mode>): Boolean {
        val supportedModesMap = displaySupportedModes.associateBy { it.modeId }
        if (
            isRefreshRateSyncEnabled &&
                (mode.refreshRate < (DEFAULT_LOW_REFRESH_RATE - 1) ||
                    mode.refreshRate > (DEFAULT_LOW_REFRESH_RATE + 1))
        ) {
            logDebug("$mode refresh rate is out of synchronization range")
            return false
        }
        if (peakHeight > 0 && mode.physicalHeight > peakHeight) {
            logDebug("$mode height is above the allowed limit")
            return false
        }
        if (peakWidth > 0 && mode.physicalWidth > peakWidth) {
            logDebug("$mode width is above the allowed limit")
            return false
        }
        if (peakRefreshRate > 0 && mode.refreshRate > peakRefreshRate) {
            logDebug("$mode refresh rate is above the allowed limit")
            return false
        }

        // If mode filtering is defined, only modes in the whitelist are allowed.
        if (!skipExternalDisplayResolutionFiltering() && allowedResolutions.isNotEmpty()) {
            // If this is an anisotropic mode, check if its base mode is supported.
            val modeToCheck =
                if ((mode.flags and Mode.FLAG_ANISOTROPY_CORRECTION) != 0) {
                    supportedModesMap[mode.parentModeId]
                } else {
                    mode
                }
            return modeToCheck?.let {
                allowedResolutions.contains(Point(it.physicalWidth, it.physicalHeight))
            } ?: false
        }

        return true
    }

    private fun loadFilterConfiguration() {
        val res = getApplication<Application>().resources
        peakRefreshRate =
            res.getInteger(com.android.internal.R.integer.config_externalDisplayPeakRefreshRate)
        peakWidth = res.getInteger(com.android.internal.R.integer.config_externalDisplayPeakWidth)
        peakHeight = res.getInteger(com.android.internal.R.integer.config_externalDisplayPeakHeight)
        isRefreshRateSyncEnabled =
            res.getBoolean(com.android.internal.R.bool.config_refreshRateSynchronizationEnabled) ||
                (!ConnectedDisplayInjector.isRefreshRateFlagsEnabled())

        val resolutionsArray = res.getIntArray(R.array.config_resolutionsShownOnExternalDisplay)
        allowedResolutions =
            resolutionsArray
                .asSequence()
                .chunked(2)
                .filter { it.size == 2 }
                .map { Point(it[0], it[1]) }
                .toSet()
        logDebug("peakRefreshRate=$peakRefreshRate")
        logDebug("peakWidth=$peakWidth")
        logDebug("peakHeight=$peakHeight")
        logDebug("isRefreshRateSyncEnabled=$isRefreshRateSyncEnabled")
        logDebug("allowedResolutions=$allowedResolutions")
    }

    fun onResolutionSelected(item: ResolutionItem) {
        val currentState = _uiState.value ?: return
        // Pre-select the highest refresh rate for the selected resolution
        val newPendingMode =
            if (currentState.currentActiveMode.hasSameResolutionAs(item)) {
                // If same resolution is selected, choose the currently applied refresh rate instead
                // of selecting the highest one
                currentState.currentActiveMode
            } else {
                allowedModes.values
                    .filter { it.hasSameResolutionAs(item) }
                    .maxByOrNull { it.refreshRate }
            }
        // Selected mode must be coming from allowedModes, and should exist here
        if (newPendingMode == null) {
            logWarn("No supported mode found for resolution item: $item")
            return
        }
        // Ignore state update if modeId is the same
        if (newPendingMode.modeId == currentState.pendingMode.modeId) return

        updateState {
            it.copy(
                pendingMode = newPendingMode,
                refreshRateItems = buildRefreshRateItems(newPendingMode),
            )
        }
    }

    fun onRefreshRateSelected(modeId: Int) {
        val currentState = _uiState.value ?: return
        // Selected mode must be coming from allowedModes, and should exist here
        val newPendingMode = allowedModes[modeId]
        if (newPendingMode == null) {
            logWarn("Mode $modeId not found")
            return
        }
        // Ignore state update if modeId is the same
        if (newPendingMode.modeId == currentState.pendingMode.modeId) return

        updateState { it.copy(pendingMode = newPendingMode) }
    }

    fun onMoreOptionsExpanded() {
        updateState { it.copy(areMoreOptionsExpanded = true) }
    }

    fun onApplyClicked() {
        val currentState = _uiState.value ?: return
        val currentPendingMode = currentState.pendingMode
        val currentActiveMode = currentState.currentActiveMode

        _confirmationDialogEvent.value =
            ConfirmationDialogEvent(currentPendingMode, currentActiveMode)
        logInfo("Try applying mode: $currentPendingMode")
        injector.setUserPreferredDisplayMode(displayId, currentPendingMode, storeMode = false)
    }

    fun onConfirmationResult(confirmed: Boolean) {
        val event = _confirmationDialogEvent.value ?: return
        val previousMode = event.previousMode
        val newMode = event.newMode
        if (confirmed) {
            logInfo("Mode change confirmed: $newMode")
            injector.setUserPreferredDisplayMode(displayId, newMode, storeMode = true)
            updateState { it.copy(currentActiveMode = newMode) }
            logResolutionChange(newMode)
        } else {
            logInfo("Mode change rejected, reverting to: $previousMode")
            injector.resetUserPreferredDisplayMode(displayId)
            updateState { it.copy(pendingMode = previousMode) }
        }
        _confirmationDialogEvent.value = null
    }

    private fun buildRefreshRateItems(resolutionSelectedMode: Mode): List<RefreshRateItem> {
        return if (isRefreshRateSyncEnabled) {
            listOf(
                RefreshRateItem(resolutionSelectedMode.modeId, resolutionSelectedMode.refreshRate)
            )
        } else {
            allowedModes.values
                .filter { it.hasSameResolutionAs(resolutionSelectedMode) }
                .sortedByDescending { it.refreshRate }
                .distinctBy { it.refreshRate }
                .map { it.toRefreshRateItem() }
        }
    }

    private fun updateState(updateAction: (UiState) -> UiState) {
        val currentState = _uiState.value ?: return
        _uiState.value = updateAction(currentState)
    }

    private fun logResolutionChange(mode: Mode) {
        val logger = ExternalDisplaySettingsLoggerStore.getLogger(displayId)
        logger.updateResolution(mode.physicalWidth, mode.physicalHeight)
        logger.log(SettingsStatsLog.EXTERNAL_DISPLAY_SETTINGS_CHANGED__SETTING__RESOLUTION)
    }

    private fun logDebug(message: String) {
        Log.d(TAG, "[Display#$displayId] $message")
    }

    private fun logInfo(message: String) {
        Log.i(TAG, "[Display#$displayId] $message")
    }

    private fun logWarn(message: String) {
        Log.w(TAG, "[Display#$displayId] $message")
    }

    companion object {

        @JvmStatic
        fun skipExternalDisplayResolutionFiltering() =
            (Build.IS_USERDEBUG || Build.IS_ENG) &&
                SystemProperties.getBoolean(PROP_SKIP_EXTERNAL_DISPLAY_RESOLUTION_FILTERING, false)

        private fun Mode.toResolutionItem(): ResolutionItem =
            ResolutionItem(this.physicalWidth, this.physicalHeight)

        private fun Mode.toRefreshRateItem(): RefreshRateItem =
            RefreshRateItem(this.modeId, this.refreshRate)

        private fun Mode.hasSameResolutionAs(other: Mode?): Boolean {
            if (other == null) return false
            return this.physicalWidth == other.physicalWidth &&
                this.physicalHeight == other.physicalHeight
        }

        private fun Mode.hasSameResolutionAs(other: ResolutionItem): Boolean {
            return this.physicalWidth == other.physicalWidth &&
                this.physicalHeight == other.physicalHeight
        }

        private const val TAG = "ResRefreshRatePref"
        private const val DEFAULT_LOW_REFRESH_RATE = 60
        private const val PROP_SKIP_EXTERNAL_DISPLAY_RESOLUTION_FILTERING =
            "persist.sys.display.skip_resolution_filtering.external"
        const val TOP_MODE_RES_MAX_COUNT = 3
    }
}
