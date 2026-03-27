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
import android.view.HapticFeedbackConstants.CLOCK_TICK
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.google.android.material.slider.Slider
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameters
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestParameterInjector

/** Tests for [SliderItem]. */
@RunWith(RobolectricTestParameterInjector::class)
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

    @TestParameters(
        value =
            [
                "{mode: 'ON_ENDS', init: 5, next: 10.0, expectedHaptic: true}",
                "{mode: 'ON_ENDS', init: 5, next: 0.0, expectedHaptic: true}",
                "{mode: 'ON_ENDS', init: 5, next: 6.0, expectedHaptic: false}",
                "{mode: 'ON_TICKS', init: 5, next: 6.0, expectedHaptic: true}",
                "{mode: 'ON_TICKS', init: 10, next: 10.0, expectedHaptic: false}",
                "{mode: 'NONE', init: 5, next: 10.0, expectedHaptic: false}",
            ]
    )
    @Test
    fun hapticFeedback_changeValue_verifyTrigger(
        mode: HapticTestMode,
        init: Int,
        next: Float,
        expectedHaptic: Boolean,
    ) {
        val spySlider = spy(rootView.findViewById<Slider>(R.id.slider))
        val spyView = spy(rootView)
        spyView.stub { on { findViewById<Slider>(R.id.slider) } doReturn spySlider }
        item.apply {
            min = 0
            max = 10
            sliderValue = init
            hapticFeedbackMode = mode.value
            updatesContinuously = true
        }
        item.onBindView(spyView)
        val argumentCaptor = argumentCaptor<Slider.OnChangeListener>()
        verify(spySlider).addOnChangeListener(argumentCaptor.capture())
        val listener = argumentCaptor.firstValue

        spySlider.stub { on { value } doReturn next }
        listener.onValueChange(spySlider, next, true)

        if (expectedHaptic) {
            verify(spySlider).performHapticFeedback(CLOCK_TICK)
        } else {
            verify(spySlider, never()).performHapticFeedback(CLOCK_TICK)
        }
    }

    @TestParameters(
        value =
            [
                "{keyCode: ${KeyEvent.KEYCODE_DPAD_LEFT}}",
                "{keyCode: ${KeyEvent.KEYCODE_DPAD_RIGHT}}",
            ]
    )
    @Test
    fun onKey_validNavigation_updatesSlider(keyCode: Int) {
        val spySlider = spy(rootView.findViewById<Slider>(R.id.slider))
        item.adjustable = true
        item.onBindView(rootView)
        spySlider.requestFocus()

        val event = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val handled = spySlider.dispatchKeyEvent(event)

        assertThat(handled).isTrue()
        verify(spySlider).onKeyDown(keyCode, event)
    }

    @TestParameters(
        value =
            [
                "{keyCode: ${KeyEvent.KEYCODE_DPAD_LEFT}}",
                "{keyCode: ${KeyEvent.KEYCODE_DPAD_RIGHT}}",
            ]
    )
    @Test
    fun onKey_invalidScenarios_ignoresEvent(keyCode: Int) {
        val spySlider = spy(rootView.findViewById<Slider>(R.id.slider))
        item.adjustable = true
        item.onBindView(rootView)
        spySlider.requestFocus()

        val event = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        val handled = spySlider.dispatchKeyEvent(event)

        assertThat(handled).isFalse()
        verify(spySlider, never()).onKeyDown(anyInt(), any())
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

    @Test
    fun onBindView_setsContentDescriptionFromProperty() {
        val customDescription = "Custom Accessibility Label"
        item.sliderContentDescription = customDescription

        item.onBindView(rootView)

        val slider = rootView.findViewById<Slider>(R.id.slider)
        assertThat(slider.contentDescription).isEqualTo(customDescription)
    }

    @Test
    fun onBindView_setsContentDescriptionFromTitle_whenPropertyIsNull() {
        val title = "Font Size"
        item.title = title
        item.sliderContentDescription = null

        item.onBindView(rootView)

        val slider = rootView.findViewById<Slider>(R.id.slider)
        assertThat(slider.contentDescription).isEqualTo(title)
    }

    @Test
    fun onBindView_setsStateDescription() {
        val stateDescription = "Level 5 of 10"
        item.sliderStateDescription = stateDescription

        item.onBindView(rootView)

        val slider = rootView.findViewById<Slider>(R.id.slider)
        assertThat(slider.stateDescription).isEqualTo(stateDescription)
    }

    @Test
    fun onBindView_clearsDescriptions_whenNull() {
        item.sliderContentDescription = null
        item.sliderStateDescription = null
        item.title = null

        item.onBindView(rootView)

        val slider = rootView.findViewById<Slider>(R.id.slider)
        assertThat(slider.contentDescription).isNull()
        assertThat(slider.stateDescription).isNull()
    }

    enum class HapticTestMode(val value: Int) {
        NONE(SliderItem.HAPTIC_FEEDBACK_MODE_NONE),
        ON_TICKS(SliderItem.HAPTIC_FEEDBACK_MODE_ON_TICKS),
        ON_ENDS(SliderItem.HAPTIC_FEEDBACK_MODE_ON_ENDS),
    }
}
