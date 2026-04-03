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
import com.android.settings.accessibility.textreading.data.FontSizeDataStore
import com.android.settings.accessibility.textreading.ui.FontSizeDelegate
import com.android.settings.accessibility.textreading.ui.FontSizePreference
import com.android.settingslib.R as SettingsLibR
import com.google.android.material.slider.Slider
import com.google.android.setupdesign.items.Item

/** Controller for the font size slider item in the Accessibility Setup Wizard. */
internal class FontSizeSliderItemController(
    private val context: Context,
    item: Item,
    private val metadata: FontSizePreference,
) : BaseItemController(item) {

    private val fontSizeDataStore = metadata.storage(context) as FontSizeDataStore
    private val fontSizes by lazy { fontSizeDataStore.fontSizeData.value.values }
    private val fontSizesLabel by lazy {
        fontSizes
            .map { value ->
                context.getString(SettingsLibR.string.font_scale_percentage, (value * 100).toInt())
            }
            .toTypedArray()
    }
    private val delegate by lazy {
        FontSizeDelegate(fontSizeDataStore = fontSizeDataStore, dataStoreKey = KEY)
    }

    override fun bindData(item: Item) {
        (item as? SliderItem)?.apply {
            with(metadata) {
                min = getMinValue(context)
                max = getMaxValue(context)
                sliderIncrement = getIncrementStep(context)
                sliderValue = delegate.sizePreview.value.currentIndex
                sliderStateDescriptionProvider =
                    SliderItem.SliderStateDescriptionProvider { index ->
                        if (index in fontSizesLabel.indices) {
                            fontSizesLabel[index]
                        } else {
                            null
                        }
                    }
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
            }
        }
    }

    override fun onItemSelected(activity: FragmentActivity) {}

    companion object {
        const val KEY = "font_size"

        /** Creates a new instance of [FontSizeSliderItemController]. */
        @JvmStatic
        fun create(context: Context, item: Item): FontSizeSliderItemController {
            val metadata =
                FontSizePreference(
                    context,
                    TextReadingPreferenceFragment.EntryPoint.SUW_VISION_SETTINGS,
                    true,
                )
            return FontSizeSliderItemController(context, item, metadata)
        }
    }
}
