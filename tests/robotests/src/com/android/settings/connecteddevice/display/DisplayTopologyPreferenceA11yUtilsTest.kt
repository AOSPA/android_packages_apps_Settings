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
import android.graphics.Rect
import android.hardware.display.DisplayTopology
import android.view.MotionEvent
import android.view.TouchDelegate
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnit
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DisplayTopologyPreferenceA11yUtilsTest {

    @get:Rule
    val mockitoRule = MockitoJUnit.rule()

    @Mock private lateinit var mockDelegate1: TouchDelegate
    @Mock private lateinit var mockDelegate2: TouchDelegate

    private lateinit var context: Context
    private lateinit var view: View
    private lateinit var composite: TouchDelegateComposite
    private lateinit var motionEvent: MotionEvent

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        view = View(context)
        composite = TouchDelegateComposite(view)
        // Create a sample MotionEvent for testing.
        motionEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 10f, 10f, 0)
    }

    @Test
    fun calculateDisplayArrowMovement_isolatedDisplay_isNotMovable() {
        // [1]
        val graph =
            DisplayTopology().apply { addDisplay(1, DISPLAY_SIZE, DISPLAY_SIZE, DENSITY) }.graph

        val result = calculateDisplayArrowMovement(graph)

        // An isolated display has no neighbors to enable movement, so all directions are false.
        assertThat(result).hasSize(1)
        assertMovement(result, 1, up = false, down = false, left = false, right = false)
    }

    @Test
    fun calculateDisplayArrowMovement_edgeAdjacency_enablesPerpendicularMovement() {
        // [1] - [2]
        val graph =
            DisplayTopology()
                .apply {
                    addDisplay(1, DISPLAY_SIZE, DISPLAY_SIZE, DENSITY)
                    addDisplay(2, DISPLAY_SIZE, DISPLAY_SIZE, DENSITY)
                    rearrange(mapOf(1 to PointF(0f, 0f), 2 to PointF(DISPLAY_SIZE.toFloat(), 0f)))
                }
                .graph

        val result = calculateDisplayArrowMovement(graph)

        assertThat(result).hasSize(2)
        // Display 1 is blocked on the right, but can move up/down
        assertMovement(result, 1, up = true, down = true, left = false, right = false)
        // Display 2 is blocked on the left, but can move up/down
        assertMovement(result, 2, up = true, down = true, left = false, right = false)
    }

    @Test
    fun calculateDisplayArrowMovement_cornerAdjacency_enablesCornerMovement() {
        // Two displays touching only at a corner
        // [1]
        //    [2]
        val graph =
            DisplayTopology()
                .apply {
                    addDisplay(1, DISPLAY_SIZE, DISPLAY_SIZE, DENSITY)
                    addDisplay(2, DISPLAY_SIZE, DISPLAY_SIZE, DENSITY)
                    rearrange(
                        mapOf(
                            1 to PointF(0f, 0f),
                            2 to PointF(DISPLAY_SIZE.toFloat(), DISPLAY_SIZE.toFloat()),
                        )
                    )
                }
                .graph

        val result = calculateDisplayArrowMovement(graph)

        assertThat(result).hasSize(2)
        // Display 1 can move towards the corner (down and right)
        assertMovement(result, 1, up = false, down = true, left = false, right = true)
        // Display 2 can move towards the corner (up and left)
        assertMovement(result, 2, up = true, down = false, left = true, right = false)
    }

    @Test
    fun calculateDisplayArrowMovement_blockedOnTwoSides_movableOnOtherSides() {
        // [2]
        // [1] - [3]
        val graph =
            DisplayTopology()
                .apply {
                    addDisplay(1, DISPLAY_SIZE, DISPLAY_SIZE, DENSITY)
                    addDisplay(2, DISPLAY_SIZE, DISPLAY_SIZE, DENSITY)
                    addDisplay(3, DISPLAY_SIZE, DISPLAY_SIZE, DENSITY)
                    rearrange(
                        mapOf(
                            1 to PointF(0f, 0f),
                            2 to PointF(0f, -DISPLAY_SIZE.toFloat()),
                            3 to PointF(DISPLAY_SIZE.toFloat(), 0f),
                        )
                    )
                }
                .graph

        val result = calculateDisplayArrowMovement(graph)

        assertThat(result).hasSize(3)
        // Down based on connection with Display 3, Left based on connection with Display 2
        assertMovement(result, 1, up = false, down = true, left = true, right = false)
        assertMovement(result, 2, up = false, down = false, left = true, right = true)
        assertMovement(result, 3, up = true, down = true, left = false, right = false)
    }

    @Test
    fun calculateDisplayArrowMovement_enclosedByAllDisplays_isNotMovable() {
        //       [5]
        //        |
        // [4] - [2] - [3]
        //        |
        //       [1]
        val graph =
            DisplayTopology()
                .apply {
                    addDisplay(1, DISPLAY_SIZE, DISPLAY_SIZE, DENSITY)
                    addDisplay(2, DISPLAY_SIZE, DISPLAY_SIZE, DENSITY)
                    addDisplay(3, DISPLAY_SIZE, DISPLAY_SIZE, DENSITY)
                    addDisplay(4, DISPLAY_SIZE, DISPLAY_SIZE, DENSITY)
                    addDisplay(5, DISPLAY_SIZE, DISPLAY_SIZE, DENSITY)
                    rearrange(
                        mapOf(
                            1 to PointF(0f, DISPLAY_SIZE.toFloat()),
                            2 to PointF(0f, 0f),
                            3 to PointF(DISPLAY_SIZE.toFloat(), 0f),
                            4 to PointF(-DISPLAY_SIZE.toFloat(), 0f),
                            5 to PointF(0f, -DISPLAY_SIZE.toFloat()),
                        )
                    )
                }
                .graph

        val result = calculateDisplayArrowMovement(graph)

        assertThat(result).hasSize(5)
        // Display 2 enclosed by all other displays, immovable
        assertMovement(result, 2, up = false, down = false, left = false, right = false)
        assertMovement(result, 1, up = false, down = false, left = true, right = true)
        assertMovement(result, 3, up = true, down = true, left = false, right = false)
        assertMovement(result, 4, up = true, down = true, left = false, right = false)
        assertMovement(result, 5, up = false, down = false, left = true, right = true)
    }

    @Test
    fun touchDelegateComposite_onTouchEvent_noDelegates_returnsFalse() {
        // No delegates added, onTouchEvent should return false.
        assertThat(composite.onTouchEvent(motionEvent)).isFalse()
    }

    @Test
    fun touchDelegateComposite_onTouchEvent_singleDelegateHandlesEvent_returnsTrue() {
        // Add a single delegate that handles the touch event.
        composite.add(mockDelegate1)
        `when`(mockDelegate1.onTouchEvent(motionEvent)).thenReturn(true)

        // The composite should return true.
        assertThat(composite.onTouchEvent(motionEvent)).isTrue()

        // Verify the delegate's onTouchEvent was called.
        verify(mockDelegate1).onTouchEvent(motionEvent)
    }

    @Test
    fun touchDelegateComposite_onTouchEvent_singleDelegateDoesNotHandleEvent_returnsFalse() {
        // Add a single delegate that does not handle the touch event.
        composite.add(mockDelegate1)
        `when`(mockDelegate1.onTouchEvent(motionEvent)).thenReturn(false)

        // The composite should return false.
        assertThat(composite.onTouchEvent(motionEvent)).isFalse()

        // Verify the delegate's onTouchEvent was called.
        verify(mockDelegate1).onTouchEvent(motionEvent)
    }

    @Test
    fun touchDelegateComposite_onTouchEvent_multipleDelegates_firstHandlesEvent_returnsTrue() {
        // Add two delegates, where the first one handles the event.
        composite.add(mockDelegate1)
        composite.add(mockDelegate2)
        `when`(mockDelegate1.onTouchEvent(motionEvent)).thenReturn(true)

        // The composite should return true immediately after the first delegate handles it.
        assertThat(composite.onTouchEvent(motionEvent)).isTrue()

        // Verify the first delegate was called.
        verify(mockDelegate1).onTouchEvent(motionEvent)
        // The second delegate should not be called.
        verify(mockDelegate2, never()).onTouchEvent(motionEvent)
    }

    @Test
    fun touchDelegateComposite_onTouchEvent_multipleDelegates_secondHandlesEvent_returnsTrue() {
        // Add two delegates, where the second one handles the event.
        composite.add(mockDelegate1)
        composite.add(mockDelegate2)
        `when`(mockDelegate1.onTouchEvent(motionEvent)).thenReturn(false)
        `when`(mockDelegate2.onTouchEvent(motionEvent)).thenReturn(true)

        // The composite should return true.
        assertThat(composite.onTouchEvent(motionEvent)).isTrue()

        // Verify both delegates were called.
        verify(mockDelegate1).onTouchEvent(motionEvent)
        verify(mockDelegate2).onTouchEvent(motionEvent)
    }

    @Test
    fun touchDelegateComposite_onTouchEvent_multipleDelegates_noneHandleEvent_returnsFalse() {
        // Add two delegates, neither of which handles the event.
        composite.add(mockDelegate1)
        composite.add(mockDelegate2)
        `when`(mockDelegate1.onTouchEvent(motionEvent)).thenReturn(false)
        `when`(mockDelegate2.onTouchEvent(motionEvent)).thenReturn(false)

        // The composite should return false.
        assertThat(composite.onTouchEvent(motionEvent)).isFalse()

        // Verify both delegates were called.
        verify(mockDelegate1).onTouchEvent(motionEvent)
        verify(mockDelegate2).onTouchEvent(motionEvent)
    }

    @Test
    fun touchDelegateComposite_clear_removesAllDelegates() {
        // Add a delegate that would handle the event.
        composite.add(mockDelegate1)
        `when`(mockDelegate1.onTouchEvent(motionEvent)).thenReturn(true)

        // Clear the delegates.
        composite.clear()

        // After clearing, onTouchEvent should return false and not call the delegate.
        assertThat(composite.onTouchEvent(motionEvent)).isFalse()
        verify(mockDelegate1, never()).onTouchEvent(motionEvent)
    }

    @Test
    fun touchDelegateComposite_onTouchEvent_resetsEventLocationForEachDelegate() {
        val originalX = motionEvent.x
        val originalY = motionEvent.y

        // Create a custom delegate that modifies the event location.
        val modifyingDelegate =
            object : TouchDelegate(Rect(), view) {
                override fun onTouchEvent(event: MotionEvent): Boolean {
                    // Verify the event has the original location.
                    assertThat(event.x).isEqualTo(originalX)
                    assertThat(event.y).isEqualTo(originalY)
                    // Modify the event location.
                    event.setLocation(99f, 99f)
                    return false // Return false to allow processing to continue.
                }
            }

        // Create a second delegate to check if the location was reset.
        val checkingDelegate =
            object : TouchDelegate(Rect(), view) {
                override fun onTouchEvent(event: MotionEvent): Boolean {
                    // Verify the event location was reset to the original coordinates.
                    assertThat(event.x).isEqualTo(originalX)
                    assertThat(event.y).isEqualTo(originalY)
                    return false
                }
            }

        composite.add(modifyingDelegate)
        composite.add(checkingDelegate)

        // Trigger the touch event. Assertions are inside the delegates.
        composite.onTouchEvent(motionEvent)
    }

    private fun assertMovement(
        resultMap: Map<Int, ArrowMovement>,
        displayId: Int,
        up: Boolean,
        down: Boolean,
        left: Boolean,
        right: Boolean,
    ) {
        val movement = resultMap[displayId]
        assertThat(movement).isNotNull()
        assertThat(movement!!.directionMapping[Direction.UP]).isEqualTo(up)
        assertThat(movement.directionMapping[Direction.DOWN]).isEqualTo(down)
        assertThat(movement.directionMapping[Direction.LEFT]).isEqualTo(left)
        assertThat(movement.directionMapping[Direction.RIGHT]).isEqualTo(right)
    }

    private companion object {
        const val DISPLAY_SIZE = 100
        const val DENSITY = 160
    }
}
