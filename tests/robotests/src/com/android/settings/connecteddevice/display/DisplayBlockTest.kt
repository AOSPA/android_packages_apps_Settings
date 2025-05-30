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

import android.content.Context
import android.graphics.PointF
import android.os.Handler
import android.view.SurfaceControl
import android.view.SurfaceView
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DisplayBlockTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val injector = TestInjector(context)
    val block = DisplayBlock(injector)
    val parentView = FrameLayout(context)

    class TestInjector(context: Context) : ConnectedDisplayInjector(context) {
        /**
         * Return value to use for wallpaper(), by display ID. If an ID is missing, null will be
         * returned.
         */
        var wallpapers = mutableMapOf<Int, SurfaceControl>()
        val updateLog = StringBuilder()

        internal val testHandler = TestHandler(context.mainThreadHandler)

        override val handler: Handler
            get() = testHandler

        override fun updateSurfaceView(oldSurfaces: List<SurfaceControl>, surface: SurfaceControl,
                wallpaperView: SurfaceView, surfaceScale: Float) {
            updateLog.append("oldSurfaces: $oldSurfaces, surface: $surface, surfaceScale: ")
                    .append("%.2f\n".format(surfaceScale))
        }

        override fun wallpaper(displayId: Int): SurfaceControl? = wallpapers.remove(displayId)
    }

    @Test
    fun normalUpdateFlow() {
        val displayId = 42
        val wallpaper42 = SurfaceControl.Builder().setName("wallpaper42").build()

        injector.wallpapers.put(displayId, wallpaper42)

        parentView.addView(block)
        block.reset(displayId, PointF(10f, 10f), PointF(20f, 20f), 0.5f)

        block.updateSurfaceView()

        assertThat(injector.updateLog.toString()).isEqualTo(
                "oldSurfaces: [], surface: $wallpaper42, surfaceScale: 0.50\n")
    }

    @Test
    fun resetTwiceBeforeSurfaceUpdate() {
        val displayId = 42
        val wallpaperA = SurfaceControl.Builder().setName("wallpaperA").build()
        val wallpaperB = SurfaceControl.Builder().setName("wallpaperB").build()

        injector.wallpapers.put(displayId, wallpaperA)

        parentView.addView(block)
        block.reset(displayId, PointF(10f, 10f), PointF(20f, 20f), 0.25f)

        // Should not have fetched wallpaper info yet. Replace wallpaper setting with wallpaperB.
        assertThat(injector.wallpapers.put(displayId, wallpaperB)).isEqualTo(wallpaperA)

        block.reset(displayId, PointF(10f, 10f), PointF(30f, 30f), 0.4f)

        // Should not have fetched wallpaper or display info yet.
        assertThat(injector.wallpapers.get(displayId)).isEqualTo(wallpaperB)
        block.updateSurfaceView()
        assertThat(injector.wallpapers.get(displayId)).isNull()

        assertThat(injector.updateLog.toString()).isEqualTo(
                "oldSurfaces: [], surface: $wallpaperB, surfaceScale: 0.40\n")
    }

    private fun applyRequestedSize() {
        block.right = block.left + block.layoutParams.width
        block.bottom = block.top + block.layoutParams.height
    }

    @Test
    fun resetWithUnchangedSizeCausesImmediateUpdate() {
        val displayId = 42
        val wallpaperX = SurfaceControl.Builder().setName("wallpaperX").build()
        val wallpaperY = SurfaceControl.Builder().setName("wallpaperY").build()

        injector.wallpapers.put(displayId, wallpaperX)

        parentView.addView(block)
        block.reset(displayId, PointF(10f, 10f), PointF(20f, 20f), 0.5f)
        block.updateSurfaceView()

        assertThat(injector.updateLog.toString()).isEqualTo(
                "oldSurfaces: [], surface: $wallpaperX, surfaceScale: 0.50\n")
        applyRequestedSize()
        injector.updateLog.setLength(0)

        // Same size and scale as before, but a new wallpaper and different position in parent view.
        injector.wallpapers.put(displayId, wallpaperY)
        block.reset(displayId, PointF(60f, 10f), PointF(70f, 20f), 0.5f)
        assertThat(injector.updateLog.toString()).isEqualTo(
                "oldSurfaces: [$wallpaperX], surface: $wallpaperY, surfaceScale: 0.50\n")
        applyRequestedSize()
        injector.updateLog.setLength(0)

        // Repeat the pattern, but with a new scale and reverting back to wallpaperX.
        injector.wallpapers.put(displayId, wallpaperX)
        block.reset(displayId, PointF(60f, 30f), PointF(70f, 40f), 0.2f)
        assertThat(injector.updateLog.toString()).isEqualTo(
                "oldSurfaces: [$wallpaperY], surface: $wallpaperX, surfaceScale: 0.20\n")
    }

    @Test
    fun retryIfWallpaperNotReady() {
        val displayId = 42
        val wallpaperW = SurfaceControl.Builder().setName("wallpaperW").build()

        parentView.addView(block)
        block.reset(displayId, PointF(10f, 10f), PointF(20f, 20f), 0.5f)
        block.updateSurfaceView()

        assertThat(injector.updateLog.toString()).isEqualTo("")
        injector.wallpapers.put(displayId, wallpaperW)

        injector.testHandler.flush()
        assertThat(injector.updateLog.toString()).isEqualTo(
                "oldSurfaces: [], surface: $wallpaperW, surfaceScale: 0.50\n")
    }
}
