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

import android.app.settings.SettingsEnums
import android.content.Context
import com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_COMPONENT_NAME
import com.android.settings.R
import com.android.settings.accessibility.shared.ui.AccessibilityShortcutPreference
import com.android.settingslib.datastore.KeyValueStore
import com.google.android.setupdesign.items.Item

/** Controller for the Magnification shortcut two target item in the Accessibility Setup Wizard. */
class MagnificationShortcutTwoTargetItemController(
    context: Context,
    item: Item,
    metadata: AccessibilityShortcutPreference,
    dataStore: KeyValueStore,
) : BaseShortcutTwoTargetItemController(context, item, metadata, dataStore) {

    override val key = KEY

    companion object {
        private const val KEY = "magnification_shortcut_preference"

        fun create(context: Context, item: Item): MagnificationShortcutTwoTargetItemController {
            val metadata =
                AccessibilityShortcutPreference(
                    context = context,
                    key = KEY,
                    purpose = R.string.magnification_shortcut_preference_purpose,
                    title = R.string.accessibility_screen_magnification_shortcut_title,
                    componentName = MAGNIFICATION_COMPONENT_NAME,
                    featureName = R.string.accessibility_screen_magnification_title,
                    metricsCategory = SettingsEnums.ACCESSIBILITY_TOGGLE_SCREEN_MAGNIFICATION,
                )
            return MagnificationShortcutTwoTargetItemController(
                context,
                item,
                metadata,
                metadata.storage(context),
            )
        }
    }
}
