/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.accessibility.setupwizard

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.android.settings.display.AutoBrightnessScreen
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.google.android.setupdesign.items.Item
import com.google.android.setupdesign.items.SwitchItem

/** Controller for the Auto-Brightness switch item in the Accessibility Setup Wizard. */
class AutoBrightnessSwitchItemController(
    private val context: Context,
    item: Item,
    private val autoBrightnessDataStore: KeyValueStore = AutoBrightnessScreen().storage(context),
) : BaseItemController(item) {

    private var autoBrightnessObserver: KeyedObserver<String>? = null

    override fun bindData(item: Item) {
        if (item is SwitchItem) {
            item.isChecked = autoBrightnessDataStore.getBoolean(AutoBrightnessScreen.KEY) ?: false

            item.setOnCheckedChangeListener { _, isChecked -> updateDataStore(isChecked) }
        }
    }

    override fun onStart() {
        super.onStart()
        val observer = KeyedObserver<String> { _, _ -> bindData(targetItem) }
        autoBrightnessObserver = observer

        autoBrightnessDataStore.addObserver(
            AutoBrightnessScreen.KEY,
            observer,
            context.mainExecutor,
        )
    }

    override fun onStop() {
        autoBrightnessObserver?.let {
            autoBrightnessDataStore.removeObserver(AutoBrightnessScreen.KEY, it)
            autoBrightnessObserver = null
        }
    }

    override fun onItemSelected(activity: FragmentActivity) {
        val switchItem = targetItem as? SwitchItem ?: return
        updateDataStore(!switchItem.isChecked)
    }

    private fun updateDataStore(value: Boolean) {
        autoBrightnessDataStore.setBoolean(AutoBrightnessScreen.KEY, value)
    }
}
