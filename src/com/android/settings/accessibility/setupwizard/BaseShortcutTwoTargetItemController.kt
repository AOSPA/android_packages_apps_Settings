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
import com.android.settings.R
import com.android.settings.accessibility.setupwizard.items.TwoTargetItem
import com.android.settings.accessibility.shared.ui.AccessibilityShortcutPreference
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.metadata.getPreferenceTitle
import com.google.android.setupdesign.items.Item

/**
 * Abstract controller for a two-target shortcut item in the Accessibility Setup Wizard.
 *
 * This controller manages a [TwoTargetItem] which typically consists of:
 * 1. A primary click target that opens a configuration fragment (via [onItemSelected]).
 * 2. A secondary toggle target (switch) that enables or disables the shortcut.
 *
 * It automatically handles data binding, lifecycle-aware observation of the underlying
 * [KeyValueStore], and updates the data store when the toggle state changes.
 *
 * @param context The application context.
 * @param item The UI item being controlled.
 * @param metadata The metadata containing title, summary, and component information for the
 *   shortcut.
 * @param dataStore The key-value store where the shortcut preference is persisted.
 */
abstract class BaseShortcutTwoTargetItemController(
    protected val context: Context,
    item: Item,
    private val metadata: AccessibilityShortcutPreference,
    private val dataStore: KeyValueStore,
) : BaseItemController(item) {

    private var shortcutObserver: KeyedObserver<String>? = null
    protected abstract val key: String

    init {
        (item as? TwoTargetItem)?.setOnCheckedChangeListener { _, isChecked ->
            updateDataStore(isChecked)
        }
    }

    override fun bindData(item: Item) {
        if (item is TwoTargetItem) {
            item.title = metadata.getPreferenceTitle(context)
            item.summary = metadata.getSummary(context)
            item.isChecked = dataStore.getBoolean(key) ?: false
        }
    }

    override fun onStart() {
        super.onStart()
        if (shortcutObserver != null) return

        val observer = KeyedObserver<String> { _, _ -> bindData(targetItem) }
        shortcutObserver = observer
        dataStore.addObserver(key, observer, context.mainExecutor)
    }

    override fun onStop() {
        shortcutObserver?.let {
            dataStore.removeObserver(key, it)
            shortcutObserver = null
        }
    }

    override fun onItemSelected(activity: FragmentActivity) {
        EditShortcutSetupWizardFragment.show(
            fragmentManager = activity.supportFragmentManager,
            containerId = R.id.fragment_container,
            metricsCategory = metadata.metricsCategory,
            screenTitle = metadata.getPreferenceTitle(context) ?: "",
            target = metadata.componentName,
        )
    }

    private fun updateDataStore(value: Boolean) = dataStore.setBoolean(key, value)
}
