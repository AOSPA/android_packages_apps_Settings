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

package com.android.settings.network.telephony.satellite.quicksettings

import android.content.Context
import android.graphics.Canvas
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SatelliteIconDrawableTest {

    private lateinit var context: Context
    private lateinit var drawable: SatelliteIconDrawable

    @Mock private lateinit var mockCanvas: Canvas

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        drawable = SatelliteIconDrawable(context)
    }

    @Test
    fun getIntrinsicWidth_returnsExpectedValue() {
        val density = context.resources.displayMetrics.density
        val expectedWidth = (SatelliteIconDrawable.INTRINSIC_WIDTH_DP * density).toInt()

        assertThat(drawable.intrinsicWidth).isEqualTo(expectedWidth)
    }

    @Test
    fun getIntrinsicHeight_returnsExpectedValue() {
        val density = context.resources.displayMetrics.density
        val expectedHeight = (SatelliteIconDrawable.INTRINSIC_HEIGHT_DP * density).toInt()

        assertThat(drawable.intrinsicHeight).isEqualTo(expectedHeight)
    }

    @Test
    fun draw_doesNotCrash() {
        // Basic smoke test to ensure draw doesn't throw exceptions
        drawable.setBounds(0, 0, 100, 50)
        drawable.draw(mockCanvas)
    }
}
