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
package com.android.settings.supervision

import android.app.settings.SettingsEnums
import android.content.Context
import com.android.settings.CatalystSettingsActivity
import com.android.settings.R
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.utils.StringUtil

/** Activity to display [SupervisionWebContentFiltersBrowserSupportedAppsScreen]. */
class SupervisionWebContentFiltersBrowserSupportedAppsActivity :
    CatalystSettingsActivity(SupervisionWebContentFiltersBrowserSupportedAppsScreen.KEY)

/**
 * Websites filters supported apps landing page (Settings > Supervision > Web content filters >
 * Websites - on x apps).
 */
@ProvidePreferenceScreen(SupervisionWebContentFiltersBrowserSupportedAppsScreen.KEY)
open class SupervisionWebContentFiltersBrowserSupportedAppsScreen :
    SupervisionWebContentFilterSupportedAppsScreen() {
    override val supportedAppsKey: String
        get() = BROWSER_FILTERS_SUPPORTED_APPS

    override val key: String
        get() = KEY

    override val screenTitle: Int
        get() = R.string.supervision_web_content_filters_browser_filter_title

    override fun getTitle(context: Context): CharSequence? =
        StringUtil.getIcuPluralsString(
            context,
            supportedApps.size,
            R.string.supervision_web_content_filters_switch_summary,
        )

    override fun getMetricsCategory() =
        SettingsEnums.SUPERVISION_WEB_CONTENT_FILTERS_BROWSER_SUPPORTED_APPS

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(
            context,
            SupervisionWebContentFiltersBrowserSupportedAppsActivity::class.java,
            metadata?.key,
        )

    companion object {
        const val KEY = "supervision_web_content_filters_browser_supported_apps"
        internal const val BROWSER_FILTERS_SUPPORTED_APPS = "browser_filters_supported_apps"
    }
}
