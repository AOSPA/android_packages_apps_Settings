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

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.android.settings.R
import com.android.settings.accessibility.screenmagnification.ui.MagnificationIllustrationPreference
import com.android.settings.accessibility.setupwizard.items.IllustrationItem
import com.google.android.setupdesign.items.Item
import com.google.android.setupdesign.util.ThemeHelper

/** Controller for the Magnification illustration item in the Accessibility Setup Wizard. */
class MagnificationIllustrationItemController(private val context: Context, item: Item) :
    BaseItemController(item) {

    private val magnificationIllustrationMetadata = MagnificationIllustrationPreference()

    override fun bindData(item: Item) {
        if (item is IllustrationItem) {
            if (ThemeHelper.shouldApplyGlifExpressiveStyle(context)) {
                item.imageResId = R.raw.accessibility_magnification_banner_expressive
            } else {
                item.imageResId = R.raw.accessibility_magnification_banner
            }
            item.contentDescription =
                magnificationIllustrationMetadata.getContentDescription(context)
        }
    }

    override fun onItemSelected(activity: FragmentActivity) {}
}
