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

import android.app.settings.SettingsEnums
import android.content.ComponentName
import androidx.annotation.VisibleForTesting
import androidx.preference.Preference
import com.android.internal.accessibility.AccessibilityShortcutController
import com.android.settings.R
import com.android.settings.accessibility.ShortcutFragment
import com.android.settings.accessibility.ToggleShortcutPreferenceController
import com.android.settings.accessibility.screenmagnification.CursorFollowingModePreferenceController
import com.android.settings.accessibility.screenmagnification.ModePreferenceController
import com.android.settings.accessibility.screenmagnification.ToggleMagnificationShortcutPreferenceController
import com.android.settings.accessibility.screenmagnification.dialogs.CursorFollowingModeChooser
import com.android.settings.accessibility.screenmagnification.dialogs.MagnificationModeChooser
import com.android.settings.search.BaseSearchIndexProvider

/** Displays the detail screen of the screen magnification feature */
open class MagnificationPreferenceFragment : ShortcutFragment() {

    override fun onDisplayPreferenceDialog(preference: Preference) {
        val preferenceKey = preference.key
        if (use(ModePreferenceController::class.java)?.preferenceKey == preferenceKey) {
            MagnificationModeChooser.showDialog(childFragmentManager, MODE_CHOOSER_REQUEST_KEY)
            return
        }
        if (
            use(CursorFollowingModePreferenceController::class.java)?.preferenceKey == preferenceKey
        ) {
            CursorFollowingModeChooser.showDialog(
                childFragmentManager,
                CURSOR_FOLLOWING_MODE_CHOOSER_REQUEST_KEY,
            )
            return
        }

        super.onDisplayPreferenceDialog(preference)
    }

    override fun getShortcutPreferenceController(): ToggleShortcutPreferenceController {
        return use(ToggleMagnificationShortcutPreferenceController::class.java)
    }

    override fun getFeatureName(): CharSequence {
        return getText(R.string.accessibility_screen_magnification_title)
    }

    override fun getFeatureComponentName(): ComponentName {
        return AccessibilityShortcutController.MAGNIFICATION_COMPONENT_NAME
    }

    override fun getPreferenceScreenResId(): Int {
        return R.xml.accessibility_magnification_screen
    }

    override fun getLogTag(): String? = TAG

    override fun getMetricsCategory(): Int {
        return SettingsEnums.ACCESSIBILITY_TOGGLE_SCREEN_MAGNIFICATION
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public override fun getSurveyKey(): String {
        return MAGNIFICATION_SURVEY_KEY
    }

    override fun getHelpResource(): Int {
        return R.string.help_url_magnification
    }

    companion object {
        private val TAG = MagnificationPreferenceFragment::class.simpleName
        private const val MODE_CHOOSER_REQUEST_KEY = "magnificationModeChooser"
        private const val CURSOR_FOLLOWING_MODE_CHOOSER_REQUEST_KEY = "cursorFollowingModeChooser"
        const val MAGNIFICATION_SURVEY_KEY: String = "A11yMagnificationUser"
        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER: BaseSearchIndexProvider =
            BaseSearchIndexProvider(R.xml.accessibility_magnification_screen)
    }
}
