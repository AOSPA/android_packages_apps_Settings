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

package com.android.settings.gestures

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import com.android.settings.R
import com.android.settings.Settings
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceCategory
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_UNCATEGORIZED
import kotlinx.coroutines.CoroutineScope

@ProvidePreferenceScreen(ButtonNavigationSettingsScreen.KEY)
class ButtonNavigationSettingsScreen : PreferenceScreenMixin {
    // TODO(b/496489797): Remove UI ONLY tag after System > Navigation mode > Navigation Mode radio
    // is migrated
    override fun tags(context: Context) = arrayOf(APP_FUNCTION_UNCATEGORIZED, UI_ONLY_PREFERENCE)

    override val key: String
        get() = KEY

    // TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.button_navigation_settings_page_purpose

    override val title: Int
        get() = R.string.button_navigation_settings_activity_title

    override val highlightMenuKey: Int
        get() = R.string.menu_key_system

    override fun fragmentClass() = ButtonNavigationSettingsFragment::class.java

    override fun getMetricsCategory() = SettingsEnums.SETTINGS_BUTTON_NAV_DLG

    override val indexable: Boolean = true

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?): Intent? =
        makeLaunchIntent(
            context,
            Settings.ButtonNavigationSettingsActivity::class.java,
            metadata?.key,
        )

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +OrderPreferenceCategory() += {
                +DefaultButtonNavigationSettingsOrderPreference(
                    ButtonNavigationSettingsOrderStore(context)
                )
                +ReverseButtonNavigationSettingsOrderPreference(
                    ButtonNavigationSettingsOrderStore(context)
                )
            }
        }

    companion object {
        const val KEY = "button_navigation_settings_page"
    }

    class OrderPreferenceCategory :
        PreferenceCategory(
            key = "button_order_group",
            purpose = R.string.button_order_group_purpose,
            title = R.string.button_navigation_settings_order_title,
        ) {
        override val keywords: Int
            get() =
                if (com.android.settings.flags.Flags.catalystSettingsSearch())
                    R.string.keywords_button_navigation_settings_order
                else 0
    }
}
