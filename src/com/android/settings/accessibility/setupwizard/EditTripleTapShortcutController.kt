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
import com.android.settings.accessibility.shortcuts.ui.TripleTapShortcutPreference
import com.android.settingslib.datastore.KeyValueStore
import com.google.android.setupdesign.items.Item
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Controller for the edit triple tap shortcut item in the Accessibility Setup Wizard. */
class EditTripleTapShortcutController(
    context: Context,
    item: Item,
    private val metadata: TripleTapShortcutPreference,
    dataStore: KeyValueStore,
    private val expandableStateProvider: () -> StateFlow<Boolean>,
) : BaseShortcutController(context, item, dataStore, KEY) {

    init {
        CoroutineScope(Dispatchers.Main).launch {
            expandableStateProvider().collect { bindData(item) }
        }
    }

    override fun updateItemVisuals(item: IllustrationCheckBoxItem) {
        val isExpanded = expandableStateProvider().value
        with(metadata) {
            item.summary = getSummary(context)
            item.imageRawResId = R.raw.accessibility_shortcut_type_tripletap
            // Start hidden, and show only when the state is expanded
            item.isVisible = isExpanded && isAvailable(context)
        }
    }

    companion object {
        const val KEY = "shortcut_triple_tap_pref"

        /** Creates a new instance of [EditTripleTapShortcutController]. */
        @JvmStatic
        fun create(
            context: Context,
            item: Item,
            targets: Set<String>,
            expandableStateProvider: () -> StateFlow<Boolean>,
        ): EditTripleTapShortcutController {
            val metadata = TripleTapShortcutPreference(context, targets, expandableStateProvider)
            return EditTripleTapShortcutController(
                context,
                item,
                metadata,
                metadata.storage(context),
                expandableStateProvider,
            )
        }
    }
}
