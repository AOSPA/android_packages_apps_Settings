/*
 * Copyright 2025 The Android Open Source Project
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

package com.android.settings.input.gamecontroller

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.android.settings.R

/**
 * A dialog fragment that asks the user for confirmation before resetting all controller
 * customizations to their default settings.
 */
class GameControllerResetDialogFragment(val viewModel: GameControllerViewModel) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.game_controller_remapping_reset_dialog_title)
            .setMessage(R.string.game_controller_remapping_reset_dialog_message)
            .setPositiveButton(R.string.game_controller_reset_dialog_button) { _, _ ->
                viewModel.resetRemapping()
                dismiss()
            }
            .setNegativeButton(R.string.cancel) { _, _ -> dismiss() }
            .create()
    }
}
