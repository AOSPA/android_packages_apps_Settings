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

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Data class representing the focus target within a PreferenceScreen.
 *
 * @property preferenceKey The unique key of the Preference containing the focused view.
 * @property viewId The ID of the specific view that has focus (e.g., a button inside a preference).
 */
data class FocusTarget(val preferenceKey: String, val viewId: Int)

/** ViewModel responsible for maintaining focus state across configuration changes. */
class FocusTargetViewModel : ViewModel() {
    private val _focusTarget = MutableStateFlow<FocusTarget?>(null)
    val focusTarget = _focusTarget.asStateFlow()

    /** Updates the current focus target. */
    fun setFocusTarget(preferenceKey: String, viewId: Int) {
        _focusTarget.value = FocusTarget(preferenceKey, viewId)
    }

    /** Clears the current focus target. */
    fun clearFocusTarget() {
        _focusTarget.value = null
    }
}
