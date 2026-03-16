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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.android.settings.R
import com.android.settings.accessibility.AccessibilitySetupWizardUtils
import com.android.settings.accessibility.textreading.dialogs.TextReadingResetDialog
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupdesign.GlifLayout
import com.google.android.setupdesign.items.RecyclerItemAdapter

/** TextReading for Setup Wizard. */
class TextReadingSetupWizardFragment : BaseSetupWizardFragment() {

    override fun createControllers(adapter: RecyclerItemAdapter): Map<Int, BaseItemController> =
        buildMap {
            val context = requireContext()
            findItem(adapter, R.id.font_size_in_suw)?.let {
                put(R.id.font_size_in_suw, FontSizeSliderItemController.create(context, it))
            }
            findItem(adapter, R.id.display_size_in_suw)?.let {
                put(R.id.display_size_in_suw, DisplaySizeSliderItemController.create(context, it))
            }
            findItem(adapter, R.id.bold_text_in_suw)?.let {
                put(R.id.bold_text_in_suw, BoldTextSwitchItemController(context, it))
            }
            findItem(adapter, R.id.outline_text_in_suw)?.let {
                put(R.id.outline_text_in_suw, OutlineTextSwitchItemController(context, it))
            }
        }

    override val fragmentLayoutResId: Int = R.layout.text_reading_suw_screen

    override val glifLayoutId: Int = R.id.text_reading_suw_screen_layout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? =
        super.onCreateView(inflater, container, savedInstanceState).also { view ->
            val glifLayout = view as? GlifLayout ?: return@also
            val mixin = glifLayout.getMixin(FooterBarMixin::class.java)

            // Set Primary Button
            AccessibilitySetupWizardUtils.setPrimaryButton(requireContext(), mixin, R.string.done) {
                parentFragmentManager.popBackStack()
            }

            // Set Secondary Button (Reset)
            AccessibilitySetupWizardUtils.setSecondaryButton(
                requireContext(),
                mixin,
                R.string.accessibility_text_reading_reset_button_title,
            ) {
                TextReadingResetDialog.showDialog(childFragmentManager)
            }
        }
}
