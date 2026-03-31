/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings.network.telephony.satellite

import android.content.Context
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Looper
import android.util.TypedValue
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SatelliteSettingIconDrawableTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var drawable: SatelliteSettingIconDrawable

    @Before
    fun setUp() {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }
        context.setTheme(R.style.Theme_Settings) // Use a Settings theme
        drawable = SatelliteSettingIconDrawable(context)
    }

    @Test
    fun constructor_initializesWithDefaultText() {
        assertThat(drawable.satText).isEqualTo("SAT")
    }

    @Test
    fun getIntrinsicWidth_returnsCorrectValue() {
        val expectedSize = dpToPx(24f)
        assertThat(drawable.intrinsicWidth).isEqualTo(expectedSize)
    }

    @Test
    fun getIntrinsicHeight_returnsCorrectValue() {
        val expectedSize = dpToPx(24f)
        assertThat(drawable.intrinsicHeight).isEqualTo(expectedSize)
    }

    @Test
    fun setAlpha_updatesPaintsAndSignalDrawable() {
        val alpha = 128
        drawable.setAlpha(alpha)

        assertThat(drawable.textPaint.alpha).isEqualTo(alpha)
    }

    @Test
    fun setColorFilter_updatesPaintsAndSignalDrawable() {
        val colorFilter: ColorFilter = PorterDuffColorFilter(Color.RED, PorterDuff.Mode.SRC_IN)
        drawable.setColorFilter(colorFilter)

        assertThat(drawable.textPaint.colorFilter).isEqualTo(colorFilter)
    }

    @Test
    fun getOpacity_isTranslucent() {
        assertThat(drawable.opacity).isEqualTo(PixelFormat.TRANSLUCENT)
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.resources.displayMetrics,
            )
            .roundToInt()
    }

    // Access extensions for testing private properties
    private val SatelliteSettingIconDrawable.textPaint: Paint
        get() =
            this.javaClass.getDeclaredField("textPaint").apply { isAccessible = true }.get(this)
                as Paint

    private val SatelliteSettingIconDrawable.satText: String
        get() =
            this.javaClass.getDeclaredField("satText").apply { isAccessible = true }.get(this)
                as String
}
