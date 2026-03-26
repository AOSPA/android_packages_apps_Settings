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

package com.android.settings.notification.modes.devicestate

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.provider.Settings.EXTRA_AUTOMATIC_ZEN_RULE_ID
import com.android.settings.R
import com.android.settings.Settings.ModeSettingsActivity
import com.android.settings.contract.TAG_DEVICE_STATE_SCREEN
import com.android.settings.core.PreferenceScreenMixin
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.metadata.METADATA_IN_UI
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

//@ProvidePreferenceScreen(ZenModeDndScreen.KEY)
open class ZenModeDndScreen :
    PreferenceScreenMixin,
    PreferenceAvailabilityProvider,
    PreferenceTitleProvider,
    PreferenceSummaryProvider {
    override val key: String
        get() = KEY

    //TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.device_state_dnd_mode_screen_purpose

    override fun getMetricsCategory() =
        SettingsEnums.PAGE_UNKNOWN // TODO: correct page id in future for non-virtual migration.

    override val highlightMenuKey: Int
        get() = R.string.menu_key_priority_modes

    override fun tags(context: Context) = arrayOf(TAG_DEVICE_STATE_SCREEN)

    override fun isFlagEnabled(context: Context) = false

    override val availabilityDescription = "The device must support DND mode."

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context) = context.hasDndMode()

    override fun getTitle(context: Context): CharSequence? = context.getDndMode()?.name

    override fun getSummary(context: Context): CharSequence? =
        context.getZenModeScreenSummary(context.getDndMode())

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?): Intent? =
        Intent(context, ModeSettingsActivity::class.java)
            .putExtra(EXTRA_AUTOMATIC_ZEN_RULE_ID, context.getDndMode()?.id)

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +ZenModeDndScreenPreference(this@ZenModeDndScreen)
            +ZenModeButtonPreference(context.getDndMode()!!)
            +ZenModeDndDisplayScreen.KEY
        }

    class ZenModeDndScreenPreference(
        private val screenMetadata : ZenModeDndScreen
    ) : PreferenceMetadata, PreferenceSummaryProvider, PreferenceAvailabilityProvider, PreferenceTitleProvider,
        PersistentPreference<String> {
        override val key : String
            get() = "device_state_dnd_mode_screen_preference"

        override val purpose : Int
            get() = screenMetadata.purpose

        override fun tags(context: Context) = arrayOf(METADATA_IN_UI)

        override val indexable = false

        override fun isEnabled(context: Context) : Boolean = screenMetadata.isEnabled(context)

        override fun getSummary(context: Context) : CharSequence? = screenMetadata.getSummary(context)

        override val availabilityDescription = screenMetadata.availabilityDescription

        override fun getAvailabilityStability() = screenMetadata.getAvailabilityStability()

        override fun isAvailable(context: Context) : Boolean = screenMetadata.isAvailable(context)

        override fun getTitle(context: Context): CharSequence? = screenMetadata.getTitle(context)

        override val supportsWrite: Boolean
            get() = false

        override val valueType = String::class.javaObjectType

        override fun storage(context: Context): KeyValueStore = createSummaryStorage(context, key)
    }

    companion object {
        const val KEY = "device_state_dnd_mode_screen" // only for device state.
    }
}
