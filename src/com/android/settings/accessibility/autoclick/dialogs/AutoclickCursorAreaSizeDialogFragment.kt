/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings.accessibility.autoclick.dialogs

import android.app.Dialog
import android.app.settings.SettingsEnums
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.accessibility.AccessibilityManager
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import com.android.settings.R
import com.android.settings.core.instrumentation.InstrumentedDialogFragment
import com.android.settingslib.datastore.SettingsSecureStore

class AutoclickCursorAreaSizeDialogFragment : InstrumentedDialogFragment() {
    override fun getMetricsCategory(): Int = SettingsEnums.ACCESSIBILITY_TOGGLE_AUTOCLICK

    private val buttonIdToValue by lazy {
        mapOf(
            R.id.autoclick_cursor_area_size_value_extra_small to 20,
            R.id.autoclick_cursor_area_size_value_small to 40,
            R.id.autoclick_cursor_area_size_value_default to 60,
            R.id.autoclick_cursor_area_size_value_large to 80,
            R.id.autoclick_cursor_area_size_value_extra_large to 100,
        )
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val contentView =
            LayoutInflater.from(context).inflate(R.layout.dialog_autoclick_cursor_area_size, null)
        val radioGroup =
            contentView.findViewById<RadioGroup>(R.id.autoclick_cursor_area_size_value_group)

        val currentSize =
            SettingsSecureStore.get(context)
                .getInt(Settings.Secure.ACCESSIBILITY_AUTOCLICK_CURSOR_AREA_SIZE)
                ?: AccessibilityManager.AUTOCLICK_CURSOR_AREA_SIZE_DEFAULT

        val currentSelectedButtonId =
            buttonIdToValue.entries.firstOrNull { it.value == currentSize }?.key
                ?: R.id.autoclick_cursor_area_size_value_default

        radioGroup.check(currentSelectedButtonId)

        return AlertDialog.Builder(context)
            .setView(contentView)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val sizeToSave =
                    buttonIdToValue[radioGroup.checkedRadioButtonId]
                        ?: AccessibilityManager.AUTOCLICK_CURSOR_AREA_SIZE_DEFAULT

                SettingsSecureStore.get(context)
                    .setInt(Settings.Secure.ACCESSIBILITY_AUTOCLICK_CURSOR_AREA_SIZE, sizeToSave)
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .create()
    }
}
