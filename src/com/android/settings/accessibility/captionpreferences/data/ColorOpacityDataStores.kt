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

@file:Suppress("UNCHECKED_CAST")

package com.android.settings.accessibility.captionpreferences.data

import android.content.Context
import android.graphics.Color
import android.provider.Settings
import android.view.accessibility.CaptioningManager.CaptionStyle
import com.android.settings.accessibility.CaptionUtils

/**
 * Data store for caption color and opacity preferences.
 *
 * @param isColor Determines if this store handles the Color component (true) or Opacity (false).
 */
abstract class ColorOpacityDataStore(
    appContext: Context,
    settingKey: String,
    private val isColor: Boolean,
) : SecureSettingsCaptionDataStore(appContext, settingKey) {

    private var cachedNonDefaultOpacity = CaptionStyle.COLOR_UNSPECIFIED

    abstract fun getFullColorFromHelper(): Int

    /**
     * Checks if the underlying caption color is valid (not transparent). Used by opacity
     * preferences to determine enablement.
     */
    fun hasValidColor(): Boolean {
        val fullColor = getFullColorFromHelper()
        // Assuming fullColor is ARGB. If alpha is 0, it's effectively disabled/none.
        return Color.alpha(fullColor) != 0
    }

    override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
        if (valueType != Int::class.javaObjectType) return null

        val fullColor = getFullColorFromHelper()

        val result =
            if (isColor) {
                CaptionUtils.parseColor(fullColor)
            } else {
                CaptionUtils.parseOpacity(fullColor)
            }
        return result as T?
    }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        if (valueType != Int::class.javaObjectType) return
        val intValue = value as Int

        val fullColor = getFullColorFromHelper()

        val newFullColor =
            if (isColor) {
                val isNonDefaultColor = CaptionStyle.hasColor(intValue)
                CaptionUtils.mergeColorOpacity(intValue, getNonDefaultOpacity(isNonDefaultColor))
            } else {
                // When setting opacity, preserve the existing color
                val currentColor = CaptionUtils.parseColor(fullColor)
                CaptionUtils.mergeColorOpacity(currentColor, intValue)
            }

        settingsSecureStore.setInt(settingKey, newFullColor)
        enableCaptioning()
    }

    private fun getNonDefaultOpacity(isNonDefaultColor: Boolean): Int {
        val fullColor = getFullColorFromHelper()
        val opacity = CaptionUtils.parseOpacity(fullColor)
        if (isNonDefaultColor) {
            val lastOpacity =
                if (cachedNonDefaultOpacity != CaptionStyle.COLOR_UNSPECIFIED)
                    cachedNonDefaultOpacity
                else opacity
            // Reset cached opacity to use current color opacity to merge color.
            cachedNonDefaultOpacity = CaptionStyle.COLOR_UNSPECIFIED
            return lastOpacity
        }
        // When default captioning color was selected, the opacity become 100% and make opacity
        // preference disable. Cache the latest opacity to show the correct opacity later.
        cachedNonDefaultOpacity = opacity
        return opacity
    }
}

class CaptionTextColorDataStore(context: Context, isColor: Boolean) :
    ColorOpacityDataStore(
        context,
        Settings.Secure.ACCESSIBILITY_CAPTIONING_FOREGROUND_COLOR,
        isColor,
    ) {
    override fun getFullColorFromHelper(): Int = captionHelper.foregroundColor
}

class CaptionBackgroundColorDataStore(context: Context, isColor: Boolean) :
    ColorOpacityDataStore(
        context,
        Settings.Secure.ACCESSIBILITY_CAPTIONING_BACKGROUND_COLOR,
        isColor,
    ) {
    override fun getFullColorFromHelper(): Int = captionHelper.backgroundColor
}

class CaptionWindowColorDataStore(context: Context, isColor: Boolean) :
    ColorOpacityDataStore(context, Settings.Secure.ACCESSIBILITY_CAPTIONING_WINDOW_COLOR, isColor) {
    override fun getFullColorFromHelper(): Int = captionHelper.windowColor
}
