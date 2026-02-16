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

import android.content.Context
import android.graphics.Outline
import android.graphics.PointF
import android.graphics.Rect
import android.util.Size
import android.view.SurfaceView
import android.view.TouchDelegate
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import androidx.annotation.VisibleForTesting
import com.android.settings.R

/** Represents a draggable block in the topology pane. */
class DisplayBlock(uiContext: Context, val injector: ConnectedDisplayInjector) :
    FrameLayout(uiContext) {
    @VisibleForTesting
    internal val cornerRadiusPx =
        context.resources.getDimensionPixelSize(R.dimen.display_block_corner_radius)

    private val highlightPx =
        context.resources.getDimensionPixelSize(R.dimen.display_block_highlight_width)

    private val horizontalArrowMargin: Int
        get() =
            if (
                a11yController.areArrowsVisible &&
                    (a11yController.isMovable(Direction.LEFT) ||
                        a11yController.isMovable(Direction.RIGHT))
            )
                a11yController.arrowSizePx
            else 0

    private val verticalArrowMargin: Int
        get() =
            if (
                a11yController.areArrowsVisible &&
                    (a11yController.isMovable(Direction.UP) ||
                        a11yController.isMovable(Direction.DOWN))
            )
                a11yController.arrowSizePx
            else 0

    // Id of the logical display this DisplayBlock represents
    val logicalDisplayId: Int
        get() = surfaceRenderer.logicalDisplayId

    private val layoutChangeListener = OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        touchDelegate =
            TouchDelegateComposite(this).apply {
                arrowButtons.values.forEach { arrowButton ->
                    // Get the button's bounds relative to this DisplayBlock
                    val bounds = Rect()
                    arrowButton.getHitRect(bounds)

                    // Expand the bounds to at least arrowTappableAreaSizePx, centered on the
                    // button's current center
                    val width = bounds.width()
                    val height = bounds.height()

                    val extraWidth =
                        (a11yController.arrowTappableAreaSizePx - width).coerceAtLeast(0)
                    val extraHeight =
                        (a11yController.arrowTappableAreaSizePx - height).coerceAtLeast(0)

                    // Only add a TouchDelegate if the bounds need to be expanded.
                    if (extraWidth > 0 || extraHeight > 0) {
                        // inset by negative to expands the rect
                        bounds.inset(-extraWidth / 2, -extraHeight / 2)
                        add(TouchDelegate(bounds, arrowButton))
                    }
                }
            }
    }

    private val wallpaperView = SurfaceView(context).apply { id = R.id.display_block_wallpaper }
    private val backgroundView =
        View(context).apply {
            id = R.id.display_block_background
            background = context.getDrawable(R.drawable.display_block_background)
        }
    private val selectionMarkerView =
        View(context).apply {
            id = R.id.display_block_selection_marker
            background = context.getDrawable(R.drawable.display_block_selection_marker_background)
        }

    /**
     * The logical position of the display's wallpaper content within the parent topology pane.
     *
     * Unlike [x] and [y], which define the top-left corner of the entire [DisplayBlock] container
     * (including variable margins for arrows and highlights), this property refers to the stable
     * visual anchor of the display itself.
     * - **Getter:** Returns the coordinate of the wallpaper, calculated by adding the current
     *   margins to the View's [x]/[y].
     * - **Setter:** Updates the View's [x]/[y] by subtracting the current margins, ensuring the
     *   wallpaper lands at the specified coordinates regardless of the container's current padding.
     */
    var positionInPane: PointF
        get() =
            PointF(x + highlightPx + horizontalArrowMargin, y + highlightPx + verticalArrowMargin)
        set(value) {
            x = value.x - highlightPx - horizontalArrowMargin
            y = value.y - highlightPx - verticalArrowMargin
        }

    var onA11yMoveListener: ((direction: Direction) -> Unit)? = null
    @VisibleForTesting
    internal val arrowButtons: Map<Direction, View>
        get() = a11yController.arrowButtons

    @VisibleForTesting
    internal val surfaceRenderer =
        DisplayBlockSurfaceRenderer(injector, wallpaperView, cornerRadiusPx)
    private val a11yController =
        DisplayBlockA11yController(context) { direction -> onA11yMoveListener?.invoke(direction) }

    init {
        isScrollContainer = false
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false

        isFocusable = true
        isScreenReaderFocusable = true

        // Prevents shadow from appearing around edge of button.
        stateListAnimator = null

        addView(wallpaperView)
        addView(backgroundView)
        addView(selectionMarkerView)

        a11yController.arrowButtons.values.forEach(::addView)
        accessibilityDelegate = a11yController.accessibilityDelegate
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        addOnLayoutChangeListener(layoutChangeListener)
        surfaceRenderer.attach()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        surfaceRenderer.detach()
        setTouchListener(null)
        onA11yMoveListener = null
        removeOnLayoutChangeListener(layoutChangeListener)
    }

    fun setHighlighted(value: Boolean) {
        selectionMarkerView.visibility = if (value) VISIBLE else INVISIBLE

        // The highlighted block must be draw last so that its highlight shows over the borders of
        // other displays.
        z = if (value) 2f else 1f
    }

    fun setArrowVisible(visible: Boolean) {
        if (a11yController.areArrowsVisible == visible) return

        updateMarginsAndCompensatePosition {
            a11yController.areArrowsVisible = visible
            a11yController.updateArrowVisibility()
        }
    }

    /**
     * Updates state within the [updateBlock] and shifts the View's X/Y coordinates to compensate
     * for any resulting changes in margins, keeping the wallpaper visually stationary.
     */
    private fun updateMarginsAndCompensatePosition(updateBlock: () -> Unit) {
        val oldHMargin = horizontalArrowMargin
        val oldVMargin = verticalArrowMargin

        updateBlock()

        val newHMargin = horizontalArrowMargin
        val newVMargin = verticalArrowMargin

        // Shift the View to compensate for the margin change.
        // The visual position of the wallpaper is determined by (View.x + Margin).
        // When arrows appear, margins increase, pushing the wallpaper inward relative to the View.
        // To keep the wallpaper visually stationary on screen, we must shift the View's origin
        // in the opposite direction by the delta of the margins.
        x += (oldHMargin - newHMargin)
        y += (oldVMargin - newVMargin)

        wallpaperView.layoutParams?.let { setupLayoutBounds(Size(it.width, it.height)) }
    }

    // DisplayBlock bounds are bigger than the actual display wallpaper (+ padding) area. Sets
    // touch listener to specific view
    fun setTouchListener(listener: OnTouchListener?) {
        if (listener == null) {
            backgroundView.setOnTouchListener(null)
        } else {
            backgroundView.setOnTouchListener { v, e -> listener.onTouch(v, e) }
        }
    }

    /**
     * Sets position and size of the block given coordinates in pane space.
     *
     * @param logicalDisplayId ID of the logical display this DisplayBlock represents
     * @param displayIdToShowWallpaper ID of the display whose wallpaper would be projected on this
     *   display block.
     * @param contentTopLeftWithoutMargins coordinates of top left corner of the block, not
     *   including highlight border and margins for arrow movements (sticking out of the display)
     * @param contentBottomRightWithoutMargins coordinates of bottom right corner of the block, not
     *   including highlight border and margins for arrow movements (sticking out of the display)
     *   border
     * @param surfaceScale scale in pixels of the size of the wallpaper mirror to the actual
     *   wallpaper on the screen - should be less than one to indicate scaling to smaller size
     * @param surfaceSize the size of the surface of the display that would be projected on the
     *   block
     */
    fun reset(
        logicalDisplayId: Int,
        displayIdToShowWallpaper: Int,
        contentTopLeftWithoutMargins: PointF,
        contentBottomRightWithoutMargins: PointF,
        surfaceScale: Float,
        surfaceSize: Size,
        arrowMovement: ArrowMovement,
    ) {
        surfaceRenderer.reset(logicalDisplayId, displayIdToShowWallpaper, surfaceScale, surfaceSize)
        a11yController.arrowMovement = arrowMovement
        a11yController.updateArrowVisibility()
        // It's important to let arrows visibility update to be done first, as the positioning will
        // rely on whether arrows are visible or not
        positionInPane = contentTopLeftWithoutMargins

        val displayDevice = injector.getDisplay(logicalDisplayId)
        contentDescription =
            context.getString(
                R.string.external_display_topology_display_block_content_description,
                displayDevice?.name ?: "Display $logicalDisplayId",
            )
        setupLayoutBounds(
            Size(
                (contentBottomRightWithoutMargins.x - contentTopLeftWithoutMargins.x).toInt(),
                (contentBottomRightWithoutMargins.y - contentTopLeftWithoutMargins.y).toInt(),
            )
        )
    }

    /**
     * DisplayBlock is rendered with multiple View stacked on top of each other. Each View serves a
     * purpose, there are 4 layers:
     * 1. Wallpaper: The actual display wallpaper rendered on a SurfaceView
     * 2. Background: Applies padding overlay on top of the surface to create an appearance of block
     *    being distanced from one another
     * 3. Highlight: Border around the wallpaper highlighting selected display
     * 4. DisplayBlock parent container: The container for the block itself, this is set to be
     *    larger than the actual visible area to accommodate for A11y arrow buttons sticking out
     *    from wallpaper (+ highlight).
     * <pre>
     * ......................  <-- DisplayBlock outermost boundary (.)
     *            ^         .  <-- A11y arrow buttons (<^>v)
     * .  ########|#######  .  <-- Highlight, `selectionMarkerView` (#)
     * .  #/------|-----\#  .  <-- Padding, `backgroundView` (-)
     * .  #|-WWWWWWWWWW-|#  .
     * .  #|-WWWWWWWWWW-|#  .  <-- Wallpaper / Surface, `wallpaperView` (W)
     * . <===WWWWWWWWWW===> .
     * .  #|-WWWWWWWWWW-|#  .
     * .  #|-WWWWWWWWWW-|#  .
     * .  #\------|-----/#  .
     * .  ########|#######  .
     * .          v          .
     * ......................
     * </pre>
     */
    private fun setupLayoutBounds(bounds: Size) {
        val paddedWidth = bounds.width + 2 * highlightPx + 2 * horizontalArrowMargin
        val paddedHeight = bounds.height + 2 * highlightPx + 2 * verticalArrowMargin

        if (width == paddedWidth && height == paddedHeight) {
            // Will not receive a surfaceChanged callback, so in case the wallpaper is different,
            // apply it.
            surfaceRenderer.updateSurfaceView()
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
            it.width = bounds.width
            it.height = bounds.height
            if (it is MarginLayoutParams) {
                it.setMargins(
                    highlightPx + horizontalArrowMargin,
                    highlightPx + verticalArrowMargin,
                    highlightPx + horizontalArrowMargin,
                    highlightPx + verticalArrowMargin,
                )
            }
            wallpaperView.layoutParams = it
        }
        // Padding is already applied in xml layout
        val boundaryPlusHighlightWidth = bounds.width + 2 * highlightPx
        val boundaryPlusHighlightHeight = bounds.height + 2 * highlightPx
        fun updateBoundaryWithHighlight(view: View) {
            (view.layoutParams as MarginLayoutParams).let {
                it.width = boundaryPlusHighlightWidth
                it.height = boundaryPlusHighlightHeight
                it.setMargins(
                    horizontalArrowMargin,
                    verticalArrowMargin,
                    horizontalArrowMargin,
                    verticalArrowMargin,
                )
            }
        }
        updateBoundaryWithHighlight(backgroundView)
        updateBoundaryWithHighlight(selectionMarkerView)

        wallpaperView.outlineProvider =
            object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, cornerRadiusPx.toFloat())
                }
            }
        wallpaperView.clipToOutline = true

        requestLayout()
    }
}
