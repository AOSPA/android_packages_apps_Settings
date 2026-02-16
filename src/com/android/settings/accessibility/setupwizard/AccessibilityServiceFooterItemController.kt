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
import com.android.settings.R
import com.android.settings.accessibility.a11yservice.ui.A11yServiceFooterPreference
import com.google.android.setupdesign.items.Item

/** Controller for the accessibility service footer item in the Accessibility Setup Wizard. */
class AccessibilityServiceFooterItemController(
    private val context: Context,
    serviceInfo: AccessibilityServiceInfo,
    item: Item,
) : BaseItemController(item) {

    private val accessibilityServiceFooterMetadata =
        A11yServiceFooterPreference(
            A11yServiceFooterPreference.HTML_FOOTER_KEY,
            purpose = R.string.a11y_service_detail_screen_html_footer_info_purpose,
            serviceInfo,
            loadHtmlFooter = true,
        )

    override fun bindData(item: Item) {
        with(accessibilityServiceFooterMetadata) {
            val title = getTitle(context)
            val intro = getIntroductionTitle(context)
            item.summary = title
            item.contentDescription = getContentDescription(intro, title)
        }
    }

    override fun onItemSelected(activity: FragmentActivity) {}
}
