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

package com.android.settings.accessibility.setupwizard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.google.android.setupdesign.GlifRecyclerLayout
import com.google.android.setupdesign.items.IItem
import com.google.android.setupdesign.items.Item
import com.google.android.setupdesign.items.RecyclerItemAdapter

/** Accessibility Vision Settings for Setup Wizard. */
class AccessibilitySetupWizardFragment : Fragment(), RecyclerItemAdapter.OnItemSelectedListener {

    private var controllers: Map<Int, BaseItemController> = emptyMap()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.accessibility_suw_screen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layout = view.findViewById<GlifRecyclerLayout>(R.id.accessibility_suw_screen_layout)
        val adapter = layout.adapter as RecyclerItemAdapter
        adapter.setOnItemSelectedListener(this)

        controllers = buildMap {
            val context = requireContext()
            val screenReaderComp =
                context.getString(com.android.internal.R.string.config_defaultAccessibilityService)
            findItem(adapter, R.id.screen_reader_in_suw)?.let {
                put(
                    R.id.screen_reader_in_suw,
                    AccessibilityServiceItemController(context, it, screenReaderComp),
                )
            }
            val selectToSpeakComponent =
                context.getString(com.android.internal.R.string.config_defaultSelectToSpeakService)
            findItem(adapter, R.id.select_to_speak_in_suw)?.let {
                put(
                    R.id.select_to_speak_in_suw,
                    AccessibilityServiceItemController(context, it, selectToSpeakComponent),
                )
            }
        }

        controllers.values.forEach { it.initialize() }
    }

    override fun onItemSelected(item: IItem) {
        val subItem = item as? Item ?: return
        controllers[subItem.id]?.onItemSelected()
    }

    private fun findItem(adapter: RecyclerItemAdapter, id: Int): Item? =
        adapter.findItemById(id).getItemAt(0) as? Item
}
