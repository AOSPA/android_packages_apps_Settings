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

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class FocusAwareFrameLayoutTest {

    private lateinit var activity: Activity
    private lateinit var layout: FocusAwareFrameLayout
    private lateinit var child1: View
    private lateinit var child2: View
    private lateinit var child3: View
    private lateinit var externalView: View

    @Mock private lateinit var nextFocusFinder: FocusAwareFrameLayout.NextFocusFinder

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        // Create an activity to host the layout. This ensures the view hierarchy is attached
        // to a window, allowing super.focusSearch() (which uses FocusFinder) to work correctly.
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        externalView = View(activity)

        layout = FocusAwareFrameLayout(activity)
        layout.layoutParams =
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        layout.setNextFocusFinder(nextFocusFinder)

        // Setup 3 children in a vertical stack.
        // For logical navigation (FORWARD/BACKWARD), the order of addition (index) matters.
        // For geometric navigation (UP/DOWN), the layout coordinates matter.

        // Child 1: Top
        child1 = createFocusableView(0, 0, 100, 100)
        // Child 2: Middle
        child2 = createFocusableView(0, 100, 100, 200)
        // Child 3: Bottom
        child3 = createFocusableView(0, 200, 100, 300)

        layout.addView(child1)
        layout.addView(child2)
        layout.addView(child3)

        activity.setContentView(layout)

        // Force a measure and layout pass to ensure children have valid coordinates
        // for geometric focus search.
        val spec = View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY)
        layout.measure(spec, spec)
        layout.layout(0, 0, 1000, 1000)
    }

    private fun createFocusableView(l: Int, t: Int, r: Int, b: Int): View {
        val view = View(activity)
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        view.visibility = View.VISIBLE

        // Set LayoutParams to position them in FrameLayout
        val params = FrameLayout.LayoutParams(r - l, b - t)
        params.leftMargin = l
        params.topMargin = t
        view.layoutParams = params

        // Explicitly set layout to ensure view has bounds immediately
        view.layout(l, t, r, b)
        return view
    }

    @Test
    fun isMovingForward_checkDirections() {
        assertThat(FocusAwareFrameLayout.isMovingForward(View.FOCUS_FORWARD)).isTrue()
        assertThat(FocusAwareFrameLayout.isMovingForward(View.FOCUS_RIGHT)).isTrue()
        assertThat(FocusAwareFrameLayout.isMovingForward(View.FOCUS_DOWN)).isTrue()

        assertThat(FocusAwareFrameLayout.isMovingForward(View.FOCUS_BACKWARD)).isFalse()
        assertThat(FocusAwareFrameLayout.isMovingForward(View.FOCUS_LEFT)).isFalse()
        assertThat(FocusAwareFrameLayout.isMovingForward(View.FOCUS_UP)).isFalse()
    }

    @Test
    fun isMovingBackward_checkDirections() {
        assertThat(FocusAwareFrameLayout.isMovingBackward(View.FOCUS_BACKWARD)).isTrue()
        assertThat(FocusAwareFrameLayout.isMovingBackward(View.FOCUS_LEFT)).isTrue()
        assertThat(FocusAwareFrameLayout.isMovingBackward(View.FOCUS_UP)).isTrue()

        assertThat(FocusAwareFrameLayout.isMovingBackward(View.FOCUS_FORWARD)).isFalse()
        assertThat(FocusAwareFrameLayout.isMovingBackward(View.FOCUS_RIGHT)).isFalse()
        assertThat(FocusAwareFrameLayout.isMovingBackward(View.FOCUS_DOWN)).isFalse()
    }

    @Test
    fun focusSearch_focusedIsNull_delegatesToSuper() {
        layout.focusSearch(null, View.FOCUS_DOWN)
        verify(nextFocusFinder, never()).findNextFocus(any(), anyInt())
    }

    @Test
    fun focusSearch_internalForward_doesNotUseFinder() {
        // Moving forward from child1 (index 0) should go to child2.
        // This is internal navigation, so NextFocusFinder should not be used.
        val result = layout.focusSearch(child1, View.FOCUS_FORWARD)

        verify(nextFocusFinder, never()).findNextFocus(any(), anyInt())
        assertThat(result).isEqualTo(child2)
    }

    @Test
    fun focusSearch_internalBackward_doesNotUseFinder() {
        // Moving backward from child3 (index 2) should go to child2.
        // This is internal navigation.
        val result = layout.focusSearch(child3, View.FOCUS_BACKWARD)

        verify(nextFocusFinder, never()).findNextFocus(any(), anyInt())
        assertThat(result).isEqualTo(child2)
    }

    @Test
    fun focusSearch_boundaryForward_usesFinder() {
        // child3 is last. Moving forward is a boundary exit.
        `when`(nextFocusFinder.findNextFocus(child3, View.FOCUS_FORWARD)).thenReturn(externalView)

        val result = layout.focusSearch(child3, View.FOCUS_FORWARD)

        verify(nextFocusFinder).findNextFocus(child3, View.FOCUS_FORWARD)
        assertThat(result).isEqualTo(externalView)
    }

    @Test
    fun focusSearch_boundaryForwardWithDirectionDown_usesFinder() {
        // child3 is last. Moving DOWN is treated as forward boundary.
        `when`(nextFocusFinder.findNextFocus(child3, View.FOCUS_DOWN)).thenReturn(externalView)

        val result = layout.focusSearch(child3, View.FOCUS_DOWN)

        verify(nextFocusFinder).findNextFocus(child3, View.FOCUS_DOWN)
        assertThat(result).isEqualTo(externalView)
    }

    @Test
    fun focusSearch_boundaryBackward_usesFinder() {
        // child1 is first. Moving backward is a boundary exit.
        `when`(nextFocusFinder.findNextFocus(child1, View.FOCUS_BACKWARD)).thenReturn(externalView)

        val result = layout.focusSearch(child1, View.FOCUS_BACKWARD)

        verify(nextFocusFinder).findNextFocus(child1, View.FOCUS_BACKWARD)
        assertThat(result).isEqualTo(externalView)
    }

    @Test
    fun focusSearch_boundaryBackwardWithDirectionUp_usesFinder() {
        // child1 is first. Moving UP is treated as backward boundary.
        `when`(nextFocusFinder.findNextFocus(child1, View.FOCUS_UP)).thenReturn(externalView)

        val result = layout.focusSearch(child1, View.FOCUS_UP)

        verify(nextFocusFinder).findNextFocus(child1, View.FOCUS_UP)
        assertThat(result).isEqualTo(externalView)
    }
}
