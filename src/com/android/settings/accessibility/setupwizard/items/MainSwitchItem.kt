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

package com.android.settings.accessibility.setupwizard.items

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.google.android.setupdesign.R
import com.google.android.setupdesign.items.SwitchItem

/** A custom [SwitchItem] used as a primary toggle in Setup Wizard screens. */
class MainSwitchItem : SwitchItem {

    constructor() : super()

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    /**
     * By returning true for [isGroupDivider], we prevent the parent list from applying its default
     * item background or selector, ensuring the custom styling applied in [onBindView].
     */
    override fun isGroupDivider(): Boolean = true

    override fun onBindView(view: View) {
        super.onBindView(view)
        view.setBackgroundResource(R.drawable.sud_card_view_container_background_selected)
    }
}
