/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.connecteddevice.display

import com.android.settings.R

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.widget.FrameLayout

import androidx.annotation.VisibleForTesting

/** Represents a draggable block in the topology pane. */
class DisplayBlock(context : Context) : FrameLayout(context) {
    @VisibleForTesting
    val mHighlightPx = context.resources.getDimensionPixelSize(
            R.dimen.display_block_highlight_width)

    val mWallpaperView = View(context)
    private val mBackgroundView = View(context).apply {
        background = context.getDrawable(R.drawable.display_block_background)
    }
    @VisibleForTesting
    val mSelectionMarkerView = View(context).apply {
        background = context.getDrawable(R.drawable.display_block_selection_marker_background)
    }

    init {
        isScrollContainer = false
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false

        // Prevents shadow from appearing around edge of button.
        stateListAnimator = null

        addView(mWallpaperView)
        addView(mBackgroundView)
        addView(mSelectionMarkerView)
    }

    fun setWallpaper(wallpaper: Bitmap?) {
        mWallpaperView.background = BitmapDrawable(context.resources, wallpaper ?: return)
    }

    /**
     * The coordinates of the upper-left corner of the block in pane coordinates, not including the
     * highlight border.
     */
    var positionInPane: PointF
        get() = PointF(x + mHighlightPx, y + mHighlightPx)
        set(value: PointF) {
            x = value.x - mHighlightPx
            y = value.y - mHighlightPx
        }

    fun setHighlighted(value: Boolean) {
        mSelectionMarkerView.visibility = if (value) View.VISIBLE else View.INVISIBLE

        // The highlighted block must be draw last so that its highlight shows over the borders of
        // other displays.
        z = if (value) 2f else 1f
    }

    /** Sets position and size of the block given unpadded bounds. */
    fun placeAndSize(bounds : RectF, scale : TopologyScale) {
        val topLeft = scale.displayToPaneCoor(bounds.left, bounds.top)
        val bottomRight = scale.displayToPaneCoor(bounds.right, bounds.bottom)
        val layout = layoutParams
        val newWidth = (bottomRight.x - topLeft.x).toInt()
        val newHeight = (bottomRight.y - topLeft.y).toInt()
        layout.width = newWidth + 2*mHighlightPx
        layout.height = newHeight + 2*mHighlightPx
        layoutParams = layout
        positionInPane = topLeft

        // The highlight is the outermost border. The highlight is shown outside of the parent
        // FrameLayout so that it consumes the padding between the blocks.
        mWallpaperView.layoutParams.let {
            it.width = newWidth
            it.height = newHeight
            if (it is MarginLayoutParams) {
                it.leftMargin = mHighlightPx
                it.topMargin = mHighlightPx
                it.bottomMargin = mHighlightPx
                it.topMargin = mHighlightPx
            }
            mWallpaperView.layoutParams = it
        }

        // The other two child views are MATCH_PARENT by default so will resize to fill up the
        // FrameLayout.
    }
}
