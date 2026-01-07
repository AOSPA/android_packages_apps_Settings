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
import android.graphics.drawable.Animatable
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.withStyledAttributes
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.android.settings.R
import com.google.android.setupdesign.items.Item
import com.google.android.setupdesign.util.LottieAnimationHelper
import com.google.android.setupdesign.util.ThemeHelper

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

    /** The lottie illustration resource id to display in [LottieAnimationView]. */
    var imageResId: Int = 0
        set(value) {
            if (field != value) {
                resetImageResourceCache(except = TYPE_RESOURCE_ID)
                field = value
                notifyItemChanged()
            }
        }

    constructor() : super()

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null) : super(context, attrs) {
        context.withStyledAttributes(attrs, R.styleable.IllustrationItem) {
            imageDrawable = getDrawable(R.styleable.IllustrationItem_android_drawable)
            imageResId = getResourceId(R.styleable.IllustrationItem_lottie_rawRes, 0)
        }
    }

    override fun getDefaultLayoutResource() = R.layout.setup_illustration_item

    override fun onBindView(view: View) {
        val context = view.context
        val illustrationView = view.findViewById<LottieAnimationView>(R.id.sud_item_illustration)
        handleImageWithAnimation(illustrationView)

        if (ThemeHelper.shouldApplyGlifExpressiveStyle(context)) {
            LottieAnimationHelper.get()
                .applyColor(
                    context,
                    illustrationView,
                    context.resources
                        .getStringArray(R.array.layout_animated_banner_customization)
                        .toList(),
                )
        }

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
        if (except != TYPE_RESOURCE_ID) imageResId = 0
    }

    private fun handleImageWithAnimation(illustrationView: LottieAnimationView) {
        imageDrawable?.let { drawable -> illustrationView.setImageDrawable(drawable) }
        imageUri?.let { uri -> illustrationView.setImageURI(uri) }
        if (imageResId != 0) {
            illustrationView.setImageResource(imageResId)
            val drawable = illustrationView.getDrawable()
            if (drawable != null) {
                startAnimation(drawable)
            } else {
                // The lottie image from the raw folder also returns null because the ImageView
                // couldn't handle it now.
                illustrationView.setFailureListener { result: Throwable? ->
                    Log.w(TAG, "Invalid illustration resource id: $imageResId", result)
                }
                illustrationView.setAnimation(imageResId)
                illustrationView.setRepeatCount(LottieDrawable.INFINITE)
                illustrationView.playAnimation()
            }
        }
    }

    private fun startAnimation(drawable: Drawable?) {
        if (drawable !is Animatable) {
            return
        }

        when (drawable) {
            is Animatable2 -> drawable.registerAnimationCallback(mAnimationCallback)
            is Animatable2Compat -> drawable.registerAnimationCallback(mAnimationCallbackCompat)
            is AnimationDrawable -> drawable.isOneShot = false
        }

        (drawable as Animatable).start()
    }

    private val mAnimationCallback: Animatable2.AnimationCallback =
        object : Animatable2.AnimationCallback() {
            override fun onAnimationEnd(drawable: Drawable) {
                (drawable as Animatable).start()
            }
        }

    private val mAnimationCallbackCompat: Animatable2Compat.AnimationCallback =
        object : Animatable2Compat.AnimationCallback() {
            override fun onAnimationEnd(drawable: Drawable) {
                (drawable as Animatable).start()
            }
        }

    companion object {
        private const val TAG = "IllustrationItem"
        private const val TYPE_DRAWABLE = "drawable"
        private const val TYPE_URI = "uri"
        private const val TYPE_RESOURCE_ID = "resId"
    }
}
