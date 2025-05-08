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

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
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
import android.view.Display
import android.view.Display.INVALID_DISPLAY
import android.view.DisplayInfo
import android.view.IWindowManager
import android.view.WindowManagerGlobal
import com.android.server.display.feature.flags.Flags.enableModeLimitForExternalDisplay
import com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.VIRTUAL_DISPLAY_PACKAGE_NAME_SYSTEM_PROPERTY
import com.android.settings.flags.FeatureFlagsImpl
import java.util.function.Consumer

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

    private fun wrapDmDisplay(display: Display, isEnabled: DisplayIsEnabled): DisplayDevice =
        DisplayDevice(display.displayId, display.name, display.mode,
                display.getSupportedModes().asList(), isEnabled)

    private fun isDisplayAllowed(display: Display): Boolean =
        display.type == Display.TYPE_EXTERNAL || display.type == Display.TYPE_OVERLAY
                || isVirtualDisplayAllowed(display);

    private fun isVirtualDisplayAllowed(display: Display): Boolean {
        val sysProp = getSystemProperty(VIRTUAL_DISPLAY_PACKAGE_NAME_SYSTEM_PROPERTY)
        return !sysProp.isEmpty() && display.type == Display.TYPE_VIRTUAL
                && sysProp == display.ownerPackageName
    }

    /**
     * @return all displays including disabled.
     */
    open fun getConnectedDisplays(): List<DisplayDevice> {
        val dm = displayManager ?: return emptyList()

        val enabledIds = dm.getDisplays().map { it.getDisplayId() }.toSet()

        return dm.getDisplays(DISPLAY_CATEGORY_ALL_INCLUDING_DISABLED)
            .filter { isDisplayAllowed(it) }
            .map {
                val isEnabled = if (enabledIds.contains(it.displayId))
                    DisplayIsEnabled.YES
                else
                    DisplayIsEnabled.NO
                wrapDmDisplay(it, isEnabled)
            }
            .toList()
    }

    /**
     * @param displayId which must be returned
     * @return display object for the displayId, or null if display is not a connected display,
     *         the ID was not found, or the ID was invalid
     */
    open fun getDisplay(displayId: Int): DisplayDevice? {
        if (displayId == INVALID_DISPLAY) {
            return null
        }
        val display = displayManager?.getDisplay(displayId) ?: return null
        return if (isDisplayAllowed(display)) {
            wrapDmDisplay(display, DisplayIsEnabled.UNKNOWN)
        } else {
            null
        }
    }

    /**
     * Register display listener.
     */
    open fun registerDisplayListener(listener: DisplayManager.DisplayListener) {
        displayManager?.registerDisplayListener(listener, handler, EVENT_TYPE_DISPLAY_ADDED or
                EVENT_TYPE_DISPLAY_CHANGED or EVENT_TYPE_DISPLAY_REMOVED,
                PRIVATE_EVENT_TYPE_DISPLAY_CONNECTION_CHANGED)
    }

    /**
     * Unregister display listener.
     */
    open fun unregisterDisplayListener(listener: DisplayManager.DisplayListener) {
        displayManager?.unregisterDisplayListener(listener)
    }

    /**
     * Enable connected display.
     */
    open fun enableConnectedDisplay(displayId: Int): Boolean {
        val dm = displayManager ?: return false
        dm.enableConnectedDisplay(displayId)
        return true
    }

    /**
     * Disable connected display.
     */
    open fun disableConnectedDisplay(displayId: Int): Boolean {
        val dm = displayManager ?: return false
        dm.disableConnectedDisplay(displayId)
        return true
    }

    /**
     * Get display rotation
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

    /**
     * Enforce display mode on the given display.
     */
    open fun setUserPreferredDisplayMode(displayId: Int, mode: Display.Mode) {
        DisplayManagerGlobal.getInstance().setUserPreferredDisplayMode(displayId, mode)
    }

    /**
     * @return true if the display mode limit flag enabled.
     */
    open fun isModeLimitForExternalDisplayEnabled(): Boolean = enableModeLimitForExternalDisplay()

    open var displayTopology : DisplayTopology?
        get() = displayManager?.displayTopology
        set(value) { displayManager?.let { it.displayTopology = value } }

    open val wallpaper: Bitmap?
        get() = WallpaperManager.getInstance(context).bitmap

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
