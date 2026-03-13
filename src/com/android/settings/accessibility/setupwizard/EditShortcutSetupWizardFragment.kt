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

import android.content.ComponentName
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_COMPONENT_NAME
import com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME
import com.android.settings.R
import com.android.settings.accessibility.AccessibilitySetupWizardUtils
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupdesign.GlifLayout
import com.google.android.setupdesign.items.RecyclerItemAdapter
import kotlinx.coroutines.flow.MutableStateFlow

/** Edit Shortcut for Setup Wizard. */
class EditShortcutSetupWizardFragment : BaseSetupWizardFragment() {

    private lateinit var screenTitle: CharSequence
    private lateinit var shortcutTargets: Set<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screenTitle = arguments?.getCharSequence(ARG_SCREEN_TITLE) ?: ""
        shortcutTargets = arguments?.getStringArray(ARG_KEY_SHORTCUT_TARGETS)?.toSet() ?: emptySet()
    }

    override fun createControllers(adapter: RecyclerItemAdapter): Map<Int, BaseItemController> =
        buildMap {
            val context = requireContext()
            findItem(adapter, R.id.edit_keyboard_shortcut_in_suw)?.let {
                put(
                    R.id.edit_keyboard_shortcut_in_suw,
                    EditKeyboardShortcutController.create(context, it, shortcutTargets),
                )
            }
            findItem(adapter, R.id.edit_quick_settings_shortcut_in_suw)?.let {
                put(
                    R.id.edit_quick_settings_shortcut_in_suw,
                    EditQuickSettingsShortcutController.create(context, it, shortcutTargets),
                )
            }
            findItem(adapter, R.id.edit_floating_button_shortcut_in_suw)?.let {
                put(
                    R.id.edit_floating_button_shortcut_in_suw,
                    EditFloatingButtonShortcutController.create(context, it, shortcutTargets),
                )
            }
            findItem(adapter, R.id.edit_nav_button_shortcut_in_suw)?.let {
                put(
                    R.id.edit_nav_button_shortcut_in_suw,
                    EditNavButtonShortcutController.create(context, it, shortcutTargets),
                )
            }
            findItem(adapter, R.id.edit_volume_keys_shortcut_in_suw)?.let {
                put(
                    R.id.edit_volume_keys_shortcut_in_suw,
                    EditVolumeKeysShortcutController.create(context, it, shortcutTargets),
                )
            }
            findItem(adapter, R.id.edit_top_row_key_shortcut_in_suw)?.let {
                put(
                    R.id.edit_top_row_key_shortcut_in_suw,
                    EditTopRowKeyShortcutController.create(context, it, shortcutTargets),
                )
            }
            val expandableStateFlow = MutableStateFlow(false)
            findItem(adapter, R.id.edit_advanced_shortcut_in_suw)?.let {
                put(
                    R.id.edit_advanced_shortcut_in_suw,
                    EditAdvancedItemController.create(context, it, shortcutTargets) {
                        expandableStateFlow
                    },
                )
            }
            findItem(adapter, R.id.edit_triple_tap_shortcut_in_suw)?.let {
                put(
                    R.id.edit_triple_tap_shortcut_in_suw,
                    EditTripleTapShortcutController.create(context, it, shortcutTargets) {
                        expandableStateFlow
                    },
                )
            }
        }

    override val fragmentLayoutResId: Int = R.layout.edit_shortcut_suw_screen

    override val glifLayoutId: Int = R.id.edit_shortcut_suw_screen_layout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        val glifLayout = view as? GlifLayout ?: return view

        // Set Header Text
        glifLayout.headerText = screenTitle
        // Set Primary Button
        val mixin = glifLayout.getMixin(FooterBarMixin::class.java)
        AccessibilitySetupWizardUtils.setPrimaryButton(context, mixin, R.string.done) {
            parentFragmentManager.popBackStack()
        }

        return view
    }

    companion object {
        const val ARG_SCREEN_TITLE = "arg_screen_title"
        const val ARG_METRICS_CATEGORY = "arg_metrics_category"
        const val ARG_KEY_SHORTCUT_TARGETS = "targets"

        fun newInstance(
            metricsCategory: Int,
            screenTitle: CharSequence,
            target: ComponentName,
        ): EditShortcutSetupWizardFragment {
            val targetString =
                if (MAGNIFICATION_COMPONENT_NAME == target) {
                    MAGNIFICATION_CONTROLLER_NAME
                } else {
                    target.flattenToString()
                }

            val args =
                Bundle().apply {
                    putInt(ARG_METRICS_CATEGORY, metricsCategory)
                    putCharSequence(ARG_SCREEN_TITLE, screenTitle)
                    putStringArray(ARG_KEY_SHORTCUT_TARGETS, arrayOf(targetString))
                }

            return EditShortcutSetupWizardFragment().apply { arguments = args }
        }

        fun show(
            fragmentManager: FragmentManager,
            containerId: Int,
            metricsCategory: Int,
            screenTitle: CharSequence,
            target: ComponentName,
        ) {
            val fragment = newInstance(metricsCategory, screenTitle, target)
            fragmentManager
                .beginTransaction()
                .replace(containerId, fragment)
                .addToBackStack(null)
                .commit()
        }
    }
}
