/*
 * Copyright (C) 2026 The Android Open Source Project
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

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.android.settings.accessibility.a11yservice.ui.A11yServiceIllustrationPreference
import com.android.settings.accessibility.setupwizard.items.IllustrationItem
import com.android.settings.accessibility.setupwizard.items.IllustrationItem.OnBindListener
import com.google.android.setupdesign.items.Item

/** Controller for the accessibility service illustration item in the Accessibility Setup Wizard. */
class AccessibilityServiceIllustrationItemController(
    private val context: Context,
    serviceInfo: AccessibilityServiceInfo,
    item: Item,
) : BaseItemController(item) {

    private val accessibilityServiceIllustrationMetadata =
        A11yServiceIllustrationPreference(serviceInfo)

    override fun bindData(item: Item) {
        if (item is IllustrationItem) {
            with(accessibilityServiceIllustrationMetadata) {
                item.imageUri = getImageUri(context)
                item.onBindListener = OnBindListener { _ ->
                    if (item.isLottieAnimation) {
                        item.contentDescription = getContentDescription(context)
                    }
                }
            }
        }
    }

    override fun onItemSelected(activity: FragmentActivity) {}
}
