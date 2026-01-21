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

package com.android.settings.device

import android.os.Bundle
import android.content.Context
import android.provider.SearchIndexableResource
import com.android.settings.R
import com.android.settings.dashboard.DashboardFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settings.utils.DesktopSettingsUtils
import com.android.settingslib.search.SearchIndexable
import android.app.settings.SettingsEnums

@SearchIndexable(forTarget = SearchIndexable.ALL)
class DeviceDashboardFragment : DashboardFragment() {

    override fun getMetricsCategory(): Int = SettingsEnums.SETTINGS_DEVICE_CATEGORY

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
    }

    override fun getPreferenceScreenResId(): Int {
        return R.xml.device_dashboard_fragment
    }

    override fun getLogTag(): String {
        return TAG
    }

    companion object {
        private const val TAG = "DeviceDashboardFragment"

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER: BaseSearchIndexProvider =
            object : BaseSearchIndexProvider() {
                override fun getXmlResourcesToIndex(
                    context: Context,
                    enabled: Boolean
                ): List<SearchIndexableResource> {
                    val result = ArrayList<SearchIndexableResource>()
                    if (DesktopSettingsUtils.shouldShowTopLevelDeviceCategory(context)) {
                        val sir = SearchIndexableResource(context)
                        sir.xmlResId = R.xml.device_dashboard_fragment
                        result.add(sir)
                    }
                    return result
                }

                override fun isPageSearchEnabled(context: Context): Boolean {
                    return DesktopSettingsUtils.shouldShowTopLevelDeviceCategory(context)
                }
            }
    }
}