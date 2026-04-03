/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.accessibility.textreading.ui

import com.android.settings.accessibility.shared.utils.DebounceConfigurationChangeCommitController
import com.android.settings.accessibility.shared.utils.DebounceConfigurationChangeCommitController.Companion.CHANGE_BY_BUTTON_DELAY
import com.android.settings.accessibility.shared.utils.DebounceConfigurationChangeCommitController.Companion.CHANGE_BY_SLIDER_DELAY
import com.android.settings.accessibility.shared.utils.DebounceConfigurationChangeCommitController.Companion.MIN_COMMIT_DELAY
import kotlin.time.Duration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A base delegate responsible for coordinating UI-triggered size adjustments with data persistence.
 *
 * This class handles the shared business logic for settings that use a slider or discrete buttons
 * to adjust a numerical index (e.g., Font Size, Display Size). It decouples immediate UI state
 * updates from the underlying data layer using a debouncing mechanism to prevent excessive disk I/O
 * or configuration changes during active user interaction.
 */
abstract class TextReadingSizeDelegate<T>(initialData: T) {
    private val _sizePreview by lazy { MutableStateFlow(initialData) }

    /**
     * The temporary size preview while the user is interacting with the UI (e.g. dragging a slider)
     * but has not yet committed the change. This is useful for displaying a real-time preview of
     * the size changes.
     */
    val sizePreview by lazy { _sizePreview.asStateFlow() }

    private var isDraggingSlider = false

    private val debounceCommitController by lazy {
        DebounceConfigurationChangeCommitController(minCommitDelay = MIN_COMMIT_DELAY)
    }

    /** Implementations must return a copy of the data with the new index. */
    protected abstract fun createUpdatedData(currentData: T, newIndex: Int): T

    /** Implementations must save the index to the respective DataStore. */
    protected abstract fun persistInternal(index: Int)

    /** Gets the current index from the data model. */
    protected abstract fun getCurrentIndex(data: T): Int

    fun onStartTrackingTouch() {
        isDraggingSlider = true
    }

    fun onStopTrackingTouch(index: Int, onCommitAction: ((Int) -> Unit)? = null) {
        isDraggingSlider = false
        commitChange(CHANGE_BY_SLIDER_DELAY, index, onCommitAction)
    }

    fun onValueChange(
        index: Int,
        onUpdateUi: ((Int) -> Unit)? = null,
        onCommitAction: ((Int) -> Unit)? = null,
    ) {
        val currentData = _sizePreview.value
        if (getCurrentIndex(currentData) != index) {
            _sizePreview.value = createUpdatedData(currentData, index)
        }

        onUpdateUi?.invoke(index)

        if (!isDraggingSlider) {
            commitChange(CHANGE_BY_BUTTON_DELAY, index, onCommitAction)
        }
    }

    private fun commitChange(delay: Duration, index: Int, onCommitAction: ((Int) -> Unit)? = null) {
        onCommitAction?.invoke(index)

        debounceCommitController.commitDelayed(delay) { persistInternal(index) }
    }
}
