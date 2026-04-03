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

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.android.settings.accessibility.TextReadingPreferenceFragment
import com.android.settings.accessibility.setupwizard.items.SliderItem
import com.android.settings.accessibility.textreading.data.DisplaySizeDataStore
import com.android.settings.accessibility.textreading.ui.DisplaySizeDelegate
import com.android.settings.accessibility.textreading.ui.DisplaySizePreference
import com.google.android.material.slider.Slider
import com.google.android.setupdesign.items.Item

/** Controller for the display size slider item in the Accessibility Setup Wizard. */
internal class DisplaySizeSliderItemController(
    private val context: Context,
    item: Item,
    private val metadata: DisplaySizePreference,
) : BaseItemController(item) {

    private val displaySizeDataStore = metadata.storage(context) as DisplaySizeDataStore
    private val delegate by lazy {
        DisplaySizeDelegate(displaySizeDataStore = displaySizeDataStore, dataStoreKey = KEY)
    }

    override fun bindData(item: Item) {
        (item as? SliderItem)?.apply {
            with(metadata) {
                val minValue = getMinValue(context)
                val maxValue = getMaxValue(context)
                if (maxValue > minValue) {
                    min = minValue
                    max = maxValue
                    sliderIncrement = getIncrementStep(context)
                    sliderValue = delegate.sizePreview.value.currentIndex
                    extraChangeListener =
                        Slider.OnChangeListener { _, value, _ ->
                            delegate.onValueChange(index = value.toInt())
                        }
                    extraTouchListener =
                        object : Slider.OnSliderTouchListener {
                            override fun onStartTrackingTouch(slider: Slider) {
                                delegate.onStartTrackingTouch()
                            }

                            override fun onStopTrackingTouch(slider: Slider) {
                                delegate.onStopTrackingTouch(slider.value.toInt())
                            }
                        }
                } else {
                    // Determine visibility based on range availability.
                    // If min equals max, we provide a valid fallback range to prevent
                    // component initialization errors while the slider is hidden.
                    min = 0
                    max = 1
                    sliderIncrement = 1
                    isVisible = false
                }
            }
        }
    }

    override fun onItemSelected(activity: FragmentActivity) {}

    companion object {
        const val KEY = "display_size"

        /** Creates a new instance of [DisplaySizeSliderItemController]. */
        @JvmStatic
        fun create(context: Context, item: Item): DisplaySizeSliderItemController {
            val metadata =
                DisplaySizePreference(
                    context,
                    TextReadingPreferenceFragment.EntryPoint.SUW_VISION_SETTINGS,
                    true,
                )
            return DisplaySizeSliderItemController(context, item, metadata)
        }
    }
}
