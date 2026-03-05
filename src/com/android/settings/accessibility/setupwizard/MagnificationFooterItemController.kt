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

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.android.settings.accessibility.screenmagnification.ui.MagnificationFooterPreference
import com.google.android.setupdesign.items.Item

/** Controller for the Magnification footer item in the Accessibility Setup Wizard. */
class MagnificationFooterItemController(private val context: Context, item: Item) :
    BaseItemController(item) {

    // helpResource = 0 avoids showing "Learn more" links in Setup Wizard flow
    private val magnificationFooterMetadata = MagnificationFooterPreference(helpResource = 0)

    override fun bindData(item: Item) {
        val title = magnificationFooterMetadata.getTitle(context)
        val intro = context.getString(magnificationFooterMetadata.introductionTitle)
        item.summary = title
        item.contentDescription = magnificationFooterMetadata.getContentDescription(intro, title)
    }

    override fun onItemSelected(activity: FragmentActivity) {}
}
