/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.settings.security

import android.app.settings.SettingsEnums
import android.content.Context
import android.provider.Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS
import android.provider.Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.LockScreenSettingsActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.notification.LockScreenNotificationPreferenceController
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.METADATA_IN_UI
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.PreferenceChangeReason
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_UNCATEGORIZED

@ProvidePreferenceScreen(LockScreenPreferenceScreen.KEY)
open class LockScreenPreferenceScreen(private val context: Context) :
    PreferenceScreenMixin, AbstractKeyedDataObservable<String>(), PreferenceSummaryProvider {
    override fun tags(context: Context) = arrayOf(APP_FUNCTION_UNCATEGORIZED)


    private val observer =
        KeyedObserver<String> { _, _ -> notifyChange(KEY, PreferenceChangeReason.STATE) }

    override val key: String
        get() = KEY

    //TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.lockscreen_from_display_settings_purpose

    override val title: Int
        get() = R.string.lockscreen_settings_title

    override val keywords: Int
        get() = R.string.keywords_ambient_display_screen

    override val highlightMenuKey: Int
        get() = R.string.menu_key_display

    override fun getMetricsCategory() = SettingsEnums.SETTINGS_LOCK_SCREEN_PREFERENCES

    override fun onFirstObserverAdded() {
        val store = SettingsSecureStore.get(context)
        val executor = HandlerExecutor.main
        // update summary when lock screen notification settings are changed
        store.addObserver(LOCK_SCREEN_SHOW_NOTIFICATIONS, observer, executor)
        store.addObserver(LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, observer, executor)
    }

    override fun onLastObserverRemoved() {
        val store = SettingsSecureStore.get(context)
        store.removeObserver(LOCK_SCREEN_SHOW_NOTIFICATIONS, observer)
        store.removeObserver(LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, observer)
    }

    override fun getSummary(context: Context): CharSequence? =
        context.getString(LockScreenNotificationPreferenceController.getSummaryResource(context))

    override fun isFlagEnabled(context: Context) = Flags.catalystLockscreenFromDisplaySettings()

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass(): Class<out Fragment>? = LockscreenDashboardFragment::class.java

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, LockScreenSettingsActivity::class.java, metadata?.key)

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) { +LockScreenPreferenceScreenPreference(this@LockScreenPreferenceScreen) }

    class LockScreenPreferenceScreenPreference(
        private val screenMetadata : LockScreenPreferenceScreen
    ) : PreferenceMetadata, PreferenceSummaryProvider, PersistentPreference<String> {
        override val key : String
            get() = "lockscreen_from_display_settings_preference"

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
    }

    companion object {
        const val KEY = "lockscreen_from_display_settings"
    }
}
