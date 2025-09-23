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

package com.android.settings.system

import android.content.ContentResolver
import android.content.Context
import android.provider.Settings
import android.util.Log
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.systemui.Flags

/**
 * The preference controller for the Settings page controlling Notifications & Quick Settings
 * panels, allowing the user to switch between "Dual Shade" and "Single Shade".
 */
class ShadePanelsPreferenceController(context: Context, key: String) :
    BasePreferenceController(context, key) {

    override fun getAvailabilityStatus(): Int = getDualShadeAvailability(mContext)

    override fun getSummary(): CharSequence? {
        if (getAvailabilityStatus() != AVAILABLE) {
            return null
        }
        return mContext.getText(
            if (mContext.contentResolver.isDualShadeEnabled()) {
                R.string.shade_panels_separate_title
            } else {
                R.string.shade_panels_combined_title
            }
        )
    }

    companion object {
        internal const val TAG = "ShadePanelsPreferenceController"

        internal const val MIN_LARGE_SCREEN_WIDTH_DP = 600

        private const val ON = 1
        private const val OFF = 0

        /** Retrieve the preference value from secure settings. */
        fun ContentResolver.isDualShadeEnabled(): Boolean {
            return Settings.Secure.getInt(this, Settings.Secure.DUAL_SHADE, ON) == ON
        }

        /** Persist the preference value to secure settings. */
        fun ContentResolver.setDualShadeEnabled(enable: Boolean): Boolean {
            return Settings.Secure.putInt(this, Settings.Secure.DUAL_SHADE, if (enable) ON else OFF)
        }

        /** Whether the Dual Shade feature is available on this device. */
        @JvmStatic
        fun isDualShadeAvailable(context: Context): Boolean {
            return getDualShadeAvailability(context) == AVAILABLE
        }

        @AvailabilityStatus
        internal fun getDualShadeAvailability(context: Context): Int {
            if (!Flags.sceneContainer()) {
                Log.i(TAG, "Scene container is disabled")
                return CONDITIONALLY_UNAVAILABLE
            }

            val settingEnabled =
                context.resources.getBoolean(
                    com.android.settingslib.R.bool.config_useDualShadeSetting
                )

            return if (settingEnabled) {
                Log.i(TAG, "The setting is enabled")
                AVAILABLE
            } else {
                Log.i(TAG, "The setting is disabled")
                CONDITIONALLY_UNAVAILABLE
            }
        }
    }
}
