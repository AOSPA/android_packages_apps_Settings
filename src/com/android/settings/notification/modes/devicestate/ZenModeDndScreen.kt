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

import android.annotation.FlaggedApi
import android.app.Flags as AppFlags
import android.content.Context
import android.content.Intent
import android.provider.Settings.EXTRA_AUTOMATIC_ZEN_RULE_ID
import com.android.settings.Settings.ModeSettingsActivity
import com.android.settings.contract.TAG_DEVICE_STATE_SCREEN
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceFragment
import com.android.settingslib.preference.PreferenceScreenCreator

@ProvidePreferenceScreen(ZenModeDndScreen.KEY)
class ZenModeDndScreen :
    PreferenceScreenCreator,
    PreferenceAvailabilityProvider,
    PreferenceTitleProvider,
    PreferenceSummaryProvider {
    override val key: String
        get() = KEY

    override fun tags(context: Context) = arrayOf(TAG_DEVICE_STATE_SCREEN)

    override fun isFlagEnabled(context: Context) = Flags.deviceState() && AppFlags.modesUi()

    override fun fragmentClass() = PreferenceFragment::class.java

    override fun isAvailable(context: Context) = AppFlags.modesUi() && context.hasDndMode()

    override fun getTitle(context: Context): CharSequence? = context.getDndMode()?.name

    override fun getSummary(context: Context): CharSequence? =
        context.getZenModeScreenSummary(context.getDndMode())

    @FlaggedApi(AppFlags.FLAG_MODES_UI)
    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?): Intent? =
        if (AppFlags.modesUi())
            Intent(context, ModeSettingsActivity::class.java)
                .putExtra(EXTRA_AUTOMATIC_ZEN_RULE_ID, context.getDndMode()?.id)
        else null

    override fun getPreferenceHierarchy(context: Context) = preferenceHierarchy(context, this) {}

    companion object {
        const val KEY = "device_state_dnd_mode_screen"
    }
}
