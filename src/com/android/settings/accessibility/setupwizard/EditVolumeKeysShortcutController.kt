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
import com.android.settings.accessibility.shortcuts.ui.VolumeKeysShortcutPreference
import com.android.settingslib.datastore.KeyValueStore
import com.google.android.setupdesign.items.Item

/** Controller for the edit volume keys shortcut item in the Accessibility Setup Wizard. */
class EditVolumeKeysShortcutController(
    context: Context,
    item: Item,
    private val metadata: VolumeKeysShortcutPreference,
    dataStore: KeyValueStore,
) : BaseShortcutController(context, item, dataStore, KEY) {

    override fun updateItemVisuals(item: IllustrationCheckBoxItem) {
        with(metadata) {
            item.summary =
                context.getString(R.string.accessibility_shortcut_edit_dialog_summary_hardware)
            // If device is keyboard enabled, then show the keyboard-specific volume key image.
            item.imageResId =
                if (isDesktopKeyboardShortcutEnabled()) {
                    R.drawable.accessibility_shortcut_type_keyboard_volume_keys
                } else {
                    R.drawable.accessibility_shortcut_type_volume_keys
                }
        }
    }

    companion object {
        const val KEY = "shortcut_volume_keys_pref"

        /** Creates a new instance of [EditVolumeKeysShortcutController]. */
        @JvmStatic
        fun create(
            context: Context,
            item: Item,
            targets: Set<String>,
        ): EditVolumeKeysShortcutController {
            val metadata = VolumeKeysShortcutPreference(context, targets)
            return EditVolumeKeysShortcutController(
                context,
                item,
                metadata,
                metadata.storage(context),
            )
        }
    }
}
