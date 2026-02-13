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
import com.android.settings.R
import com.google.android.setupcompat.partnerconfig.PartnerConfigHelper
import com.google.android.setupdesign.items.SwitchItem

/** A custom [SwitchItem] with two target areas divided by a vertical divider. */
class TwoTargetItem : SwitchItem {

    constructor() : super()

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        if (PartnerConfigHelper.isGlifExpressiveEnabled(context)) {
            setLayoutResource(R.layout.setup_two_target_item_expressive)
        }
    }

    override fun getDefaultLayoutResource(): Int = R.layout.setup_two_target_item
}
