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

package com.android.settings.display

import android.app.settings.SettingsEnums
import android.content.Context
import android.hardware.display.ColorDisplayManager
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.NightDisplaySettingsActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.PrimarySwitchPreferenceBinding
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.METADATA_IN_UI
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceHierarchy
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.TopIntroPreference
import kotlinx.coroutines.CoroutineScope
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_UNCATEGORIZED

// LINT.IfChange
@ProvidePreferenceScreen(NightDisplayScreen.KEY)
open class NightDisplayScreen(val context: Context) :
    PreferenceScreenMixin,
    BooleanValuePreference,
    PrimarySwitchPreferenceBinding,
    PreferenceAvailabilityProvider,
    PreferenceSummaryProvider {
    override fun tags(context: Context) = arrayOf(
        APP_FUNCTION_UNCATEGORIZED,
        // exclude this screen from api result since we have the same data (and more) in api_night_display screen
        UI_ONLY_PREFERENCE
    )

    val colorDisplayManager: ColorDisplayManager? =
        context.getSystemService(ColorDisplayManager::class.java)

    val timeFormatter: NightDisplayTimeFormatter
        get() = NightDisplayTimeFormatter(context)

    override fun getSummary(context: Context): CharSequence? =
        timeFormatter.getAutoModeSummary(context, colorDisplayManager)

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.night_display_purpose

    override val highlightMenuKey: Int
        get() = R.string.menu_key_display

    override val title: Int
        get() = R.string.night_display_title

    override val keywords: Int
        get() = R.string.keywords_display_night_display

    override fun hasCompleteHierarchy() = false

    override fun getReadPermissions(context: Context) = SettingsSecureStore.getReadPermissions()

    override fun getWritePermissions(context: Context) = SettingsSecureStore.getWritePermissions()

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val supportsWrite = true
    override fun storage(context: Context): KeyValueStore = NightDisplayStorage(context)

    override fun fragmentClass(): Class<out Fragment>? = NightDisplaySettings::class.java

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, NightDisplaySettingsActivity::class.java, metadata?.key)

    override fun getPreferenceHierarchy(
        context: Context,
        coroutineScope: CoroutineScope,
    ): PreferenceHierarchy = preferenceHierarchy(context) {
        +NightDisplayScreenPreference(this@NightDisplayScreen)
        +NightDisplayTopIntroPreference()
    }

    override val availabilityDescription =
        "The device must support night display settings."

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context): Boolean = context.isNightDisplaySettingsAvailable

    override fun getMetricsCategory(): Int = SettingsEnums.NIGHT_DISPLAY_SETTINGS

    override val sensitivityLevel: @SensitivityLevel Int
        get() = SensitivityLevel.NO_SENSITIVITY

    class NightDisplayScreenPreference(
        private val screenMetadata : NightDisplayScreen
    ) : PreferenceMetadata, PreferenceSummaryProvider,
        BooleanValuePreference, PreferenceAvailabilityProvider {
        override val key : String
            get() = "night_display_preference"

        override val purpose : Int
            get() = screenMetadata.purpose

        override fun tags(context: Context) = arrayOf(METADATA_IN_UI)

        override val indexable = false

        override fun getEnabledDescription(): String? = screenMetadata.getEnabledDescription()

        override fun isEnabled(context: Context) : Boolean = screenMetadata.isEnabled(context)

        override fun getSummary(context: Context) : CharSequence? = screenMetadata.getSummary(context)

        override val availabilityDescription = screenMetadata.availabilityDescription

        override fun getAvailabilityStability() = screenMetadata.getAvailabilityStability()

        override fun isAvailable(context: Context) : Boolean = screenMetadata.isAvailable(context)

        override fun storage(context: Context) : KeyValueStore = screenMetadata.storage(context)

        override fun getReadPermissions(context: Context) = screenMetadata.getReadPermissions(context)

        override fun getReadPermit(
            context: Context,
            callingPid: Int,
            callingUid: Int
        ) : @ReadWritePermit Int = screenMetadata.getReadPermit(context, callingPid, callingUid)

        override fun getWritePermissions(context: Context) = screenMetadata.getWritePermissions(context)

        override fun getWritePermit(
            context: Context,
            callingPid: Int,
            callingUid: Int
        ) : @ReadWritePermit Int = screenMetadata.getWritePermit(context, callingPid, callingUid)
        override val supportsWrite = true
    }

    companion object {
        const val KEY = "night_display"
    }
}

internal class NightDisplayTopIntroPreference :
    PreferenceMetadata, PreferenceBinding, PreferenceAvailabilityProvider {

    override val key: String
        get() = "night_display_top_intro"

    override val purpose: Int
        get() = R.string.night_display_top_intro_purpose

    override val title: Int
        get() = R.string.night_display_text

    override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)

    override val indexable
        get() = false

    override fun createWidget(context: Context) = TopIntroPreference(context)

    override val availabilityDescription = UI_ONLY_PREFERENCE

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context): Boolean = context.isNightDisplaySettingsAvailable
}
// LINT.ThenChange(NightDisplaySettings.java, NightDisplayApiScreen.kt)
