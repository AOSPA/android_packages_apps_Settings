/*
 * Copyright 2026 The Android Open Source Project
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
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.View.AccessibilityDelegate
import android.view.View.GONE
import android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
import android.view.View.VISIBLE
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.android.settings.R

/** Controller for accessibility actions and arrow button UI for [DisplayBlock]. */
class DisplayBlockA11yController(
    private val context: Context,
    private val onMove: (Direction) -> Unit,
) {
    var arrowMovement: ArrowMovement = ArrowMovement.immovable()
    var areArrowsVisible: Boolean = false

    val arrowSizePx = context.resources.getDimensionPixelSize(R.dimen.display_block_arrow_size)

    val arrowTappableAreaSizePx =
        context.resources.getDimensionPixelSize(R.dimen.display_block_arrow_tappable_area_size)

    val arrowButtons: Map<Direction, View> =
        ARROW_BUTTON_PROPERTIES.mapValues { (_, props) -> createArrowButton(props) }

    val accessibilityDelegate =
        object : AccessibilityDelegate() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfo,
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                if (isMovable(Direction.UP)) {
                    info.addAction(a11yActionMoveUp)
                } else {
                    info.removeAction(a11yActionMoveUp)
                }

                if (isMovable(Direction.DOWN)) {
                    info.addAction(a11yActionMoveDown)
                } else {
                    info.removeAction(a11yActionMoveDown)
                }

                if (isMovable(Direction.LEFT)) {
                    info.addAction(a11yActionMoveLeft)
                } else {
                    info.removeAction(a11yActionMoveLeft)
                }

                if (isMovable(Direction.RIGHT)) {
                    info.addAction(a11yActionMoveRight)
                } else {
                    info.removeAction(a11yActionMoveRight)
                }
            }

            override fun performAccessibilityAction(
                host: View,
                action: Int,
                args: Bundle?,
            ): Boolean {
                return when (action) {
                    R.id.action_move_display_block_up -> {
                        onMove(Direction.UP)
                        true
                    }
                    R.id.action_move_display_block_down -> {
                        onMove(Direction.DOWN)
                        true
                    }
                    R.id.action_move_display_block_left -> {
                        onMove(Direction.LEFT)
                        true
                    }
                    R.id.action_move_display_block_right -> {
                        onMove(Direction.RIGHT)
                        true
                    }
                    else -> super.performAccessibilityAction(host, action, args)
                }
            }
        }

    private val a11yActionMoveUp =
        AccessibilityAction(
            R.id.action_move_display_block_up,
            context.getString(R.string.external_display_topology_a11y_action_move_up),
        )

    private val a11yActionMoveDown =
        AccessibilityAction(
            R.id.action_move_display_block_down,
            context.getString(R.string.external_display_topology_a11y_action_move_down),
        )

    private val a11yActionMoveLeft =
        AccessibilityAction(
            R.id.action_move_display_block_left,
            context.getString(R.string.external_display_topology_a11y_action_move_left),
        )

    private val a11yActionMoveRight =
        AccessibilityAction(
            R.id.action_move_display_block_right,
            context.getString(R.string.external_display_topology_a11y_action_move_right),
        )

    fun updateArrowVisibility() {
        arrowButtons.forEach { (direction, buttonView) ->
            buttonView.visibility = if (isMovable(direction) && areArrowsVisible) VISIBLE else GONE
        }
    }

    fun isMovable(direction: Direction) = arrowMovement.directionMapping[direction] ?: false

    private fun createArrowButton(props: ArrowButtonProperties) =
        FrameLayout(context).apply {
            layoutParams =
                FrameLayout.LayoutParams(
                        arrowTappableAreaSizePx,
                        arrowTappableAreaSizePx,
                        props.gravity,
                    )
                    .apply {
                        when (props.direction) {
                            Direction.UP -> topMargin = props.verticalOffset
                            Direction.DOWN -> bottomMargin = props.verticalOffset
                            Direction.LEFT -> leftMargin = props.horizontalOffset
                            Direction.RIGHT -> rightMargin = props.horizontalOffset
                        }
                    }
            addView(
                ImageButton(context).apply {
                    setImageResource(props.drawableRes)
                    background = context.getDrawable(R.drawable.display_block_arrow_background)
                    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
                    isFocusable = false
                    setClickable(false)
                },
                FrameLayout.LayoutParams(arrowSizePx, arrowSizePx, Gravity.CENTER),
            )
            contentDescription = context.getString(props.contentDescriptionRes)
            isFocusable = true
            setOnClickListener { _ -> onMove(props.direction) }
            visibility = GONE
        }

    private data class ArrowButtonProperties(
        @param:DrawableRes val drawableRes: Int,
        @param:StringRes val contentDescriptionRes: Int,
        val direction: Direction,
        val gravity: Int,
        val horizontalOffset: Int = 0,
        val verticalOffset: Int = 0,
    )

    companion object {
        private val ARROW_BUTTON_PROPERTIES =
            mapOf(
                Direction.UP to
                    ArrowButtonProperties(
                        R.drawable.display_block_arrow_up,
                        R.string.external_display_topology_a11y_action_move_up,
                        Direction.UP,
                        Gravity.TOP or Gravity.CENTER_HORIZONTAL,
                    ),
                Direction.DOWN to
                    ArrowButtonProperties(
                        R.drawable.display_block_arrow_down,
                        R.string.external_display_topology_a11y_action_move_down,
                        Direction.DOWN,
                        Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
                    ),
                Direction.LEFT to
                    ArrowButtonProperties(
                        R.drawable.display_block_arrow_left,
                        R.string.external_display_topology_a11y_action_move_left,
                        Direction.LEFT,
                        Gravity.START or Gravity.CENTER_VERTICAL,
                    ),
                Direction.RIGHT to
                    ArrowButtonProperties(
                        R.drawable.display_block_arrow_right,
                        R.string.external_display_topology_a11y_action_move_right,
                        Direction.RIGHT,
                        Gravity.END or Gravity.CENTER_VERTICAL,
                    ),
            )
    }
}
