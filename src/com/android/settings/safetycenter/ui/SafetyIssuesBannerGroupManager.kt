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
     * This method also handles the potential reordering of active issues if a [focusedIssueKey] is
     * provided, ensuring the focused issue is prioritized in the display. The number of visible
     * preferences in the collapsed state of the [bannerGroup] is adjusted accordingly.
     *
     * @param newActiveIssues A list of currently active [SafetyCenterIssue]s.
     * @param newDismissedIssues A list of currently dismissed [SafetyCenterIssue]s.
     * @param resolvedIssues A map where keys are issue IDs and values are action IDs that have been
     *   successfully resolved.
     * @param focusedIssueKey An optional key identifying a specific issue to bring to the user's
     *   attention. If provided and found, this issue will be reordered to the top or second
     *   position in the active issues list.
     * @param issueIdToHeaderResIdMap A map of Issue ID to the String Resource ID for the header.
     */
    fun updateBannerGroup(
        newActiveIssues: List<SafetyCenterIssue>,
        newDismissedIssues: List<SafetyCenterIssue>,
        resolvedIssues: Map<String, String>,
        focusedIssueKey: FocusedIssueKey?,
        issueIdToHeaderResIdMap: Map<String, Int>,
    ) {
        val reorderResult = calculateReorderedIssues(newActiveIssues, focusedIssueKey)
        val reorderedNewActiveIssues = reorderResult.issues
        bannerGroup.visiblePreferencesWhenCollapsedCount = reorderResult.visibleCountWhenCollapsed
        val newActiveIssueKeys = reorderedNewActiveIssues.map { getActiveBannerKey(it.id) }.toSet()
        removeStaleBanners(currentActiveIssueBanners, newActiveIssueKeys)
        createOrUpdateBanners(
            reorderedNewActiveIssues,
            currentActiveIssueBanners,
            isDismissed = false,
            resolvedIssues,
            issueIdToHeaderResIdMap,
        )

        val newDismissedIssueKeys = newDismissedIssues.map { getDismissedBannerKey(it.id) }.toSet()
        removeStaleBanners(currentDismissedIssueBanners, newDismissedIssueKeys)

        if (newDismissedIssues.isNotEmpty()) {
            createOrUpdateBanners(
                newDismissedIssues,
                currentDismissedIssueBanners,
                isDismissed = true,
                resolvedIssues,
                issueIdToHeaderResIdMap,
            )
        }
    }

    /**
     * Calculates the reordered list of active [SafetyCenterIssue]s to prioritize the issue matching
     * the [focusedIssueKey] and determines the number of items to show when collapsed.
     *
     * The focused issue is moved to the top of the list (index 0) if it has the highest severity or
     * if the list is empty. Otherwise, it's placed at index 1.
     *
     * @param issues The original list of active [SafetyCenterIssue]s, sorted by severity.
     * @param focusedIssueKey The key identifying the issue to be prioritized.
     * @return A [ReorderedIssuesResult] containing the new list and the count of visible items when
     *   collapsed.
     */
    private fun calculateReorderedIssues(
        issues: List<SafetyCenterIssue>,
        focusedIssueKey: FocusedIssueKey?,
    ): ReorderedIssuesResult {
        if (focusedIssueKey == null || issues.isEmpty() || issues.size == 1) {
            return ReorderedIssuesResult(issues, VISIBLE_COUNT_DEFAULT)
        }

        val mutableIssueList = issues.toMutableList()
        val focusedIssue = findAndRemoveFocusedIssue(mutableIssueList, focusedIssueKey)

        if (focusedIssue == null) {
            Log.w(TAG, "Focused issue not found: $focusedIssueKey")
            return ReorderedIssuesResult(issues, VISIBLE_COUNT_DEFAULT)
        }

        Log.d(TAG, "Found focused issue: $focusedIssueKey")
        return if (focusedIssue.severityLevel >= mutableIssueList[0].severityLevel) {
            mutableIssueList.add(0, focusedIssue)
            Log.d(TAG, "Focused issue is of highest severity, placed at top of the list.")
            ReorderedIssuesResult(mutableIssueList, VISIBLE_COUNT_DEFAULT)
        } else {
            mutableIssueList.add(1, focusedIssue)
            Log.d(
                TAG,
                "Focused issue is not of highest severity, placed at second position in the list.",
            )
            ReorderedIssuesResult(mutableIssueList, VISIBLE_COUNT_FOCUSED_SECOND)
        }
    }

    /**
     * Finds the focused issue in the list, removes it, and returns it.
     *
     * @param issues The mutable list of issues to search within.
     * @param focusedIssueKey The key of the issue to find.
     * @return The found and removed [SafetyCenterIssue], or null if not found.
     */
    private fun findAndRemoveFocusedIssue(
        issues: MutableList<SafetyCenterIssue>,
        focusedIssueKey: FocusedIssueKey,
    ): SafetyCenterIssue? {
        val index = issues.indexOfFirst { focusedIssueKey.matchesSafetyCenterIssue(it) }
        return if (index != -1) {
            issues.removeAt(index)
        } else {
            null
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
            currentIssueBanners.remove(key)?.let { banner ->
                bannerGroup.removePreference(banner, /* moveToDismissed= */ false)
            }
        }
    }

    /**
     * Creates new [BannerMessagePreference]s or updates existing ones for the given list of issues.
     *
     * @param newIssues The list of [SafetyCenterIssue]s to process.
     * @param currentIssueBanners The mutable map storing the current banners.
     * @param isDismissed True if these issues are dismissed, false otherwise.
     * @param resolvedIssues A map of successfully resolved issue IDs to action IDs.
     * @param issueIdToHeaderResIdMap A map of Issue ID to the String Resource ID for the header.
     */
    private fun createOrUpdateBanners(
        newIssues: List<SafetyCenterIssue>,
        currentIssueBanners: MutableMap<String, SafetyIssueBannerPreference>,
        isDismissed: Boolean,
        resolvedIssues: Map<String, String>,
        issueIdToHeaderResIdMap: Map<String, Int>,
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

            val headerResId = issueIdToHeaderResIdMap[issue.id] ?: R.string.safety_center_title
            banner.updateBanner(issue, isDismissed, resolvedIssues, headerResId)
            banner.order = index

            bannerGroup.addPreference(banner, isDismissed)
        }
    }

    /**
     * Holds the result of the issue reordering logic, including the reordered list and the number
     * of issues to display when the banner group is collapsed.
     *
     * @property issues The potentially reordered list of [SafetyCenterIssue]s.
     * @property visibleCountWhenCollapsed The number of preferences to show in the
     *   [BannerMessagePreferenceGroup] when it is in its collapsed state. This count is adjusted
     *   based on whether an issue was focused and its severity.
     */
    private data class ReorderedIssuesResult(
        val issues: List<SafetyCenterIssue>,
        val visibleCountWhenCollapsed: Int,
    )

    /**
     * Why we prefix keys: Banner keys must be unique within the PreferenceGroup. Since the same
     * issue ID can exist in either the active or dismissed state, we prefix the issue ID to
     * distinguish between the BannerMessagePreference instances representing each state.
     */
    private fun getActiveBannerKey(issueId: String) = "active_$issueId"

    private fun getDismissedBannerKey(issueId: String) = "dismissed_$issueId"

    private companion object {
        const val TAG = "SafetyIssuesBannerMgr"
        private const val VISIBLE_COUNT_DEFAULT = 1
        private const val VISIBLE_COUNT_FOCUSED_SECOND = 2
    }
}
