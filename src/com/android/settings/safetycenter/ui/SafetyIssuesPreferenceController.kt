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
import androidx.annotation.DrawableRes
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settings.safetycenter.ui.model.LiveSafetyCenterViewModel
import com.android.settingslib.safetycenter.SafetyCenterUiData
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
    private var bannerGroupManager: SafetyIssuesBannerGroupManager? = null
    var viewModel: LiveSafetyCenterViewModel? = null
    var focusedIssueKey: FocusedIssueKey? = null
    var fragmentManager: FragmentManager? = null
    var activityTaskId: Int? = null

    // Configuration for subpage behavior
    var isSubpage = false
    var relatedSafetySources: List<String> = emptyList()
    var illustrationPreferenceKey: CharSequence? = null
    @DrawableRes var illustrationResId: Int? = null
    var illustrationPreference: IllustrationPreference? = null

    override fun onViewCreated(owner: LifecycleOwner) {
        if (viewModel == null) {
            Log.w(TAG, "[$preferenceKey] ViewModel not set, cannot observe LiveData")
            return
        }
        viewModel!!.safetyCenterUiLiveData.observe(owner) { data ->
            if (data == null) {
                Log.d(TAG, "[$preferenceKey] LiveData received null")
                return@observe
            }
            Log.d(TAG, "[$preferenceKey] safetyCenterUiLiveData observer notified")
            bannerGroup?.let { group -> updatePreferenceUi(group, data) }
        }
    }

    override fun getAvailabilityStatus(): Int {
        return AVAILABLE
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        if (illustrationPreferenceKey != null) {
            illustrationPreference = screen.findPreference(illustrationPreferenceKey!!)
            illustrationPreference?.imageDrawable = mContext.getDrawable(illustrationResId!!)
        }
        bannerGroup = screen.findPreference(preferenceKey)
        bannerGroup?.let {
            // Set titles for the expand/collapse buttons
            it.title =
                mContext.getString(R.string.safety_center_issues_banner_group_expandable_title)
            it.setCollapseTitle(
                mContext.getString(R.string.safety_center_issues_banner_group_collapsible_title)
            )

            val uiData = viewModel?.safetyCenterUiLiveData?.value
            if (uiData != null) {
                updatePreferenceUi(bannerGroup as BannerMessagePreferenceGroup, uiData)
            }
        }
    }

    override fun updateState(preference: Preference?) {
        super.updateState(preference)
        val uiData = viewModel?.safetyCenterUiLiveData?.value
        if (preference != null && uiData != null) {
            updatePreferenceUi(preference as BannerMessagePreferenceGroup, uiData)
        }
    }

    /**
     * Updates the UI of the [BannerMessagePreferenceGroup] with the provided [SafetyCenterUiData].
     *
     * @param group The [BannerMessagePreferenceGroup] to be updated.
     * @param data The [SafetyCenterUiData] containing the latest safety issues.
     */
    private fun updatePreferenceUi(group: BannerMessagePreferenceGroup, data: SafetyCenterUiData) {
        ensureBannerGroupManagerInitialized(group)
        val bannerGroupManager = this.bannerGroupManager
        if (bannerGroupManager == null) {
            Log.w(TAG, "Skipping banner update, SafetyIssuesBannerGroupManager not initialized.")
            return
        }

        val issueIdToHeaderResIdMap = viewModel!!.getIssueIdToHeaderResIdMap(data)

        if (isSubpage) {
            updateIssuesInSubpage(group, data, bannerGroupManager, issueIdToHeaderResIdMap)
        } else {
            updateIssuesInMainPage(group, data, bannerGroupManager, issueIdToHeaderResIdMap)
        }
    }

    /**
     * Creates an instance of [SafetyIssuesBannerGroupManager] if it hasn't been created yet and all
     * necessary dependencies are available.
     *
     * @param bannerGroup The [BannerMessagePreferenceGroup] to be managed.
     */
    private fun ensureBannerGroupManagerInitialized(bannerGroup: BannerMessagePreferenceGroup) {
        if (this.bannerGroupManager == null) {
            val viewModel = this.viewModel
            val fragmentManager = this.fragmentManager
            val activityTaskId = this.activityTaskId

            if (viewModel != null && fragmentManager != null && activityTaskId != null) {
                Log.d(TAG, "Creating new SafetyIssuesBannerGroupManager instance.")
                this.bannerGroupManager =
                    SafetyIssuesBannerGroupManager(
                        mContext,
                        bannerGroup,
                        viewModel,
                        fragmentManager,
                        activityTaskId,
                    )
            } else {
                Log.w(
                    TAG,
                    "Failed to create SafetyIssuesBannerGroupManager. Missing ViewModel, FragmentManager, or ActivityTaskId.",
                )
            }
        }
    }

    /** Updates the [BannerMessagePreferenceGroup] with all active issues for the main page. */
    private fun updateIssuesInMainPage(
        bannerGroup: BannerMessagePreferenceGroup,
        data: SafetyCenterUiData,
        bannerGroupManager: SafetyIssuesBannerGroupManager,
        issueIdToHeaderResIdMap: Map<String, Int>,
    ) {
        val activeIssues = data.getActiveIssues()
        Log.d(TAG, "[$preferenceKey] Updating main page with ${activeIssues.size} active issues")
        bannerGroupManager.updateBannerGroup(
            newActiveIssues = activeIssues,
            newDismissedIssues = emptyList(),
            resolvedIssues = data.resolvedIssues,
            focusedIssueKey = focusedIssueKey,
            issueIdToHeaderResIdMap = issueIdToHeaderResIdMap,
        )
        bannerGroup.isVisible = !activeIssues.isEmpty()
    }

    /**
     * Updates the [BannerMessagePreferenceGroup] with filtered active and dismissed issues for a
     * subpage. Hides the illustration if any active issues are present.
     */
    private fun updateIssuesInSubpage(
        bannerGroup: BannerMessagePreferenceGroup,
        data: SafetyCenterUiData,
        bannerGroupManager: SafetyIssuesBannerGroupManager,
        issueIdToHeaderResIdMap: Map<String, Int>,
    ) {
        val activeIssues = data.getActiveIssuesForSources(relatedSafetySources)
        val dismissedIssues = data.getDismissedIssuesForSources(relatedSafetySources)
        Log.d(
            TAG,
            "[$preferenceKey] Updating subpage with ${activeIssues.size} active, ${dismissedIssues.size} dismissed issues.",
        )
        bannerGroupManager.updateBannerGroup(
            newActiveIssues = activeIssues,
            newDismissedIssues = dismissedIssues,
            resolvedIssues = data.resolvedIssues,
            focusedIssueKey = focusedIssueKey,
            issueIdToHeaderResIdMap = issueIdToHeaderResIdMap,
        )

        illustrationPreference?.isVisible = activeIssues.isEmpty()
        bannerGroup.isVisible = !(activeIssues.isEmpty() && dismissedIssues.isEmpty())
    }

    companion object {
        private const val TAG = "SafetyIssuesPrefCtrl"
    }
}
