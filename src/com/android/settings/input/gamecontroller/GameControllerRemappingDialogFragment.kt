/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import android.view.LayoutInflater
import android.view.Window
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.settings.R
import com.android.settings.input.gamecontroller.GameControllerUtils.preferenceKeyToAxesMap
import com.android.settings.input.gamecontroller.GameControllerUtils.preferenceKeyToButtonMap
import com.android.settings.input.gamecontroller.GameControllerUtils.preferenceKeyToIconMap
import com.android.settings.input.gamecontroller.GameControllerUtils.preferenceKeyToNameMap
import com.android.settingslib.Utils.getColorAttrDefaultColor

/** Fragment that displays game controller remapping list of preferences. */
class GameControllerRemappingDialogFragment(val viewModel: GameControllerViewModel) :
    DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.game_controller_remapping_dialog, null)

        val fromPreferenceKey = requireArguments().getString(ARGS_FROM_PREFERENCE_KEY)!!
        val toPreferenceKey = requireArguments().getString(ARGS_TO_PREFERENCE_KEY)!!

        // Determine if we are remapping a button or an axis and filter the choices accordingly.
        val isButtonRemapping = preferenceKeyToButtonMap.containsKey(fromPreferenceKey)
        val validKeys =
            if (isButtonRemapping) {
                preferenceKeyToNameMap.filterKeys { preferenceKeyToButtonMap.containsKey(it) }
                preferenceKeyToNameMap.keys.filter { preferenceKeyToButtonMap.containsKey(it) }
            } else {
                preferenceKeyToNameMap.filterKeys { preferenceKeyToAxesMap.containsKey(it) }
                preferenceKeyToNameMap.keys.filter { preferenceKeyToAxesMap.containsKey(it) }
            }
        val color = getColorAttrDefaultColor(context, android.R.attr.colorControlNormal)
        val choices =
            validKeys.associateWith { key ->
                val glyph =
                    preferenceKeyToButtonMap[key]?.let {
                        viewModel.glyphMap?.getDrawableForKeycode(context, it)
                    }
                glyph?.setTint(color)
                val displayName =
                    preferenceKeyToButtonMap[key]?.let {
                        viewModel.glyphMap?.getDisplayNameForKeycode(it)
                    }
                GameControllerRemappingAdapter.Choice(
                    displayName ?: context.getString(preferenceKeyToNameMap.getValue(key)),
                    glyph ?: context.getDrawable(preferenceKeyToIconMap.getValue(key))!!,
                )
            }

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter =
            GameControllerRemappingAdapter(
                choices = choices,
                currentSelectionKey = toPreferenceKey,
            ) { selectedKey ->
                // When an item is clicked, send the result and close the dialog
                setFragmentResult(
                    REQUEST_REMAPPING,
                    bundleOf(
                        ARGS_FROM_PREFERENCE_KEY to fromPreferenceKey,
                        ARGS_TO_PREFERENCE_KEY to selectedKey,
                    ),
                )
                dismiss()
            }

        val dialog = AlertDialog.Builder(context).setView(view).create()
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    companion object {
        const val REQUEST_REMAPPING = "request_remapping"
        const val ARGS_FROM_PREFERENCE_KEY = "from_preference_key"
        const val ARGS_TO_PREFERENCE_KEY = "to_preference_key"
    }
}
