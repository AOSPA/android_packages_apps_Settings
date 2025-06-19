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

package com.android.settings.accessibility.flashnotifications.ui

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.util.FeatureFlagUtils
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.FlashNotificationsActivity
import com.android.settings.accessibility.Flags
import com.android.settings.accessibility.FlashNotificationsPreferenceFragment
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

@ProvidePreferenceScreen(FlashNotificationsScreen.KEY)
class FlashNotificationsScreen : PreferenceScreenMixin, PreferenceAvailabilityProvider {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.flash_notifications_title

    override val highlightMenuKey: Int
        get() = R.string.menu_key_accessibility

    override fun getMetricsCategory(): Int = SettingsEnums.FLASH_NOTIFICATION_SETTINGS

    override fun isFlagEnabled(context: Context): Boolean = Flags.catalystFlashNotifications()

    override fun fragmentClass(): Class<out Fragment>? =
        FlashNotificationsPreferenceFragment::class.java

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?): Intent? =
        makeLaunchIntent(context, FlashNotificationsActivity::class.java, metadata?.key)

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {}

    override fun isAvailable(context: Context): Boolean =
        FeatureFlagUtils.isEnabled(context, FeatureFlagUtils.SETTINGS_FLASH_NOTIFICATIONS)

    companion object {
        const val KEY = "flash_notifications"
    }
}
