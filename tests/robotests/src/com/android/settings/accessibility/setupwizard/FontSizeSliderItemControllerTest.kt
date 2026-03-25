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
import com.android.settings.accessibility.textreading.ui.FontSizePreference
import com.android.settings.testutils.SettingsStoreRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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
    fun bindData_setsCorrectIncrement() {
        val expectedIncrement = metadata.getIncrementStep(context)

        controller.bindData(item)

        assertThat(item.sliderIncrement).isEqualTo(expectedIncrement)
    }
}
