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
import android.app.supervision.flags.Flags
import android.content.Context
import com.android.settings.CatalystSettingsActivity
import com.android.settings.R
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

/** Activity to display [SupervisionWebContentFiltersSearchSupportedAppsScreen]. */
class SupervisionWebContentFiltersSearchSupportedAppsActivity :
    CatalystSettingsActivity(SupervisionWebContentFiltersSearchSupportedAppsScreen.KEY)

/**
 * Search results filters supported apps landing page (Settings > Supervision > Web content
 * filters > Search results - on x apps).
 */
@ProvidePreferenceScreen(SupervisionWebContentFiltersSearchSupportedAppsScreen.KEY)
open class SupervisionWebContentFiltersSearchSupportedAppsScreen : PreferenceScreenMixin {

    override fun isFlagEnabled(context: Context) = Flags.enableSupervisionSettingsUiUpdates()

    override val key: String
        get() = KEY

    override val screenTitle: Int
        get() = R.string.supervision_web_content_filters_search_filter_title

    override val indexable
        get() = true

    override val highlightMenuKey: Int
        get() = R.string.menu_key_supervision

    override fun getMetricsCategory() =
        SettingsEnums.SUPERVISION_WEB_CONTENT_FILTERS_SEARCH_SUPPORTED_APPS

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(
            context,
            SupervisionWebContentFiltersSearchSupportedAppsActivity::class.java,
            metadata?.key,
        )

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {}

    companion object {
        const val KEY = "supervision_web_content_filters_search_supported_apps"
    }
}
