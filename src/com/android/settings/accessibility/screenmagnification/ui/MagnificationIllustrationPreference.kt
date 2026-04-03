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

package com.android.settings.accessibility.screenmagnification.ui

import android.content.Context
import com.airbnb.lottie.LottieAnimationView
import com.android.settings.R
import com.android.settings.accessibility.shared.utils.adjustIllustrationLayoutForSetupWizard
import com.android.settings.accessibility.shared.utils.handleIllustrationAnimationForSetupWizard
import com.android.settings.inputmethod.InputPeripheralsSettingsUtils
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.IllustrationPreference
import com.android.settingslib.widget.SettingsThemeHelper

internal class MagnificationIllustrationPreference : PreferenceMetadata, PreferenceBinding {

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.magnification_preference_screen_animated_image_purpose

    override val indexable
        get() = false

    override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)

    override fun createWidget(context: Context): IllustrationPreference {
        val hasPointingDevice =
            InputPeripheralsSettingsUtils.isMouse() || InputPeripheralsSettingsUtils.isTouchpad()
        val lottieResId =
            if (SettingsThemeHelper.isExpressiveTheme(context)) {
                if (hasPointingDevice) {
                    R.raw.accessibility_magnification_with_cursor_banner
                } else {
                    R.raw.accessibility_magnification_banner_expressive
                }
            } else {
                R.raw.accessibility_magnification_banner
            }

        return IllustrationPreference(context).apply {
            isSelectable = false
            lottieAnimationResId = lottieResId
            contentDescription = getContentDescription(context)
            if (hasPointingDevice) {
                applyIlloColors()
            } else {
                applyDynamicColor()
            }
            setOnBindListener { view: LottieAnimationView? ->
                view?.let { animationView ->
                    adjustIllustrationLayoutForSetupWizard(animationView)
                    handleIllustrationAnimationForSetupWizard(animationView)
                }
            }
        }
    }

    fun getContentDescription(context: Context): CharSequence =
        context.getString(
            R.string.accessibility_illustration_content_description,
            context.getText(R.string.accessibility_screen_magnification_title),
        )

    companion object {
        const val KEY = "animated_image"
    }
}
