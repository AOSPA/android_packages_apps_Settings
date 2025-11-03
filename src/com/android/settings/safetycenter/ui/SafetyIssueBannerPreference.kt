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
import android.view.View
import android.widget.LinearLayout
import androidx.fragment.app.FragmentManager
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

    init {
        key = bannerKey
        setButtonOrientation(LinearLayout.VERTICAL)
    }

    /**
     * Updates the content and button states of the banner.
     *
     * @param issue The latest [SafetyCenterIssue] data.
     * @param isDismissed Whether the issue is currently in the dismissed state.
     * @param resolvedIssues A map of issue IDs to action IDs for resolved actions.
     */
    fun updateBanner(
        issue: SafetyCenterIssue,
        isDismissed: Boolean,
        resolvedIssues: Map<String, String>,
    ) {
        title = issue.title
        summary = issue.summary
        setHeader(issue.attributionTitle)
        setSubtitle(issue.subtitle)
        setAttentionLevel(SafetyCenterSeverityConverter.toBannerAttentionLevel(issue.severityLevel))

        configureActionButtons(issue, resolvedIssues)
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
        resolvedIssues: Map<String, String>,
    ) {
        val resolvedActionId = resolvedIssues[issue.id]

        val primaryAction = issue.actions.getOrNull(0)
        if (primaryAction != null) {
            setPositiveButtonText(primaryAction.label)
            setPositiveButtonEnabled(primaryAction.id != resolvedActionId)
            setPositiveButtonVisible(true)
            setPositiveButtonOnClickListener(
                ActionButtonOnClickListener(issue, primaryAction, this)
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
                ActionButtonOnClickListener(issue, secondaryAction, this)
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
            setDismissButtonOnClickListener {
                Log.d(TAG, "Dismiss button clicked for issue '${issue.id}'")
                if (issue.shouldConfirmDismissal()) {
                    Log.d(TAG, "Showing dismiss confirmation for issue '${issue.id}'")
                    ConfirmDismissalDialogFragment.newInstance(issue)
                        .showNow(fragmentManager, /* tag= */ null)
                } else {
                    viewModel.dismissIssue(issue)
                }
            }
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
        private val action: SafetyCenterIssue.Action,
        private val banner: BannerMessagePreference,
    ) : View.OnClickListener {
        override fun onClick(v: View?) {
            Log.d(TAG, "Action '${action.id}' clicked for issue '${issue.id}'")
            if (action.confirmationDialogDetails != null) {
                Log.d(TAG, "Showing confirmation dialog for action '${action.id}'")
                ConfirmActionDialogFragment.newInstance(issue, action, activityTaskId)
                    .showNow(fragmentManager, /* tag= */ null)
            } else {
                if (action.willResolve()) {
                    banner.setPositiveButtonEnabled(false)
                    banner.setNegativeButtonEnabled(false)
                }
                viewModel.executeIssueAction(issue, action, activityTaskId)
            }
        }
    }

    private companion object {
        const val TAG = "SafetyIssueBanner"
    }
}
