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

package com.android.settings.network.telephony.satellite

import android.annotation.AttrRes
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.LayoutDirection
import android.util.Log
import android.util.TypedValue
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.android.settingslib.graph.SignalDrawable
import kotlin.math.roundToInt

/** A drawable for satellite icon with a specific satellite word. */
class SatelliteSettingIconDrawable(
    private val context: Context,
    private val satText: String =
        context.getString(com.android.internal.R.string.satellite_indicator),
) : Drawable() {
    private val defaultColor: Int = context.resolveColor(android.R.attr.colorControlNormal)

    private val textPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            // In some languages, images may be cropped if the value set over than 9f.
            textSize = dpToPx(9f).toFloat()
            color = defaultColor
            textAlign = Paint.Align.LEFT
            typeface = Typeface.create("google-sans-bold", Typeface.BOLD)
        }

    private val textBounds = Rect()
    private val fontMetrics = Paint.FontMetrics()
    private val signalDrawable = SignalDrawable(context)
    private val intrinsicSize = dpToPx(24f)
    private val gap = dpToPx(3f) // The gap between SAT text and Signal bars

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        if (bounds.isEmpty) {
            Log.d(TAG, "No Bounds!")
            return
        }

        // --- Text Sizing ---
        textPaint.getFontMetrics(fontMetrics)
        textPaint.getTextBounds(satText, 0, satText.length, textBounds)

        val textHeight = textBounds.height()
        val textWidth = textPaint.measureText(satText)

        // Signal height is text height + 3dp
        val signalHeight = textHeight + dpToPx(3f)
        // Keep aspect ratio square for signal drawable
        val signalWidth = signalHeight
        // Get base text position.
        val textBaselineY = bounds.centerY() - (fontMetrics.ascent + fontMetrics.descent) / 2f

        // Center Content
        val contentStartX = bounds.centerX() - ((textWidth + gap + signalWidth) / 2f)

        // --- SignalDrawable Sizing and Positioning ---
        val signalBottom = bounds.top + textBaselineY.roundToInt()
        val signalTop = signalBottom - signalHeight
        if (layoutDirection == LayoutDirection.RTL) {
            val signalLeft = contentStartX
            val signalRight = signalLeft + signalWidth
            // Set SignalDrawable bounds within the right partition
            signalDrawable.setBounds(
                signalLeft.toInt(),
                signalTop,
                signalRight.toInt(),
                signalBottom,
            )
            canvas.drawText(satText, contentStartX + signalWidth + gap, textBaselineY, textPaint)
        } else {
            val signalLeft = contentStartX + textWidth + gap
            val signalRight = signalLeft + signalWidth
            // Set SignalDrawable bounds within the right partition
            signalDrawable.setBounds(
                signalLeft.toInt(),
                signalTop,
                signalRight.toInt(),
                signalBottom,
            )
            canvas.drawText(satText, contentStartX, textBaselineY, textPaint)
        }
        // --- Draw SignalDrawable ---
        signalDrawable.draw(canvas)
    }

    override fun setAlpha(alpha: Int) {
        textPaint.alpha = alpha
        signalDrawable.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        textPaint.colorFilter = colorFilter
        signalDrawable.colorFilter = colorFilter
        invalidateSelf()
    }

    override fun getColorFilter(): ColorFilter? = textPaint.colorFilter

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun getIntrinsicWidth(): Int = intrinsicSize

    override fun getIntrinsicHeight(): Int = intrinsicSize

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.resources.displayMetrics,
            )
            .roundToInt()
    }

    /**
     * Resolves a color attribute defined in the current theme.
     *
     * @param attr The attribute resource ID to resolve (e.g., R.attr.colorPrimary).
     * @return The resolved color integer. Returns [Color.GRAY] as a fallback if the attribute
     *   cannot be resolved or is not a color.
     */
    @ColorInt
    fun Context.resolveColor(@AttrRes attr: Int): Int {
        // 1. Create a holder for the resolved value
        val typedValue = TypedValue()
        // 2. Try to find the attribute in the current theme.
        // The 'true' parameter ensures the system follows references to find the actual value.
        if (this.theme.resolveAttribute(attr, typedValue, true)) {
            // 3. Check if the value is a raw color (e.g., #FFFFFF) defined directly in the theme
            if (
                typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT &&
                    typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT
            ) {
                return typedValue.data
            }
            // 4. Check if the value is a reference to a color resource (e.g., @color/blue)
            if (typedValue.resourceId != 0) {
                return ContextCompat.getColor(this, typedValue.resourceId)
            }
        }

        // 5. Fallback value if the attribute is missing or invalid
        return Color.GRAY
    }

    companion object {
        private const val TAG = "SatelliteIconDrawable"
    }
}
