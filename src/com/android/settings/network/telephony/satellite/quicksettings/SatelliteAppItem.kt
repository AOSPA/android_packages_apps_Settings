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

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.android.settingslib.spaprivileged.model.app.AppRecord

/**
 * Represents a satellite-enabled app item to be displayed in the landing page.
 *
 * @property app The [ApplicationInfo] of the app.
 * @property intent The [Intent] to launch the app.
 * @property appLabel An optional custom label for the app.
 */
data class SatelliteAppItem(
    override val app: ApplicationInfo,
    val intent: Intent?,
    val appLabel: String? = null,
    val summary: String? = null,
) : AppRecord {
    /** Returns the display label for the app, falling back to the app's default label. */
    fun getAppLabel(pm: PackageManager): String = appLabel ?: app.loadLabel(pm).toString()
}
