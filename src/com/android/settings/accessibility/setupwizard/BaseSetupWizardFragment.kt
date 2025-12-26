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
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import com.google.android.setupdesign.GlifRecyclerLayout
import com.google.android.setupdesign.items.IItem
import com.google.android.setupdesign.items.Item
import com.google.android.setupdesign.items.RecyclerItemAdapter

/** Base fragment for Accessibility Setup Wizard screens that utilize a [GlifRecyclerLayout]. */
abstract class BaseSetupWizardFragment : Fragment(), RecyclerItemAdapter.OnItemSelectedListener {
    @get:LayoutRes abstract val fragmentLayoutResId: Int

    @get:IdRes abstract val glifLayoutId: Int

    abstract fun createControllers(adapter: RecyclerItemAdapter): Map<Int, BaseItemController>

    private var controllers: Map<Int, BaseItemController> = emptyMap()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? = inflater.inflate(fragmentLayoutResId, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layout = view.findViewById<GlifRecyclerLayout>(glifLayoutId)
        val adapter = layout.adapter as RecyclerItemAdapter
        adapter.setOnItemSelectedListener(this)

        controllers = createControllers(adapter)
        controllers.values.forEach { it.initialize() }
    }

    override fun onItemSelected(item: IItem) {
        val itemId = (item as? Item)?.id ?: return
        controllers[itemId]?.onItemSelected(requireActivity())
    }

    override fun onStart() {
        super.onStart()
        controllers.values.forEach { it.onStart() }
    }

    override fun onStop() {
        super.onStop()
        controllers.values.forEach { it.onStop() }
    }

    protected fun findItem(adapter: RecyclerItemAdapter, @IdRes id: Int): Item? =
        adapter.findItemById(id)?.getItemAt(0) as? Item
}
