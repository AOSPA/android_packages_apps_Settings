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
package com.android.settings.safetycenter.ui

import android.app.settings.SettingsEnums
import android.content.Context
import com.android.settings.R
import com.android.settings.dashboard.DashboardFragment
import com.android.settings.flags.Flags
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable

/**
 * Fragment for the Safety Center UI.
 *
 * This fragment hosts the preferences for the Security & privacy settings page and is searchable
 * when the feature flag is enabled.
 */
@SearchIndexable
class SafetyCenterFragment : DashboardFragment() {

    protected override fun getPreferenceScreenResId(): Int {
        return R.xml.safety_center_main_page
    }

    override fun getLogTag(): String {
        return TAG
    }

    override fun getMetricsCategory(): Int {
        return SettingsEnums.SAFETY_CENTER
    }

    companion object {
        private const val TAG = "SafetyCenterFragment"

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER: BaseSearchIndexProvider =
            object : BaseSearchIndexProvider(R.xml.safety_center_main_page) {
                protected override fun isPageSearchEnabled(context: Context?): Boolean {
                    return Flags.enableSafetyCenterNewUi()
                }
            }
    }
}
