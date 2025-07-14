/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.DisplayManager.DISPLAY_CATEGORY_ALL_INCLUDING_DISABLED
import android.hardware.display.DisplayManager.EVENT_TYPE_DISPLAY_ADDED
import android.hardware.display.DisplayManager.EVENT_TYPE_DISPLAY_CHANGED
import android.hardware.display.DisplayManager.EVENT_TYPE_DISPLAY_REMOVED
import android.hardware.display.DisplayManager.PRIVATE_EVENT_TYPE_DISPLAY_CONNECTION_CHANGED
import android.hardware.display.DisplayManagerGlobal
import android.hardware.display.DisplayTopology
import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import android.os.SystemProperties
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.Display
import android.view.Display.INVALID_DISPLAY
import android.view.DisplayInfo
import android.view.IWindowManager
import android.view.SurfaceControl
import android.view.SurfaceView
import android.view.View
import android.view.ViewManager
import android.view.WindowManager
import android.view.WindowManagerGlobal
import com.android.server.display.feature.flags.Flags.enableModeLimitForExternalDisplay
import com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.VIRTUAL_DISPLAY_PACKAGE_NAME_SYSTEM_PROPERTY
import com.android.settings.flags.FeatureFlagsImpl
import java.util.function.Consumer

/**
 * Wallpaper is forced-revealed using a View added to the window manager with
 * LayoutParams.FLAG_SHOW_WALLPAPER set. In order to clean these views up and avoid adding more than
 * one, we keep RevealedWallpaper instances as markers, both to avoid re-adding and to remove when
 * needed.
 */
data class RevealedWallpaper(val displayId: Int, val revealer: View, val viewManager: ViewManager)

open class ConnectedDisplayInjector(open val context: Context?) {

    open val flags: DesktopExperienceFlags by lazy { DesktopExperienceFlags(FeatureFlagsImpl()) }
    open val handler: Handler by lazy { Handler(Looper.getMainLooper()) }

    /**
     * @param name of a system property.
     * @return the value of the system property.
     */
    open fun getSystemProperty(name: String): String = SystemProperties.get(name)

    /** The display manager instance, or null if context is null. */
    val displayManager: DisplayManager? by lazy {
        context?.getSystemService(DisplayManager::class.java)
    }

    /** The window manager instance, or null if it cannot be retrieved. */
    val windowManager: IWindowManager? by lazy { WindowManagerGlobal.getWindowManagerService() }

