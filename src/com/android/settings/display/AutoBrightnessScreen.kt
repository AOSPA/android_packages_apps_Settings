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
package com.android.settings.display

import android.app.settings.SettingsEnums
import android.app.settings.SettingsEnums.ACTION_ADAPTIVE_BRIGHTNESS
import android.content.Context
import android.os.UserManager
import android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE
import android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
import android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings
import com.android.settings.contract.KEY_ADAPTIVE_BRIGHTNESS
import com.android.settings.core.BasePreferenceController.AVAILABLE
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.metrics.PreferenceActionMetricsProvider
import com.android.settings.restriction.PreferenceRestrictionMixin
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.PrimarySwitchPreferenceBinding
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsStore
import com.android.settingslib.datastore.SettingsSystemStore
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.METADATA_IN_UI
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_UNCATEGORIZED
import kotlinx.coroutines.CoroutineScope

@ProvidePreferenceScreen(AutoBrightnessScreen.KEY)
open class AutoBrightnessScreen :
    PreferenceScreenMixin,
    PrimarySwitchPreferenceBinding,
    PreferenceActionMetricsProvider,
    PreferenceAvailabilityProvider,
    PreferenceRestrictionMixin,
    BooleanValuePreference {
    override fun tags(context: Context) =
        arrayOf(
            APP_FUNCTION_UNCATEGORIZED,
            KEY_ADAPTIVE_BRIGHTNESS,
            // exclude this screen from api result since we have the same data in api_auto_brightness_entry
            UI_ONLY_PREFERENCE
        )

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.auto_brightness_entry_purpose

    override val title: Int
        get() = R.string.auto_brightness_title

    override val highlightMenuKey: Int
        get() = R.string.menu_key_display

    override fun getMetricsCategory() = SettingsEnums.SETTINGS_AUTO_BRIGHTNESS

    override val preferenceActionMetrics: Int
        get() = ACTION_ADAPTIVE_BRIGHTNESS

    override fun isFlagEnabled(context: Context) = Flags.catalystScreenBrightnessMode()

    override fun fragmentClass(): Class<out Fragment>? = AutoBrightnessSettings::class.java

    override fun hasCompleteHierarchy() = false

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) { +AutoBrightnessScreenPreference(this@AutoBrightnessScreen) }

    override fun storage(context: Context): KeyValueStore =
        AutoBrightnessDataStore(SettingsSystemStore.get(context))

    override fun getReadPermissions(context: Context) = SettingsSystemStore.getReadPermissions()

    override fun getWritePermissions(context: Context) = SettingsSystemStore.getWritePermissions()

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(
        context: Context,
        value: Boolean?,
        callingPid: Int,
        callingUid: Int,
    ) = ReadWritePermit.ALLOW

    override val supportsWrite = true
    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, Settings.AdaptiveBrightnessActivity::class.java, metadata?.key)

    override val availabilityDescription =
        "The device must support adaptive brightness."

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context) =
        context.autoBrightnessAvailabilityStatus == AVAILABLE

    override fun getEnabledDescription(): String = "This setting must not be restricted by a device administrator."

    override fun getEnabledStability() = PreconditionStability.UNSTABLE

    override fun isEnabled(context: Context) = super<PreferenceRestrictionMixin>.isEnabled(context)

    override val restrictionKeys
        get() = arrayOf(UserManager.DISALLOW_CONFIG_BRIGHTNESS)

    override val useAdminDisabledSummary: Boolean
        get() = true

    /**
     * The datastore for brightness, which is persisted as integer but the external type is boolean.
     */
    @Suppress("UNCHECKED_CAST")
    private class AutoBrightnessDataStore(private val settingsStore: SettingsStore) :
        AbstractKeyedDataObservable<String>(), KeyedObserver<String>, KeyValueStore {

        override fun contains(key: String) = settingsStore.contains(SCREEN_BRIGHTNESS_MODE)

        override fun <T : Any> getDefaultValue(key: String, valueType: Class<T>) =
            DEFAULT_VALUE.toBoolean() as T

        override fun <T : Any> getValue(key: String, valueType: Class<T>) =
            (settingsStore.getInt(SCREEN_BRIGHTNESS_MODE) ?: DEFAULT_VALUE).toBoolean() as T

        override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) =
            settingsStore.setInt(SCREEN_BRIGHTNESS_MODE, (value as? Boolean)?.toBrightnessMode())

        override fun onFirstObserverAdded() {
            // observe the underlying storage key
            settingsStore.addObserver(SCREEN_BRIGHTNESS_MODE, this, HandlerExecutor.main)
        }

        override fun onKeyChanged(key: String, reason: Int) {
            // forward data change to preference hierarchy key
            notifyChange(KEY, reason)
        }

        override fun onLastObserverRemoved() {
            settingsStore.removeObserver(SCREEN_BRIGHTNESS_MODE, this)
        }

        /** Converts brightness mode integer to boolean. */
        private fun Int.toBoolean() = this == SCREEN_BRIGHTNESS_MODE_AUTOMATIC

        /** Converts boolean value to brightness mode integer. */
        private fun Boolean.toBrightnessMode() =
            if (this) SCREEN_BRIGHTNESS_MODE_AUTOMATIC else SCREEN_BRIGHTNESS_MODE_MANUAL
    }

    class AutoBrightnessScreenPreference(
        private val screenMetadata : AutoBrightnessScreen
    ) : PreferenceMetadata, BooleanValuePreference,
        PreferenceAvailabilityProvider {
        override val key : String
            get() = "auto_brightness_entry_preference"

        override val purpose : Int
            get() = screenMetadata.purpose

        override fun tags(context: Context) = arrayOf(METADATA_IN_UI)

        override val indexable = false

        override fun isEnabled(context: Context) : Boolean = screenMetadata.isEnabled(context)

        override val availabilityDescription = screenMetadata.availabilityDescription

        override fun getAvailabilityStability() = screenMetadata.getAvailabilityStability()

        override fun isAvailable(context: Context) : Boolean = screenMetadata.isAvailable(context)

        override val sensitivityLevel = SensitivityLevel.NO_SENSITIVITY

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
            value: Boolean?,
            callingPid: Int,
            callingUid: Int,
        ) : @ReadWritePermit Int = screenMetadata.getWritePermit(context, value,  callingPid, callingUid)
        override val supportsWrite = true
    }

    companion object {
        const val KEY = "auto_brightness_entry"
        private const val DEFAULT_VALUE = SCREEN_BRIGHTNESS_MODE_MANUAL
    }
}
