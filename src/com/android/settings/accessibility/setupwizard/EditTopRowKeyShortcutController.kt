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

package com.android.settings.accessibility.setupwizard

import android.content.Context
import com.android.settings.R
import com.android.settings.accessibility.setupwizard.items.IllustrationCheckBoxItem
import com.android.settings.accessibility.shortcuts.ui.TopRowKeyShortcutPreference
import com.android.settingslib.datastore.KeyValueStore
import com.google.android.setupdesign.items.Item

/** Controller for the edit top row key shortcut item in the Accessibility Setup Wizard. */
class EditTopRowKeyShortcutController(
    context: Context,
    item: Item,
    private val metadata: TopRowKeyShortcutPreference,
    dataStore: KeyValueStore,
) : BaseShortcutController(context, item, dataStore, KEY) {

    override fun updateItemVisuals(item: IllustrationCheckBoxItem) {
        item.summary =
            context.getString(R.string.accessibility_shortcut_edit_dialog_summary_top_row_key)
        item.imageResId = R.drawable.accessibility_shortcut_type_top_row
        item.isVisible = metadata.isAvailable(context)
    }

    companion object {
        const val KEY = "shortcut_top_row_key_pref"

        /** Creates a new instance of [EditTopRowKeyShortcutController]. */
        @JvmStatic
        fun create(
            context: Context,
            item: Item,
            targets: Set<String>,
        ): EditTopRowKeyShortcutController {
            val metadata = TopRowKeyShortcutPreference(context, targets)
            return EditTopRowKeyShortcutController(
                context,
                item,
                metadata,
                metadata.storage(context),
            )
        }
    }
}
