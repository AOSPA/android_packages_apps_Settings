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
package com.android.settings.supervision.webcontentfilters

import android.content.Context
import android.content.pm.PackageManager.MATCH_UNINSTALLED_PACKAGES
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.android.settings.R
import com.android.settings.supervision.ipc.SupportedApp

class SupervisionWebContentFiltersSupportedAppsEntryPointPreference(
    context: Context,
    var supportedApps: List<SupportedApp>,
) : Preference(context) {

    init {
        widgetLayoutResource = R.layout.supervision_supported_apps_entry_point_preference
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val iconIds =
            listOf<Int>(
                R.id.supervision_supported_apps_entry_point_icon_1,
                R.id.supervision_supported_apps_entry_point_icon_2,
                R.id.supervision_supported_apps_entry_point_icon_3,
            )
        val matrix = ColorMatrix()
        matrix.setSaturation(0.0f)
        val grayScaleFilter = ColorMatrixColorFilter(matrix)
        for ((index, supportedApp) in supportedApps.withIndex()) {
            if (index == iconIds.size) {
                (holder.findViewById(
                        R.id.supervision_supported_apps_entry_point_remaining_apps_count
                    ) as? TextView)
                    ?.apply {
                        text =
                            context.getString(
                                R.string.supervision_remaining_supported_apps_count,
                                supportedApps.size - 3,
                            )
                    }
                (holder.findViewById(
                        R.id.supervision_supported_apps_entry_point_remaining_apps_icon
                    ) as? FrameLayout)
                    ?.apply {
                        visibility = View.VISIBLE
                        alpha = if (isEnabled) ENABLED_ALPHA else DISABLED_ALPHA
                        background?.colorFilter = if (isEnabled) null else grayScaleFilter
                    }
                break
            }

            val packageName = supportedApp.packageName
            if (packageName != null) {
                val packageManager = context.packageManager
                val applicationInfo =
                    packageManager.getApplicationInfo(packageName, MATCH_UNINSTALLED_PACKAGES)
                val icon = packageManager.getApplicationIcon(applicationInfo)
                (holder.findViewById(iconIds[index]) as? ImageView)?.apply {
                    setImageDrawable(icon)
                    contentDescription = supportedApp.title
                    visibility = View.VISIBLE
                    if (!isEnabled) {
                        colorFilter = grayScaleFilter
                        alpha = DISABLED_ALPHA
                    } else {
                        colorFilter = null
                        alpha = ENABLED_ALPHA
                    }
                }
            }
        }
    }

    fun updateSupportedApps(newSupportedApps: List<SupportedApp>) {
        supportedApps = newSupportedApps
        notifyChanged()
    }

    companion object {
        private const val DISABLED_ALPHA = 0.38f
        private const val ENABLED_ALPHA = 1.0f
    }
}
