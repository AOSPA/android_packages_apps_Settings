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
import com.android.settings.accessibility.setupwizard.items.IllustrationCheckBoxItem
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.google.android.setupdesign.items.Item

/** Abstract controller for accessibility shortcut items in the Setup Wizard. */
abstract class BaseShortcutController(
    protected val context: Context,
    item: Item,
    private val dataStore: KeyValueStore,
    private val preferenceKey: String,
) : BaseItemController(item) {

    private var shortcutObserver: KeyedObserver<String>? = null

    init {
        (item as? IllustrationCheckBoxItem)?.setOnCheckedChangeListener { _, isChecked ->
            updateDataStore(isChecked)
        }
    }

    override fun bindData(item: Item) {
        if (item is IllustrationCheckBoxItem) {
            updateItemVisuals(item)
            item.isChecked = dataStore.getBoolean(preferenceKey) ?: false
        }
    }

    /** Subclasses should override this to set specific icons and summaries, etc. */
    abstract fun updateItemVisuals(item: IllustrationCheckBoxItem)

    override fun onStart() {
        super.onStart()
        if (shortcutObserver != null) return

        val observer = KeyedObserver<String> { _, _ -> bindData(targetItem) }
        shortcutObserver = observer
        dataStore.addObserver(preferenceKey, observer, context.mainExecutor)
    }

    override fun onStop() {
        shortcutObserver?.let {
            dataStore.removeObserver(preferenceKey, it)
            shortcutObserver = null
        }
    }

    override fun onItemSelected(activity: FragmentActivity) {
        val checkBoxItem = targetItem as? IllustrationCheckBoxItem ?: return
        updateDataStore(!checkBoxItem.isChecked)
    }

    private fun updateDataStore(value: Boolean) {
        dataStore.setBoolean(preferenceKey, value)
    }
}
