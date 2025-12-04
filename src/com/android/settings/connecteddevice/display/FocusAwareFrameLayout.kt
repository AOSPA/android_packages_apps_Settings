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
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout

/**
 * A custom FrameLayout that detects when keyboard focus is about to exit its boundaries and
 * notifies an [NextFocusFinder]. This allows for orchestrating complex focus navigation between
 * different components on a screen.
 */
open class FocusAwareFrameLayout
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    FrameLayout(context, attrs, defStyleAttr) {

    private var nextFocusFinder: NextFocusFinder? = null

    override fun focusSearch(focused: View?, direction: Int): View? {
        if (focused == null) {
            return super.focusSearch(focused, direction)
        }

        // Get a fresh list of all focusable children in their logical navigation order
        val descendantFocusables = ArrayList<View>()
        addFocusables(descendantFocusables, direction)

        // Determine if the currently focused view is the first or the last in the list
        val currentIndex = descendantFocusables.indexOf(focused)
        val isMovingForward = isMovingForward(direction)
        val isMovingBackward = isMovingBackward(direction)

        val isAtBoundary =
            (isMovingForward && currentIndex == descendantFocusables.size - 1) ||
                (isMovingBackward && currentIndex == 0)
        if (isAtBoundary) {
            // Focus is exiting, leaving next focus target to parent orchestrating the views
            val nextFocus = nextFocusFinder?.findNextFocus(focused, direction)
            if (nextFocus != null) {
                return nextFocus
            }
        }

        // Use default behavior if it's not at boundary or focus exit behavior is not determined
        return super.focusSearch(focused, direction)
    }

    fun setNextFocusFinder(finder: NextFocusFinder?) {
        this.nextFocusFinder = finder
    }

    interface NextFocusFinder {

        /**
         * Determine the next view to focus (from parent hierarchy) when focus left the bounds
         *
         * @param focused The view that currently has focus
         * @param direction The direction of focus travel (e.g., [View.FOCUS_FORWARD])
         * @return The next view that should receive focus, or [null] to allow default behavior
         */
        fun findNextFocus(focused: View?, direction: Int): View?
    }

    companion object {
        fun isMovingForward(direction: Int): Boolean {
            return direction == FOCUS_FORWARD || direction == FOCUS_RIGHT || direction == FOCUS_DOWN
        }

        fun isMovingBackward(direction: Int): Boolean {
            return direction == FOCUS_BACKWARD || direction == FOCUS_LEFT || direction == FOCUS_UP
        }
    }
}
