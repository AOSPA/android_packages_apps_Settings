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

package com.android.settings.notification

import android.app.settings.SettingsEnums
import android.content.Context
import android.provider.Settings.Secure.NOTIFICATION_BUBBLES
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.BubbleNotificationSettingsActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.METADATA_IN_UI
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_NOTIFICATIONS

// LINT.IfChange
@ProvidePreferenceScreen(BubbleNotificationScreen.KEY)
open class BubbleNotificationScreen :
    PreferenceScreenMixin, PreferenceSummaryProvider, PreferenceAvailabilityProvider {
    override fun tags(context: Context) = arrayOf(APP_FUNCTION_NOTIFICATIONS)

    override val key: String
        get() = KEY

    // TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.notification_bubbles_purpose

    override val title: Int
        get() = R.string.notification_bubbles_title

    override val screenTitle: Int
        get() = R.string.bubbles_app_toggle_title

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) { +BubbleNotificationScreenPreference(this@BubbleNotificationScreen) }

    override fun fragmentClass(): Class<out Fragment>? = BubbleNotificationSettings::class.java

    override fun hasCompleteHierarchy() = false

    override val highlightMenuKey
        get() = R.string.menu_key_notifications

    override fun getMetricsCategory(): Int = SettingsEnums.BUBBLE_SETTINGS

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, BubbleNotificationSettingsActivity::class.java, metadata?.key)

    override fun getSummary(context: Context): CharSequence? {
        val enabled =
            SettingsSecureStore.get(context).getInt(NOTIFICATION_BUBBLES)
                ?: BubbleHelper.SYSTEM_WIDE_ON
        return context.getString(
            if (enabled == BubbleHelper.SYSTEM_WIDE_ON)
                R.string.notifications_bubble_setting_on_summary
            else R.string.switch_off_text
        )
    }

    override val availabilityDescription = "The device must support bubbles and must not be a low-ram device."

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context): Boolean = BubbleHelper.isSupportedByDevice(context)

    class BubbleNotificationScreenPreference(
        private val screenMetadata : BubbleNotificationScreen
    ) : PreferenceMetadata, PreferenceSummaryProvider, PreferenceAvailabilityProvider, PersistentPreference<String> {
        override val key : String
            get() = "notification_bubbles_preference"

        override val purpose : Int
            get() = screenMetadata.purpose

        override fun tags(context: Context) = arrayOf(METADATA_IN_UI)

        override val indexable = false

        override fun isEnabled(context: Context) : Boolean = screenMetadata.isEnabled(context)

        override fun getSummary(context: Context) : CharSequence? = screenMetadata.getSummary(context)

        override val supportsWrite: Boolean
            get() = false

        override val valueType = String::class.javaObjectType

        override fun storage(context: Context): KeyValueStore = createSummaryStorage(context, key)

        override val availabilityDescription = screenMetadata.availabilityDescription

        override fun getAvailabilityStability() = screenMetadata.getAvailabilityStability()

        override fun isAvailable(context: Context) : Boolean = screenMetadata.isAvailable(context)
    }

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    companion object {
        const val KEY = "notification_bubbles"
    }
}
// LINT.ThenChange(BubbleNotificationSettings.java,
//                 BubbleSummaryNotificationPreferenceController.java)
