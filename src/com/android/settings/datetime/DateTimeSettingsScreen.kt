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
package com.android.settings.datetime

import android.app.settings.SettingsEnums
import android.content.Context
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datetime.ZoneGetter
import com.android.settingslib.metadata.METADATA_IN_UI
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_UNCATEGORIZED

/** Date Time settings. */
// LINT.IfChange
@ProvidePreferenceScreen(DateTimeSettingsScreen.KEY)
open class DateTimeSettingsScreen : PreferenceScreenMixin, PreferenceSummaryProvider {
    override fun tags(context: Context) = arrayOf(APP_FUNCTION_UNCATEGORIZED)

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.date_time_settings_purpose

    override val title: Int
        get() = R.string.date_and_time

    override val icon: Int
        get() = R.drawable.ic_settings_date_time

    override val keywords: Int
        get() = R.string.keywords_date_and_time

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) { +DateTimeSettingsScreenPreference(this@DateTimeSettingsScreen) }

    override fun getSummary(context: Context): CharSequence? {
        val now = Calendar.getInstance()
        return ZoneGetter.getTimeZoneOffsetAndName(context, now.getTimeZone(), now.getTime())
    }

    override fun fragmentClass(): Class<out Fragment>? = DateTimeSettings::class.java

    override fun hasCompleteHierarchy() = false

    override val highlightMenuKey
        get() = R.string.menu_key_system

    override fun getMetricsCategory(): Int = SettingsEnums.DATE_TIME

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, Settings.DateTimeSettingsActivity::class.java, metadata?.key)

    class DateTimeSettingsScreenPreference(
        private val screenMetadata : DateTimeSettingsScreen
    ) : PreferenceMetadata, PreferenceSummaryProvider, PersistentPreference<String> {
        override val key : String
            get() = "date_time_settings_preference"

        override val purpose : Int
            get() = screenMetadata.purpose

        override fun tags(context: Context) = arrayOf(METADATA_IN_UI)

        override val supportsWrite: Boolean
            get() = false

        override val valueType = String::class.javaObjectType

        override fun storage(context: Context): KeyValueStore = createSummaryStorage(context, key)

        override val indexable = false

        override fun isEnabled(context: Context) : Boolean = screenMetadata.isEnabled(context)

        override fun getSummary(context: Context) : CharSequence? = screenMetadata.getSummary(context)
    }

    companion object {
        const val KEY = "date_time_settings"
    }
}
// LINT.ThenChange(DateTimeSettings.java)
