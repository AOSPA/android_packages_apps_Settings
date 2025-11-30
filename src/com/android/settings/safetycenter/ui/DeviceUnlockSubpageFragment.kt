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

import android.annotation.SuppressLint
import android.app.settings.SettingsEnums
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.viewModels
import com.android.settings.R
import com.android.settings.core.SubSettingLauncher
import com.android.settings.dashboard.DashboardFragment
import com.android.settings.flags.Flags
import com.android.settings.safetycenter.ui.model.LiveSafetyCenterViewModel
import com.android.settings.safetycenter.ui.model.LiveSafetyCenterViewModelFactory
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.core.AbstractPreferenceController
import com.android.settingslib.search.SearchIndexable
import com.android.settingslib.search.SearchIndexableRaw

/** Fragment for displaying device unlock subpage within the Safety Center in Settings. */
// Suppressing MissingPermission lint: The Settings app holds the MANAGE_SAFETY_CENTER permission,
// which is required by the SafetyCenterManager APIs used by the ViewModel.
@SuppressLint("MissingPermission")
@SearchIndexable
class DeviceUnlockSubpageFragment : DashboardFragment() {

    private val viewModel: LiveSafetyCenterViewModel by viewModels {
        LiveSafetyCenterViewModelFactory(requireActivity().application)
    }
    private var safetySourceIds: List<String> = emptyList()

    override fun getPreferenceScreenResId(): Int {
        return R.xml.safety_center_device_unlock_subpage
    }

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        val entries =
            viewModel
                .getCurrentSafetyCenterDataAsUiData()
                .getDynamicEntriesForSources(safetySourceIds)
        if (entries.isEmpty()) {
            Log.d(TAG, "Redirecting from an empty subpage to Safety Center home")
            SubSettingLauncher(requireContext())
                .setDestination(SafetyCenterFragment::class.java.getName())
                .setSourceMetricsCategory(METRICS_CATEGORY_UNKNOWN)
                .launch()
        }
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
        listOf(SafetyIssuesPreferenceController(context, DEVICE_UNLOCK_ISSUES_KEY))

    private fun setupSafetyIssuesPreferenceController(
        safetyIssuesPreferenceController: SafetyIssuesPreferenceController
    ) {
        Log.d(TAG, "Setting Up the safety issues preference controller")
        safetySourceIds =
            SafetyCenterSubpageRegistry.getAllSafetySourceIds(
                requireContext(),
                SafetyCenterSubpageRegistry.DEVICE_UNLOCK_SUBPAGE_KEY,
            )
        safetyIssuesPreferenceController.apply {
            viewModel = this@DeviceUnlockSubpageFragment.viewModel
            fragmentManager = childFragmentManager
            activityTaskId = requireActivity().taskId
            isSubpage = true
            relatedSafetySources = safetySourceIds
            illustrationPreferenceKey =
                SafetyCenterSubpageRegistry.getIllustrationPrefKey(
                    SafetyCenterSubpageRegistry.DEVICE_UNLOCK_SUBPAGE_KEY
                )
            illustrationResId =
                SafetyCenterSubpageRegistry.getIllustrationResId(
                    SafetyCenterSubpageRegistry.DEVICE_UNLOCK_SUBPAGE_KEY
                )
        }
    }

    private fun setupSafetySourcePreferenceController(
        safetySourcePreferenceController: SafetySourcePreferenceController
    ) {
        val preferenceKey = safetySourcePreferenceController.preferenceKey
        Log.d(TAG, "Setting up the safety source preference controller for [$preferenceKey]")
        safetySourcePreferenceController.apply {
            viewModel = this@DeviceUnlockSubpageFragment.viewModel
            activityTaskId = requireActivity().taskId
        }
    }

    override fun getLogTag(): String {
        return TAG
    }

    override fun getMetricsCategory(): Int {
        return SettingsEnums.SAFETY_CENTER
    }

    companion object {
        private const val TAG = "DeviceUnlockSubpageFrag"
        private const val DEVICE_UNLOCK_ISSUES_KEY = "device_unlock_issues_banner_group"

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER: BaseSearchIndexProvider =
            object : BaseSearchIndexProvider(R.xml.safety_center_device_unlock_subpage) {
                override fun isPageSearchEnabled(context: Context?): Boolean {
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
                            SafetyCenterSubpageRegistry.DEVICE_UNLOCK_SUBPAGE_KEY,
                        )
                    )
                    return rawData
                }
            }
    }
}
