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

package com.android.settings.datetime

import android.app.settings.SettingsEnums
import android.content.Context
import android.os.UserManager
import com.android.settings.R
import com.android.settings.dashboard.RestrictedDashboardFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable

@SearchIndexable
class TimeNotificationsSettings :
    RestrictedDashboardFragment(UserManager.DISALLOW_CONFIG_DATE_TIME) {

    override fun getMetricsCategory(): Int {
        return SettingsEnums.DATE_TIME_NOTIFICATIONS
    }

    override fun getLogTag(): String {
        return TAG
    }

    override fun getPreferenceScreenResId(): Int {
        return R.xml.time_notifications_prefs
    }

    companion object {
        private const val TAG = "TimeNotificationsSettings"

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = BaseSearchIndexProvider(R.xml.time_notifications_prefs)
    }
}
