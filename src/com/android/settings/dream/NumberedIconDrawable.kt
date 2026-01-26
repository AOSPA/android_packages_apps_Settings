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

package com.android.settings.dream

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.annotation.ColorRes
import com.android.settings.R
import kotlin.math.min

/** A drawable that displays a number inside a circle. */
class NumberedIconDrawable(context: Context, number: Int, @ColorRes textColorResId: Int) :
    Drawable() {

    private val numberText: String = number.toString()
    private val textBounds = Rect()

    private val defaultColor = context.getColor(com.android.internal.R.color.materialColorOnSurface)
    private val textColorList: ColorStateList = context.getColorStateList(textColorResId)
    private var tintList: ColorStateList? = null

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private val textPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColorList.defaultColor
            textAlign = Paint.Align.CENTER
            textSize = context.resources.getDimensionPixelSize(R.dimen.dream_item_icon_size) / 2f
            isFakeBoldText = true
        }

    init {
        textPaint.getTextBounds(numberText, 0, numberText.length, textBounds)
        updateCirclePaintColor()
    }

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        if (bounds.isEmpty) {
            return
        }

        val centerX = bounds.exactCenterX()
        val centerY = bounds.exactCenterY()
        val radius = min(bounds.width(), bounds.height()) / 2f

        canvas.drawCircle(centerX, centerY, radius, circlePaint)

        // Center the text in the circle.
        val textY = centerY - textBounds.exactCenterY()
        canvas.drawText(numberText, centerX, textY, textPaint)
    }

    override fun setAlpha(alpha: Int) {
        if (circlePaint.alpha != alpha) {
            circlePaint.alpha = alpha
            invalidateSelf()
        }
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        circlePaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java") override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun setTintList(tint: ColorStateList?) {
        tintList = tint
        if (updateCirclePaintColor()) {
            invalidateSelf()
        }
    }

    override fun onStateChange(state: IntArray): Boolean {
        if (updateCirclePaintColor()) {
            invalidateSelf()
            return true
        }
        return false
    }

    override fun isStateful(): Boolean = tintList?.isStateful ?: false

    private fun updateCirclePaintColor(): Boolean {
        val colorForState = tintList?.getColorForState(state, defaultColor) ?: defaultColor
        textPaint.color = textColorList.getColorForState(state, textColorList.defaultColor)
        if (colorForState != circlePaint.color) {
            circlePaint.color = colorForState
            return true
        }
        return false
    }
}
