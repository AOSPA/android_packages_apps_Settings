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
import com.android.settings.dashboard.DashboardFragment
import com.android.settings.flags.Flags
import com.android.settings.safetycenter.ui.SafetyCenterSessionUtils.EXTRA_SESSION_ID
import com.android.settings.safetycenter.ui.SafetyCenterSessionUtils.INVALID_SESSION_ID
import com.android.settings.safetycenter.ui.SafetyCenterSessionUtils.getOrGenerateSessionId
import com.android.settings.safetycenter.ui.model.LiveSafetyCenterViewModel
import com.android.settings.safetycenter.ui.model.LiveSafetyCenterViewModelFactory
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.core.AbstractPreferenceController
import com.android.settingslib.search.SearchIndexable
import com.android.settingslib.search.SearchIndexableRaw

/**
 * Fragment for the Safety Center UI.
 *
 * This fragment hosts the preferences for the Security & privacy settings page and is searchable
 * when the feature flag is enabled.
 */
// Suppressing MissingPermission lint: The Settings app holds the MANAGE_SAFETY_CENTER permission,
// which is required by the SafetyCenterManager APIs used by the ViewModel.
@SuppressLint("MissingPermission")
@SearchIndexable
class SafetyCenterFragment : DashboardFragment() {
    private val viewModel: LiveSafetyCenterViewModel by viewModels {
        LiveSafetyCenterViewModelFactory(requireActivity().application)
    }

    private var focusedIssueKey: FocusedIssueKey? = null

    private var sessionId = INVALID_SESSION_ID

    override fun onAttach(context: Context) {
        super.onAttach(context)
        retrieveSessionId()
        configureInteractionLogger()

        val allControllers: List<AbstractPreferenceController> = preferenceControllers.flatten()
        for (controller in allControllers) {
            when (controller) {
                is StatusBannerPreferenceController ->
                    setupStatusBannerPreferenceController(controller)
                is SafetyIssuesPreferenceController ->
                    setupSafetyIssuesPreferenceController(controller)
                is SubpagePreferenceController -> setupSubpagePreferenceController(controller)
                is SafetySourcePreferenceController ->
                    setupSafetySourcePreferenceController(controller)
            }
        }
    }

    private fun retrieveSessionId() {
        val intentSessionId =
            requireActivity().intent?.getLongExtra(EXTRA_SESSION_ID, INVALID_SESSION_ID)
        if (intentSessionId != null && intentSessionId != INVALID_SESSION_ID) {
            sessionId = intentSessionId
            Log.d(TAG, "Session ID retrieved from Intent: $sessionId")
        } else {
            sessionId = getOrGenerateSessionId(arguments ?: Bundle())
            Log.d(TAG, "Session ID not found in Intent, fallback to fragment arguments: $sessionId")
        }
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
        logSafetyCenterViewedEvent()
    }

    private fun configureInteractionLogger() {
        viewModel.interactionLogger.apply {
            sessionId = this@SafetyCenterFragment.sessionId
            viewType = ViewType.FULL
            navigationSource =
                NavigationSource.fromIntentOrArguments(requireActivity().intent, arguments)
        }
    }

    private fun logSafetyCenterViewedEvent() {
        // If Safety Center was opened due to an associated notification click (i.e. intent has an
        // associated issue), record that issue's metadata on the SAFETY_CENTER_VIEWED event
        val focusedIssue =
            focusedIssueKey?.let { key ->
                viewModel.getCurrentSafetyCenterDataAsUiData().getActiveIssues().find { issue ->
                    key.matchesSafetyCenterIssue(issue)
                }
            }

        if (focusedIssue == null) {
            viewModel.interactionLogger.record(Action.SAFETY_CENTER_VIEWED)
        } else {
            viewModel.interactionLogger.recordForIssue(
                Action.SAFETY_CENTER_VIEWED,
                focusedIssue,
                isDismissed = false,
            )
        }
    }

    override fun createPreferenceControllers(context: Context): List<AbstractPreferenceController> =
        listOf(SafetyIssuesPreferenceController(context, SAFETY_ISSUES_BANNER_KEY))

    private fun setupStatusBannerPreferenceController(
        statusBannerPreferenceController: StatusBannerPreferenceController
    ) {
        Log.d(TAG, "Setting up StatusBannerPreferenceController")
        statusBannerPreferenceController.viewModel = viewModel
    }

    private fun setupSafetyIssuesPreferenceController(
        safetyIssuesPreferenceController: SafetyIssuesPreferenceController
    ) {
        Log.d(TAG, "Setting Up the safety issues preference controller")
        focusedIssueKey =
            SafetyCenterIntentParser.getFocusedIssueKeyFromIntent(requireActivity().intent)
        safetyIssuesPreferenceController.apply {
            viewModel = this@SafetyCenterFragment.viewModel
            fragmentManager = childFragmentManager
            activityTaskId = requireActivity().taskId
            focusedIssueKey = this@SafetyCenterFragment.focusedIssueKey
        }
    }

    private fun setupSubpagePreferenceController(
        subpagePreferenceController: SubpagePreferenceController
    ) {
        val preferenceKey = subpagePreferenceController.preferenceKey
        Log.d(TAG, "Setting Up the sub-page preference controller for [$preferenceKey]")
        subpagePreferenceController.viewModel = viewModel
        subpagePreferenceController.sessionId = sessionId
    }

    private fun setupSafetySourcePreferenceController(
        safetySourcePreferenceController: SafetySourcePreferenceController
    ) {
        val preferenceKey = safetySourcePreferenceController.preferenceKey
        Log.d(TAG, "Setting up the safety source preference controller for [$preferenceKey]")
        safetySourcePreferenceController.apply {
            viewModel = this@SafetyCenterFragment.viewModel
            activityTaskId = requireActivity().taskId
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

    companion object {
        private const val TAG = "SafetyCenterFragment"
        private const val SAFETY_ISSUES_BANNER_KEY = "issues_banner_group"

        fun initializeSubpagePrefControllerForSearchIndex(
            context: Context,
            controller: SubpagePreferenceController,
        ) {
            val preferenceKey = controller.preferenceKey
            controller.relatedSafetySources =
                SafetyCenterSubpageRegistry.getXmlSafetySourceIds(context, preferenceKey)
        }

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER: BaseSearchIndexProvider =
            object : BaseSearchIndexProvider(R.xml.safety_center_main_page) {
                protected override fun isPageSearchEnabled(context: Context?): Boolean {
                    return Flags.enableSafetyCenterNewUi()
                }

                override fun getDynamicRawDataToIndex(
                    context: Context,
                    enabled: Boolean,
                ): List<SearchIndexableRaw> {
                    val rawData = super.getDynamicRawDataToIndex(context, enabled).toMutableList()

                    // Add dynamic data for SafetySourcePreferences on the main page
                    rawData.addAll(
                        SafetyCenterSearchIndexUtils.getDynamicRawDataForIndexingFromXml(
                            context,
                            R.xml.safety_center_main_page,
                            R.string.safety_center_title,
                        )
                    )
                    return rawData
                }

                override fun getPreferenceControllers(
                    context: Context?
                ): MutableList<AbstractPreferenceController?> {
                    val allControllers = super.getPreferenceControllers(context)

                    for (controller in allControllers) {
                        if (controller is SubpagePreferenceController) {
                            initializeSubpagePrefControllerForSearchIndex(context!!, controller)
                        }
                    }
                    return allControllers
                }
            }
    }
}
