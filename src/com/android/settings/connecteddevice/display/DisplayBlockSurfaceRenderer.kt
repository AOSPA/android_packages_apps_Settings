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

import android.graphics.Rect
import android.os.Trace
import android.util.Log
import android.util.Size
import android.view.SurfaceControl
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.annotation.VisibleForTesting
import com.android.settingslib.utils.ThreadUtils
import kotlin.time.Duration.Companion.milliseconds

/**
 * Handles the logic for fetching, scaling, and rendering the wallpaper surface onto the
 * [DisplayBlock]'s SurfaceView.
 */
class DisplayBlockSurfaceRenderer(
    private val injector: ConnectedDisplayInjector,
    private val wallpaperView: SurfaceView,
    private val cornerRadiusPx: Int,
) {

    // Id of the logical display this block represents
    var logicalDisplayId: Int = -1
        private set

    // This doesn't necessarily refer to the actual display this block represents. In case of
    // mirroring, it will be the id of the mirrored display
    private var displayIdToShowWallpaper: Int? = null

    /** Scale of the mirrored wallpaper to the actual wallpaper size. */
    @VisibleForTesting
    var surfaceScale: Float? = null
        private set

    // Size of the surface of the display that would be projected on this block
    @VisibleForTesting
    var surfaceSize: Size? = null
        private set

    private val updateSurfaceViewRunnable = Runnable { updateSurfaceView() }

    private val holderCallback =
        object : SurfaceHolder.Callback {
            override fun surfaceCreated(h: SurfaceHolder) {}

            override fun surfaceChanged(
                h: SurfaceHolder,
                format: Int,
                newWidth: Int,
                newHeight: Int,
            ) {
                updateSurfaceView()
            }

            override fun surfaceDestroyed(h: SurfaceHolder) {}
        }

    // These are surfaces which must be removed from the display block hierarchy and released once
    // the new surface is put in place.
    private val oldSurfaces = mutableListOf<SurfaceControl>()
    private var letterboxBackgroundSurface: SurfaceControl? = null
    private var wallpaperSurface: SurfaceControl? = null

    fun attach() {
        wallpaperView.holder.addCallback(holderCallback)
    }

    fun detach() {
        wallpaperView.holder.removeCallback(holderCallback)
        injector.handler.removeCallbacks(updateSurfaceViewRunnable)
    }

    fun reset(
        logicalDisplayId: Int,
        displayIdToShowWallpaper: Int,
        surfaceScale: Float,
        surfaceSize: Size,
    ) {
        wallpaperSurface?.let { oldSurfaces.add(it) }
        letterboxBackgroundSurface?.let { oldSurfaces.add(it) }
        injector.handler.removeCallbacks(updateSurfaceViewRunnable)
        wallpaperSurface = null
        letterboxBackgroundSurface = null

        this.logicalDisplayId = logicalDisplayId
        this.displayIdToShowWallpaper = displayIdToShowWallpaper
        this.surfaceScale = surfaceScale
        this.surfaceSize = surfaceSize
    }

    @VisibleForTesting
    fun updateSurfaceView() {
        val displayId = displayIdToShowWallpaper ?: return

        // Check if the view is attached
        if (wallpaperView.parent == null) {
            Log.i(TAG, "View for display $displayId has no parent - cancelling update")
            return
        }

        val currentSurface = wallpaperSurface
        if (currentSurface != null) {
            renderSurfaceView(currentSurface)
            return
        }
        ThreadUtils.postOnBackgroundThread {
            Trace.beginSection("Settings Wallpaper fetchSurfaceView $logicalDisplayId")
            val fetchedSurface = injector.wallpaper(displayId)
            Trace.endSection()
            if (fetchedSurface == null) {
                // Fetch failed, schedule a retry
                injector.handler.postDelayed(
                    updateSurfaceViewRunnable,
                    REFETCH_WALLPAPER_DELAY.inWholeMilliseconds,
                )
            } else {
                injector.handler.post { handleFetchedWallpaperSurface(displayId, fetchedSurface) }
            }
        }
    }

    private fun handleFetchedWallpaperSurface(
        fetchedForDisplayId: Int,
        fetchedSurface: SurfaceControl,
    ) {
        // If the view is no longer attached or the target display has changed, ignore the result
        if (wallpaperView.parent == null || fetchedForDisplayId != displayIdToShowWallpaper) {
            Log.i(TAG, "Wallpaper display changed or view detached, ignoring stale surface.")
            return
        }
        wallpaperSurface = fetchedSurface
        renderSurfaceView(fetchedSurface)
    }

    private fun renderSurfaceView(surface: SurfaceControl) {
        val surfaceScale = surfaceScale ?: return
        val surfaceSize = surfaceSize ?: return
        val isMirroringOtherDisplay = logicalDisplayId != displayIdToShowWallpaper

        Trace.beginSection("Settings Wallpaper renderSurfaceView display#$displayIdToShowWallpaper")

        val t = injector.createSurfaceTransaction()
        t.reparent(surface, wallpaperView.surfaceControl)

        // Calculate the final (scaled) dimensions of the wallpaper content
        val scaledSurfaceWidth = surfaceSize.width * surfaceScale
        val scaledSurfaceHeight = surfaceSize.height * surfaceScale
        // Calculate the top-left position needed to center the surface within the wallpaperView
        val positionX = (wallpaperView.width - scaledSurfaceWidth) / 2f
        val positionY = (wallpaperView.height - scaledSurfaceHeight) / 2f
        val destinationCrop = Rect(0, 0, surfaceSize.width, surfaceSize.height)

        t.setScale(surface, surfaceScale, surfaceScale)
        t.setPosition(surface, positionX, positionY)
        t.setCrop(surface, destinationCrop)
        if (isMirroringOtherDisplay) {
            // In mirroring mode, area outside the letterbox will be translucent, causing display
            // behind to be spilling over. The workaround here is to add another black background
            // surface behind the original wallpaper surface
            val backgroundSurface =
                injector
                    .getSurfaceControlBuilder()
                    .setName("Letterbox background display#$logicalDisplayId")
                    .setColorLayer()
                    .build()
            letterboxBackgroundSurface = backgroundSurface

            t.reparent(backgroundSurface, wallpaperView.surfaceControl)
            // Set main wallpaper surface to be on top of the background
            t.setLayer(surface, 1)

            t.setAlpha(backgroundSurface, 0.5f)
            t.show(backgroundSurface)
        } else {
            // Corner radius should only be applied when display is not letterboxed, and has to be
            // set on the original surface size, so apply a inverse scale here.
            t.setCornerRadius(surface, cornerRadiusPx.toFloat() / surfaceScale)
            letterboxBackgroundSurface = null
        }

        oldSurfaces.forEach { t.remove(it) }
        t.apply()
        oldSurfaces.forEach { it.release() }
        oldSurfaces.clear()

        Trace.endSection()
    }

    companion object {
        private const val TAG = "DisplayBlockRenderer"
        private val REFETCH_WALLPAPER_DELAY = 500.milliseconds
    }
}
