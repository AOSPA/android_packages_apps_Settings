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
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
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
import com.android.settingslib.widget.IllustrationPreference

/** Fragment for displaying System and Updates subpage within the Safety Center in Settings. */
@SearchIndexable
class SystemAndUpdatesSubpageFragment : DashboardFragment() {

    private var safetyIssuesPreferenceController: SafetyIssuesPreferenceController? = null
    private val viewModel: LiveSafetyCenterViewModel by viewModels {
        LiveSafetyCenterViewModelFactory(requireActivity().application)
    }
    private var safetySourceIds: List<String> = emptyList()

    override fun getPreferenceScreenResId(): Int {
        return R.xml.safety_center_system_and_updates_subpage
    }

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        safetySourceIds =
            SafetyCenterSubpageRegistry.getAllSafetySourceIds(
                requireContext(),
                SafetyCenterSubpageRegistry.SYSTEM_AND_UPDATES_SUBPAGE_KEY,
            )
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupIllustration()
        setupSafetyIssuesPreferenceController(viewLifecycleOwner)
        setupSafetySourcePreferenceControllers(viewLifecycleOwner)
    }

    override fun createPreferenceControllers(context: Context): List<AbstractPreferenceController> {
        val controllers = mutableListOf<AbstractPreferenceController>()
        safetyIssuesPreferenceController =
            SafetyIssuesPreferenceController(context, SYSTEM_AND_UPDATES_ISSUES_KEY)
        controllers.add(safetyIssuesPreferenceController!!)
        return controllers
    }

    private fun setupIllustration() {
        Log.d(TAG, "Setting Up the illustration")
        val illustrationPreference: IllustrationPreference =
            findPreference(SYSTEM_AND_UPDATES_ILLUSTRATION_KEY)!!
        illustrationPreference.imageDrawable =
            context?.getDrawable(R.drawable.safety_center_system_and_updates_subpage_illustration)
    }

    private fun setupSafetyIssuesPreferenceController(owner: LifecycleOwner) {
        Log.d(TAG, "Setting Up the safety issues preference controller")
        safetyIssuesPreferenceController?.apply {
            setViewModelAndLifecycle(viewModel, owner)
            this.fragmentManager = childFragmentManager
            this.activityTaskId = requireActivity().taskId

            val illustrationPreference: IllustrationPreference =
                findPreference(SYSTEM_AND_UPDATES_ILLUSTRATION_KEY)!!
            setSubpageSafetySourcesAndIllustration(safetySourceIds, illustrationPreference)
        }
    }

    private fun setupSafetySourcePreferenceControllers(owner: LifecycleOwner) {
        Log.d(TAG, "Setting Up the safety source preference controllers")
        val allControllers: List<AbstractPreferenceController> = preferenceControllers.flatten()
        for (controller in allControllers) {
            if (controller is SafetySourcePreferenceController) {
                controller.setViewModelAndLifecycle(viewModel, owner)
                controller.setActivityTaskId(requireActivity().taskId)
            }
        }
    }

    override fun getLogTag(): String {
        return TAG
    }

    override fun getMetricsCategory(): Int {
        return SettingsEnums.SAFETY_CENTER
    }

    companion object {
        private const val TAG = "SystemUpdatesSubpage"
        private const val SYSTEM_AND_UPDATES_ILLUSTRATION_KEY = "system_and_updates_illustration"
        private const val SYSTEM_AND_UPDATES_ISSUES_KEY = "system_and_updates_issues_banner_group"

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER: BaseSearchIndexProvider =
            object : BaseSearchIndexProvider(R.xml.safety_center_system_and_updates_subpage) {
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
                            SafetyCenterSubpageRegistry.SYSTEM_AND_UPDATES_SUBPAGE_KEY,
                        )
                    )
                    return rawData
                }
            }
    }
}
