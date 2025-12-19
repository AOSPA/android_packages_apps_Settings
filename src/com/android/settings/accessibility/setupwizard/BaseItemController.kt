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

import com.google.android.setupdesign.items.Item

/**
 * Abstract base controller for managing the state and behavior of a specific [Item] within a Setup
 * Wizard list.
 *
 * This class serves as a bridge between the business logic (e.g., checking if a service is enabled)
 * and the [Item] UI component. It ensures that data binding and interaction logic for a single list
 * entry are encapsulated.
 */
abstract class BaseItemController(private val targetItem: Item) {

    /**
     * Initializes the controller by locating the target [Item].
     *
     * This should be called after the adapter's hierarchy has been constructed.
     */
    fun initialize() {
        bindData(targetItem)
    }

    /**
     * Binds data to the managed [item].
     *
     * Implementations should use this method to set properties like the title, summary, icon, or
     * visibility state of the item.
     *
     * @param item The non-null item instance managed by this controller.
     */
    protected abstract fun bindData(item: Item)

    /** Action to perform when the managed item is selected or clicked. */
    abstract fun onItemSelected()
}
