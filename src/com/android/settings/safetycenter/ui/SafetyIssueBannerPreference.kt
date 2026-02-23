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
import android.content.res.Configuration
import android.safetycenter.SafetyCenterIssue
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentManager
import androidx.preference.PreferenceViewHolder
import com.android.settings.R
import com.android.settings.safetycenter.SafetyCenterSeverityConverter
import com.android.settings.safetycenter.ui.model.LiveSafetyCenterViewModel
import com.android.settingslib.widget.BannerMessagePreference

/**
 * A [BannerMessagePreference] specifically designed to display a [SafetyCenterIssue]. It handles
 * the configuration of its content, action buttons, dismiss button, and resolution animations based
 * on the provided issue data and ViewModel.
 *
 * @property context The application context.
 * @property bannerKey The unique key for this preference.
 * @property viewModel The ViewModel for handling actions.
 * @property fragmentManager The FragmentManager for showing dialogs.
 * @property activityTaskId The task ID of the hosting Activity.
 */
class SafetyIssueBannerPreference(
    context: Context,
    private val bannerKey: String,
    private val viewModel: LiveSafetyCenterViewModel,
    private val fragmentManager: FragmentManager,
    private val activityTaskId: Int,
) : BannerMessagePreference(context) {

    lateinit var issue: SafetyCenterIssue
    var isDismissed: Boolean = false

    init {
        key = bannerKey
        if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setButtonOrientation(LinearLayout.HORIZONTAL)
        } else {
            setButtonOrientation(LinearLayout.VERTICAL)
        }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        viewModel.interactionLogger.recordIssueViewed(issue!!, isDismissed!!)
    }

    /**
     * Updates the content and button states of the banner.
     *
     * @param issue The [SafetyCenterIssue] to display.
     * @param isDismissed Whether the issue is currently in the dismissed state.
     * @param resolvedIssues A map of issue IDs to action IDs for resolved actions.
     * @param headerResId The String Resource ID for the banner header.
     */
    fun updateBanner(
        issue: SafetyCenterIssue,
        isDismissed: Boolean,
        resolvedIssues: Map<String, String>,
        @StringRes headerResId: Int,
    ) {
        this.issue = issue
        this.isDismissed = isDismissed

        title = issue.title
        summary = issue.summary
        setHeader(context.getString(headerResId))
        setSubtitle(issue.subtitle)
        setAttentionLevel(SafetyCenterSeverityConverter.toBannerAttentionLevel(issue.severityLevel))

        configureActionButtons(issue, isDismissed, resolvedIssues)
        configureDismissButton(issue, isDismissed)
        maybeStartResolution(issue, resolvedIssues)
    }

    /**
     * Configures the action buttons based on the issue's actions and resolution state.
     *
     * @param issue The [SafetyCenterIssue].
     * @param resolvedIssues A map of issue IDs to action IDs for resolved actions.
     */
    private fun configureActionButtons(
        issue: SafetyCenterIssue,
        isDismissed: Boolean,
        resolvedIssues: Map<String, String>,
    ) {
        val resolvedActionId = resolvedIssues[issue.id]

        val primaryAction = issue.actions.getOrNull(0)
        if (primaryAction != null) {
            setPositiveButtonText(primaryAction.label)
            setPositiveButtonEnabled(primaryAction.id != resolvedActionId)
            setPositiveButtonVisible(true)
            setPositiveButtonOnClickListener(
                ActionButtonOnClickListener(
                    issue,
                    isDismissed,
                    isPrimaryButton = true,
                    primaryAction,
                    this,
                )
            )
        } else {
            setPositiveButtonVisible(false)
            setPositiveButtonOnClickListener(null)
        }

        val secondaryAction = issue.actions.getOrNull(1)
        if (secondaryAction != null) {
            setNegativeButtonText(secondaryAction.label)
            setNegativeButtonEnabled(secondaryAction.id != resolvedActionId)
            setNegativeButtonVisible(true)
            setNegativeButtonOnClickListener(
                ActionButtonOnClickListener(
                    issue,
                    isDismissed,
                    isPrimaryButton = false,
                    secondaryAction,
                    this,
                )
            )
        } else {
            setNegativeButtonVisible(false)
            setNegativeButtonOnClickListener(null)
        }
    }

    /**
     * Configures the dismiss button.
     *
     * @param issue The [SafetyCenterIssue].
     * @param isDismissed Whether the issue is currently in the dismissed state.
     */
    private fun configureDismissButton(issue: SafetyCenterIssue, isDismissed: Boolean) {
        if (issue.isDismissible && !isDismissed) {
            setDismissButtonVisible(true)
            setDismissButtonOnClickListener(
                {
                    Log.d(TAG, "Dismiss button clicked for issue '${issue.id}'")
                    if (issue.shouldConfirmDismissal()) {
                        Log.d(TAG, "Showing dismiss confirmation for issue '${issue.id}'")
                        ConfirmDismissalDialogFragment.newInstance(issue, this.key)
                            .showNow(fragmentManager, /* tag= */ null)
                    } else {
                        this.animateDismiss {
                            viewModel.dismissIssue(issue)
                            viewModel.interactionLogger.recordForIssue(
                                Action.ISSUE_DISMISS_CLICKED,
                                issue,
                                isDismissed = false,
                            )
                        }
                    }
                },
                /* autoCollapse= */ false,
            )
        } else {
            setDismissButtonVisible(false)
            setDismissButtonOnClickListener(null)
        }
    }

    /**
     * Starts the resolution animation on the banner if the issue has a successfully resolved
     * action.
     *
     * @param issue The [SafetyCenterIssue].
     * @param resolvedIssues A map of successfully resolved issue IDs to action IDs.
     */
    private fun maybeStartResolution(
        issue: SafetyCenterIssue,
        resolvedIssues: Map<String, String>,
    ) {
        val resolvedActionId = resolvedIssues[issue.id]
        if (resolvedActionId == null) {
            this.clearResolutionAnimation()
            return
        }

        val action = issue.actions.firstOrNull { it.id == resolvedActionId } ?: return

        val successMessage =
            action.successMessage?.ifEmpty { null }
                ?: context.getString(R.string.safety_center_resolved_issue_fallback)

        Log.d(
            TAG,
            "Starting resolution animation for issue '${issue.id}' with message: '$successMessage'",
        )
        this.showResolutionAnimation(successMessage) {
            viewModel.markIssueResolvedUiCompleted(issue.id)
        }
    }

    /**
     * An [View.OnClickListener] for action buttons on a [BannerMessagePreference]. Handles showing
     * a confirmation dialog or executing the action directly via the [viewModel]. Disables buttons
     * if the action will resolve the issue.
     *
     * @property issue The [SafetyCenterIssue] this action belongs to.
     * @property action The specific [SafetyCenterIssue.Action] to execute.
     * @property banner The [BannerMessagePreference] hosting the button.
     */
    private inner class ActionButtonOnClickListener(
        private val issue: SafetyCenterIssue,
        private val isDismissed: Boolean,
        private val isPrimaryButton: Boolean,
        private val action: SafetyCenterIssue.Action,
        private val banner: BannerMessagePreference,
    ) : View.OnClickListener {
        override fun onClick(v: View?) {
            Log.d(TAG, "Action '${action.id}' clicked for issue '${issue.id}'")
            if (action.confirmationDialogDetails != null) {
                Log.d(TAG, "Showing confirmation dialog for action '${action.id}'")
                ConfirmActionDialogFragment.newInstance(
                        issue,
                        isDismissed,
                        isPrimaryButton,
                        action,
                        activityTaskId,
                    )
                    .showNow(fragmentManager, /* tag= */ null)
            } else {
                if (action.willResolve()) {
                    banner.setPositiveButtonEnabled(false)
                    banner.setNegativeButtonEnabled(false)
                }
                val launchTaskId = calculateLaunchTaskId(issue.safetySourceIds, activityTaskId)
                viewModel.executeIssueAction(issue, action, launchTaskId)
                viewModel.interactionLogger.recordForIssue(
                    if (isPrimaryButton) {
                        Action.ISSUE_PRIMARY_ACTION_CLICKED
                    } else {
                        Action.ISSUE_SECONDARY_ACTION_CLICKED
                    },
                    issue,
                    isDismissed,
                )
            }
        }

        private fun calculateLaunchTaskId(safetySourceIds: Set<String>, activityTaskId: Int): Int? =
            // If any of the related sources should be kept in the same task, use same task.
            safetySourceIds
                .map { PendingIntentSender.getTaskIdToSend(context, it, activityTaskId) }
                .firstOrNull { it != null }
    }

    private companion object {
        const val TAG = "SafetyIssueBanner"
    }
}
