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
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupdesign.GlifLayout
import com.google.android.setupdesign.items.RecyclerItemAdapter

/** Magnification for Setup Wizard. */
class MagnificationSetupWizardFragment : BaseSetupWizardFragment() {

    override fun createControllers(adapter: RecyclerItemAdapter): Map<Int, BaseItemController> =
        buildMap {
            val context = requireContext()
            findItem(adapter, R.id.magnification_illustration_in_suw)?.let {
                put(
                    R.id.magnification_illustration_in_suw,
                    MagnificationIllustrationItemController(context, it),
                )
            }
            findItem(adapter, R.id.magnification_shortcut_in_suw)?.let {
                put(
                    R.id.magnification_shortcut_in_suw,
                    MagnificationShortcutTwoTargetItemController.create(context, it),
                )
            }
            findItem(adapter, R.id.magnify_keyboard_in_suw)?.let {
                put(R.id.magnify_keyboard_in_suw, MagnifyKeyboardSwitchItemController(context, it))
            }
            findItem(adapter, R.id.magnification_footer_in_suw)?.let {
                put(
                    R.id.magnification_footer_in_suw,
                    MagnificationFooterItemController(context, it),
                )
            }
        }

    override val fragmentLayoutResId: Int = R.layout.magnification_suw_screen

    override val glifLayoutId: Int = R.id.magnification_suw_screen_layout

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
        }
}
