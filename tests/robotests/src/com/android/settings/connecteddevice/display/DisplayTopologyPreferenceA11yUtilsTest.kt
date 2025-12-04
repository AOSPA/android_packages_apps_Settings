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

import android.graphics.PointF
import android.hardware.display.DisplayTopology
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DisplayTopologyPreferenceA11yUtilsTest {

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
