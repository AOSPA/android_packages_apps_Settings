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

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.android.settings.R
import com.android.settings.accessibility.AccessibilitySetupWizardUtils
import com.android.settings.accessibility.data.AccessibilityRepositoryProvider
import com.android.settings.accessibility.extensions.getFeatureName
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupdesign.GlifLayout
import com.google.android.setupdesign.items.RecyclerItemAdapter

/** Accessibility service for Setup Wizard. */
class AccessibilityServiceSetupWizardFragment : BaseSetupWizardFragment() {

    private lateinit var serviceInfo: AccessibilityServiceInfo
    private lateinit var descriptionText: CharSequence

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val componentName = arguments?.getParcelable(ARG_COMPONENT_NAME, ComponentName::class.java)
        val info =
            componentName?.let {
                AccessibilityRepositoryProvider.get(requireContext())
                    .getAccessibilityServiceInfo(it)
            }

        if (info == null) {
            parentFragmentManager.popBackStack()
            return
        }

        serviceInfo = info
        descriptionText = arguments?.getString(ARG_DESCRIPTION) ?: ""
    }

    override fun createControllers(adapter: RecyclerItemAdapter): Map<Int, BaseItemController> =
        buildMap {
            val context = requireContext()
            findItem(adapter, R.id.accessibility_service_illustration_in_suw)?.let { item ->
                put(
                    R.id.accessibility_service_illustration_in_suw,
                    AccessibilityServiceIllustrationItemController(context, serviceInfo, item),
                )
            }
            findItem(adapter, R.id.accessibility_service_main_switch_in_suw)?.let { item ->
                put(
                    R.id.accessibility_service_main_switch_in_suw,
                    AccessibilityServiceMainSwitchItemController(context, serviceInfo, item),
                )
            }
            findItem(adapter, R.id.accessibility_service_shortcut_in_suw)?.let {
                put(
                    R.id.accessibility_service_shortcut_in_suw,
                    AccessibilityServiceShortcutTwoTargetItemController.create(
                        context,
                        it,
                        serviceInfo,
                    ),
                )
            }
            findItem(adapter, R.id.accessibility_service_footer_in_suw)?.let { item ->
                put(
                    R.id.accessibility_service_footer_in_suw,
                    AccessibilityServiceFooterItemController(context, serviceInfo, item),
                )
            }
        }

    override val fragmentLayoutResId: Int = R.layout.accessibility_service_suw_screen

    override val glifLayoutId: Int = R.id.accessibility_service_suw_screen_layout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        val glifLayout = view as? GlifLayout ?: return view

        val context = requireContext()
        // Set Header Text
        glifLayout.headerText = serviceInfo.getFeatureName(context)
        // Set Description Text
        glifLayout.descriptionText = descriptionText
        // Set Primary Button
        val mixin = glifLayout.getMixin(FooterBarMixin::class.java)
        AccessibilitySetupWizardUtils.setPrimaryButton(context, mixin, R.string.done) {
            parentFragmentManager.popBackStack()
        }

        return view
    }

    companion object {
        const val ARG_COMPONENT_NAME = "arg_component_name"
        const val ARG_DESCRIPTION = "arg_description"

        /**
         * Use this factory method to create a new instance of this fragment using the provided
         * parameters.
         */
        fun newInstance(
            componentName: ComponentName,
            description: String,
        ): AccessibilityServiceSetupWizardFragment =
            AccessibilityServiceSetupWizardFragment().apply {
                arguments =
                    Bundle().apply {
                        putParcelable(ARG_COMPONENT_NAME, componentName)
                        putString(ARG_DESCRIPTION, description)
                    }
            }

        fun show(
            fragmentManager: FragmentManager,
            containerId: Int,
            componentName: ComponentName,
            description: String,
        ) {
            val fragment = newInstance(componentName, description)
            fragmentManager
                .beginTransaction()
                .replace(containerId, fragment)
                .addToBackStack(null)
                .commit()
        }
    }
}
