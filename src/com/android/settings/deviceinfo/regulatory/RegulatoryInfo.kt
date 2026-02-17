/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.deviceinfo.regulatory

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.SystemProperties
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.VisibleForTesting
import com.android.settings.R
import java.io.File

/** To load Regulatory Info from device. */
object RegulatoryInfo {
    private const val LOG_TAG = "RegulatoryInfo"

    private const val REGULATORY_INFO_RESOURCE = "regulatory_info"

    @VisibleForTesting const val KEY_COO = "ro.boot.hardware.coo"
    @VisibleForTesting const val KEY_SKU = "ro.boot.hardware.sku"
    @VisibleForTesting const val KEY_TEST_LABEL_PERMIT_BOOL = "persist.settings.test.elabel"
    @VisibleForTesting const val USER_BUILD_TYPE = "user"

    /** Gets the regulatory drawable. */
    fun Context.getRegulatoryInfo(): Drawable? {
        val sku = getSku().lowercase()
        val coo = getCoo().lowercase()
        // When hardware coo property exists, use regulatory_info_<sku>_<coo> resource if valid.
        val fileName = buildString {
            append(REGULATORY_INFO_RESOURCE)
            if (sku.isNotBlank()) {
                append("_$sku")
                // Use regulatory_info_<sku> resource if valid.
                if (coo.isNotBlank()) {
                    append("_$coo")
                }
            }
        }

        if (shouldShowTestELabel()) {
            getInternalRegulatoryInfo(fileName)?.let {
                return it
            }
        }

        return getRegulatoryInfo(fileName)
    }

    private fun shouldShowTestELabel(): Boolean {
        val isUserType = Build.TYPE == USER_BUILD_TYPE
        return !isUserType && SystemProperties.getBoolean(KEY_TEST_LABEL_PERMIT_BOOL, false)
    }

    fun getCoo(): String = SystemProperties.get(KEY_COO)

    fun getSku(): String = SystemProperties.get(KEY_SKU)

    private fun Context.getRegulatoryInfo(fileName: String): Drawable? {
        val overlayPackageName =
            resources.getString(R.string.config_regulatory_info_overlay_package_name).ifBlank {
                packageName
            }
        val resources = packageManager.getResourcesForApplication(overlayPackageName)
        val id = resources.getIdentifier(fileName, "drawable", overlayPackageName)
        return if (id > 0) resources.getRegulatoryInfo(id) else null
    }

    /**
     * Helper to load a png drawable from the app's Device Encrypted (DE) internal storage. Target
     * path: /data/user_de/0/com.android.settings/files/<fileName>
     */
    private fun Context.getInternalRegulatoryInfo(fileName: String): Drawable? {
        try {
            val deContext = createDeviceProtectedStorageContext()
            val targetFileName = "$fileName.png"
            val file = File(deContext.filesDir, targetFileName)
            if (!file.exists()) {
                Log.d(LOG_TAG, "Test elabel file not found: ${file.absolutePath}")
                return null
            }

            val absolutePath = file.absolutePath
            val drawable = Drawable.createFromPath(absolutePath)

            if (drawable == null) {
                Log.e(LOG_TAG, "Failed to create drawable from path: $absolutePath")
            } else {
                Log.d(LOG_TAG, "Successfully loaded drawable from path: $absolutePath")
            }
            return drawable
        } catch (e: SecurityException) {
            Log.e(LOG_TAG, "Security exception accessing file: $fileName", e)
            return null
        } catch (e: Exception) {
            // Catch any other unexpected errors
            Log.e(LOG_TAG, "Error loading internal regulatory info for $fileName", e)
            return null
        }
    }

    private fun Resources.getRegulatoryInfo(@DrawableRes resId: Int): Drawable? =
        try {
            getDrawable(resId, null).takeIf {
                // Ignore the placeholder image
                it.intrinsicWidth > 10 && it.intrinsicHeight > 10
            }
        } catch (_: Resources.NotFoundException) {
            null
        }
}
