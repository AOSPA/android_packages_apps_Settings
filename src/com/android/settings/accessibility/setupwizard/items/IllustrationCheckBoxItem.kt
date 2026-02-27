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
import android.content.res.Resources
import android.util.AttributeSet
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import com.airbnb.lottie.LottieAnimationView
import com.android.settings.R
import com.android.settingslib.widget.LottieColorUtils
import com.google.android.setupdesign.items.CheckBoxItem

/** A custom [CheckBoxItem] with an Illustration. */
class IllustrationCheckBoxItem : CheckBoxItem {

    /** The image drawable to display in [LottieAnimationView]. */
    @DrawableRes
    var imageResId: Int = Resources.ID_NULL
        set(value) {
            if (field != value) {
                resetImageResourceCache(TYPE_RESOURCE_ID)
                field = value
                notifyChanged()
            }
        }

    /** The lottie illustration resource id to display in [LottieAnimationView]. */
    @RawRes
    var imageRawResId: Int = Resources.ID_NULL
        set(value) {
            if (field != value) {
                resetImageResourceCache(TYPE_RAW_RESOURCE_ID)
                field = value
                notifyChanged()
            }
        }

    constructor() : super()

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun getDefaultLayoutResource(): Int = R.layout.setup_shortcut_option_check_box_item

    override fun onBindView(view: View) {
        super.onBindView(view)
        view.findViewById<LottieAnimationView>(R.id.sud_items_image)?.apply {
            val hasRawRes = imageRawResId != Resources.ID_NULL
            val hasImageRes = imageResId != Resources.ID_NULL

            if (!hasRawRes && !hasImageRes) {
                visibility = View.GONE
                return@apply
            }

            visibility = View.VISIBLE
            if (hasRawRes) {
                setAnimation(imageRawResId)
                repeatCount = 0
                LottieColorUtils.applyDynamicColors(context, this)
                LottieColorUtils.applyMaterialColor(context, this)
                playAnimation()
            } else {
                setImageResource(imageResId)
            }
        }
    }

    /** Clears all other resources except the one currently being set. */
    private fun resetImageResourceCache(except: String) {
        if (except != TYPE_RESOURCE_ID) imageResId = Resources.ID_NULL
        if (except != TYPE_RAW_RESOURCE_ID) imageRawResId = Resources.ID_NULL
    }

    companion object {
        private const val TYPE_RESOURCE_ID = "resId"
        private const val TYPE_RAW_RESOURCE_ID = "rawResId"
    }
}