    /**
     * Reveals the wallpaper on the given display using a View with FLAG_SHOW_WALLPAPER flag set in
     * LayoutParams. This can be cleaned up later using the returned RevealedWallpaper object.
     *
     * @return a RevealedWallpaper which contains the display's window manager and the view that was
     *   added to it, or null if the view could not be added or the WindowManager was not available
     */
    open fun revealWallpaper(displayId: Int): RevealedWallpaper? {
        val display = displayManager?.getDisplay(displayId) ?: return null
        val windowCtx =
            context?.createWindowContext(
                display,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                /* options= */ null,
            )
        val windowManager = windowCtx?.getSystemService(WindowManager::class.java) ?: return null

        val view = View(windowCtx)
        windowManager.addView(
            view,
            WindowManager.LayoutParams().also {
                it.width = 1
                it.height = 1
                it.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                it.flags =
                    (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
                it.format = PixelFormat.TRANSLUCENT
            },
        )
        return RevealedWallpaper(display.displayId, view, windowManager)
    }

    private fun wrapDmDisplay(
        display: Display,
        isEnabled: DisplayIsEnabled,
        isConnectedDisplay: Boolean,
    ): DisplayDevice =
        DisplayDevice(
            display.displayId,
            display.name,
            display.mode,
            display.supportedModes.asList(),
            isEnabled,
            isConnectedDisplay,
        )

    private fun isConnectedDisplay(display: Display): Boolean =
        display.type == Display.TYPE_EXTERNAL ||
            display.type == Display.TYPE_OVERLAY ||
            isVirtualDisplayAllowed(display)

    private fun isVirtualDisplayAllowed(display: Display): Boolean {
        val sysProp = getSystemProperty(VIRTUAL_DISPLAY_PACKAGE_NAME_SYSTEM_PROPERTY)
        return !sysProp.isEmpty() &&
            display.type == Display.TYPE_VIRTUAL &&
            sysProp == display.ownerPackageName
    }

    /**
     * Reparents surface to the SurfaceControl of wallpaperView, so that view will render `surface`.
     * Any surfaces which may be parented to wallpaperView already should be passed in oldSurfaces
     * and they will be removed from the wallpaperView's hierarchy and released.
     *
     * TODO(b/426102638): In mirroring mode, area outside the letterbox will be translucent, causing
     *   display behind to be shown. This can possibly be fixed by adding another surface with just
     *   empty opaque background
     */
    open fun updateSurfaceView(
        oldSurfaces: List<SurfaceControl>,
        surface: SurfaceControl,
        wallpaperView: SurfaceView,
        surfaceScale: Float,
        surfaceSize: Size,
        cornerRadiusPx: Float,
        isMirroringOtherDisplay: Boolean,
    ) {
        // TODO(426163319): Move this calculation to DisplayBlock to properly test the calculations
        val t = SurfaceControl.Transaction()
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
        // Corner radius should not be applied when display is letterboxed
        if (!isMirroringOtherDisplay) {
            // Corner radius has to be set on the original surface size, so apply a inverse scale
            // here.
            t.setCornerRadius(surface, cornerRadiusPx / surfaceScale)
        }

        oldSurfaces.forEach { t.remove(it) }
        t.apply(true)
        oldSurfaces.forEach { it.release() }
    }

    /** @return all displays including disabled. */
    open fun getDisplays(): List<DisplayDevice> {
        val dm = displayManager ?: return emptyList()

        val enabledIds = dm.displays.map { it.displayId }.toSet()

        return dm.getDisplays(DISPLAY_CATEGORY_ALL_INCLUDING_DISABLED)
            .map {
                val isEnabled =
                    if (enabledIds.contains(it.displayId)) DisplayIsEnabled.YES
                    else DisplayIsEnabled.NO
                wrapDmDisplay(it, isEnabled, isConnectedDisplay(it))
            }
            .toList()
    }

    /**
     * @param displayId which must be returned
     * @return display object for the displayId, or null if display is not a connected display, the
     *   ID was not found, or the ID was invalid
     */
    open fun getDisplay(displayId: Int): DisplayDevice? {
        if (displayId == INVALID_DISPLAY) {
            return null
        }
        val display = displayManager?.getDisplay(displayId) ?: return null
        return if (isConnectedDisplay(display)) {
            wrapDmDisplay(display, DisplayIsEnabled.UNKNOWN, true)
        } else {
            null
        }
    }

    /** Register display listener. */
    open fun registerDisplayListener(listener: DisplayManager.DisplayListener) {
        displayManager?.registerDisplayListener(
            listener,
            handler,
            EVENT_TYPE_DISPLAY_ADDED or EVENT_TYPE_DISPLAY_CHANGED or EVENT_TYPE_DISPLAY_REMOVED,
            PRIVATE_EVENT_TYPE_DISPLAY_CONNECTION_CHANGED,
        )
    }

    /** Unregister display listener. */
    open fun unregisterDisplayListener(listener: DisplayManager.DisplayListener) {
        displayManager?.unregisterDisplayListener(listener)
    }

    /** Enable connected display. */
    open fun enableConnectedDisplay(displayId: Int): Boolean {
        val dm = displayManager ?: return false
        dm.enableConnectedDisplay(displayId)
        return true
    }

    /** Disable connected display. */
    open fun disableConnectedDisplay(displayId: Int): Boolean {
        val dm = displayManager ?: return false
        dm.disableConnectedDisplay(displayId)
        return true
    }

    /**
     * Get display rotation
     *
     * @param displayId display identifier
     * @return rotation
     */
    open fun getDisplayUserRotation(displayId: Int): Int {
        val wm = windowManager ?: return 0
        try {
            return wm.getDisplayUserRotation(displayId)
        } catch (e: RemoteException) {
            Log.e(TAG, "Error getting user rotation of display $displayId", e)
            return 0
        }
    }

    /**
     * Freeze rotation of the display in the specified rotation.
     *
     * @param displayId display identifier
     * @param rotation [0, 1, 2, 3]
     * @return true if successful
     */
    open fun freezeDisplayRotation(displayId: Int, rotation: Int): Boolean {
        val wm = windowManager ?: return false
        try {
            wm.freezeDisplayRotation(displayId, rotation, "ExternalDisplayPreferenceFragment")
            return true
        } catch (e: RemoteException) {
            Log.e(TAG, "Error while freezing user rotation of display $displayId", e)
            return false
        }
    }

    /** Enforce display mode on the given display. */
    open fun setUserPreferredDisplayMode(displayId: Int, mode: Display.Mode) {
        DisplayManagerGlobal.getInstance().setUserPreferredDisplayMode(displayId, mode)
    }

    /** @return true if the display mode limit flag enabled. */
    open fun isModeLimitForExternalDisplayEnabled(): Boolean = enableModeLimitForExternalDisplay()

    open var displayTopology: DisplayTopology?
        get() = displayManager?.displayTopology
        set(value) {
            displayManager?.let { it.displayTopology = value }
        }

    /**
     * Mirrors the wallpaper of the given display.
     *
     * @return a SurfaceControl for the top of the new hierarchy, or null if an exception occurred.
     */
    open fun wallpaper(displayId: Int): SurfaceControl? {
        try {
            val surface = WindowManagerGlobal.getInstance().mirrorWallpaperSurface(displayId)
            if (surface == null) {
                Log.e(TAG, "mirrorWallpaperSurface returned null SurfaceControl")
            }
            return surface
        } catch (e: RemoteException) {
            Log.e(TAG, "Error while mirroring wallpaper of display $displayId", e)
            return null
        } catch (e: NullPointerException) {
            // This can happen if the display has been detached (b/416291830). The caller should
            // check if the display is still attached, but let's keep this here to prevent the app
            // from crashing.
            Log.e(TAG, "NPE while mirroring wallpaper of display $displayId - already detached?", e)
            return null
        }
    }

    /**
     * This density is the density of the current display (showing the Settings app UI). It is
     * necessary to use this density here because the topology pane coordinates are in physical
     * pixels, and the display bounds and accessibility constraints are in density-independent
     * pixels.
     */
    open val densityDpi: Int by lazy {
        val c = context
        val info = DisplayInfo()
        if (c != null && c.display.getDisplayInfo(info)) {
            info.logicalDensityDpi
        } else {
            DisplayMetrics.DENSITY_DEFAULT
        }
    }

    open fun getLogicalSize(displayId: Int): Size? {
        val display = displayManager?.getDisplay(displayId) ?: return null
        val displayInfo = DisplayInfo()
        display.getDisplayInfo(displayInfo)
        return Size(displayInfo.logicalWidth, displayInfo.logicalHeight)
    }

    open fun registerTopologyListener(listener: Consumer<DisplayTopology>) {
        val executor = context?.mainExecutor
        if (executor != null) displayManager?.registerTopologyListener(executor, listener)
    }

    open fun unregisterTopologyListener(listener: Consumer<DisplayTopology>) {
        displayManager?.unregisterTopologyListener(listener)
    }

    private companion object {
        private const val TAG = "ConnectedDisplayInjector"
    }
}
