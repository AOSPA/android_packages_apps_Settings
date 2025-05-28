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

import android.graphics.Outline
import android.graphics.PointF
import android.util.Log
import android.view.SurfaceControl
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import androidx.annotation.VisibleForTesting
import com.android.settings.R

/** Represents a draggable block in the topology pane. */
class DisplayBlock(val injector: ConnectedDisplayInjector) : FrameLayout(injector.context!!) {
    @VisibleForTesting
    val mHighlightPx = context.resources.getDimensionPixelSize(
            R.dimen.display_block_highlight_width)
    val cornerRadiusPx = context.resources.getDimensionPixelSize(
        R.dimen.display_block_corner_radius)

    private var mDisplayId: Int? = null

    /** Scale of the mirrored wallpaper to the actual wallpaper size. */
    private var mSurfaceScale: Float? = null

    val displayId: Int?
        get() = mDisplayId

    // These are surfaces which must be removed from the display block hierarchy and released once
    // the new surface is put in place. This list can have more than one item because we may get
    // two reset calls before we get a single surfaceChange callback.
    private val mOldSurfaces = mutableListOf<SurfaceControl>()
    private var mWallpaperSurface: SurfaceControl? = null

    private val mUpdateSurfaceView = Runnable { updateSurfaceView() }

    @VisibleForTesting fun updateSurfaceView() {
        val displayId = mDisplayId ?: return

        if (parent == null) {
            Log.i(TAG, "View for display $displayId has no parent - cancelling update")
            return
        }

        var surface = mWallpaperSurface
        if (surface == null) {
            surface = injector.wallpaper(displayId)
            if (surface == null) {
                injector.handler.postDelayed(mUpdateSurfaceView, /* delayMillis= */ 500)
                return
            }
            mWallpaperSurface = surface
        }

        val surfaceScale = mSurfaceScale ?: return
        injector.updateSurfaceView(mOldSurfaces, surface, mWallpaperView, surfaceScale)
        mOldSurfaces.clear()
    }

    private val mHolderCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(h: SurfaceHolder) {}

        override fun surfaceChanged(h: SurfaceHolder, format: Int, newWidth: Int, newHeight: Int) {
            updateSurfaceView()
        }

        override fun surfaceDestroyed(h: SurfaceHolder) {}
    }

    val mWallpaperView = SurfaceView(context)
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

        mWallpaperView.holder.addCallback(mHolderCallback)
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

    /**
     * Sets position and size of the block given coordinates in pane space.
     *
     * @param displayId ID of display this block represents, needed for fetching wallpaper
     * @param topLeft coordinates of top left corner of the block, not including highlight border
     * @param bottomRight coordinates of bottom right corner of the block, not including highlight
     *                    border
     * @param surfaceScale scale in pixels of the size of the wallpaper mirror to the actual
     *                     wallpaper on the screen - should be less than one to indicate scaling to
     *                     smaller size
     */
    fun reset(displayId: Int, topLeft: PointF, bottomRight: PointF, surfaceScale: Float) {
        mWallpaperSurface?.let { mOldSurfaces.add(it) }
        injector.handler.removeCallbacks(mUpdateSurfaceView)
        mWallpaperSurface = null
        setHighlighted(false)
        positionInPane = topLeft

        mDisplayId = displayId
        mSurfaceScale = surfaceScale

        val newWidth = (bottomRight.x - topLeft.x).toInt()
        val newHeight = (bottomRight.y - topLeft.y).toInt()

        val paddedWidth = newWidth + 2*mHighlightPx
        val paddedHeight = newHeight + 2*mHighlightPx

        if (width == paddedWidth && height == paddedHeight) {
            // Will not receive a surfaceChanged callback, so in case the wallpaper is different,
            // apply it.
            updateSurfaceView()
            return
        }

        layoutParams.let {
            it.width = paddedWidth
            it.height = paddedHeight
            layoutParams = it
        }

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

        mWallpaperView.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, cornerRadiusPx.toFloat())
            }
        }
        mWallpaperView.clipToOutline = true

        // The other two child views are MATCH_PARENT by default so will resize to fill up the
        // FrameLayout.
    }

    private companion object {
        private const val TAG = "DisplayBlock"
    }
}
