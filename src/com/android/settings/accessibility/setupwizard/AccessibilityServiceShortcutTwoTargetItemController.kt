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
import com.android.settings.R
import com.android.settings.accessibility.extensions.getFeatureName
import com.android.settings.accessibility.shared.ui.AccessibilityShortcutPreference
import com.android.settings.accessibility.shared.ui.ShortcutFeatureNameProvider
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.google.android.setupdesign.items.Item

/**
 * Controller for the Color Inversion shortcut two target item in the Accessibility Setup Wizard.
 */
class AccessibilityServiceShortcutTwoTargetItemController(
    context: Context,
    item: Item,
    metadata: AccessibilityShortcutPreference,
    dataStore: KeyValueStore,
) : BaseShortcutTwoTargetItemController(context, item, metadata, dataStore) {

    override val key = KEY

    companion object {
        private const val KEY = "shortcut_preference_key"

        fun create(
            context: Context,
            item: Item,
            serviceInfo: AccessibilityServiceInfo,
        ): AccessibilityServiceShortcutTwoTargetItemController {
            val metricsCategory =
                featureFactory.accessibilityPageIdFeatureProvider.getCategory(
                    serviceInfo.componentName
                )
            val metadata =
                object :
                    AccessibilityShortcutPreference(
                        context = context,
                        key = KEY,
                        purpose = R.string.service_shortcut_purpose,
                        componentName = serviceInfo.componentName,
                        metricsCategory = metricsCategory,
                    ),
                    PreferenceTitleProvider,
                    ShortcutFeatureNameProvider {

                    override fun getTitle(context: Context): CharSequence {
                        return context.getString(
                            R.string.accessibility_shortcut_title,
                            getFeatureName(context),
                        )
                    }

                    override fun getFeatureName(context: Context): CharSequence {
                        return serviceInfo.getFeatureName(context)
                    }
                }

            return AccessibilityServiceShortcutTwoTargetItemController(
                context,
                item,
                metadata,
                metadata.storage(context),
            )
        }
    }
}
