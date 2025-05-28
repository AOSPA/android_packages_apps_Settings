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
import android.graphics.RectF
import android.hardware.display.DisplayTopology
import android.hardware.display.DisplayTopology.TreeNode.POSITION_BOTTOM
import android.hardware.display.DisplayTopology.TreeNode.POSITION_LEFT
import android.hardware.display.DisplayTopology.TreeNode.POSITION_TOP
import android.util.DisplayMetrics
import android.util.Size
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewManager
import android.widget.FrameLayout
import androidx.preference.PreferenceViewHolder
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.view.MotionEventBuilder
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import java.util.function.Consumer
import kotlin.math.abs
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DisplayTopologyPreferenceTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val injector = TestInjector(context)
    val preference = DisplayTopologyPreference(injector)
    val rootView = View.inflate(context, preference.layoutResource, /*parent=*/ null)
    val holder = PreferenceViewHolder.createInstanceForTests(rootView)

    init {
        preference.onBindViewHolder(holder)
    }

    class TestInjector(context : Context) : ConnectedDisplayInjector(context) {
        var displaysSize = mutableMapOf<Int, Size>()
        var topology: DisplayTopology? = null

        var topologyListener: Consumer<DisplayTopology>? = null

        override var displayTopology : DisplayTopology?
            get() = topology
            set(value) { topology = value }

        /**
         * A log of events related to wallpaper revealing.
         */
        val revealLog = mutableListOf<String>()

        override val densityDpi = DisplayMetrics.DENSITY_DEFAULT

        override fun getLogicalSize(displayId: Int): Size? {
            return displaysSize.get(displayId)
        }

        override fun registerTopologyListener(listener: Consumer<DisplayTopology>) {
            if (topologyListener != null) {
                throw IllegalStateException(
                        "already have a listener registered: ${topologyListener}")
            }
            topologyListener = listener
        }

        override fun unregisterTopologyListener(listener: Consumer<DisplayTopology>) {
            if (topologyListener != listener) {
                throw IllegalStateException("no such listener registered: ${listener}")
            }
            topologyListener = null
        }

        override fun revealWallpaper(displayId: Int): RevealedWallpaper? {
            val childView = View(context)
            val viewManager = object : ViewManager {
                override fun addView(view: View, params: ViewGroup.LayoutParams) {
                    revealLog.add("unexpected invocation of addView")
                }

                override fun updateViewLayout(view: View, params: ViewGroup.LayoutParams) {
                    revealLog.add("unexpected invocation of updateViewLayout")
                }

                override fun removeView(view: View) {
                    if (childView === view) {
                        revealLog.add("removed wallpaper revealer for display $displayId")
                    } else {
                        revealLog.add("invalid invocation of removeView")
                    }
                }
            }

            revealLog.add("revealWallpaper invoked for display $displayId")

            return RevealedWallpaper(displayId, childView, viewManager)
        }
    }

    @Test
    fun disabledTopology() {
        preference.onAttached()
        preference.refreshPane()

        assertThat(preference.mPaneContent.childCount).isEqualTo(0)
        // TODO(b/352648432): update test when we show the main display even when
        // a topology is not active.
        assertThat(preference.mTopologyHint.text).isEqualTo("")
    }

    /**
     * Returns the bounds of the non-highlighting part of the block relative to the parent.
     */
    private fun virtualBounds(block: DisplayBlock): RectF {
        val d = block.mHighlightPx.toFloat()
        val x = block.x + d
        val y = block.y + d
        // Using layoutParams as a proxy for the actual width and height appears to be standard
        // practice in Robolectric tests, as they do not actually process layout requests.
        val w = block.layoutParams.width - 2*d
        val h = block.layoutParams.height - 2*d
        return RectF(x, y, x + w, y + h)
    }

    private fun getPaneChildren(): List<DisplayBlock> =
        (0..preference.mPaneContent.childCount-1)
                .map { preference.mPaneContent.getChildAt(it) as DisplayBlock }
                .toList()

    fun setupSingleDisplay() {
        val primaryId = 22;

        val root = DisplayTopology.TreeNode(
                primaryId, DISPLAY_SIZE_1.width, DISPLAY_SIZE_1.height,
                /* logicalDensity= */ 160, POSITION_LEFT, /* offset= */ 0f)

        injector.topology = DisplayTopology(root, primaryId)
        injector.displaysSize = mutableMapOf(Pair(primaryId, DISPLAY_SIZE_1))
    }

    fun setupTwoDisplays(childPosition: Int, childOffset: Float) {
        val primaryId = 1

        val child = DisplayTopology.TreeNode(
                /* displayId= */ 42, DISPLAY_SIZE_2.width, DISPLAY_SIZE_2.height,
                /* logicalDensity= */ 160, childPosition, childOffset)
        val root = DisplayTopology.TreeNode(
                primaryId, DISPLAY_SIZE_1.width, DISPLAY_SIZE_1.height,
                /* logicalDensity= */ 160, POSITION_LEFT, /* offset= */ 0f)
        root.addChild(child)

        injector.topology = DisplayTopology(root, primaryId)
        injector.displaysSize = mutableMapOf(Pair(primaryId, DISPLAY_SIZE_1), Pair(42, DISPLAY_SIZE_2))
    }

    /** Uses the topology in the injector to populate and prepare the pane for interaction. */
    fun preparePane() {
        // This layoutParams needs to be non-null for the global layout handler.
        preference.mPaneHolder.layoutParams = FrameLayout.LayoutParams(
                /* width= */ 640, /* height= */ 480)

        // Force pane width to have a reasonable value (hundreds of dp) so the TopologyScale is
        // calculated reasonably.
        preference.mPaneContent.left = 0
        preference.mPaneContent.right = 640

        preference.onAttached()
        preference.refreshPane()
    }

    /**
     * Sets up a simple topology in the pane with two displays. Returns the left-hand display and
     * right-hand display in order in a list. The right-hand display is the root.
     */
    fun setupPaneWithTwoDisplays(childPosition: Int = POSITION_LEFT, childOffset: Float = 42f):
            List<DisplayBlock> {
        setupTwoDisplays(childPosition, childOffset)

        preparePane()

        val paneChildren = getPaneChildren()
        assertThat(paneChildren).hasSize(2)

        // Block of child display is on the left.
        return if (paneChildren[0].x < paneChildren[1].x)
                paneChildren
        else
                paneChildren.reversed()
    }

    fun assertSelected(block: DisplayBlock, expected: Boolean) {
        val vis = if (expected) View.VISIBLE else View.INVISIBLE
        assertThat(block.mSelectionMarkerView.visibility).isEqualTo(vis)
    }

    @Test
    fun twoDisplaysGenerateBlocks() {
        val (childBlock, rootBlock) = setupPaneWithTwoDisplays()
        val childBounds = virtualBounds(childBlock)
        val rootBounds = virtualBounds(rootBlock)

        // After accounting for padding, child should be half the length of root in each dimension.
        assertThat(childBounds.width())
                .isEqualTo(rootBounds.width() / 2)
        assertThat(childBounds.height())
                .isEqualTo(rootBounds.height() / 2)
        assertThat(childBounds.top).isGreaterThan(rootBounds.top)
        assertSelected(childBlock, false)
        assertSelected(rootBlock, false)
        assertThat(rootBounds.left).isEqualTo(childBounds.right)

        assertThat(preference.mTopologyHint.text)
                .isEqualTo(context.getString(R.string.external_display_topology_hint))
    }

    @Test
    fun dragDisplayDownward() {
        val (leftBlock, _) = setupPaneWithTwoDisplays()

        preference.mTimesRefreshedBlocks = 0

        val downEvent = MotionEventBuilder.newBuilder()
                .setPointer(0f, 0f)
                .setAction(MotionEvent.ACTION_DOWN)
                .build()

        // Move the left block half of its height downward. This is 40 pixels in display
        // coordinates. The original offset is 42, so the new offset will be 42 + 40.
        val leftBounds = virtualBounds(leftBlock)
        val moveEvent = MotionEventBuilder.newBuilder()
                .setAction(MotionEvent.ACTION_MOVE)
                .setPointer(0f, leftBounds.height() / 2f)
                .build()
        val upEvent = MotionEventBuilder.newBuilder().setAction(MotionEvent.ACTION_UP).build()

        leftBlock.dispatchTouchEvent(downEvent)
        leftBlock.dispatchTouchEvent(moveEvent)
        leftBlock.dispatchTouchEvent(upEvent)

        val rootChildren = injector.topology!!.root!!.children
        assertThat(rootChildren).hasSize(1)
        val child = rootChildren[0]
        assertThat(child.position).isEqualTo(POSITION_LEFT)
        assertThat(child.offset).isWithin(1f).of(82f)

        assertThat(preference.mTimesRefreshedBlocks).isEqualTo(1)
    }

    @Test
    fun dragRootDisplayToNewSide() {
        val (leftBlock, rightBlock) = setupPaneWithTwoDisplays()
        val leftBounds = virtualBounds(leftBlock)

        assertThat(injector.revealLog).containsExactly(
                "revealWallpaper invoked for display 1",
                "revealWallpaper invoked for display 42")

        injector.revealLog.clear()

        preference.mTimesRefreshedBlocks = 0

        val downEvent = MotionEventBuilder.newBuilder()
                .setAction(MotionEvent.ACTION_DOWN)
                .setPointer(0f, 0f)
                .build()

        // Move the right block left and upward. We won't move it into exactly the correct position,
        // relying on the clamp algorithm to choose the correct side and offset.
        val moveEvent = MotionEventBuilder.newBuilder()
                .setAction(MotionEvent.ACTION_MOVE)
                .setPointer(-leftBounds.width(), -leftBounds.height() / 2f)
                .build()

        val upEvent = MotionEventBuilder.newBuilder().setAction(MotionEvent.ACTION_UP).build()

        assertThat(leftBlock.y).isGreaterThan(rightBlock.y)

        rightBlock.dispatchTouchEvent(downEvent)
        rightBlock.dispatchTouchEvent(moveEvent)
        rightBlock.dispatchTouchEvent(upEvent)

        assertThat(injector.revealLog).isEmpty()

        val rootChildren = injector.topology!!.root!!.children
        assertThat(rootChildren).hasSize(1)
        val child = rootChildren[0]
        assertThat(child.position).isEqualTo(POSITION_BOTTOM)
        assertThat(child.offset).isWithin(1f).of(0f)

        // After rearranging blocks, the original block views should still be present.
        val paneChildren = getPaneChildren()
        assertThat(paneChildren.indexOf(leftBlock)).isNotEqualTo(-1)
        assertThat(paneChildren.indexOf(rightBlock)).isNotEqualTo(-1)

        // Left edge of both blocks should be aligned after dragging.
        assertThat(paneChildren[0].x)
                .isWithin(1f)
                .of(paneChildren[1].x)

        assertThat(preference.mTimesRefreshedBlocks).isEqualTo(1)
    }

    @Test
    fun noRefreshForUnmovingDrag() {
        val (leftBlock, rightBlock) = setupPaneWithTwoDisplays()

        preference.mTimesRefreshedBlocks = 0

        val downEvent = MotionEventBuilder.newBuilder()
                .setAction(MotionEvent.ACTION_DOWN)
                .setPointer(0f, 0f)
                .build()

        val upEvent = MotionEventBuilder.newBuilder().setAction(MotionEvent.ACTION_UP).build()

        rightBlock.dispatchTouchEvent(downEvent)
        rightBlock.dispatchTouchEvent(upEvent)

        // After drag, the original views should still be present.
        val paneChildren = getPaneChildren()
        assertThat(paneChildren.indexOf(leftBlock)).isNotEqualTo(-1)
        assertThat(paneChildren.indexOf(rightBlock)).isNotEqualTo(-1)

        assertThat(preference.mTimesRefreshedBlocks).isEqualTo(0)
    }

    @Test
    fun keepOriginalViewsWhenAddingMore() {
        setupPaneWithTwoDisplays()
        assertThat(injector.revealLog).containsExactly(
                "revealWallpaper invoked for display 1",
                "revealWallpaper invoked for display 42")
        injector.revealLog.clear()
        val childrenBefore = getPaneChildren()
        val newDisplayId = 101
        val newDisplaySize = Size(320, 240)
        injector.topology!!.addDisplay(newDisplayId, newDisplaySize.width, newDisplaySize.height, /* logicalDensity= */ 160)
        injector.displaysSize.put(newDisplayId, newDisplaySize)
        preference.refreshPane()
        assertThat(injector.revealLog).containsExactly("revealWallpaper invoked for display 101")
        injector.revealLog.clear()
        val childrenAfter = getPaneChildren()

        assertThat(childrenBefore).hasSize(2)
        assertThat(childrenAfter).hasSize(3)
        assertThat(childrenAfter.subList(0, 2)).isEqualTo(childrenBefore)

        assertThat(injector.topology!!.removeDisplay(42)).isTrue()
        assertThat(injector.topology!!.removeDisplay(newDisplayId)).isTrue()
        preference.refreshPane()
        assertThat(injector.revealLog).containsExactly(
                "removed wallpaper revealer for display 42",
                "removed wallpaper revealer for display 101")
    }

    @Test
    fun applyNewTopologyViaListenerUpdate() {
        setupPaneWithTwoDisplays()

        preference.mTimesRefreshedBlocks = 0
        val newDisplayId = 8008
        val newDisplaySize = Size(300, 320)
        val newTopology = injector.topology!!.copy()
        newTopology.addDisplay(newDisplayId, newDisplaySize.width, newDisplaySize.height,
                /* logicalDensity= */ 160)
        injector.displaysSize.put(newDisplayId, newDisplaySize)

        injector.topology = newTopology
        injector.topologyListener!!.accept(newTopology)

        assertThat(preference.mTimesRefreshedBlocks).isEqualTo(1)
        val paneChildren = getPaneChildren()
        assertThat(paneChildren).hasSize(3)

        // Look for a display with the same unusual aspect ratio as the one we've added.
        val expectedAspectRatio = newDisplaySize.width.toFloat() / newDisplaySize.height.toFloat()
        assertThat(paneChildren
                .map { virtualBounds(it) }
                .map { it.width() / it.height() }
                .filter { abs(it - expectedAspectRatio) < 0.001f }
        ).hasSize(1)
    }

    @Test
    fun ignoreListenerUpdateOfUnchangedTopology() {
        setupTwoDisplays(POSITION_TOP, /* offset= */ 12.0f)
        preparePane()

        preference.mTimesRefreshedBlocks = 0
        setupTwoDisplays(POSITION_TOP, /* offset= */ 12.1f)
        injector.topologyListener!!.accept(injector.topology!!)

        assertThat(preference.mTimesRefreshedBlocks).isEqualTo(0)
    }

    @Test
    fun cannotMoveSingleDisplay() {
        setupSingleDisplay()
        preparePane()

        val paneChildren = getPaneChildren()
        assertThat(paneChildren).hasSize(1)
        val block = paneChildren[0]

        val origY = block.y

        block.dispatchTouchEvent(MotionEventBuilder.newBuilder()
                .setAction(MotionEvent.ACTION_DOWN)
                .setPointer(0f, 0f)
                .build())
        block.dispatchTouchEvent(MotionEventBuilder.newBuilder()
                .setAction(MotionEvent.ACTION_MOVE)
                .setPointer(0f, 30f)
                .build())

        assertThat(block.y).isWithin(0.01f).of(origY)

        block.dispatchTouchEvent(MotionEventBuilder.newBuilder()
                .setAction(MotionEvent.ACTION_UP)
                .build())

        // Block should be back to original position.
        assertThat(block.y).isWithin(0.01f).of(origY)
    }

    @Test
    fun updatedTopologyCancelsDragIfNonTrivialChange() {
        val (leftBlock, _) = setupPaneWithTwoDisplays(POSITION_LEFT, /* childOffset= */ 42f)

        assertThat(leftBlock.positionInPane.y).isWithin(0.05f).of(143.76f)

        leftBlock.dispatchTouchEvent(MotionEventBuilder.newBuilder()
                .setAction(MotionEvent.ACTION_DOWN)
                .setPointer(0f, 0f)
                .build())
        leftBlock.dispatchTouchEvent(MotionEventBuilder.newBuilder()
                .setAction(MotionEvent.ACTION_MOVE)
                .setPointer(0f, 30f)
                .build())
        assertThat(leftBlock.positionInPane.y).isWithin(0.05f).of(173.76f)

        // Offset is only different by 0.5 dp, so the drag will not cancel.
        setupTwoDisplays(POSITION_LEFT, /* childOffset= */ 41.5f)
        injector.topologyListener!!.accept(injector.topology!!)

        assertThat(leftBlock.positionInPane.y).isWithin(0.05f).of(173.76f)
        // Move block farther downward.
        leftBlock.dispatchTouchEvent(MotionEventBuilder.newBuilder()
                .setAction(MotionEvent.ACTION_MOVE)
                .setPointer(0f, 50f)
                .build())
        assertThat(leftBlock.positionInPane.y).isWithin(0.05f).of(193.76f)

        setupTwoDisplays(POSITION_LEFT, /* childOffset= */ 20f)
        injector.topologyListener!!.accept(injector.topology!!)

        assertThat(leftBlock.positionInPane.y).isWithin(0.05f).of(115.60f)
        // Another move in the opposite direction should not move the left block.
        leftBlock.dispatchTouchEvent(MotionEventBuilder.newBuilder()
                .setAction(MotionEvent.ACTION_MOVE)
                .setPointer(0f, -20f)
                .build())
        assertThat(leftBlock.positionInPane.y).isWithin(0.05f).of(115.60f)
    }

    @Test
    fun highlightDuringDrag() {
        val (leftBlock, _) = setupPaneWithTwoDisplays(POSITION_LEFT, /* childOffset= */ 42f)

        assertSelected(leftBlock, false)
        leftBlock.dispatchTouchEvent(MotionEventBuilder.newBuilder()
                .setAction(MotionEvent.ACTION_DOWN)
                .setPointer(0f, 0f)
                .build())
        assertSelected(leftBlock, true)
        leftBlock.dispatchTouchEvent(MotionEventBuilder.newBuilder()
                .setAction(MotionEvent.ACTION_UP)
                .build())
        assertSelected(leftBlock, false)
    }

    fun dragBlockWithOneMoveEvent(
            block: DisplayBlock, startTimeMs: Long, endTimeMs: Long, xDiff: Float, yDiff: Float) {
        block.dispatchTouchEvent(MotionEventBuilder.newBuilder()
                .setAction(MotionEvent.ACTION_DOWN)
                .setPointer(0f, 0f)
                .setEventTime(startTimeMs)
                .build())
        block.dispatchTouchEvent(MotionEventBuilder.newBuilder()
                .setAction(MotionEvent.ACTION_MOVE)
                .setPointer(xDiff, yDiff)
                .setEventTime((startTimeMs + endTimeMs) / 2)
                .build())
        block.dispatchTouchEvent(MotionEventBuilder.newBuilder()
                .setAction(MotionEvent.ACTION_UP)
                .setPointer(xDiff, yDiff)
                .setEventTime(endTimeMs)
                .build())
    }

    @Test
    fun accidentalDrag_LittleAndBriefEnoughToBeAccidental() {
        val (leftBlock, _) = setupPaneWithTwoDisplays(POSITION_LEFT, childOffset = 42f)
        val startTime = 424242L
        val startX = leftBlock.x
        val startY = leftBlock.y

        preference.mTimesRefreshedBlocks = 0
        dragBlockWithOneMoveEvent(
                leftBlock, startTime,
                endTimeMs = startTime + preference.accidentalDragTimeLimitMs - 10,
                xDiff = preference.accidentalDragDistancePx - 1f, yDiff = 0f,
        )
        assertThat(leftBlock.x).isEqualTo(startX)
        assertThat(preference.mTimesRefreshedBlocks).isEqualTo(0)

        dragBlockWithOneMoveEvent(
                leftBlock, startTime,
                endTimeMs = startTime + preference.accidentalDragTimeLimitMs - 10,
                xDiff = 0f, yDiff = preference.accidentalDragDistancePx - 1f,
        )
        assertThat(leftBlock.y).isEqualTo(startY)
        assertThat(preference.mTimesRefreshedBlocks).isEqualTo(0)
    }

    @Test
    fun accidentalDrag_TooFarToBeAccidentalXAxis() {
        val (topBlock, _) = setupPaneWithTwoDisplays(POSITION_TOP, childOffset = -42f)
        val startTime = 88888L
        val startX = topBlock.x

        preference.mTimesRefreshedBlocks = 0
        dragBlockWithOneMoveEvent(
                topBlock, startTime,
                endTimeMs = startTime + preference.accidentalDragTimeLimitMs - 10,
                xDiff = preference.accidentalDragDistancePx + 1f, yDiff = 0f,
        )
        assertThat(preference.mTimesRefreshedBlocks).isEqualTo(1)
        assertThat(topBlock.x).isNotEqualTo(startX)
    }

    @Test
    fun accidentalDrag_TooFarToBeAccidentalYAxis() {
        val (leftBlock, _) = setupPaneWithTwoDisplays(POSITION_LEFT, childOffset = 42f)
        val startTime = 88888L
        val startY = leftBlock.y

        preference.mTimesRefreshedBlocks = 0
        dragBlockWithOneMoveEvent(
                leftBlock, startTime,
                endTimeMs = startTime + preference.accidentalDragTimeLimitMs - 10,
                xDiff = 0f, yDiff = preference.accidentalDragDistancePx + 1f,
        )
        assertThat(leftBlock.y).isNotEqualTo(startY)
        assertThat(preference.mTimesRefreshedBlocks).isEqualTo(1)
    }

    @Test
    fun accidentalDrag_TooSlowToBeAccidental() {
        val (topBlock, _) = setupPaneWithTwoDisplays(POSITION_TOP, childOffset = -42f)
        val startTime = 88888L
        val startX = topBlock.x

        preference.mTimesRefreshedBlocks = 0
        dragBlockWithOneMoveEvent(
                topBlock, startTime,
                endTimeMs = startTime + preference.accidentalDragTimeLimitMs + 10,
                xDiff = preference.accidentalDragDistancePx - 1f, yDiff = 0f,
        )
        assertThat(topBlock.x).isNotEqualTo(startX)
        assertThat(preference.mTimesRefreshedBlocks).isEqualTo(1)
    }

    private companion object {
        private val DISPLAY_SIZE_1 = Size(200, 160)
        private val DISPLAY_SIZE_2 = Size(100, 80)
    }
}
