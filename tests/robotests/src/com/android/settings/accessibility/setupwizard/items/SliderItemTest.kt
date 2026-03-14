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

package com.android.settings.accessibility.setupwizard.items

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests for [SliderItem]. */
@RunWith(RobolectricTestRunner::class)
class SliderItemTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val item = SliderItem()
    private val rootView: View = LayoutInflater.from(context).inflate(item.layoutResource, null)

    @Test
    fun setSliderValue_outOfBounds_coercesValue() {
        item.min = 10
        item.max = 20

        item.sliderValue = 5
        assertThat(item.sliderValue).isEqualTo(10)

        item.sliderValue = 25
        assertThat(item.sliderValue).isEqualTo(20)
    }

    @Test
    fun setMax_lowerThanCurrentValue_updatesSliderValue() {
        item.sliderValue = 50
        item.max = 30

        assertThat(item.sliderValue).isEqualTo(30)
    }

    @Test
    fun setMin_higherThanCurrentValue_updatesSliderValue() {
        item.sliderValue = 10
        item.min = 20

        assertThat(item.sliderValue).isEqualTo(20)
    }

    @Test
    fun setSliderIncrement_exceedingRange_coercesIncrement() {
        item.min = 0
        item.max = 10
        item.sliderIncrement = 50

        assertThat(item.sliderIncrement).isEqualTo(10)
    }

    @Test
    fun clickStartIcon_decrementsValue() {
        item.min = 0
        item.max = 100
        item.sliderValue = 50
        item.sliderIncrement = 10
        item.iconStartId = android.R.drawable.ic_media_play

        item.onBindView(rootView)

        val startIconFrame = rootView.findViewById<View>(R.id.icon_start).parent as View
        startIconFrame.performClick()

        assertThat(item.sliderValue).isEqualTo(40)
    }

    @Test
    fun clickEndIcon_incrementsValue() {
        item.min = 0
        item.max = 100
        item.sliderValue = 50
        item.sliderIncrement = 10
        item.iconEndId = android.R.drawable.ic_media_play

        item.onBindView(rootView)

        val endIconFrame = rootView.findViewById<View>(R.id.icon_end).parent as View
        endIconFrame.performClick()

        assertThat(item.sliderValue).isEqualTo(60)
    }

    @Test
    fun icons_hidden_whenIdsAreZero() {
        item.iconStartId = 0
        item.iconEndId = 0

        item.onBindView(rootView)

        val startIconFrame = rootView.findViewById<View>(R.id.icon_start).parent as View
        val endIconFrame = rootView.findViewById<View>(R.id.icon_end).parent as View

        assertThat(startIconFrame.visibility).isEqualTo(View.GONE)
        assertThat(endIconFrame.visibility).isEqualTo(View.GONE)
    }
}
