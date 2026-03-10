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

package com.android.settings.display

import android.content.Context
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.view.Display
import android.util.Log

/** Return true if the device contains two (or more) resolutions. */
fun Context.checkSupportedResolutions(): Boolean {
    val displayManager = getSystemService(DisplayManager::class.java)
    val display = displayManager?.getDisplay(Display.DEFAULT_DISPLAY) ?: return false
    val TAG = "ScreenResolutionExtensions"

    val resolutions = mutableSetOf<Point>()
    for (mode in display.supportedModes) {
        resolutions.add(Point(mode.physicalWidth, mode.physicalHeight))
    }

    if (resolutions.size != 2) {
        Log.e(TAG, "No support")
        return false
    }

    val resolutionList = resolutions.toList().sortedBy { it.x * it.y }
    val highWidth = resolutionList[0].x
    val fullWidth = resolutionList[1].x

    return highWidth != 0 && fullWidth != 0
}
