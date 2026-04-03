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
import com.android.settings.accessibility.textreading.data.DisplaySizeDataStore
import com.android.settings.accessibility.textreading.ui.DisplaySizePreference
import com.android.settings.testutils.SettingsStoreRule
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests for [DisplaySizeSliderItemController]. */
@RunWith(RobolectricTestRunner::class)
class DisplaySizeSliderItemControllerTest {

    @get:Rule val settingsStoreRule = SettingsStoreRule()

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val item = SliderItem()
    private val metadata =
        DisplaySizePreference(
            context,
            TextReadingPreferenceFragment.EntryPoint.SUW_VISION_SETTINGS,
            /* isUiOnly= */ true,
        )
    private val controller = DisplaySizeSliderItemController(context, item, metadata)
    private val displayStore: DisplaySizeDataStore
        get() = metadata.storage(context) as DisplaySizeDataStore

    private val displaySizes = displayStore.displaySizeData.value.values

    @Test
    fun bindData_withMultipleSizes_initializesSliderRange() {
        assumeTrue("Environment must support multiple display sizes", displaySizes.size > 1)
        val expectedMax = metadata.getMaxValue(context)
        val expectedMin = metadata.getMinValue(context)
        val expectedIncrement = metadata.getIncrementStep(context)

        controller.bindData(item)

        assertThat(item.isVisible).isTrue()
        assertThat(item.min).isEqualTo(expectedMax)
        assertThat(item.max).isEqualTo(expectedMin)
        assertThat(item.sliderIncrement).isEqualTo(expectedIncrement)
    }

    @Test
    fun bindData_withSingleSize_hidesSliderAndSetsFallback() {
        assumeTrue("Environment has only one display size", displaySizes.size <= 1)

        controller.bindData(item)

        assertThat(item.isVisible).isFalse()
        assertThat(item.min).isEqualTo(0)
        assertThat(item.max).isEqualTo(1)
        assertThat(item.sliderIncrement).isEqualTo(1)
    }
}
