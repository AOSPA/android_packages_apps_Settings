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

import androidx.fragment.app.FragmentActivity
import com.google.android.setupdesign.items.Item

/**
 * Abstract base controller for managing the state and behavior of a specific [Item] within a Setup
 * Wizard list.
 *
 * This class serves as a bridge between the business logic (e.g., checking if a service is enabled)
 * and the [Item] UI component. It ensures that data binding and interaction logic for a single list
 * entry are encapsulated.
 */
abstract class BaseItemController(val targetItem: Item) {

    /**
     * Prepares the controller's structural state.
     *
     * This is called once when the view hierarchy is created (e.g., in onViewCreated). Unlike
     * [onStart], this should be used for one-time setup that does not depend on dynamic state that
     * might change while the fragment is stopped.
     */
    open fun initialize() {}

    /**
     * Binds data to the managed [item].
     *
     * Implementations must ensure this method is safe to call multiple times during the lifecycle
     * (e.g., on initial start, after dynamic data changes, or when returning from the backstack).
     *
     * @param item The non-null item instance managed by this controller.
     */
    abstract fun bindData(item: Item)

    /**
     * Called when the controller becomes visible. Synchronizes the UI state.
     *
     * This method triggers [bindData] to ensure the item reflects the latest information, including
     * changes that occurred while the screen was in the background. It should also be used to
     * register observers or listeners.
     */
    open fun onStart() {
        bindData(targetItem)
    }

    /**
     * Called when the controller is no longer active or visible.
     *
     * Use this to unregister listeners or perform cleanup to prevent memory leaks.
     */
    open fun onStop() {}

    /**
     * Action to perform when the managed item is selected or clicked.
     *
     * @param activity The activity currently handling the selection callback.
     */
    abstract fun onItemSelected(activity: FragmentActivity)
}
