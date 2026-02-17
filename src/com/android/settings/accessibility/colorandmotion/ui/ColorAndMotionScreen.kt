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

package com.android.settings.accessibility.colorandmotion.ui

import android.app.settings.SettingsEnums
import android.content.Context
import android.hardware.display.ColorDisplayManager
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.ColorAndMotionActivity
import com.android.settings.accessibility.ColorAndMotionFragment
import com.android.settings.accessibility.FeedbackManager
import com.android.settings.accessibility.colorcorrection.ui.ColorCorrectionScreen
import com.android.settings.accessibility.colorinversion.ui.ColorInversionScreen
import com.android.settings.accessibility.shared.ui.FeedbackButtonPreference
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.display.darkmode.DarkModeScreenOnAccessibility
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceCategory
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_UNCATEGORIZED

@ProvidePreferenceScreen(ColorAndMotionScreen.KEY)
open class ColorAndMotionScreen : PreferenceScreenMixin {
    override fun tags(context: Context) = arrayOf(APP_FUNCTION_UNCATEGORIZED)

    override val key: String
        get() = KEY

    // TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.accessibility_color_and_motion_purpose

    override val title: Int
        get() = R.string.accessibility_color_and_motion_title

    override val summary: Int
        get() = R.string.accessibility_color_and_motion_subtext

    override val icon: Int
        get() = R.drawable.ic_color_and_motion

    override val indexable
        get() = true

    override fun getMetricsCategory() = SettingsEnums.ACCESSIBILITY_COLOR_AND_MOTION

    override val highlightMenuKey
        get() = R.string.menu_key_accessibility

    override fun fragmentClass(): Class<out Fragment>? = ColorAndMotionFragment::class.java

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            if (ColorDisplayManager.isColorTransformAccelerated(context)) {
                +ColorCorrectionScreen.KEY
                +ColorInversionScreen.KEY
                +DarkModeScreenOnAccessibility.KEY
                +BlurSwitchPreference()
                +RemoveAnimationsPreference()
                +TextCursorBlinkRatePreference(context)
            } else {
                +ColorInversionScreen.KEY
                +DarkModeScreenOnAccessibility.KEY
                +BlurSwitchPreference()
                +PreferenceCategory(
                    key = "experimental_category",
                    purpose = R.string.experimental_category_purpose,
                    title = R.string.experimental_category_title,
                ) +=
                    {
                        +ColorCorrectionScreen.KEY
                        +RemoveAnimationsPreference()
                        +TextCursorBlinkRatePreference(context)
                    }
            }
            +FeedbackButtonPreference { FeedbackManager(context, metricsCategory) }
        }

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, ColorAndMotionActivity::class.java, metadata?.key)

    companion object {
        const val KEY = "accessibility_color_and_motion"
    }
}
