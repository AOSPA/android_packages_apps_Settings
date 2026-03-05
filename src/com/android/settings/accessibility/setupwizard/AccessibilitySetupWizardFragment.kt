/*
 * Copyright (C) 2025 The Android Open Source Project
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

import com.android.settings.R
import com.google.android.setupdesign.items.RecyclerItemAdapter

/** Accessibility Vision Settings for Setup Wizard. */
class AccessibilitySetupWizardFragment : BaseSetupWizardFragment() {

    override fun createControllers(adapter: RecyclerItemAdapter): Map<Int, BaseItemController> =
        buildMap {
            val context = requireContext()
            val screenReaderComp =
                context.getString(com.android.internal.R.string.config_defaultAccessibilityService)
            findItem(adapter, R.id.screen_reader_in_suw)?.let {
                put(
                    R.id.screen_reader_in_suw,
                    AccessibilityServiceItemController(context, it, screenReaderComp),
                )
            }
            val selectToSpeakComponent =
                context.getString(com.android.internal.R.string.config_defaultSelectToSpeakService)
            findItem(adapter, R.id.select_to_speak_in_suw)?.let {
                put(
                    R.id.select_to_speak_in_suw,
                    AccessibilityServiceItemController(context, it, selectToSpeakComponent),
                )
            }
            findItem(adapter, R.id.text_reading_options_in_suw)?.let {
                put(R.id.text_reading_options_in_suw, TextReadingItemController(it))
            }
            findItem(adapter, R.id.color_inversion_in_suw)?.let {
                put(R.id.color_inversion_in_suw, ColorInversionItemController(context, it))
            }
            findItem(adapter, R.id.screen_magnification_in_suw)?.let {
                put(R.id.screen_magnification_in_suw, MagnificationItemController(it))
            }
            findItem(adapter, R.id.auto_brightness_in_suw)?.let {
                put(R.id.auto_brightness_in_suw, AutoBrightnessSwitchItemController(context, it))
            }
            findItem(adapter, R.id.brightness_level_in_suw)?.let {
                put(R.id.brightness_level_in_suw, BrightnessLevelItemController(context, it))
            }
        }

    override val fragmentLayoutResId: Int = R.layout.accessibility_suw_screen

    override val glifLayoutId: Int = R.id.accessibility_suw_screen_layout
}
