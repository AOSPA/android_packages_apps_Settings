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
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import androidx.core.content.withStyledAttributes
import com.airbnb.lottie.LottieAnimationView
import com.android.settings.R
import com.google.android.setupdesign.items.Item

/**
 * An item that is displayed with an Illustration, with methods to manipulate state of the
 * imageView.
 */
class IllustrationItem : Item {

    /** The image drawable to display in [LottieAnimationView]. */
    var imageDrawable: Drawable? = null
        set(value) {
            if (field != value) {
                resetImageResourceCache(except = TYPE_DRAWABLE)
                field = value
                notifyItemChanged()
            }
        }

    /** The image uri to display in [LottieAnimationView]. */
    var imageUri: Uri? = null
        set(value) {
            if (field != value) {
                resetImageResourceCache(except = TYPE_URI)
                field = value
                notifyItemChanged()
            }
        }

    constructor() : super()

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null) : super(context, attrs) {
        context.withStyledAttributes(attrs, R.styleable.IllustrationItem) {
            imageDrawable = getDrawable(R.styleable.IllustrationItem_android_drawable)
        }
    }

    override fun getDefaultLayoutResource() = R.layout.setup_illustration_item

    override fun onBindView(view: View) {
        val illustrationView = view.findViewById<LottieAnimationView>(R.id.sud_item_illustration)
        handleImageWithAnimation(illustrationView)

        view.contentDescription = contentDescription
    }

    /**
     * IllustrationItem is set as GroupDivider to remove the default item background that are set in
     * ListView and RecyclerViews for all the items that are not group divider.
     */
    override fun isGroupDivider(): Boolean = true

    /**
     * This is disabled to remove the touch feedback for imageView items. If there is any touch
     * event for the IllustrationItem, override this method to set isEnabled() true
     */
    override fun isEnabled(): Boolean = false

    /** Clears all other resources except the one currently being set. */
    private fun resetImageResourceCache(except: String) {
        if (except != TYPE_DRAWABLE) imageDrawable = null
        if (except != TYPE_URI) imageUri = null
    }

    private fun handleImageWithAnimation(illustrationView: LottieAnimationView) {
        imageDrawable?.let { drawable -> illustrationView.setImageDrawable(drawable) }
        imageUri?.let { uri -> illustrationView.setImageURI(uri) }
    }

    companion object {
        private const val TYPE_DRAWABLE = "drawable"
        private const val TYPE_URI = "uri"
    }
}
