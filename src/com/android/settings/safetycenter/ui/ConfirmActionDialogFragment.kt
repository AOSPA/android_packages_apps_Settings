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

import android.app.Dialog
import android.os.Bundle
import android.safetycenter.SafetyCenterIssue
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.android.settings.safetycenter.ui.model.LiveSafetyCenterViewModel
import com.android.settings.safetycenter.ui.model.LiveSafetyCenterViewModelFactory

/** DialogFragment to confirm whether the user wants to perform a SafetyCenterIssue.Action. */
class ConfirmActionDialogFragment : DialogFragment() {

    private val viewModel: LiveSafetyCenterViewModel by
        viewModels(
            ownerProducer = { requireParentFragment() },
            factoryProducer = { LiveSafetyCenterViewModelFactory(requireActivity().application) },
        )

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val issue = requireArguments().getParcelable(ISSUE_KEY, SafetyCenterIssue::class.java)!!
        val isDismissed = requireArguments().getBoolean(IS_DISMISSED_KEY)
        val isPrimaryButton = requireArguments().getBoolean(IS_PRIMARY_BUTTON_KEY)
        val action =
            requireArguments().getParcelable(ACTION_KEY, SafetyCenterIssue.Action::class.java)!!
        val launchTaskId = requireArguments().getInt(LAUNCH_TASK_ID_KEY)
        val confirmationDialogDetails = action.confirmationDialogDetails!!

        return AlertDialog.Builder(requireContext())
            .setTitle(confirmationDialogDetails.title)
            .setMessage(confirmationDialogDetails.text)
            .setPositiveButton(confirmationDialogDetails.acceptButtonText) { _, _ ->
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
            .setNegativeButton(confirmationDialogDetails.denyButtonText, /* listener= */ null)
            .create()
    }

    companion object {
        private const val ISSUE_KEY = "issue"
        private const val IS_DISMISSED_KEY = "isDismissed"
        private const val IS_PRIMARY_BUTTON_KEY = "isPrimaryButton"
        private const val ACTION_KEY = "action"
        private const val LAUNCH_TASK_ID_KEY = "launchTaskId"

        /**
         * Creates a new instance of the dialog.
         *
         * @param issue The SafetyCenterIssue the action belongs to.
         * @param isDismissed Whether the issue is currently in the dismissed state.
         * @param isPrimaryButton Whether the action being confirmed is the primary action.
         * @param action The SafetyCenterIssue.Action to be confirmed.
         * @param launchTaskId The task ID to launch the action in.
         */
        fun newInstance(
            issue: SafetyCenterIssue,
            isDismissed: Boolean,
            isPrimaryButton: Boolean,
            action: SafetyCenterIssue.Action,
            launchTaskId: Int,
        ): ConfirmActionDialogFragment {
            val fragment = ConfirmActionDialogFragment()
            val args = Bundle()
            args.putParcelable(ISSUE_KEY, issue)
            args.putBoolean(IS_DISMISSED_KEY, isDismissed)
            args.putBoolean(IS_PRIMARY_BUTTON_KEY, isPrimaryButton)
            args.putParcelable(ACTION_KEY, action)
            args.putInt(LAUNCH_TASK_ID_KEY, launchTaskId)
            fragment.arguments = args
            return fragment
        }
    }
}
