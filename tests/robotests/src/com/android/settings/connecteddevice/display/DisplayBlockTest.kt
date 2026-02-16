/*
 * Copyright 2025 The Android Open Source Project
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
import android.graphics.PointF
import android.os.Handler
import android.util.Size
import android.view.SurfaceControl
import android.view.SurfaceView
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.anyFloat
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.reset
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DisplayBlockTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var injector: TestInjector
    private lateinit var block: DisplayBlock
    private lateinit var parentView: FrameLayout

    private lateinit var mockTransaction: SurfaceControl.Transaction

    @Before
    fun setUp() {
        mockTransaction = mock(SurfaceControl.Transaction::class.java)
        injector = TestInjector(context, mockTransaction)
        block = DisplayBlock(context, injector)
        parentView = FrameLayout(context)

        parentView.addView(block)

        // Stub the chained methods to return the mock itself.
        `when`(mockTransaction.reparent(any(), any())).thenReturn(mockTransaction)
        `when`(mockTransaction.setScale(any(), anyFloat(), anyFloat())).thenReturn(mockTransaction)
        `when`(mockTransaction.setPosition(any(), anyFloat(), anyFloat()))
            .thenReturn(mockTransaction)
        `when`(mockTransaction.setCrop(any(), any())).thenReturn(mockTransaction)
        `when`(mockTransaction.setCornerRadius(any(), anyFloat())).thenReturn(mockTransaction)
        `when`(mockTransaction.remove(any())).thenReturn(mockTransaction)
    }

    class TestInjector(context: Context, private val mockTransaction: SurfaceControl.Transaction) :
        ConnectedDisplayInjector(context) {

        var wallpapers = mutableMapOf<Int, SurfaceControl>()
        internal val testHandler = TestHandler(context.mainThreadHandler)

        override val handler: Handler
            get() = testHandler

        override fun createSurfaceTransaction(): SurfaceControl.Transaction {
            return mockTransaction
        }

        override fun getSurfaceControlBuilder(): SurfaceControl.Builder {
            return object : SurfaceControl.Builder() {
                override fun build(): SurfaceControl {
                    val mockSurfaceControl = mock(SurfaceControl::class.java)
                    `when`(mockSurfaceControl.isValid()).thenReturn(true)
                    return mockSurfaceControl
                }
            }
        }

        override fun wallpaper(displayId: Int): SurfaceControl? = wallpapers.remove(displayId)
    }

    @Test
    fun normalUpdateFlow() {
        val wallpaper = SurfaceControl.Builder().setName("wallpaper").build()
        injector.wallpapers[DISPLAY_ID] = wallpaper
        block.reset(
            DISPLAY_ID,
            DISPLAY_ID,
            PointF(10f, 10f),
            PointF(20f, 20f),
            0.5f,
            DISPLAY_SIZE,
            ArrowMovement.immovable(),
        )
        injector.testHandler.flush()

        block.surfaceRenderer.updateSurfaceView()
        injector.testHandler.flush()

        verify(mockTransaction).reparent(eq(wallpaper), any())
        verify(mockTransaction).setScale(eq(wallpaper), eq(0.5f), eq(0.5f))
        verify(mockTransaction, never()).remove(any())
        verify(mockTransaction).apply()
    }

    @Test
    fun resetTwiceBeforeSurfaceUpdate() {
        val wallpaperA = SurfaceControl.Builder().setName("wallpaperA").build()
        val wallpaperB = SurfaceControl.Builder().setName("wallpaperB").build()
        injector.wallpapers[DISPLAY_ID] = wallpaperA

        block.reset(
            DISPLAY_ID,
            DISPLAY_ID,
            PointF(10f, 10f),
            PointF(20f, 20f),
            0.25f,
            DISPLAY_SIZE,
            ArrowMovement.immovable(),
        )

        // Should not have fetched wallpaper info yet. Replace wallpaper setting with wallpaperB.
        assertThat(injector.wallpapers.put(DISPLAY_ID, wallpaperB)).isEqualTo(wallpaperA)

        block.reset(
            DISPLAY_ID,
            DISPLAY_ID,
            PointF(10f, 10f),
            PointF(30f, 30f),
            0.4f,
            DISPLAY_SIZE,
            ArrowMovement.immovable(),
        )
        injector.testHandler.flush()

        // Should not have fetched wallpaper or display info yet.
        assertThat(injector.wallpapers.get(DISPLAY_ID)).isEqualTo(wallpaperB)
        block.surfaceRenderer.updateSurfaceView()
        injector.testHandler.flush()
        assertThat(injector.wallpapers.get(DISPLAY_ID)).isNull()

        verify(mockTransaction).reparent(eq(wallpaperB), any())
        verify(mockTransaction).setScale(eq(wallpaperB), eq(0.4f), eq(0.4f))
        // wallpaperA is never rendered, so it never becomes an "old surface" and remove() should
        // not be called.
        verify(mockTransaction, never()).remove(any())
        verify(mockTransaction).apply()
    }

    @Test
    fun resetWithUnchangedSizeCausesImmediateUpdate() {
        val wallpaperA = SurfaceControl.Builder().setName("wallpaperA").build()
        val wallpaperB = SurfaceControl.Builder().setName("wallpaperB").build()

        injector.wallpapers[DISPLAY_ID] = wallpaperA
        block.reset(
            DISPLAY_ID,
            DISPLAY_ID,
            PointF(0.0f, 0.0f),
            PointF(BLOCK_WIDTH, BLOCK_HEIGHT),
            0.5f,
            DISPLAY_SIZE,
            ArrowMovement.immovable(),
        )
        block.surfaceRenderer.updateSurfaceView()
        injector.testHandler.flush()

        verify(mockTransaction).reparent(eq(wallpaperA), any())
        verify(mockTransaction).setScale(eq(wallpaperA), eq(0.5f), eq(0.5f))
        verify(mockTransaction).apply()

        applyRequestedSize()
        reset(mockTransaction)

        // Same size and scale as before, but a new wallpaper and different position in parent view.
        injector.wallpapers[DISPLAY_ID] = wallpaperB
        val moveOffsetPx = 10f
        block.reset(
            DISPLAY_ID,
            DISPLAY_ID,
            PointF(moveOffsetPx, moveOffsetPx),
            PointF(moveOffsetPx + BLOCK_WIDTH, moveOffsetPx + BLOCK_HEIGHT),
            0.5f,
            DISPLAY_SIZE,
            ArrowMovement.immovable(),
        )
        injector.testHandler.flush()
        verify(mockTransaction).reparent(eq(wallpaperB), any())
        verify(mockTransaction).setScale(eq(wallpaperB), eq(0.5f), eq(0.5f))
        verify(mockTransaction).remove(eq(wallpaperA))
        verify(mockTransaction).apply()

        applyRequestedSize()
        reset(mockTransaction)

        // Repeat the pattern, but with a new scale and reverting back to wallpaperA.
        injector.wallpapers.put(DISPLAY_ID, wallpaperA)
        block.reset(
            DISPLAY_ID,
            DISPLAY_ID,
            PointF(0.0f, 0.0f),
            PointF(BLOCK_WIDTH, BLOCK_HEIGHT),
            0.2f,
            DISPLAY_SIZE,
            ArrowMovement.immovable(),
        )
        injector.testHandler.flush()

        verify(mockTransaction).reparent(eq(wallpaperA), any())
        verify(mockTransaction).setScale(eq(wallpaperA), eq(0.2f), eq(0.2f))
        verify(mockTransaction).remove(eq(wallpaperB))
        verify(mockTransaction).apply()
    }

    @Test
    fun retryIfWallpaperNotReady() {
        block.reset(
            DISPLAY_ID,
            DISPLAY_ID,
            PointF(10f, 10f),
            PointF(20f, 20f),
            0.5f,
            DISPLAY_SIZE,
            ArrowMovement.immovable(),
        )
        injector.testHandler.flush()
        block.surfaceRenderer.updateSurfaceView()
        injector.testHandler.flush()

        verify(mockTransaction, never()).apply()

        val wallpaper = SurfaceControl.Builder().setName("wallpaper").build()
        injector.wallpapers[DISPLAY_ID] = wallpaper
        // One wait to resume the deferred retry
        injector.testHandler.flush()
        // One wait to wait for task posted to main thread to run
        injector.testHandler.flush()

        verify(mockTransaction).reparent(eq(wallpaper), any())
        verify(mockTransaction).setScale(eq(wallpaper), eq(0.5f), eq(0.5f))
        verify(mockTransaction).apply()
    }

    @Test
    fun renderSurfaceView_calculatesCenteringPositionCorrectly() {
        val wallpaper = SurfaceControl.Builder().setName("wallpaper").build()
        injector.wallpapers[DISPLAY_ID] = wallpaper
        val surfaceScale = 0.25f
        block.reset(
            DISPLAY_ID,
            DISPLAY_ID,
            PointF(0f, 0f),
            PointF(BLOCK_WIDTH, BLOCK_HEIGHT),
            surfaceScale,
            DISPLAY_SIZE,
            ArrowMovement.immovable(),
        )

        block.surfaceRenderer.updateSurfaceView()
        injector.testHandler.flush()

        val wallpaperViewWidth = block.wallpaperView().width.toFloat()
        val wallpaperViewHeight = block.wallpaperView().height.toFloat()
        val scaledSurfaceWidth = DISPLAY_SIZE.width * surfaceScale
        val scaledSurfaceHeight = DISPLAY_SIZE.height * surfaceScale
        val expectedPosX = (wallpaperViewWidth - scaledSurfaceWidth) / 2f
        val expectedPosY = (wallpaperViewHeight - scaledSurfaceHeight) / 2f

        verify(mockTransaction).setPosition(eq(wallpaper), eq(expectedPosX), eq(expectedPosY))
    }

    @Test
    fun renderSurfaceView_calculatesCornerRadiusCorrectly() {
        val wallpaper = SurfaceControl.Builder().setName("wallpaper").build()
        injector.wallpapers[DISPLAY_ID] = wallpaper
        val surfaceScale = 0.5f
        block.reset(
            DISPLAY_ID,
            DISPLAY_ID,
            PointF(0f, 0f),
            PointF(BLOCK_WIDTH, BLOCK_HEIGHT),
            surfaceScale,
            DISPLAY_SIZE,
            ArrowMovement.immovable(),
        )

        block.surfaceRenderer.updateSurfaceView()
        injector.testHandler.flush()

        val expectedRadius = block.cornerRadiusPx.toFloat() / surfaceScale
        verify(mockTransaction).setCornerRadius(eq(wallpaper), eq(expectedRadius))
    }

    @Test
    fun renderSurfaceView_inMirroringMode_doesNotSetCornerRadius() {
        val wallpaper = SurfaceControl.Builder().setName("wallpaper").build()
        injector.wallpapers[MIRRORED_DISPLAY_ID] = wallpaper
        block.reset(
            DISPLAY_ID,
            MIRRORED_DISPLAY_ID,
            PointF(0f, 0f),
            PointF(BLOCK_WIDTH, BLOCK_HEIGHT),
            0.5f,
            DISPLAY_SIZE,
            ArrowMovement.immovable(),
        )

        block.surfaceRenderer.updateSurfaceView()
        injector.testHandler.flush()

        verify(mockTransaction, never()).setCornerRadius(any(), anyFloat())
    }

    @Test
    fun renderSurfaceView_inMirroringMode_createsAndShowsBackground() {
        val wallpaper = SurfaceControl.Builder().setName("wallpaper").build()
        injector.wallpapers[MIRRORED_DISPLAY_ID] = wallpaper
        block.reset(
            DISPLAY_ID,
            MIRRORED_DISPLAY_ID,
            PointF(0f, 0f),
            PointF(BLOCK_WIDTH, BLOCK_HEIGHT),
            0.5f,
            DISPLAY_SIZE,
            ArrowMovement.immovable(),
        )

        block.surfaceRenderer.updateSurfaceView()
        injector.testHandler.flush()

        val surfaceCaptor = ArgumentCaptor.forClass(SurfaceControl::class.java)
        // Verify both the wallpaper and backgroundSurface is reparented
        verify(mockTransaction, times(2))
            .reparent(surfaceCaptor.capture(), eq(block.wallpaperView().surfaceControl))
        val backgroundSurface = surfaceCaptor.allValues.get(1)

        verify(mockTransaction).setAlpha(eq(backgroundSurface), eq(0.5f))
        verify(mockTransaction).show(eq(backgroundSurface))

        verify(mockTransaction).setLayer(eq(wallpaper), eq(1))
    }

    @Test
    fun init_addsArrowButtons_notVisible() {
        assertThat(block.arrowButtons.keys)
            .containsExactly(Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT)

        for (arrowButtonView in block.arrowButtons.values) {
            assertThat(arrowButtonView.parent).isEqualTo(block)
            assertThat(arrowButtonView.visibility).isEqualTo(View.GONE)
        }
    }

    @Test
    fun setArrowVisible_updatesVisibilityBasedOnMovability() {
        // Setup: Define which directions are movable
        val arrowMovement =
            ArrowMovement(
                mapOf(
                    Direction.UP to true,
                    Direction.DOWN to false,
                    Direction.LEFT to true,
                    Direction.RIGHT to false,
                )
            )
        block.reset(
            DISPLAY_ID,
            DISPLAY_ID,
            PointF(10f, 10f),
            PointF(20f, 20f),
            0.5f,
            DISPLAY_SIZE,
            arrowMovement,
        )

        // Action: Set arrows to be visible
        block.setArrowVisible(true)

        // Assertions for visible arrows based on movability
        assertThat(block.arrowButtons[Direction.UP]?.visibility).isEqualTo(View.VISIBLE)
        assertThat(block.arrowButtons[Direction.LEFT]?.visibility).isEqualTo(View.VISIBLE)
        assertThat(block.arrowButtons[Direction.DOWN]?.visibility).isEqualTo(View.GONE)
        assertThat(block.arrowButtons[Direction.RIGHT]?.visibility).isEqualTo(View.GONE)

        // Action: Set arrows to be invisible
        block.setArrowVisible(false)

        // Assertions: All arrows should be gone regardless of movability
        assertThat(block.arrowButtons[Direction.UP]?.visibility).isEqualTo(View.GONE)
        assertThat(block.arrowButtons[Direction.DOWN]?.visibility).isEqualTo(View.GONE)
        assertThat(block.arrowButtons[Direction.LEFT]?.visibility).isEqualTo(View.GONE)
        assertThat(block.arrowButtons[Direction.RIGHT]?.visibility).isEqualTo(View.GONE)
    }

    @Test
    fun onInitializeAccessibilityNodeInfo_addsAndRemovesActionsForMovableDirections() {
        // Setup: Define which directions are movable
        val arrowMovement =
            ArrowMovement(
                mapOf(
                    Direction.UP to true,
                    Direction.DOWN to false,
                    Direction.LEFT to true,
                    Direction.RIGHT to false,
                )
            )
        block.reset(
            DISPLAY_ID,
            DISPLAY_ID,
            PointF(10f, 10f),
            PointF(20f, 20f),
            0.5f,
            DISPLAY_SIZE,
            arrowMovement,
        )
        val mockNodeInfo = mock(AccessibilityNodeInfo::class.java)

        // Action
        block.onInitializeAccessibilityNodeInfo(mockNodeInfo)

        // Verification for added actions (movable directions)
        val addActionCaptor = ArgumentCaptor.forClass(AccessibilityAction::class.java)
        verify(mockNodeInfo, times(2)).addAction(addActionCaptor.capture())
        val addedActionIds = addActionCaptor.allValues.map { it.id }
        assertThat(addedActionIds)
            .containsExactly(R.id.action_move_display_block_up, R.id.action_move_display_block_left)
            .inOrder()

        // Verification for removed actions (non-movable directions)
        val removeActionCaptor = ArgumentCaptor.forClass(AccessibilityAction::class.java)
        verify(mockNodeInfo, times(2)).removeAction(removeActionCaptor.capture())
        val removedActionIds = removeActionCaptor.allValues.map { it.id }
        assertThat(removedActionIds)
            .containsExactly(
                R.id.action_move_display_block_down,
                R.id.action_move_display_block_right,
            )
            .inOrder()
    }

    @Test
    fun onInitializeAccessibilityNodeInfo_removesAllActionsWhenImmovable() {
        // Setup: Block is immovable
        block.reset(
            DISPLAY_ID,
            DISPLAY_ID,
            PointF(10f, 10f),
            PointF(20f, 20f),
            0.5f,
            DISPLAY_SIZE,
            ArrowMovement.immovable(),
        )
        val mockNodeInfo = mock(AccessibilityNodeInfo::class.java)

        // Action
        block.onInitializeAccessibilityNodeInfo(mockNodeInfo)

        // Verification: No actions should be added
        verify(mockNodeInfo, never()).addAction(any(AccessibilityAction::class.java))

        // Verification: All directional actions should be removed
        val removeActionCaptor = ArgumentCaptor.forClass(AccessibilityAction::class.java)
        verify(mockNodeInfo, times(4)).removeAction(removeActionCaptor.capture())
        val removedActionIds = removeActionCaptor.allValues.map { it.id }
        assertThat(removedActionIds)
            .containsExactly(
                R.id.action_move_display_block_up,
                R.id.action_move_display_block_down,
                R.id.action_move_display_block_left,
                R.id.action_move_display_block_right,
            )
            .inOrder()
    }

    @Test
    fun onInitializeAccessibilityNodeInfo_addsAllActionsWhenFullyMovable() {
        // Setup: Block is fully movable
        val arrowMovement =
            ArrowMovement(
                mapOf(
                    Direction.UP to true,
                    Direction.DOWN to true,
                    Direction.LEFT to true,
                    Direction.RIGHT to true,
                )
            )
        block.reset(
            DISPLAY_ID,
            DISPLAY_ID,
            PointF(10f, 10f),
            PointF(20f, 20f),
            0.5f,
            DISPLAY_SIZE,
            arrowMovement,
        )
        val mockNodeInfo = mock(AccessibilityNodeInfo::class.java)

        // Action
        block.onInitializeAccessibilityNodeInfo(mockNodeInfo)

        // Verification: No actions should be removed
        verify(mockNodeInfo, never()).removeAction(any(AccessibilityAction::class.java))

        // Verification: All directional actions should be added
        val addActionCaptor = ArgumentCaptor.forClass(AccessibilityAction::class.java)
        verify(mockNodeInfo, times(4)).addAction(addActionCaptor.capture())
        val addedActionIds = addActionCaptor.allValues.map { it.id }
        assertThat(addedActionIds)
            .containsExactly(
                R.id.action_move_display_block_up,
                R.id.action_move_display_block_down,
                R.id.action_move_display_block_left,
                R.id.action_move_display_block_right,
            )
            .inOrder()
    }

    private fun applyRequestedSize() {
        block.right = block.left + block.layoutParams.width
        block.bottom = block.top + block.layoutParams.height
    }

    private companion object {

        private fun DisplayBlock.wallpaperView(): SurfaceView {
            return findViewById<SurfaceView>(R.id.display_block_wallpaper)
        }

        private const val DISPLAY_ID = 123
        private const val MIRRORED_DISPLAY_ID = 456
        private val DISPLAY_SIZE = Size(1280, 720)
        private const val BLOCK_WIDTH = 200f
        private const val BLOCK_HEIGHT = 300f
    }
}
