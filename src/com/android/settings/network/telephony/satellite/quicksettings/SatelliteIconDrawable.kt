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

package com.android.settings.network.telephony.satellite.quicksettings

import android.content.Context
import android.content.res.ColorStateList
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
import androidx.annotation.ColorInt
import androidx.annotation.VisibleForTesting
import com.android.settings.R
import com.android.settingslib.graph.SignalDrawable

/**
 * A custom drawable for the satellite icon.
 *
 * This drawable renders a "pill" shape containing the text "SAT" and a signal strength indicator.
 * It is used primarily on the Satellite Landing Page.
 *
 * @property context The context used to access resources and display metrics.
 */
class SatelliteIconDrawable(private val context: Context) : Drawable() {

    private val text: String = context.getString(com.android.internal.R.string.satellite_indicator)

    private val signalDrawable: SignalDrawable? =
        try {
            SignalDrawable(context).apply {
                // Level 4 out of 4 (Full Signal)
                level = SignalDrawable.getState(4, 4, false)
                setDarkIntensity(1f)
                setTintList(ColorStateList.valueOf(ICON_COLOR_DEFAULT))
            }
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Failed to initialize SignalDrawable.", e)
            null
        }

    private val textPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = context.resources.displayMetrics.density * TEXT_SIZE_SP
            typeface = Typeface.create(FONT_FAMILY_GOOGLE_SANS_BOLD, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
            color = ICON_COLOR_DEFAULT
        }

    private val backgroundPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = BACKGROUND_COLOR_DEFAULT }

    private val density: Float = context.resources.displayMetrics.density
    private val spacing: Float = density * SPACING_DP
    private val textBounds = Rect()

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        val radius = bounds.height() / 2f

        // Draw Pill Background
        canvas.drawRoundRect(
            bounds.left.toFloat(),
            bounds.top.toFloat(),
            bounds.right.toFloat(),
            bounds.bottom.toFloat(),
            radius,
            radius,
            backgroundPaint,
        )

        // Measure Content
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        val textHeight = textBounds.height()
        val textWidth = textPaint.measureText(text)

        // Signal height is text height + 3dp
        val signalHeight = textHeight + (3f * density)
        // Keep aspect ratio square for signal drawable
        val signalWidth = signalHeight

        val totalContentWidth = textWidth + spacing + signalWidth

        // Center Content
        val contentStartX = bounds.centerX() - (totalContentWidth / 2f)
        val centerY = bounds.centerY().toFloat()

        // Center text vertically
        val textY = centerY - (textBounds.top + textBounds.bottom) / 2f

        // Align the bottom of the signal drawable with the visual bottom of the text
        val signalBottom = textY + textBounds.bottom
        val signalTop = signalBottom - signalHeight

        // Determine X positions based on layout direction
        val textX: Float
        val signalLeft: Float

        val isRtl = layoutDirection == LayoutDirection.RTL
        if (isRtl) {
            // For Right-to-Left (RTL) languages (like Arabic or Hebrew)
            // RTL: Signal -> Spacing -> Text
            signalLeft = contentStartX
            textX = contentStartX + signalWidth + spacing
        } else {
            // LTR: Text -> Spacing -> Signal
            textX = contentStartX
            signalLeft = contentStartX + textWidth + spacing
        }

        // Draw Text
        canvas.drawText(text, textX, textY, textPaint)

        // Draw Signal
        signalDrawable?.let {
            it.setBounds(
                signalLeft.toInt(),
                signalTop.toInt(),
                (signalLeft + signalWidth).toInt(),
                (signalTop + signalHeight).toInt(),
            )
            it.draw(canvas)
        }
    }

    override fun setAlpha(alpha: Int) {
        textPaint.alpha = alpha
        backgroundPaint.alpha = alpha
        signalDrawable?.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        textPaint.colorFilter = colorFilter
        backgroundPaint.colorFilter = colorFilter
        signalDrawable?.colorFilter = colorFilter
        invalidateSelf()
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun setTintList(tint: ColorStateList?) {
        super.setTintList(tint)
        if (tint != null) {
            val color = tint.getColorForState(state, tint.defaultColor)
            backgroundPaint.color = color

            // Adjust content color for contrast
            val isDarkBackground = Color.luminance(color) < 0.5
            val contentColor = if (isDarkBackground) Color.WHITE else ICON_COLOR_DEFAULT

            textPaint.color = contentColor
            signalDrawable?.setTintList(ColorStateList.valueOf(contentColor))
        }
        invalidateSelf()
    }

    override fun onLayoutDirectionChanged(layoutDirection: Int): Boolean {
        signalDrawable?.layoutDirection = layoutDirection
        invalidateSelf()
        return true
    }

    override fun getIntrinsicWidth(): Int {
        return (INTRINSIC_WIDTH_DP * density).toInt()
    }

    override fun getIntrinsicHeight(): Int {
        return (INTRINSIC_HEIGHT_DP * density).toInt()
    }

    companion object {
        private const val TAG = "SatelliteIconDrawable"
        private const val FONT_FAMILY_GOOGLE_SANS_BOLD = "google-sans-bold"
        // Dark blue color for text and icons
        @ColorInt private val ICON_COLOR_DEFAULT = 0xFF001324.toInt()
        // Light blue color for the background pill
        @ColorInt private val BACKGROUND_COLOR_DEFAULT = 0xFFA8C7FA.toInt()

        private const val TEXT_SIZE_SP = 14f
        private const val SPACING_DP = 3f

        // 73:32 Aspect Ratio
        @VisibleForTesting const val INTRINSIC_WIDTH_DP = 73
        @VisibleForTesting const val INTRINSIC_HEIGHT_DP = 32
    }
}
