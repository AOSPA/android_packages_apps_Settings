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
import com.android.settings.dashboard.DashboardFragment
import com.android.settings.flags.Flags
import com.android.settings.safetycenter.ui.model.LiveSafetyCenterViewModel
import com.android.settings.safetycenter.ui.model.LiveSafetyCenterViewModelFactory
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.core.AbstractPreferenceController
import com.android.settingslib.safetycenter.SafetySourcePreference
import com.android.settingslib.search.SearchIndexable
import com.android.settingslib.widget.IllustrationPreference

/** Fragment for displaying device unlock subpage within the Safety Center in Settings. */
@SearchIndexable
class DeviceUnlockSubPageFragment : DashboardFragment() {

    private var safetyIssuesPreferenceController: SafetyIssuesPreferenceController? = null
    private val viewModel: LiveSafetyCenterViewModel by viewModels {
        LiveSafetyCenterViewModelFactory(requireActivity().application)
    }

    override fun getPreferenceScreenResId(): Int {
        return R.xml.safety_center_device_unlock_subpage
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
            SafetyIssuesPreferenceController(context, DEVICE_UNLOCK_ISSUES_KEY)
        controllers.add(safetyIssuesPreferenceController!!)
        return controllers
    }

    private fun setupIllustration() {
        val illustrationPreference: IllustrationPreference =
            findPreference(DEVICE_UNLOCK_ILLUSTRATION_KEY)!!
        illustrationPreference.imageDrawable =
            context?.getDrawable(R.drawable.illustration_expressive_android_lock_screen_sources)
    }

    private fun setupSafetyIssuesPreferenceController(owner: LifecycleOwner) {
        Log.d(TAG, "Setting Up the safety issues preference controller")
        safetyIssuesPreferenceController?.setViewModelAndLifecycle(viewModel, owner)

        val illustrationPreference: IllustrationPreference =
            findPreference(DEVICE_UNLOCK_ILLUSTRATION_KEY)!!
        val safetySources = collectSafetySourceIds()
        safetyIssuesPreferenceController?.setSubpageSafetySourcesAndIllustration(
            safetySources,
            illustrationPreference,
        )
    }

    private fun collectSafetySourceIds(): List<String> {
        val safetySourceIds = mutableListOf<String>()
        for (i in 0 until preferenceScreen.preferenceCount) {
            val pref = preferenceScreen.getPreference(i)
            if (pref is SafetySourcePreference) {
                pref.safetySource?.let { safetySourceIds.add(it) }
            }
        }
        return safetySourceIds.distinct()
    }

    private fun setupSafetySourcePreferenceControllers(owner: LifecycleOwner) {
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
        private const val TAG = "DeviceUnlockSubPageFrag"
        private const val DEVICE_UNLOCK_ILLUSTRATION_KEY = "device_unlock_illustration"
        private const val DEVICE_UNLOCK_ISSUES_KEY = "device_unlock_issues_banner_group"

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER: BaseSearchIndexProvider =
            object : BaseSearchIndexProvider(R.xml.safety_center_device_unlock_subpage) {
                override fun isPageSearchEnabled(context: Context?): Boolean {
                    return Flags.enableSafetyCenterNewUi()
                }
            }
    }
}
