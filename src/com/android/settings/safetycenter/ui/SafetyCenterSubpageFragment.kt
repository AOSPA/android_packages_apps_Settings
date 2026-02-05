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
import com.android.settings.core.SubSettingLauncher
import com.android.settings.dashboard.DashboardFragment
import com.android.settings.safetycenter.ui.SafetyCenterSessionUtils.getOrGenerateSessionId
import com.android.settings.safetycenter.ui.model.LiveSafetyCenterViewModel
import com.android.settings.safetycenter.ui.model.LiveSafetyCenterViewModelFactory
import com.android.settingslib.core.AbstractPreferenceController

/**
 * A base fragment for all Safety Center subpages. This class contains the common logic for
 * initializing the ViewModel, setting up preference controllers, and handling redirection for empty
 * subpages.
 */
// Suppressing MissingPermission lint: The Settings app holds the MANAGE_SAFETY_CENTER permission,
// which is required by the SafetyCenterManager APIs used by the ViewModel.
@SuppressLint("MissingPermission")
abstract class SafetyCenterSubpageFragment : DashboardFragment() {

    private val viewModel: LiveSafetyCenterViewModel by viewModels {
        LiveSafetyCenterViewModelFactory(requireActivity().application)
    }
    private var safetySourceIds: List<String> = emptyList()

    /** The unique preference key for the subpage, used to look up configuration. */
    abstract val subpageKey: String

    override fun getPreferenceScreenResId(): Int {
        return SafetyCenterSubpageRegistry.getXmlResId(subpageKey)!!
    }

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        redirectIfEmpty()
    }

    /** Redirects to the Safety Center home page if the subpage is empty. */
    open fun redirectIfEmpty() {
        if (SafetyCenterSubpageRegistry.hasInjectedTiles(subpageKey)) {
            return
        }
        val uiData = viewModel.safetyCenterUiLiveData.value
        val entries = uiData?.getDynamicEntriesForSources(safetySourceIds)
        if (entries?.isEmpty() == true) {
            Log.d(logTag, "Redirecting from an empty subpage to Safety Center home")
            SubSettingLauncher(requireContext())
                .setDestination(SafetyCenterFragment::class.java.getName())
                .setSourceMetricsCategory(METRICS_CATEGORY_UNKNOWN)
                .launch()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        configureInteractionLogger()

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
        listOf(
            SafetyIssuesPreferenceController(
                context,
                SafetyCenterSubpageRegistry.getIssuesBannerGroupPrefKey(subpageKey)!!,
            )
        )

    override fun onResume() {
        super.onResume()
        viewModel.pageOpen(safetySourceIds)
        logSafetyCenterViewedEvent()
    }

    private fun configureInteractionLogger() {
        viewModel.interactionLogger.apply {
            sessionId = getOrGenerateSessionId(arguments ?: Bundle())
            viewType = ViewType.SUBPAGE
            navigationSource =
                NavigationSource.fromIntentOrArguments(requireActivity().intent, arguments)
            subpageId = this@SafetyCenterSubpageFragment.subpageKey
        }
    }

    private fun logSafetyCenterViewedEvent() {
        viewModel.interactionLogger.record(Action.SAFETY_CENTER_VIEWED)
    }

    private fun setupSafetyIssuesPreferenceController(
        safetyIssuesPreferenceController: SafetyIssuesPreferenceController
    ) {
        Log.d(logTag, "Setting Up the safety issues preference controller")
        safetySourceIds =
            SafetyCenterSubpageRegistry.getAllSafetySourceIds(requireContext(), subpageKey)
        safetyIssuesPreferenceController.apply {
            viewModel = this@SafetyCenterSubpageFragment.viewModel
            fragmentManager = childFragmentManager
            activityTaskId = requireActivity().taskId
            isSubpage = true
            relatedSafetySources = safetySourceIds
            illustrationPreferenceKey =
                SafetyCenterSubpageRegistry.getIllustrationPrefKey(subpageKey)
            illustrationResId = SafetyCenterSubpageRegistry.getIllustrationResId(subpageKey)
        }
    }

    private fun setupSafetySourcePreferenceController(
        safetySourcePreferenceController: SafetySourcePreferenceController
    ) {
        val preferenceKey = safetySourcePreferenceController.preferenceKey
        Log.d(logTag, "Setting up the safety source preference controller for [$preferenceKey]")
        safetySourcePreferenceController.apply {
            viewModel = this@SafetyCenterSubpageFragment.viewModel
            activityTaskId = requireActivity().taskId
        }
    }

    override fun getMetricsCategory(): Int {
        return SettingsEnums.SAFETY_CENTER
    }
}
