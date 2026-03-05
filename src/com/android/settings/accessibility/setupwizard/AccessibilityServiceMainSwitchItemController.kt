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

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.android.settings.accessibility.a11yservice.ui.UseServicePreference
import com.android.settings.accessibility.setupwizard.items.MainSwitchItem
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.google.android.setupdesign.items.Item
import com.google.android.setupdesign.items.SwitchItem

class AccessibilityServiceMainSwitchItemController(
    private val context: Context,
    serviceInfo: AccessibilityServiceInfo,
    item: Item,
    private val useServiceMetadata: UseServicePreference =
        UseServicePreference(context, serviceInfo, sourceMetricsCategory = 0),
    private val useServiceMetadataDataStore: KeyValueStore = useServiceMetadata.storage(context),
) : BaseItemController(item) {

    private var useServiceObserver: KeyedObserver<String>? = null

    override fun bindData(item: Item) {
        if (item is MainSwitchItem) {
            with(useServiceMetadata) {
                item.isVisible = isAvailable(context)
                item.title = getTitle(context)
                item.isChecked =
                    useServiceMetadataDataStore.getBoolean(UseServicePreference.KEY) ?: false
                item.setOnCheckedChangeListener { _, isChecked -> updateDataStore(isChecked) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (useServiceObserver != null) {
            return
        }

        val observer = KeyedObserver<String> { _, _ -> bindData(targetItem) }
        useServiceObserver = observer

        useServiceMetadataDataStore.addObserver(
            UseServicePreference.KEY,
            observer,
            context.mainExecutor,
        )
    }

    override fun onStop() {
        useServiceObserver?.let {
            useServiceMetadataDataStore.removeObserver(UseServicePreference.KEY, it)
            useServiceObserver = null
        }
    }

    override fun onItemSelected(activity: FragmentActivity) {
        val switchItem = targetItem as? SwitchItem ?: return
        updateDataStore(!switchItem.isChecked)
    }

    private fun updateDataStore(value: Boolean) {
        useServiceMetadataDataStore.setBoolean(UseServicePreference.KEY, value)
    }
}
