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
import android.hardware.display.DisplayManager
import android.view.Display
import com.android.settings.core.BasePreferenceController.AVAILABLE
import com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE

val Context.isBrightnessLevelSettingsAvailable: Boolean
    get() = brightnessLevelAvailabilityStatus == AVAILABLE

val Context.brightnessLevelAvailabilityStatus: Int
    get() =
        try {
            // These settings are not available if shown on an external display.
            if (display.isInternal) AVAILABLE else CONDITIONALLY_UNAVAILABLE
        } catch (_: UnsupportedOperationException) {
            // This context is not associated with a display, no reason to hide the settings.
            AVAILABLE
        }
