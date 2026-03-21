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
import com.android.settings.accessibility.FlashNotificationsUtil
import com.android.settings.accessibility.FlashNotificationsUtil.State
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.metadata.METADATA_IN_UI
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.widget.UntitledPreferenceCategoryMetadata
import kotlinx.coroutines.CoroutineScope
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_UNCATEGORIZED

@ProvidePreferenceScreen(FlashNotificationsScreen.KEY)
open class FlashNotificationsScreen :
    PreferenceScreenMixin, PreferenceAvailabilityProvider, PreferenceSummaryProvider {
    override fun tags(context: Context) = arrayOf(APP_FUNCTION_UNCATEGORIZED)

    override val key: String
        get() = KEY

    //TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.flash_notifications_purpose

    override val title: Int
        get() = R.string.flash_notifications_title

    override val highlightMenuKey: Int
        get() = R.string.menu_key_accessibility

    override val indexable
        get() = true

    override val keywords: Int
        get() = R.string.flash_notifications_keywords

    override val icon: Int
        get() = R.drawable.ic_flash_notification

    override fun getMetricsCategory(): Int = SettingsEnums.FLASH_NOTIFICATION_SETTINGS

    override fun isFlagEnabled(context: Context): Boolean = Flags.catalystFlashNotifications()

    override fun fragmentClass(): Class<out Fragment>? =
        FlashNotificationsPreferenceFragment::class.java

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?): Intent? =
        makeLaunchIntent(context, FlashNotificationsActivity::class.java, metadata?.key)

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +FlashNotificationsScreenPreference(this@FlashNotificationsScreen)
            +FlashNotificationsTopIntroPreference()
            +FlashNotificationsIllustrationPreference()
            +UntitledPreferenceCategoryMetadata(
                CATEGORY_KEY,
                purpose = R.string.flash_notifications_category_purpose
            ) += {
                +CameraFlashSwitchPreference()
                +ScreenFlashSwitchPreference()
            }
            +FlashNotificationsPreviewPreference()
            +FlashNotificationsFooterPreference()
        }

    override val availabilityDescription =
        "The device must support the flash notifications feature."

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context): Boolean =
        FeatureFlagUtils.isEnabled(context, FeatureFlagUtils.SETTINGS_FLASH_NOTIFICATIONS)

    override fun getSummary(context: Context): CharSequence? {
        return when (FlashNotificationsUtil.getFlashNotificationsState(context)) {
            State.CAMERA,
            State.SCREEN,
            State.CAMERA_SCREEN -> context.getString(R.string.flash_notifications_summary_on)
            else -> context.getString(R.string.flash_notifications_summary_off)
        }
    }

    class FlashNotificationsScreenPreference(
        private val screenMetadata : FlashNotificationsScreen
    ) : PreferenceMetadata, PreferenceAvailabilityProvider, PreferenceSummaryProvider, PersistentPreference<String> {
        override val key : String
            get() = "flash_notifications_preference"

        override val purpose : Int
            get() = screenMetadata.purpose

        override fun tags(context: Context) = arrayOf(METADATA_IN_UI)

        override val indexable = false

        override fun isEnabled(context: Context) : Boolean = screenMetadata.isEnabled(context)

        override fun getSummary(context: Context) : CharSequence? = screenMetadata.getSummary(context)

        override val supportsWrite = false

        override val valueType = String::class.javaObjectType

        override fun storage(context: Context): KeyValueStore = createSummaryStorage(context, key)

        override val availabilityDescription = screenMetadata.availabilityDescription

    override fun getAvailabilityStability() = screenMetadata.getAvailabilityStability()

        override fun isAvailable(context: Context) : Boolean = screenMetadata.isAvailable(context)
    }

    companion object {
        const val KEY = "flash_notifications"
        const val CATEGORY_KEY = "flash_notifications_category"
    }
}
