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

import android.app.settings.SettingsEnums
import android.content.Context
import android.util.Log
import androidx.fragment.app.viewModels
import com.android.settings.R
import com.android.settings.dashboard.DashboardFragment
import com.android.settings.flags.Flags
import com.android.settings.safetycenter.ui.model.LiveSafetyCenterViewModel
import com.android.settings.safetycenter.ui.model.LiveSafetyCenterViewModelFactory
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.core.AbstractPreferenceController
import com.android.settingslib.search.SearchIndexable
import com.android.settingslib.search.SearchIndexableRaw

/**
 * Fragment that displays various privacy controls. This fragment is a sub-page of the main Safety
 * Center UI. It hosts preferences for privacy hosts preferences for privacy-related settings`
 */
@SearchIndexable
class PrivacyControlsFragment : DashboardFragment() {

    private val TAG = "PrivacyControlsFragment"

    private val viewModel: LiveSafetyCenterViewModel by viewModels {
        LiveSafetyCenterViewModelFactory(requireActivity().application)
    }

    override fun getPreferenceScreenResId(): Int {
        return R.xml.safety_center_privacy_controls_settings
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        val allControllers: List<AbstractPreferenceController> = preferenceControllers.flatten()
        for (controller in allControllers) {
            when (controller) {
                is SafetyIssuesPreferenceController ->
                    setupSafetyIssuesPreferenceController(controller)
                is SafetySourcePreferenceController ->
                    setupSafetySourcePreferenceController(controller)
            }
        }
    }

    override fun createPreferenceControllers(context: Context): List<AbstractPreferenceController> =
        listOf(SafetyIssuesPreferenceController(context, PRIVACY_CONTROLS_ISSUES_KEY))

    private fun setupSafetyIssuesPreferenceController(
        safetyIssuesPreferenceController: SafetyIssuesPreferenceController
    ) {
        Log.d(TAG, "Setting Up the safety issues preference controller")
        safetyIssuesPreferenceController.apply {
            viewModel = this@PrivacyControlsFragment.viewModel
            fragmentManager = childFragmentManager
            activityTaskId = requireActivity().taskId
            isSubpage = true
            relatedSafetySources =
                SafetyCenterSubpageRegistry.getAllSafetySourceIds(
                    requireContext(),
                    SafetyCenterSubpageRegistry.PRIVACY_CONTROLS_SUBPAGE_KEY,
                )
        }
    }

    private fun setupSafetySourcePreferenceController(
        safetySourcePreferenceController: SafetySourcePreferenceController
    ) {
        val preferenceKey = safetySourcePreferenceController.preferenceKey
        Log.d(TAG, "Setting up the safety source preference controller for [$preferenceKey]")
        safetySourcePreferenceController.apply {
            viewModel = this@PrivacyControlsFragment.viewModel
            activityTaskId = requireActivity().taskId
        }
    }

    override fun getLogTag(): String = TAG

    override fun getMetricsCategory(): Int = SettingsEnums.SAFETY_CENTER

    companion object {

        private const val PRIVACY_CONTROLS_ISSUES_KEY = "privacy_controls_issues_banner_group"

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER =
            object : BaseSearchIndexProvider(R.xml.safety_center_privacy_controls_settings) {
                public override fun isPageSearchEnabled(context: Context): Boolean {
                    return Flags.enableSafetyCenterNewUi()
                }

                override fun getDynamicRawDataToIndex(
                    context: Context,
                    enabled: Boolean,
                ): List<SearchIndexableRaw> {
                    val rawData = super.getDynamicRawDataToIndex(context, enabled).toMutableList()
                    rawData.addAll(
                        SafetyCenterSearchIndexUtils.getDynamicRawDataForIndexingSubpage(
                            context,
                            SafetyCenterSubpageRegistry.PRIVACY_CONTROLS_SUBPAGE_KEY,
                        )
                    )
                    return rawData
                }
            }
    }
}
