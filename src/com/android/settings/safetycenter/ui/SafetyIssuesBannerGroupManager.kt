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

import android.content.Context
import android.safetycenter.SafetyCenterIssue
import android.util.Log
import androidx.fragment.app.FragmentManager
import com.android.settings.R
import com.android.settings.safetycenter.ui.model.LiveSafetyCenterViewModel
import com.android.settingslib.widget.BannerMessagePreference
import com.android.settingslib.widget.BannerMessagePreferenceGroup

/**
 * Manages the creation, addition, removal, and updating of [BannerMessagePreference]s within a
 * [BannerMessagePreferenceGroup] for active and dismissed [SafetyCenterIssue]s.
 *
 * @property context The [Context] used to create views and access resources.
 * @property bannerGroup The [BannerMessagePreferenceGroup] where issue banners will be displayed.
 * @property viewModel The [LiveSafetyCenterViewModel] used to execute issue actions and dismissals.
 * @property fragmentManager The [FragmentManager] used to show confirmation dialogs.
 * @property activityTaskId The task ID of the hosting activity, used when executing actions.
 */
class SafetyIssuesBannerGroupManager(
    private val context: Context,
    private val bannerGroup: BannerMessagePreferenceGroup,
    private val viewModel: LiveSafetyCenterViewModel,
    private val fragmentManager: FragmentManager,
    private val activityTaskId: Int,
) {
    private val currentActiveIssueBanners = mutableMapOf<String, SafetyIssueBannerPreference>()
    private val currentDismissedIssueBanners = mutableMapOf<String, SafetyIssueBannerPreference>()

    /**
     * Updates the [bannerGroup] with the latest lists of active and dismissed [SafetyCenterIssue]s.
     * Banners are created, updated, or removed to reflect the current state.
     *
     * @param newActiveIssues A list of currently active [SafetyCenterIssue]s.
     * @param newDismissedIssues A list of currently dismissed [SafetyCenterIssue]s.
     * @param resolvedIssues A map where keys are issue IDs and values are action IDs that have been
     *   successfully resolved.
     */
    fun updateBannerGroup(
        newActiveIssues: List<SafetyCenterIssue>,
        newDismissedIssues: List<SafetyCenterIssue>,
        resolvedIssues: Map<String, String>,
    ) {
        val newActiveIssueKeys = newActiveIssues.map { getActiveBannerKey(it.id) }.toSet()
        removeStaleBanners(currentActiveIssueBanners, newActiveIssueKeys)
        createOrUpdateBanners(
            newActiveIssues,
            currentActiveIssueBanners,
            isDismissed = false,
            resolvedIssues,
        )

        val newDismissedIssueKeys = newDismissedIssues.map { getDismissedBannerKey(it.id) }.toSet()
        removeStaleBanners(currentDismissedIssueBanners, newDismissedIssueKeys)

        if (newDismissedIssues.isNotEmpty()) {
            bannerGroup.addSubsection(
                context.getString(R.string.safety_center_dismissed_issues_subsection_title)
            )
            createOrUpdateBanners(
                newDismissedIssues,
                currentDismissedIssueBanners,
                isDismissed = true,
                resolvedIssues,
            )
        } else {
            bannerGroup.removeSubsection()
        }
    }

    /**
     * Removes banners from the [bannerGroup] that are no longer present in the [newIssueKeys].
     *
     * @param currentIssueBanners The mutable map of current banners to check.
     * @param newIssueKeys The set of keys for issues that should currently exist.
     */
    private fun removeStaleBanners(
        currentIssueBanners: MutableMap<String, SafetyIssueBannerPreference>,
        newIssueKeys: Set<String>,
    ) {
        val staleKeys = currentIssueBanners.keys.toSet() - newIssueKeys
        staleKeys.forEach { key ->
            currentIssueBanners.remove(key)?.let { banner -> bannerGroup.removePreference(banner) }
        }
    }

    /**
     * Creates new [BannerMessagePreference]s or updates existing ones for the given list of issues.
     *
     * @param newIssues The list of [SafetyCenterIssue]s to process.
     * @param currentIssueBanners The mutable map storing the current banners.
     * @param isDismissed True if these issues are dismissed, false otherwise.
     * @param resolvedIssues A map of successfully resolved issue IDs to action IDs.
     */
    private fun createOrUpdateBanners(
        newIssues: List<SafetyCenterIssue>,
        currentIssueBanners: MutableMap<String, SafetyIssueBannerPreference>,
        isDismissed: Boolean,
        resolvedIssues: Map<String, String>,
    ) {
        newIssues.forEachIndexed { index, issue ->
            val bannerKey =
                if (isDismissed) getDismissedBannerKey(issue.id) else getActiveBannerKey(issue.id)
            val banner =
                currentIssueBanners.getOrPut(bannerKey) {
                    Log.d(TAG, "Creating new banner for ${issue.id} (dismissed: $isDismissed)")
                    SafetyIssueBannerPreference(
                        context,
                        bannerKey,
                        viewModel,
                        fragmentManager,
                        activityTaskId,
                    )
                }

            banner.updateBanner(issue, isDismissed, resolvedIssues)
            banner.order = index

            if (isDismissed) {
                bannerGroup.addSubsectionPreference(banner)
            } else {
                bannerGroup.addPreference(banner)
            }
        }
    }

    /**
     * Why we prefix keys: Banner keys must be unique within the PreferenceGroup. Since the same
     * issue ID can exist in either the active or dismissed state, we prefix the issue ID to
     * distinguish between the BannerMessagePreference instances representing each state.
     */
    private fun getActiveBannerKey(issueId: String) = "active_$issueId"

    private fun getDismissedBannerKey(issueId: String) = "dismissed_$issueId"

    private companion object {
        const val TAG = "SafetyIssuesBannerMgr"
    }
}
