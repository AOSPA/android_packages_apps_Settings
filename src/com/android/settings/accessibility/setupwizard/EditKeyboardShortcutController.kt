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
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.android.settings.accessibility.setupwizard.items.IllustrationCheckBoxItem
import com.android.settings.accessibility.shortcuts.ui.KeyboardShortcutPreference
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.google.android.setupdesign.items.Item

/** Controller for the edit keyboard shortcut item in the Accessibility Setup Wizard. */
class EditKeyboardShortcutController(
    private val context: Context,
    item: Item,
    private val keyboardShortcutMetadata: KeyboardShortcutPreference,
    private val keyboardShortcutMetadataDataStore: KeyValueStore,
) : BaseItemController(item) {

    private var keyboardShortcutObserver: KeyedObserver<String>? = null

    init {
        (item as? IllustrationCheckBoxItem)?.setOnCheckedChangeListener { _, isChecked ->
            updateDataStore(isChecked)
        }
    }

    override fun bindData(item: Item) {
        if (item is IllustrationCheckBoxItem) {
            with(keyboardShortcutMetadata) {
                item.summary = getSummary(context)
                val resId = getIconResId(context)
                if (resId != 0) {
                    item.icon = ContextCompat.getDrawable(context, resId)
                }
            }
            item.isChecked = keyboardShortcutMetadataDataStore.getBoolean(KEY) ?: false
        }
    }

    override fun onStart() {
        super.onStart()
        if (keyboardShortcutObserver != null) {
            return
        }

        val observer = KeyedObserver<String> { _, _ -> bindData(targetItem) }
        keyboardShortcutObserver = observer

        keyboardShortcutMetadataDataStore.addObserver(KEY, observer, context.mainExecutor)
    }

    override fun onStop() {
        keyboardShortcutObserver?.let {
            keyboardShortcutMetadataDataStore.removeObserver(KEY, it)
            keyboardShortcutObserver = null
        }
    }

    override fun onItemSelected(activity: FragmentActivity) {
        val checkBoxItem = targetItem as? IllustrationCheckBoxItem ?: return
        updateDataStore(!checkBoxItem.isChecked)
    }

    private fun updateDataStore(value: Boolean) {
        keyboardShortcutMetadataDataStore.setBoolean(KEY, value)
    }

    companion object {
        const val KEY = "shortcut_keyboard_pref"

        /** Creates a new instance of [EditKeyboardShortcutController]. */
        @JvmStatic
        fun create(
            context: Context,
            item: Item,
            targets: Set<String>,
        ): EditKeyboardShortcutController {
            val metadata = KeyboardShortcutPreference(context, targets)
            return EditKeyboardShortcutController(
                context = context,
                item = item,
                keyboardShortcutMetadata = metadata,
                keyboardShortcutMetadataDataStore = metadata.storage(context),
            )
        }
    }
}
