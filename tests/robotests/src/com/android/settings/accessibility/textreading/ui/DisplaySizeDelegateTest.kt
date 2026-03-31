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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.settings.accessibility.textreading.data.DisplaySizeDataStore
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Test for [DisplaySizeDelegate]. */
@RunWith(RobolectricTestRunner::class)
class DisplaySizeDelegateTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val displaySizeDataStore = DisplaySizeDataStore(context)
    private val delegate = DisplaySizeDelegate(displaySizeDataStore, DisplaySizePreference.KEY)

    @Test
    fun displaySizePreview_initialValue_matchesDataStore() {
        val expectedData = displaySizeDataStore.displaySizeData.value

        assertThat(delegate.sizePreview.value).isEqualTo(expectedData)
    }

    @Test
    fun onValueChange_updatesPreviewStateImmediately() {
        val initialIndex = delegate.sizePreview.value.currentIndex
        val newIndex = (initialIndex + 1) % delegate.sizePreview.value.values.size

        delegate.onValueChange(index = newIndex)

        assertThat(delegate.sizePreview.value.currentIndex).isEqualTo(newIndex)
    }

    @Test
    fun onValueChange_withoutDragging_triggersCommitAction() {
        var capturedIndex = -1
        val onCommitAction: (Int) -> Unit = { capturedIndex = it }
        val testIndex = 0

        delegate.onValueChange(index = testIndex, onCommitAction = onCommitAction)

        assertThat(capturedIndex).isEqualTo(testIndex)
    }

    @Test
    fun onStopTrackingTouch_triggersCommitAction() {
        var capturedIndex = -1
        val onCommitAction: (Int) -> Unit = { capturedIndex = it }
        val testIndex = 0

        delegate.onStartTrackingTouch()
        delegate.onValueChange(index = testIndex)
        assertThat(capturedIndex).isEqualTo(-1)

        delegate.onStopTrackingTouch(index = testIndex, onCommitAction = onCommitAction)
        assertThat(capturedIndex).isEqualTo(testIndex)
    }
}
