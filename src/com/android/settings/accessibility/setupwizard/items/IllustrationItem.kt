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
import android.view.View.AccessibilityDelegate
import android.view.View.IMPORTANT_FOR_ACCESSIBILITY_YES
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction
import androidx.annotation.RawRes
import androidx.core.content.withStyledAttributes
import androidx.core.view.postDelayed
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.android.settings.R
import com.android.settingslib.widget.preference.illustration.R as IllustrationR
import com.google.android.setupcompat.util.DelightHelper
import com.google.android.setupdesign.R as SetupR
import com.google.android.setupdesign.items.Item
import com.google.android.setupdesign.util.LottieAnimationHelper
import com.google.android.setupdesign.util.ThemeHelper
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

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

    /** The callback invoked when the [LottieAnimationView] is being bound. */
    var onBindListener: OnBindListener? = null

    /** Callback to be invoked when the animation view is bound to its View Holder. */
    fun interface OnBindListener {
        /**
         * Called when when [.onBindViewHolder] occurs.
         *
         * @param animationView the animation view for this preference.
         */
        fun onBind(animationView: LottieAnimationView?)
    }

    internal var isLottieAnimation = false
        private set

    private var isLottieAnimationPaused = false
    private var pendingAnimationRunnable: Runnable? = null

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

        if (!contentDescription.isNullOrBlank()) {
            illustrationView.contentDescription = contentDescription
            illustrationView.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        }
        handleImageWithAnimation(illustrationView)
        handleAnimationControl(illustrationView)

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

        onBindListener?.onBind(illustrationView)
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
        resetAnimations(illustrationView)
        isLottieAnimation = false
        isLottieAnimationPaused = false

        when {
            imageDrawable != null -> {
                illustrationView.setImageDrawable(imageDrawable)
                illustrationView.visibility = View.VISIBLE
                executeWithThemeDelay(illustrationView) { startAnimatableDrawable(imageDrawable) }
            }
            imageUri != null -> {
                illustrationView.setImageURI(imageUri)
                illustrationView.visibility = View.VISIBLE
                val drawable = illustrationView.drawable
                if (drawable != null) {
                    executeWithThemeDelay(illustrationView) { startAnimatableDrawable(drawable) }
                } else {
                    // The lottie image from the raw folder also returns null because the
                    // ImageView
                    isLottieAnimation = true
                    illustrationView.prepareAnimation(imageUri!!)
                    executeWithThemeDelay(illustrationView) { illustrationView.playAnimation() }
                }
            }
            imageResId != 0 -> {
                if (!isRawResourceValid(illustrationView, imageResId)) {
                    illustrationView.visibility = View.GONE
                    return
                }
                illustrationView.setImageResource(imageResId)
                illustrationView.visibility = View.VISIBLE
                val drawable = illustrationView.drawable
                if (drawable != null) {
                    executeWithThemeDelay(illustrationView) { startAnimatableDrawable(drawable) }
                } else {
                    // The lottie image from the raw folder also returns null because the
                    // ImageView
                    isLottieAnimation = true
                    illustrationView.prepareAnimation(imageResId)
                    executeWithThemeDelay(illustrationView) { illustrationView.playAnimation() }
                }
            }
        }
    }

    private fun LottieAnimationView.prepareAnimation(uri: Uri) {
        val inputStream: InputStream =
            try {
                getInputStreamFromUri(context, uri) ?: return
            } catch (e: IOException) {
                Log.e(TAG, "Error opening InputStream from URI: $uri", e)
                return
            }
        apply {
            setAnimation(inputStream, null)
            repeatCount = LottieDrawable.INFINITE
            setFailureListener { throwable ->
                Log.w(TAG, "Failed to load Lottie animation from URI: $uri", throwable)
            }
        }
    }

    private fun LottieAnimationView.prepareAnimation(@RawRes resId: Int) {
        apply {
            setAnimation(resId)
            repeatCount = LottieDrawable.INFINITE
            setFailureListener { throwable ->
                Log.w(TAG, "Failed to load Lottie animation from resource ID: $resId", throwable)
            }
        }
    }

    private fun executeWithThemeDelay(illustrationView: LottieAnimationView, action: () -> Unit) {
        val context = illustrationView.context

        if (DelightHelper.shouldApplyAnimatedIcon(context)) {
            val delay =
                context.resources.getInteger(SetupR.integer.sud_lottie_animation_delay_ms).toLong()
            pendingAnimationRunnable = illustrationView.postDelayed(delay, action)
        } else {
            action()
        }
    }

    /** Enable pause and resume abilities to animation only. */
    private fun handleAnimationControl(illustrationView: LottieAnimationView) {
        if (!isLottieAnimation) {
            return
        }

        // TODO(b/397340540): list out pages having illustration without a content description.
        if (contentDescription.isNullOrBlank()) {
            // Default content description will be attached if there's no content description.
            illustrationView.apply {
                contentDescription =
                    context.getString(
                        IllustrationR.string.settingslib_illustration_content_description
                    )
            }
            Log.w(TAG, "Animated illustration should have a content description")
        }

        illustrationView.setOnClickListener {
            isLottieAnimationPaused = !isLottieAnimationPaused
            if (isLottieAnimationPaused) {
                illustrationView.pauseAnimation()
            } else {
                illustrationView.resumeAnimation()
            }
            updateAccessibilityAction(illustrationView)
        }
        updateAccessibilityAction(illustrationView)
    }

    private fun updateAccessibilityAction(illustrationView: LottieAnimationView) {
        illustrationView.setAccessibilityDelegate(
            object : AccessibilityDelegate() {
                override fun onInitializeAccessibilityNodeInfo(
                    host: View,
                    info: AccessibilityNodeInfo,
                ) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    val clickAction =
                        AccessibilityAction(
                            AccessibilityNodeInfo.ACTION_CLICK,
                            getActionLabelForAnimation(host.context),
                        )
                    info.addAction(clickAction)
                }
            }
        )
    }

    private fun getActionLabelForAnimation(context: Context) =
        if (isLottieAnimationPaused) {
            context.getString(IllustrationR.string.settingslib_action_label_resume)
        } else {
            context.getString(IllustrationR.string.settingslib_action_label_pause)
        }

    private fun getInputStreamFromUri(context: Context, uri: Uri): InputStream? {
        try {
            return context.contentResolver.openInputStream(uri)
        } catch (e: FileNotFoundException) {
            Log.w(TAG, "Cannot find content uri: $uri", e)
            return null
        }
    }

    private fun resetAnimations(illustrationView: LottieAnimationView) {
        pendingAnimationRunnable?.let { illustrationView.removeCallbacks(it) }
        resetAnimation(illustrationView.drawable)
        illustrationView.cancelAnimation()
    }

    private fun resetAnimation(drawable: Drawable?) {
        if (drawable !is Animatable) {
            return
        }

        when (drawable) {
            is Animatable2 -> drawable.clearAnimationCallbacks()
            is Animatable2Compat -> drawable.clearAnimationCallbacks()
        }

        (drawable as Animatable).stop()
    }

    private fun startAnimatableDrawable(drawable: Drawable?) {
        if (drawable !is Animatable) {
            return
        }

        when (drawable) {
            is Animatable2 -> drawable.registerAnimationCallback(animationCallback)
            is Animatable2Compat -> drawable.registerAnimationCallback(animationCallbackCompat)
            is AnimationDrawable -> drawable.isOneShot = false
        }

        (drawable as Animatable).start()
    }

    private fun isRawResourceValid(view: View, resId: Int): Boolean {
        return try {
            view.resources.openRawResource(resId).use {
                // -1 = end of stream. if first read is end of stream, then file is empty
                it.read() != -1
            }
        } catch (e: IOException) {
            Log.w(TAG, "Unable to open resource: $resId", e)
            false
        }
    }

    private val animationCallback: Animatable2.AnimationCallback =
        object : Animatable2.AnimationCallback() {
            override fun onAnimationEnd(drawable: Drawable) {
                (drawable as Animatable).start()
            }
        }

    private val animationCallbackCompat: Animatable2Compat.AnimationCallback =
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
