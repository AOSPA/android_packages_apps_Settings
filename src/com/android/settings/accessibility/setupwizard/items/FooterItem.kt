/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.accessibility.setupwizard.items

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import com.android.settings.R
import com.google.android.setupdesign.items.Item

/**
 * A custom item acting as "footer" of a page. It has a field for icon and text. It is added to
 * screen as the last item.
 */
class FooterItem : Item {

    constructor() : super() {
        clickable = false
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        clickable = false
        icon = icon ?: getDefaultIcon(context)
    }

    override fun getDefaultLayoutResource(): Int = R.layout.setup_items_footer

    override fun onBindView(view: View) {
        super.onBindView(view)
        // TODO(b/407080818): Investigate why ItemAdapter#updateBackground fails to suppress the
        //  default item background when isGroupDivider is true.
        view.background = null
        view.setPadding(0, view.paddingTop, 0, view.paddingBottom)
    }

    /**
     * FooterItem is set as GroupDivider to remove the default item background that are set in
     * ListView and RecyclerViews for all the items that are not group divider and ensure the prior
     * item is styled correctly as the last element in its group.
     */
    override fun isGroupDivider(): Boolean = true

    private fun getDefaultIcon(context: Context) =
        AppCompatResources.getDrawable(
            context,
            com.android.settingslib.widget.preference.footer.R.drawable
                .settingslib_ic_info_outline_24,
        )
}
