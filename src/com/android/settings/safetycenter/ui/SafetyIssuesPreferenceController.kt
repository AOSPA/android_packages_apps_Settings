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
import android.content.Context
import android.safetycenter.SafetyCenterIssue
import android.util.Log
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settings.safetycenter.SafetyCenterSeverityConverter
import com.android.settings.safetycenter.ui.model.LiveSafetyCenterViewModel
import com.android.settingslib.safetycenter.SafetyCenterUiData
import com.android.settingslib.widget.BannerMessagePreference
import com.android.settingslib.widget.BannerMessagePreferenceGroup
import com.android.settingslib.widget.IllustrationPreference

/**
 * Controller for managing the display of [SafetyCenterIssue] items within a
 * [BannerMessagePreferenceGroup].
 *
 * This controller can be used on both the main Safety Center page and on subpages. On subpages, it
 * filters issues based on [relatedSafetySources] and can display dismissed issues in a separate
 * subsection. It also manages the visibility of an optional [IllustrationPreference].
 */
// Suppressing MissingPermission lint: The Settings app holds the MANAGE_SAFETY_CENTER permission,
// which is required by the SafetyCenterManager APIs used by the ViewModel.
@SuppressLint("MissingPermission")
class SafetyIssuesPreferenceController(context: Context, preferenceKey: String) :
    BasePreferenceController(context, preferenceKey) {

    private var bannerGroup: BannerMessagePreferenceGroup? = null
    private var viewModel: LiveSafetyCenterViewModel? = null
    private var fragmentManager: FragmentManager? = null

    // Configuration for subpage behavior
    private var relatedSafetySources: List<String> = emptyList()
    private var isSubpage = false
    private var illustrationPreference: IllustrationPreference? = null

    /**
     * Sets the [LiveSafetyCenterViewModel] and [LifecycleOwner] for this controller. Observes
     * [SafetyCenterUiData] changes to update the UI.
     *
     * @param viewModel The ViewModel providing Safety Center data.
     * @param owner The LifecycleOwner scoping the LiveData observation.
     */
    fun setViewModelAndLifecycle(viewModel: LiveSafetyCenterViewModel, owner: LifecycleOwner) {
        this.viewModel = viewModel
        viewModel.safetyCenterUiLiveData.observe(owner) { data ->
            if (data == null) {
                Log.d(TAG, "[$preferenceKey] LiveData received null, hiding bannerGroup")
                bannerGroup?.isVisible = false
                if (isSubpage) illustrationPreference?.isVisible = true
                return@observe
            }
            Log.d(TAG, "[$preferenceKey] safetyCenterUiLiveData observer notified")
            bannerGroup?.let { group ->
                if (isSubpage) {
                    updateIssuesInSubpage(group, data)
                } else {
                    updateIssuesInMainPage(group, data)
                }
            }
        }
    }

    /**
     * Sets the [FragmentManager] to be used for showing DialogFragments. This should typically be
     * the childFragmentManager of the hosting fragment.
     *
     * @param fragmentManager The FragmentManager instance.
     */
    fun setFragmentManager(fragmentManager: FragmentManager) {
        this.fragmentManager = fragmentManager
    }

    /**
     * Configures the controller for use on a subpage.
     *
     * @param sources A list of safety source IDs to filter issues by.
     * @param illustrationPref The [IllustrationPreference] to hide when issues are present.
     */
    fun setSubpageSafetySourcesAndIllustration(
        sources: List<String>,
        illustrationPref: IllustrationPreference,
    ) {
        this.isSubpage = true
        this.relatedSafetySources = sources
        this.illustrationPreference = illustrationPref
        Log.d(TAG, "[$preferenceKey] Configured for subpage with sources: $sources")
    }

    override fun getAvailabilityStatus(): Int {
        return AVAILABLE
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        bannerGroup = screen.findPreference(preferenceKey)
        bannerGroup?.let {
            it.isVisible = false
            // Set titles for the expand/collapse buttons
            it.title =
                mContext.getString(R.string.safety_center_issues_banner_group_expandable_title)
            it.setCollapseTitle(
                mContext.getString(R.string.safety_center_issues_banner_group_collapsible_title)
            )
        }
        if (isSubpage) {
            illustrationPreference?.isVisible = true
        }
    }

    /** Updates the [BannerMessagePreferenceGroup] with all active issues for the main page. */
    private fun updateIssuesInMainPage(
        bannerGroup: BannerMessagePreferenceGroup,
        data: SafetyCenterUiData,
    ) {
        bannerGroup.removeAll()
        val activeIssues = data.getActiveIssues()

        if (activeIssues.isEmpty()) {
            Log.d(TAG, "[$preferenceKey] No active issues for main page, hiding bannerGroup")
            bannerGroup.isVisible = false
            return
        }
        Log.d(TAG, "[$preferenceKey] Updating main page with ${activeIssues.size} active issues")
        updateBannerGroup(bannerGroup, activeIssues, dismissedIssues = emptyList())
    }

    /**
     * Updates the [BannerMessagePreferenceGroup] with filtered active and dismissed issues for a
     * subpage. Hides the illustration if any active issues are present.
     */
    private fun updateIssuesInSubpage(
        bannerGroup: BannerMessagePreferenceGroup,
        data: SafetyCenterUiData,
    ) {
        bannerGroup.removeAll()
        val activeIssues = data.getActiveIssuesForSources(relatedSafetySources)
        val dismissedIssues = data.getDismissedIssuesForSources(relatedSafetySources)
        illustrationPreference?.isVisible = activeIssues.isEmpty()

        if (activeIssues.isEmpty() && dismissedIssues.isEmpty()) {
            Log.d(TAG, "[$preferenceKey] No issues for subpage, hiding bannerGroup")
            bannerGroup.isVisible = false
            return
        }

        Log.d(
            TAG,
            "[$preferenceKey] Updating subpage with ${activeIssues.size} active, ${dismissedIssues.size} dismissed issues.",
        )
        updateBannerGroup(bannerGroup, activeIssues, dismissedIssues)
    }

    /** Populates the [BannerMessagePreferenceGroup] with the given active and dismissed issues. */
    private fun updateBannerGroup(
        bannerGroup: BannerMessagePreferenceGroup,
        activeIssues: List<SafetyCenterIssue>,
        dismissedIssues: List<SafetyCenterIssue>,
    ) {
        activeIssues.forEach { issue ->
            bannerGroup.addPreference(createBannerForIssue(issue, isDismissed = false))
        }

        if (dismissedIssues.isNotEmpty()) {
            bannerGroup.addSubsection(
                mContext.getString(R.string.safety_center_dismissed_issues_subsection_title)
            )
            dismissedIssues.forEach { issue ->
                bannerGroup.addSubsectionPreference(createBannerForIssue(issue, isDismissed = true))
            }
        }
        bannerGroup.isVisible = true
    }

    /**
     * Creates and configures a [BannerMessagePreference] for a given [SafetyCenterIssue].
     *
     * @param issue The issue to display.
     * @param isDismissed Whether the issue is in the dismissed list.
     */
    private fun createBannerForIssue(
        issue: SafetyCenterIssue,
        isDismissed: Boolean,
    ): BannerMessagePreference {
        return BannerMessagePreference(mContext).apply {
            key = issue.id
            title = issue.title
            summary = issue.summary
            setHeader(issue.attributionTitle)
            setSubtitle(issue.subtitle)
            setAttentionLevel(
                SafetyCenterSeverityConverter.toBannerAttentionLevel(issue.severityLevel)
            )

            configureActionButtons(this, issue)
            configureDismissButton(this, issue, isDismissed)
        }
    }

    /** Configures the action buttons for the banner based on the issue's actions. */
    private fun configureActionButtons(banner: BannerMessagePreference, issue: SafetyCenterIssue) {
        if (issue.actions.isNotEmpty()) {
            val primaryAction = issue.actions[0]
            banner.setPositiveButtonText(primaryAction.label)
            banner.setPositiveButtonVisible(true)
            // TODO: b/424134511 - Implement click listener for the action button
        } else {
            banner.setPositiveButtonVisible(false)
        }
    }

    /** Configures the dismiss button visibility and click listener. */
    private fun configureDismissButton(
        banner: BannerMessagePreference,
        issue: SafetyCenterIssue,
        isDismissed: Boolean,
    ) {
        if (issue.isDismissible && !isDismissed) {
            banner.setDismissButtonVisible(true)
            banner.setDismissButtonOnClickListener {
                if (issue.shouldConfirmDismissal()) {
                    showDismissConfirmationDialog(issue)
                } else {
                    viewModel?.dismissIssue(issue)
                }
            }
        } else {
            banner.setDismissButtonVisible(false)
        }
    }

    /** Shows a confirmation dialog before dismissing an issue. */
    private fun showDismissConfirmationDialog(issue: SafetyCenterIssue) {
        Log.d(TAG, "[$preferenceKey] Showing dismiss confirmation for issue: ${issue.id}")
        ConfirmDismissalDialogFragment.newInstance(issue)
            .showNow(fragmentManager!!, /* tag= */ null)
    }

    companion object {
        private const val TAG = "SafetyIssuesPrefCtrl"
    }
}
