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

import com.android.settings.accessibility.textreading.data.FontSize
import com.android.settings.accessibility.textreading.data.FontSizeDataStore

/**
 * A concrete implementation of [TextReadingSizeDelegate] for Font Size adjustments.
 *
 * This delegate bridges Font Size UI events (slider drags or button clicks) to the
 * [FontSizeDataStore]. It manages the temporary [FontSize] preview state and handles the specific
 * persistence logic for font scaling.
 */
internal class FontSizeDelegate(
    private val fontSizeDataStore: FontSizeDataStore,
    private val dataStoreKey: String,
) : TextReadingSizeDelegate<FontSize>(fontSizeDataStore.fontSizeData.value) {

    override fun createUpdatedData(currentData: FontSize, newIndex: Int): FontSize {
        return currentData.copy(currentIndex = newIndex)
    }

    override fun getCurrentIndex(data: FontSize): Int = data.currentIndex

    override fun persistInternal(index: Int) {
        fontSizeDataStore.setInt(dataStoreKey, index)
    }
}
