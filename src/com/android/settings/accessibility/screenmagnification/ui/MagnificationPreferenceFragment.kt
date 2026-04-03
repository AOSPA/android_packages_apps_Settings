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
import android.content.Context
import com.android.settings.R
import com.android.settings.dashboard.DashboardFragment

/** Displays the detail screen of the screen magnification feature */
open class MagnificationPreferenceFragment : DashboardFragment() {

    override fun getPreferenceScreenResId(): Int {
        return 0
    }

    override fun getLogTag(): String? = TAG

    override fun getMetricsCategory(): Int {
        return SettingsEnums.ACCESSIBILITY_TOGGLE_SCREEN_MAGNIFICATION
    }

    override fun getHelpResource(): Int {
        return R.string.help_url_magnification
    }

    override fun getPreferenceScreenBindingKey(context: Context): String? = MagnificationScreen.KEY

    companion object {
        private val TAG = MagnificationPreferenceFragment::class.simpleName
        const val MAGNIFICATION_SURVEY_KEY: String = "A11yMagnificationUser"
    }
}
