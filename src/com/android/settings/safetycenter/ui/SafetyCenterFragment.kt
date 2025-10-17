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
package com.android.settings.safetycenter.ui

import android.annotation.StringRes
import android.app.settings.SettingsEnums
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import com.android.settings.R
import com.android.settings.dashboard.DashboardFragment
import com.android.settings.flags.Flags
import com.android.settings.safetycenter.ui.model.LiveSafetyCenterViewModel
import com.android.settings.safetycenter.ui.model.LiveSafetyCenterViewModelFactory
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.core.AbstractPreferenceController
import com.android.settingslib.search.SearchIndexable

/**
 * Fragment for the Safety Center UI.
 *
 * This fragment hosts the preferences for the Security & privacy settings page and is searchable
 * when the feature flag is enabled.
 */
@SearchIndexable
class SafetyCenterFragment : DashboardFragment() {

    private var safetyIssuesPreferenceController: SafetyIssuesPreferenceController? = null

    private val viewModel: LiveSafetyCenterViewModel by viewModels {
        LiveSafetyCenterViewModelFactory(requireActivity().application)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (requireActivity().isChangingConfigurations) {
            viewModel.changingConfigurations()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.pageOpen()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupStatusBannerController(viewLifecycleOwner)
        setupSafetyIssuesPreferenceController(viewLifecycleOwner)
        setupSubpagePreferenceControllers(viewLifecycleOwner)
    }

    override fun createPreferenceControllers(context: Context): List<AbstractPreferenceController> {
        val controllers = mutableListOf<AbstractPreferenceController>()
        safetyIssuesPreferenceController =
            SafetyIssuesPreferenceController(context, SAFETY_ISSUES_BANNER_KEY)
        controllers.add(safetyIssuesPreferenceController!!)
        return controllers
    }

    private fun setupStatusBannerController(owner: LifecycleOwner) {
        Log.d(TAG, "Setting up StatusBannerPreferenceController")

        val statusBannerController =
            preferenceControllers.flatten().firstOrNull { it is StatusBannerPreferenceController }
                as? StatusBannerPreferenceController

        statusBannerController?.setViewModelAndLifecycle(viewModel, owner)
    }

    private fun setupSafetyIssuesPreferenceController(owner: LifecycleOwner) {
        Log.d(TAG, "Setting Up the safety issues preference controller")
        safetyIssuesPreferenceController?.setViewModelAndLifecycle(viewModel, owner)
        safetyIssuesPreferenceController?.setFragmentManager(childFragmentManager)
        safetyIssuesPreferenceController?.setActivityTaskId(requireActivity().taskId)
    }

    private fun setupSubpagePreferenceControllers(owner: LifecycleOwner) {
        Log.d(TAG, "Setting Up the sub-page preference controllers")
        val allControllers: List<AbstractPreferenceController> = preferenceControllers.flatten()

        for (controller in allControllers) {
            if (controller is SubpagePreferenceController) {
                when (controller.preferenceKey) {
                    APP_SECURITY_SUBPAGE_KEY ->
                        initializeSubpagePreferenceController(
                            controller = controller,
                            subpageKey = SafetyCenterSubpageRegistry.SubpageKey.APP_SECURITY,
                            summaryResId = R.string.safety_center_app_security_summary,
                            lifecycleOwner = owner,
                        )
                    DEVICE_UNLOCK_SUBPAGE_KEY ->
                        initializeSubpagePreferenceController(
                            controller = controller,
                            subpageKey = SafetyCenterSubpageRegistry.SubpageKey.DEVICE_UNLOCK,
                            summaryResId = R.string.safety_center_device_unlock_summary,
                            lifecycleOwner = owner,
                        )
                    ACCOUNT_SECURITY_SUBPAGE_KEY ->
                        initializeSubpagePreferenceController(
                            controller = controller,
                            subpageKey = SafetyCenterSubpageRegistry.SubpageKey.ACCOUNT_SECURITY,
                            summaryResId = R.string.safety_center_account_security_summary,
                            lifecycleOwner = owner,
                        )
                    DEVICE_FINDERS_SUBPAGE_KEY ->
                        initializeSubpagePreferenceController(
                            controller = controller,
                            subpageKey = SafetyCenterSubpageRegistry.SubpageKey.DEVICE_FINDERS,
                            summaryResId = R.string.safety_center_device_finders_summary,
                            lifecycleOwner = owner,
                        )
                    SYSTEM_AND_UPDATES_SUBPAGE_KEY ->
                        initializeSubpagePreferenceController(
                            controller = controller,
                            subpageKey = SafetyCenterSubpageRegistry.SubpageKey.SYSTEM_AND_UPDATES,
                            summaryResId = R.string.safety_center_system_and_updates_summary,
                            lifecycleOwner = owner,
                        )
                    CELLULAR_NETWORK_SECURITY_SUBPAGE_KEY ->
                        initializeSubpagePreferenceController(
                            controller = controller,
                            subpageKey =
                                SafetyCenterSubpageRegistry.SubpageKey.CELLULAR_NETWORK_SECURITY,
                            summaryResId = R.string.safety_center_cellular_network_security_summary,
                            lifecycleOwner = owner,
                        )
                    PRIVACY_CONTROLS_SUBPAGE_KEY ->
                        initializeSubpagePreferenceController(
                            controller = controller,
                            subpageKey = SafetyCenterSubpageRegistry.SubpageKey.PRIVACY_CONTROLS,
                            summaryResId = R.string.privacy_sources_summary,
                            lifecycleOwner = owner,
                        )
                }
            }
        }
    }

    protected override fun getPreferenceScreenResId(): Int {
        return R.xml.safety_center_main_page
    }

    override fun getLogTag(): String {
        return TAG
    }

    override fun getMetricsCategory(): Int {
        return SettingsEnums.SAFETY_CENTER
    }

    private fun initializeSubpagePreferenceController(
        controller: SubpagePreferenceController,
        subpageKey: SafetyCenterSubpageRegistry.SubpageKey,
        @StringRes summaryResId: Int,
        lifecycleOwner: LifecycleOwner,
    ) {
        controller.setRelatedSafetySources(
            SafetyCenterSubpageRegistry.getXmlSafetySourceIds(requireContext(), subpageKey)
        )
        controller.setRelatedIssueOnlySafetySources(
            SafetyCenterSubpageRegistry.getIssueOnlySafetySourceIds(subpageKey)
        )
        controller.setDefaultSummaryResId(summaryResId)
        controller.setViewModelAndLifecycle(viewModel, lifecycleOwner)
    }

    companion object {
        private const val TAG = "SafetyCenterFragment"
        private const val SAFETY_ISSUES_BANNER_KEY = "issues_banner_group"
        private const val APP_SECURITY_SUBPAGE_KEY = "app_security_subpage"
        private const val DEVICE_UNLOCK_SUBPAGE_KEY = "device_unlock_subpage"
        private const val ACCOUNT_SECURITY_SUBPAGE_KEY = "account_security_subpage"
        private const val DEVICE_FINDERS_SUBPAGE_KEY = "device_finders_subpage"
        private const val SYSTEM_AND_UPDATES_SUBPAGE_KEY = "system_and_updates_subpage"
        private const val CELLULAR_NETWORK_SECURITY_SUBPAGE_KEY =
            "cellular_network_security_subpage"
        private const val PRIVACY_CONTROLS_SUBPAGE_KEY = "privacy_controls_page"

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER: BaseSearchIndexProvider =
            object : BaseSearchIndexProvider(R.xml.safety_center_main_page) {
                protected override fun isPageSearchEnabled(context: Context?): Boolean {
                    return Flags.enableSafetyCenterNewUi()
                }
            }
    }
}
