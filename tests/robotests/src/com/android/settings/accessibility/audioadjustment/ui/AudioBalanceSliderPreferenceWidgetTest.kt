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

package com.android.settings.accessibility.audioadjustment.ui

import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.testutils.inflateViewHolder
import com.android.settingslib.widget.preference.slider.R as SettingsLibSliderR
import com.google.android.material.slider.Slider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Test for [AudioBalanceSliderPreferenceWidget] */
@RunWith(AndroidJUnit4::class)
class AudioBalanceSliderPreferenceWidgetTest {

    private lateinit var context: Context
    private lateinit var preference: AudioBalanceSliderPreferenceWidget

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        preference = AudioBalanceSliderPreferenceWidget(context)
    }

    @Test
    fun onBindViewHolder_setsSliderCenteredAndForcesLtr() {
        val viewHolder = preference.inflateViewHolder()
        val sliderFrame = viewHolder.findViewById(SettingsLibSliderR.id.slider_frame)!!
        val labelFrame = viewHolder.findViewById(SettingsLibSliderR.id.label_frame)!!
        val slider = viewHolder.findViewById(SettingsLibSliderR.id.slider) as Slider

        // Set to RTL to test if it's forced to LTR
        sliderFrame.layoutDirection = View.LAYOUT_DIRECTION_RTL
        labelFrame.layoutDirection = View.LAYOUT_DIRECTION_RTL
        slider.isCentered = false

        preference.onBindViewHolder(viewHolder)

        assertThat(slider.isCentered).isTrue()
        assertThat(sliderFrame.layoutDirection).isEqualTo(View.LAYOUT_DIRECTION_LTR)
        assertThat(labelFrame.layoutDirection).isEqualTo(View.LAYOUT_DIRECTION_LTR)
    }
}
