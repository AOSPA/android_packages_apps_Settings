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

import android.app.settings.SettingsEnums.ACTION_AMBIENT_DISPLAY_ALWAYS_ON
import android.content.Context
import android.hardware.display.AmbientDisplayConfiguration
import android.os.SystemProperties
import android.os.UserHandle
import android.os.UserManager
import com.android.settings.R
import com.android.settings.contract.KEY_AMBIENT_DISPLAY_ALWAYS_ON
import com.android.settings.display.AmbientDisplayAlwaysOnPreferenceController.isAodSuppressedByBedtime
import com.android.settings.display.ambient.AmbientDisplayMainSwitchPreference
import com.android.settings.display.ambient.AmbientDisplayStorage
import com.android.settings.display.ambient.AmbientDisplayTopIntroPreference
import com.android.settings.display.ambient.AmbientWallpaperOptionsCategory
import com.android.settings.display.ambient.AmbientWallpaperPreference
import com.android.settings.metrics.PreferenceActionMetricsProvider
import com.android.settings.restriction.PreferenceRestrictionMixin
import com.android.settingslib.PrimarySwitchPreferenceBinding
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceFragment
import com.android.settingslib.preference.PreferenceScreenCreator
import com.android.systemui.shared.Flags.ambientAod

// LINT.IfChange
/**
 * Contains the PrimarySwitchPreference for use on the Display setting page, and also the preference
 * subpage for additional related settings.
 */
@ProvidePreferenceScreen(AmbientDisplayAlwaysOnPreferenceScreen.KEY)
class AmbientDisplayAlwaysOnPreferenceScreen :
    BooleanValuePreference,
    PreferenceActionMetricsProvider,
    PreferenceScreenCreator,
    PrimarySwitchPreferenceBinding,
    PreferenceAvailabilityProvider,
    PreferenceRestrictionMixin,
    PreferenceLifecycleProvider,
    PreferenceSummaryProvider {

    private val ambientWallpaperPreference = AmbientWallpaperPreference()

    override val title: Int
        get() = if (ambientAod()) R.string.doze_always_on_title2 else R.string.doze_always_on_title

    override val key: String
        get() = KEY

    override val keywords: Int
        get() = R.string.keywords_always_show_time_info

    override val preferenceActionMetrics: Int
        get() = ACTION_AMBIENT_DISPLAY_ALWAYS_ON

    override fun tags(context: Context) = arrayOf(KEY_AMBIENT_DISPLAY_ALWAYS_ON)

    override val restrictionKeys: Array<String>
        get() = arrayOf(UserManager.DISALLOW_AMBIENT_DISPLAY)

    override fun isEnabled(context: Context) = super<PreferenceRestrictionMixin>.isEnabled(context)

    override fun isAvailable(context: Context): Boolean {
        if (!ambientAod()) return false
        return !SystemProperties.getBoolean(PROP_AWARE_AVAILABLE, false) &&
            AmbientDisplayConfiguration(context).alwaysOnAvailableForUser(UserHandle.myUserId())
    }

    override fun dependencies(context: Context) = arrayOf(AmbientWallpaperPreference.KEY)

    override fun getSummary(context: Context): CharSequence? =
        context.getText(
            if (isAodSuppressedByBedtime(context)) {
                R.string.aware_summary_when_bedtime_on
            } else if (ambientWallpaperPreference.isAvailable(context)) {
                if (ambientWallpaperPreference.isChecked(context)) {
                    R.string.doze_always_on_summary_with_wallpaper
                } else {
                    R.string.doze_always_on_summary_without_wallpaper
                }
            } else {
                R.string.doze_always_on_summary
            }
        )

    override fun onStart(context: PreferenceLifecycleContext) {
        context.notifyPreferenceChange(KEY)
    }

    override fun fragmentClass() = AmbientPreferenceFragment::class.java

    override fun getPreferenceHierarchy(context: Context) =
        preferenceHierarchy(context, this) {
            +AmbientDisplayTopIntroPreference()
            +AmbientDisplayMainSwitchPreference()
            +AmbientWallpaperOptionsCategory() += { +ambientWallpaperPreference }
        }

    override fun storage(context: Context): KeyValueStore = AmbientDisplayStorage(context)

    override fun getReadPermissions(context: Context) = SettingsSecureStore.getReadPermissions()

    override fun getWritePermissions(context: Context) = SettingsSecureStore.getWritePermissions()

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    companion object {
        const val KEY = "ambient_display_always_on_screen"
        const val PROP_AWARE_AVAILABLE = "ro.vendor.aware_available"
    }
}

// LINT.ThenChange(AmbientDisplayAlwaysOnPreferenceController.java)

class AmbientPreferenceFragment : PreferenceFragment() {
    override fun getPreferenceScreenBindingKey(context: Context): String {
        return AmbientDisplayAlwaysOnPreferenceScreen.KEY
    }
}
