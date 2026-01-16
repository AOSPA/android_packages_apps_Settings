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

package com.android.settings.connecteddevice.display

import android.content.Context
import android.os.Bundle
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.connecteddevice.DevicePreferenceCallback
import com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.DISPLAY_ID_ARG
import com.android.settingslib.RestrictedPreference

/** Updater to list connected external displays as individual preferences. */
open class ExternalDisplayListUpdater(
    callback: DevicePreferenceCallback,
    metricsCategory: Int
) : BaseExternalDisplayUpdater(callback, metricsCategory) {

    private val preferenceMap = HashMap<Int, RestrictedPreference>()

    override fun update() {
        val injector = injector ?: return
        val context = context ?: return

        val allDisplays = injector.getDisplays().filter {
            it.isConnectedDisplay && it.isEnabled == DisplayIsEnabled.YES
        }

        // 1. Add new displays or update existing ones
        for (display in allDisplays) {
            var pref = preferenceMap[display.id]
            if (pref == null) {
                pref = createDisplayPreference(context, display)
                preferenceMap[display.id] = pref
                devicePreferenceCallback.onDeviceAdded(pref)
            }

            pref.title = display.name
            applyUsbDataSignalingPolicy(pref, context)
        }

        // 2. Remove disconnected displays
        val currentDisplayIds = allDisplays.map { it.id }.toSet()
        val it = preferenceMap.entries.iterator()
        while (it.hasNext()) {
            val entry = it.next()
            if (!currentDisplayIds.contains(entry.key)) {
                devicePreferenceCallback.onDeviceRemoved(entry.value)
                it.remove()
            }
        }
    }

    private fun createDisplayPreference(
        context: Context,
        display: DisplayDevice
    ): RestrictedPreference {
        val pref = RestrictedPreference(context)
        pref.key = "external_display_${display.id}"
        pref.setOnPreferenceClickListener { p ->
            metricsFeatureProvider.logClickedPreference(p, metricsCategory)
            val args = Bundle().apply {
                putInt(DISPLAY_ID_ARG, display.id)
            }
            launchSettings(context, args)
            true
        }
        return pref
    }
}
