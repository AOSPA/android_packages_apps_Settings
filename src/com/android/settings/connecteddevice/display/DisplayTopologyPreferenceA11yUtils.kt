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

package com.android.settings.connecteddevice.display

import android.graphics.PointF
import android.graphics.Rect
import android.hardware.display.DisplayTopology
import android.hardware.display.DisplayTopologyGraph
import android.view.InputDevice
import android.view.MotionEvent
import android.view.TouchDelegate
import android.view.View

const val A11Y_DRAG_STEPS = 10
const val A11Y_MOVE_DISTANCE_DP = 16f

enum class Direction {
    UP,
    DOWN,
    LEFT,
    RIGHT,
}

fun createMotionEvent(
    downTime: Long,
    eventTime: Long,
    action: Int,
    point: PointF,
    screenPositionOffset: PointF,
): MotionEvent {
    val properties =
        MotionEvent.PointerProperties().apply {
            id = 0
            toolType = MotionEvent.TOOL_TYPE_FINGER
        }
    val coords =
        MotionEvent.PointerCoords().apply {
            x = point.x + screenPositionOffset.x
            y = point.y + screenPositionOffset.y
            pressure = 1f
            size = 1f
        }

    return MotionEvent.obtain(
        downTime,
        eventTime,
        action,
        /* pointerCount= */ 1,
        /* pointerProperties= */ arrayOf(properties),
        /* pointerCoords= */ arrayOf(coords),
        /* metaState= */ 0,
        /* buttonState= */ 0,
        /* xPrecision= */ 1f,
        /* yPrecision= */ 1f,
        /* deviceId= */ 0,
        /* edgeFlags= */ 0,
        /* source= */ InputDevice.SOURCE_TOUCHSCREEN,
        /* flags= */ 0,
    )
}

private data class MovementState(var canMove: Boolean = false, var isBlocked: Boolean = false)

data class ArrowMovement(val directionMapping: Map<Direction, Boolean>) {

    companion object {
        fun immovable(): ArrowMovement {
            return ArrowMovement(Direction.entries.associateWith { false })
        }
    }
}

/**
 * Calculates the possible movement direction for each displays in the [DisplayTopologyGraph]
 * 1. Check if 2 displays are connected on a corner. If so, display should be able to move on those
 *    directions
 * 2. Check if any display is blocking on any direction. If there is, mark that direction as
 *    blocked. At the same time,allow movement perpendicular to the blocked display direction (e.g.
 *    if there's a display is on the left / right, then the current display can move down or up
 * 3. Check if display can move to a particular direction, and not blocked in step2 by any other
 *    displays, then this direction should be a move option
 */
fun calculateDisplayArrowMovement(graph: DisplayTopologyGraph): Map<Int, ArrowMovement> {
    val allNodes = graph.getDisplayNodes()

    return (0 until allNodes.size()).associate { i ->
        val currentNode = allNodes.valueAt(i)
        val currentId = currentNode.getDisplayId()

        val states = Direction.entries.associateWith { MovementState() }.toMutableMap()

        val neighborEdges =
            currentNode.getAdjacentEdges().groupBy { it.getDisplayNode().getDisplayId() }
        for ((_, edges) in neighborEdges) {
            val positions = edges.map { it.getPosition() }.toSet()
            val u = states.getValue(Direction.UP)
            val d = states.getValue(Direction.DOWN)
            val l = states.getValue(Direction.LEFT)
            val r = states.getValue(Direction.RIGHT)

            when {
                // Corner adjacency enable movement.
                positions.contains(DisplayTopology.POSITION_TOP) &&
                    positions.contains(DisplayTopology.POSITION_LEFT) -> {
                    u.canMove = true
                    l.canMove = true
                }
                positions.contains(DisplayTopology.POSITION_TOP) &&
                    positions.contains(DisplayTopology.POSITION_RIGHT) -> {
                    u.canMove = true
                    r.canMove = true
                }
                positions.contains(DisplayTopology.POSITION_BOTTOM) &&
                    positions.contains(DisplayTopology.POSITION_LEFT) -> {
                    d.canMove = true
                    l.canMove = true
                }
                positions.contains(DisplayTopology.POSITION_BOTTOM) &&
                    positions.contains(DisplayTopology.POSITION_RIGHT) -> {
                    d.canMove = true
                    r.canMove = true
                }
                // Edge adjacencies block one direction and enable perpendicular ones.
                positions.contains(DisplayTopology.POSITION_TOP) -> {
                    u.isBlocked = true
                    l.canMove = true
                    r.canMove = true
                }
                positions.contains(DisplayTopology.POSITION_BOTTOM) -> {
                    d.isBlocked = true
                    l.canMove = true
                    r.canMove = true
                }
                positions.contains(DisplayTopology.POSITION_LEFT) -> {
                    l.isBlocked = true
                    u.canMove = true
                    d.canMove = true
                }
                positions.contains(DisplayTopology.POSITION_RIGHT) -> {
                    r.isBlocked = true
                    u.canMove = true
                    d.canMove = true
                }
            }
        }

        val visibilities = states.mapValues { (_, state) -> state.canMove && !state.isBlocked }

        currentId to ArrowMovement(visibilities)
    }
}

// Copied from {@code com.android.internal.widget.ConversationLayout}
class TouchDelegateComposite(view: View) : TouchDelegate(Rect(), view) {
    private val mDelegates = ArrayList<TouchDelegate>()

    fun add(delegate: TouchDelegate) {
        mDelegates.add(delegate)
    }

    fun clear() {
        mDelegates.clear()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        for (delegate in mDelegates) {
            event.setLocation(x, y)
            if (delegate.onTouchEvent(event)) {
                return true
            }
        }
        return false
    }
}
