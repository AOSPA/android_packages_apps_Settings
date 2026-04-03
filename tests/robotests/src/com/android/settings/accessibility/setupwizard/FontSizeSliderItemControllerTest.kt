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

package com.android.settings.accessibility.setupwizard

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.android.settings.accessibility.TextReadingPreferenceFragment
import com.android.settings.accessibility.setupwizard.items.SliderItem
import com.android.settings.accessibility.textreading.data.FontSizeDataStore
import com.android.settings.accessibility.textreading.ui.FontSizePreference
import com.android.settings.testutils.SettingsStoreRule
import com.google.android.material.slider.Slider
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowChoreographer
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowSystemClock

/** Tests for [FontSizeSliderItemController]. */
@RunWith(RobolectricTestRunner::class)
class FontSizeSliderItemControllerTest {

    @get:Rule val settingsStoreRule = SettingsStoreRule()

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val item = SliderItem()
    private val metadata =
        FontSizePreference(
            context,
            TextReadingPreferenceFragment.EntryPoint.SUW_VISION_SETTINGS,
            /* isUiOnly= */ true,
        )
    private val controller = FontSizeSliderItemController(context, item, metadata)

    @Test
    fun bindData_initializesSliderRange() {
        val expectedMax = metadata.getMaxValue(context)
        val expectedMin = metadata.getMinValue(context)

        controller.bindData(item)

        assertThat(item.min).isEqualTo(expectedMin)
        assertThat(item.max).isEqualTo(expectedMax)
    }

    @Test
    fun bindData_initializesSliderValue() {
        val dataStore = metadata.storage(context) as FontSizeDataStore
        val expectedIndex = dataStore.fontSizeData.value.currentIndex

        controller.bindData(item)

        assertThat(item.sliderValue).isEqualTo(expectedIndex)
    }

    @Test
    fun bindData_setsCorrectIncrement() {
        val expectedIncrement = metadata.getIncrementStep(context)

        controller.bindData(item)

        assertThat(item.sliderIncrement).isEqualTo(expectedIncrement)
    }

    @Test
    fun onValueChange_whenNotDragging_commitsWithButtonDelay() {
        val dataStore = metadata.storage(context) as FontSizeDataStore
        controller.bindData(item)
        val mockSlider = mock<Slider>()

        item.extraChangeListener?.onValueChange(mockSlider, 2f, true)
        ShadowLooper.idleMainLooper()
        triggerFrame()

        assertThat(dataStore.getInt(FontSizeSliderItemController.KEY)).isEqualTo(2)
    }

    @Test
    fun onValueChange_whileDragging_doesNotCommitImmediately() {
        val dataStore = metadata.storage(context) as FontSizeDataStore
        controller.bindData(item)
        val mockSlider = mock<Slider>()

        item.extraTouchListener?.onStartTrackingTouch(mockSlider)
        item.extraChangeListener?.onValueChange(mockSlider, 3f, true)
        ShadowLooper.idleMainLooper()
        triggerFrame()

        assertThat(dataStore.getInt(FontSizeSliderItemController.KEY)).isNotEqualTo(3f)
    }

    @Test
    fun onStopTrackingTouch_commitsWithSliderDelay() {
        val dataStore = metadata.storage(context) as FontSizeDataStore
        controller.bindData(item)
        val mockSlider: Slider = mock { on { value } doReturn 4f }

        item.extraTouchListener?.onStartTrackingTouch(mockSlider)
        item.extraTouchListener?.onStopTrackingTouch(mockSlider)
        ShadowLooper.idleMainLooper()
        triggerFrame()

        assertThat(dataStore.getInt(FontSizeSliderItemController.KEY)).isEqualTo(4)
    }

    private fun triggerFrame() {
        // Advance the system clock by the frame delay (usually 16ms)
        ShadowSystemClock.advanceBy(ShadowChoreographer.getFrameDelay())
        // Execute the tasks scheduled on the main looper
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    }
}
