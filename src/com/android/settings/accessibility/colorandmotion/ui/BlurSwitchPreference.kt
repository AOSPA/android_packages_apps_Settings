/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings.accessibility.colorandmotion.ui

import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import android.view.CrossWindowBlurListeners.CROSS_WINDOW_BLUR_SUPPORTED
import com.android.settings.R
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.SettingsGlobalStore
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.SwitchPreference

class BlurSwitchPreference :
    SwitchPreference(
        key = KEY,
        purpose = R.string.disable_window_blurs_purpose,
        title = R.string.blur_switch,
    ),
    PreferenceAvailabilityProvider,
    PreferenceSummaryProvider {

    override val availabilityDescription = "The device must support cross window blur."

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context) = CROSS_WINDOW_BLUR_SUPPORTED

    override fun getEnabledDescription(): String = "Battery saver must be turned off."

    override fun getEnabledStability() = PreconditionStability.UNSTABLE

    override fun isEnabled(context: Context) = !context.isPowerSaveMode()

    override val icon: Int
        get() = R.drawable.ic_blur

    override val keywords: Int
        get() = R.string.keywords_blur_switch

    override fun storage(context: Context): KeyValueStore =
        SettingsGlobalStore.get(context).apply { setDefaultValue(KEY, false) }

    override fun getSummary(context: Context): CharSequence? {
        return context.getString(
            if (!isEnabled(context)) R.string.blur_switch_disabled_summary
            else R.string.blur_switch_summary
        )
    }

    override fun getWritePermit(
        context: Context,
        value: Boolean?,
        callingPid: Int,
        callingUid: Int,
    ) = ReadWritePermit.ALLOW

    override val supportsWrite = true

    override val sensitivityLevel: Int
        get() = SensitivityLevel.NO_SENSITIVITY

    companion object {
        const val KEY = Settings.Global.DISABLE_WINDOW_BLURS

        private fun Context.isPowerSaveMode() =
            getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
    }
}
