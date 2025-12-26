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
import android.os.Looper
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Tests for [TopologyHintTextView] */
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [SettingsShadowResources::class])
class TopologyHintTextViewTest {

    // Use simple, non-descriptive strings to verify the correct resource ID is used.
    private val ARROWS_SHOWN_HINT = "arrows_shown_hint"
    private val DRAG_HINT = "drag_hint"
    private val TAP_HINT = "tap_hint"

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var hintView: TopologyHintTextView

    @Before
    fun setUp() {
        SettingsShadowResources.overrideResource(
            R.string.external_display_tap_arrows_to_rearrange,
            ARROWS_SHOWN_HINT,
        )
        SettingsShadowResources.overrideResource(R.string.external_display_topology_hint, DRAG_HINT)
        SettingsShadowResources.overrideResource(
            R.string.external_display_tap_to_show_arrows,
            TAP_HINT,
        )

        hintView = TopologyHintTextView(context)

        // Disable animations to make text changes synchronous for testing.
        hintView.disableAnimation()
    }

    @Test
    fun updateState_withOneDisplay_showsNoHint() {
        hintView.updateState(arrowsShown = false, displayCount = 1)
        assertThat(hintView.text.toString()).isEmpty()
    }

    @Test
    fun updateState_defaultCycling_showsInitialDragHint() {
        hintView.updateState(arrowsShown = false, displayCount = 2)
        assertThat(hintView.text.toString()).isEqualTo(DRAG_HINT)
    }

    @Test
    fun defaultCycling_cyclesToTapHintAfterDelay() {
        hintView.updateState(arrowsShown = false, displayCount = 2)
        assertThat(hintView.text.toString()).isEqualTo(DRAG_HINT)

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(5000))
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(hintView.text.toString()).isEqualTo(TAP_HINT)

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(5000))
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(hintView.text.toString()).isEqualTo(DRAG_HINT)
    }

    @Test
    fun stateTransition_fromCyclingDragHintToArrows_showsArrowsHint() {
        hintView.updateState(arrowsShown = false, displayCount = 2)
        assertThat(hintView.text.toString()).isEqualTo(DRAG_HINT)

        hintView.updateState(arrowsShown = true, displayCount = 2)
        assertThat(hintView.text.toString()).isEqualTo(ARROWS_SHOWN_HINT)
    }

    @Test
    fun stateTransition_fromCyclingTapHintToArrows_showsArrowsHint() {
        hintView.updateState(arrowsShown = false, displayCount = 2)
        assertThat(hintView.text.toString()).isEqualTo(DRAG_HINT)

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(5000))
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(hintView.text.toString()).isEqualTo(TAP_HINT)

        hintView.updateState(arrowsShown = true, displayCount = 2)
        assertThat(hintView.text.toString()).isEqualTo(ARROWS_SHOWN_HINT)
    }

    @Test
    fun updateState_withArrowsShown_showsArrowsHint() {
        hintView.updateState(arrowsShown = true, displayCount = 2)
        assertThat(hintView.text.toString()).isEqualTo(ARROWS_SHOWN_HINT)
    }

    @Test
    fun stateTransition_fromArrowsToCycling_showsInitialDragHint() {
        hintView.updateState(arrowsShown = true, displayCount = 2)
        assertThat(hintView.text.toString()).isEqualTo(ARROWS_SHOWN_HINT)

        hintView.updateState(arrowsShown = false, displayCount = 2)
        assertThat(hintView.text.toString()).isEqualTo(DRAG_HINT)
    }

    @Test
    fun stateTransition_toNoHint_clearsText() {
        hintView.updateState(arrowsShown = true, displayCount = 2)
        assertThat(hintView.text.toString()).isEqualTo(ARROWS_SHOWN_HINT)
        assertThat(hintView.visibility).isEqualTo(View.VISIBLE)

        hintView.updateState(arrowsShown = false, displayCount = 1)
        assertThat(hintView.text.toString()).isEmpty()
        assertThat(hintView.visibility).isEqualTo(View.GONE)
    }

    @Test
    fun updateState_withSameState_doesNothing() {
        hintView.updateState(arrowsShown = false, displayCount = 2)
        val initialText = hintView.text.toString()

        hintView.updateState(arrowsShown = false, displayCount = 2)

        assertThat(hintView.text.toString()).isEqualTo(initialText)
    }
}
