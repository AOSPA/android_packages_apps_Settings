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
    val highlightPx = context.resources.getDimensionPixelSize(R.dimen.display_block_highlight_width)
    val cornerRadiusPx =
        context.resources.getDimensionPixelSize(R.dimen.display_block_corner_radius)

    // Id of the logical display this DisplayBlock represents
    var logicalDisplayId: Int = -1
        private set

    // This doesn't necessarily refer to the actual display this block represents. In case of
    // mirroring, it will be the id of the mirrored display
    private var displayIdToShowWallpaper: Int? = null

    /** Scale of the mirrored wallpaper to the actual wallpaper size. */
    private var surfaceScale: Float? = null

    // These are surfaces which must be removed from the display block hierarchy and released once
    // the new surface is put in place. This list can have more than one item because we may get
    // two reset calls before we get a single surfaceChange callback.
    private val oldSurfaces = mutableListOf<SurfaceControl>()
    private var wallpaperSurface: SurfaceControl? = null

    private val updateSurfaceView = Runnable { updateSurfaceView() }

    @VisibleForTesting
    fun updateSurfaceView() {
        val displayId = displayIdToShowWallpaper ?: return

        if (parent == null) {
            Log.i(TAG, "View for display $displayId has no parent - cancelling update")
            return
        }

        var surface = wallpaperSurface
        if (surface == null) {
            surface = injector.wallpaper(displayId)
            if (surface == null) {
                injector.handler.postDelayed(updateSurfaceView, /* delayMillis= */ 500)
                return
            }
            wallpaperSurface = surface
        }

        val surfaceScale = surfaceScale ?: return
        injector.updateSurfaceView(oldSurfaces, surface, wallpaperView, surfaceScale)
        oldSurfaces.clear()
    }

    private val holderCallback =
        object : SurfaceHolder.Callback {
            override fun surfaceCreated(h: SurfaceHolder) {}

            override fun surfaceChanged(
                h: SurfaceHolder,
                format: Int,
                newWidth: Int,
                newHeight: Int,
            ) {
                updateSurfaceView()
            }

            override fun surfaceDestroyed(h: SurfaceHolder) {}
        }

    val wallpaperView = SurfaceView(context)
    private val backgroundView =
        View(context).apply {
            background = context.getDrawable(R.drawable.display_block_background)
        }
    @VisibleForTesting
    val selectionMarkerView =
        View(context).apply {
            background = context.getDrawable(R.drawable.display_block_selection_marker_background)
        }

    init {
        isScrollContainer = false
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false

        // Prevents shadow from appearing around edge of button.
        stateListAnimator = null

        addView(wallpaperView)
        addView(backgroundView)
        addView(selectionMarkerView)

        wallpaperView.holder.addCallback(holderCallback)
    }

    /**
     * The coordinates of the upper-left corner of the block in pane coordinates, not including the
     * highlight border.
     */
    var positionInPane: PointF
        get() = PointF(x + highlightPx, y + highlightPx)
        set(value: PointF) {
            x = value.x - highlightPx
            y = value.y - highlightPx
        }

    fun setHighlighted(value: Boolean) {
        selectionMarkerView.visibility = if (value) VISIBLE else INVISIBLE

        // The highlighted block must be draw last so that its highlight shows over the borders of
        // other displays.
        z = if (value) 2f else 1f
    }

    /**
     * Sets position and size of the block given coordinates in pane space.
     *
     * @param logicalDisplayId ID of the logical display this DisplayBlock represents
     * @param displayIdToShowWallpaper ID of the display whose wallpaper would be projected on this
     *  display block.
     * @param topLeft coordinates of top left corner of the block, not including highlight border
     * @param bottomRight coordinates of bottom right corner of the block, not including highlight
     *   border
     * @param surfaceScale scale in pixels of the size of the wallpaper mirror to the actual
     *   wallpaper on the screen - should be less than one to indicate scaling to smaller size
     */
    fun reset(
        logicalDisplayId: Int,
        displayIdToShowWallpaper: Int,
        topLeft: PointF,
        bottomRight: PointF,
        surfaceScale: Float,
    ) {
        wallpaperSurface?.let { oldSurfaces.add(it) }
        injector.handler.removeCallbacks(updateSurfaceView)
        wallpaperSurface = null
        positionInPane = topLeft

        this.logicalDisplayId = logicalDisplayId
        this.displayIdToShowWallpaper = displayIdToShowWallpaper
        this.surfaceScale = surfaceScale

        val newWidth = (bottomRight.x - topLeft.x).toInt()
        val newHeight = (bottomRight.y - topLeft.y).toInt()

        val paddedWidth = newWidth + 2 * highlightPx
        val paddedHeight = newHeight + 2 * highlightPx

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
        wallpaperView.layoutParams.let {
            it.width = newWidth
            it.height = newHeight
            if (it is MarginLayoutParams) {
                it.leftMargin = highlightPx
                it.topMargin = highlightPx
                it.bottomMargin = highlightPx
                it.topMargin = highlightPx
            }
            wallpaperView.layoutParams = it
        }

        wallpaperView.outlineProvider =
            object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, cornerRadiusPx.toFloat())
                }
            }
        wallpaperView.clipToOutline = true

        // The other two child views are MATCH_PARENT by default so will resize to fill up the
        // FrameLayout.
    }

    private companion object {
        private const val TAG = "DisplayBlock"
    }
}
