/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.connecteddevice.display

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import androidx.annotation.VisibleForTesting
import androidx.appcompat.widget.AppCompatTextView
import com.android.settings.R
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * A custom TextView that manages its own state to display contextual hints for the display topology
 * arrangement panel
 */
class TopologyHintTextView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    AppCompatTextView(context, attrs, defStyleAttr) {

    private val handler = Handler(Looper.getMainLooper())
    private var enableAnimation = true
    private var cycleRunnable: Runnable? = null
    private var currentCyclingTextState = CyclingTextState.DRAG

    private enum class State {
        NO_HINT,
        DEFAULT_CYCLING,
        ARROWS_SHOWN,
    }

    private enum class CyclingTextState {
        DRAG,
        TAP,
    }

    private var currentState: State = State.NO_HINT

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopCycling()
        animate().cancel()
    }

    @VisibleForTesting
    internal fun disableAnimation() {
        enableAnimation = false
    }

    /**
     * Updates the hint's text and behavior based on the current state of the display topology.
     *
     * @param arrowsShown True if the navigation arrows are visible on a display block.
     * @param displayCount The total number of displays being shown.
     */
    fun updateState(arrowsShown: Boolean, displayCount: Int) {
        val newState =
            when {
                displayCount <= 1 -> State.NO_HINT
                arrowsShown -> State.ARROWS_SHOWN
                else -> State.DEFAULT_CYCLING
            }

        if (newState == currentState) return

        currentState = newState
        // Stop any previous cycling before deciding on the next action.
        stopCycling()

        when (currentState) {
            State.NO_HINT -> animateTextChange("")
            State.ARROWS_SHOWN ->
                animateTextChange(
                    context.getString(R.string.external_display_tap_arrows_to_rearrange)
                )
            State.DEFAULT_CYCLING -> startCycling()
        }
    }

    private fun startCycling() {
        // Reset to the first message in the cycle
        currentCyclingTextState = CyclingTextState.DRAG
        val initialText = context.getString(R.string.external_display_topology_hint)
        animateTextChange(initialText)

        cycleRunnable =
            Runnable {
                    currentCyclingTextState =
                        if (currentCyclingTextState == CyclingTextState.DRAG) {
                            animateTextChange(
                                context.getString(R.string.external_display_tap_to_show_arrows)
                            )
                            CyclingTextState.TAP
                        } else {
                            animateTextChange(
                                context.getString(R.string.external_display_topology_hint)
                            )
                            CyclingTextState.DRAG
                        }
                    // Post the next cycle
                    cycleRunnable?.let {
                        handler.postDelayed(it, HINT_CYCLE_DELAY_MS.inWholeMilliseconds)
                    }
                }
                .also {
                    // Start the first cycle after the initial text is shown
                    handler.postDelayed(it, HINT_CYCLE_DELAY_MS.inWholeMilliseconds)
                }
    }

    private fun stopCycling() {
        cycleRunnable?.let { handler.removeCallbacks(it) }
        cycleRunnable = null
    }

    private fun animateTextChange(newText: String) {
        if (text.toString() == newText) return
        if (!enableAnimation) {
            text = newText
            return
        }

        animate().cancel()
        animate()
            .alpha(0f)
            .setDuration(HINT_ANIMATION_DURATION_MS.inWholeMilliseconds)
            .withEndAction {
                this@TopologyHintTextView.text = newText
                animate()
                    .alpha(1f)
                    .setDuration(HINT_ANIMATION_DURATION_MS.inWholeMilliseconds)
                    .withEndAction(null)
            }
    }

    private companion object {
        val HINT_CYCLE_DELAY_MS = 5.seconds
        val HINT_ANIMATION_DURATION_MS = 250.milliseconds
    }
}
