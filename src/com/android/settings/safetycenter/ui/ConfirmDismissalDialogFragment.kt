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
import com.android.settings.R
import com.android.settings.safetycenter.ui.model.LiveSafetyCenterViewModel
import com.android.settings.safetycenter.ui.model.LiveSafetyCenterViewModelFactory

/** DialogFragment to confirm whether the user wants to dismiss a SafetyCenterIssue. */
class ConfirmDismissalDialogFragment : DialogFragment() {

    private val viewModel: LiveSafetyCenterViewModel by
        viewModels(
            ownerProducer = { requireParentFragment() },
            factoryProducer = { LiveSafetyCenterViewModelFactory(requireActivity().application) },
        )

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val issue = requireArguments().getParcelable(ISSUE_KEY, SafetyCenterIssue::class.java)!!

        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.safety_center_issue_card_dismiss_confirmation_title)
            .setMessage(R.string.safety_center_issue_card_dismiss_confirmation_message)
            .setPositiveButton(R.string.dismiss) { _, _ -> viewModel.dismissIssue(issue) }
            .setNegativeButton(R.string.cancel, /* listener= */ null)
            .create()
    }

    companion object {
        private const val ISSUE_KEY = "issue"

        /**
         * Creates a new instance of the dialog.
         *
         * @param issue The SafetyCenterIssue to be dismissed.
         */
        fun newInstance(issue: SafetyCenterIssue): ConfirmDismissalDialogFragment {
            val fragment = ConfirmDismissalDialogFragment()
            val args = Bundle()
            args.putParcelable(ISSUE_KEY, issue)
            fragment.arguments = args
            return fragment
        }
    }
}
