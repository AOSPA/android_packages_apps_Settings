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
import androidx.fragment.app.FragmentActivity
import com.android.settings.accessibility.shortcuts.ui.AdvancedPreference
import com.google.android.setupdesign.items.Item
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Controller for the "Advanced" collapsed item in the Accessibility Setup Wizard.
 *
 * This controller manages an item that acts as a trigger to expand a list of additional
 * accessibility shortcut options. It listens to the [expandableStateProvider] to hide itself once
 * the advanced section has been expanded.
 */
class EditAdvancedItemController(
    private val context: Context,
    item: Item,
    private val metadata: AdvancedPreference,
    private val expandableStateProvider: () -> StateFlow<Boolean>,
) : BaseItemController(item) {

    init {
        CoroutineScope(Dispatchers.Main).launch {
            expandableStateProvider().collect { bindData(item) }
        }
    }

    override fun bindData(item: Item) {
        val isExpanded = expandableStateProvider().value
        item.isVisible = !isExpanded && metadata.isAvailable(context)
    }

    override fun onItemSelected(activity: FragmentActivity) {
        val flow = expandableStateProvider()
        if (flow is MutableStateFlow) {
            flow.value = true
        }
    }

    companion object {
        const val KEY = "advanced_shortcuts_collapsed"

        /** Creates a new instance of [EditAdvancedItemController]. */
        @JvmStatic
        fun create(
            context: Context,
            item: Item,
            targets: Set<String>,
            expandableStateProvider: () -> StateFlow<Boolean>,
        ): EditAdvancedItemController {
            val metadata = AdvancedPreference(targets)
            return EditAdvancedItemController(context, item, metadata, expandableStateProvider)
        }
    }
}
