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

package com.android.settings.dream

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.android.settings.Utils
import com.android.settings.display.AmbientDisplayAlwaysOnPreferenceController
import com.android.settings.flags.Flags
import com.android.settings.R
import com.android.settingslib.dream.DreamBackend
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceScreenCreator

// LINT.IfChange
@ProvidePreferenceScreen(ScreensaverScreen.KEY)
class ScreensaverScreen(fooContext: Context) :
    PreferenceScreenCreator, PreferenceAvailabilityProvider, PreferenceSummaryProvider {

    private var dreamBackend: DreamBackend = DreamBackend.getInstance(fooContext)

    private var ambientModeSuppressionProvider: AmbientModeSuppressionProvider =
        object : AmbientModeSuppressionProvider {
            override fun isSuppressedByBedtime(context: Context) =
                AmbientDisplayAlwaysOnPreferenceController.isAodSuppressedByBedtime(context)
        }

    private var summaryStringsProvider: SummaryStringsProvider = object: SummaryStringsProvider {
        override fun dreamOff(context: Context) =
            context.resources.getString(R.string.screensaver_settings_summary_off)

        override fun dreamOn(context: Context, activeDreamName: CharSequence) =
            context.resources.getString(
                R.string.screensaver_settings_summary_on,
                activeDreamName
            )

        override fun dreamOffBedtime(context: Context) =
            context.resources.getString(R.string.screensaver_settings_when_to_dream_bedtime)
    }

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.screensaver_settings_title

    override fun isFlagEnabled(context: Context) = Flags.catalystScreensaver()

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass() = DreamSettings::class.java

    override fun getPreferenceHierarchy(context: Context) = preferenceHierarchy(context, this) {}

    override fun getSummary(context: Context): CharSequence {
        val dreamsDisabledByAmbientModeSuppression = context.resources.getBoolean(
            com.android.internal.R.bool.config_dreamsDisabledByAmbientModeSuppressionConfig)
        return if (dreamsDisabledByAmbientModeSuppression
            && ambientModeSuppressionProvider.isSuppressedByBedtime(context)) {
            summaryStringsProvider.dreamOffBedtime(context)
        } else {
            getSummaryTextWithDreamName(context)
        }
    }

    override val keywords: Int
        get() = R.string.keywords_screensaver

    override fun isAvailable(context: Context) = Utils.areDreamsAvailableToCurrentUser(context)

    @VisibleForTesting
    fun setDreamBackend(backend: DreamBackend) {
        this.dreamBackend = backend
    }

    @VisibleForTesting
    fun setAmbientModeSuppressionProvider(provider: AmbientModeSuppressionProvider) {
        ambientModeSuppressionProvider = provider
    }

    @VisibleForTesting
    fun setSummaryStringsProvider(provider: SummaryStringsProvider) {
        summaryStringsProvider = provider
    }

    private fun getSummaryTextWithDreamName(context: Context): CharSequence {
        return if (dreamBackend.isEnabled) {
            // In practice, activeDreamName would not be null, but it can be in tests and in that
            // case it needs to have the same behavior as the corresponding method in
            // DreamSettings (see b/411793272).
            summaryStringsProvider.dreamOn(context, dreamBackend.activeDreamName ?: "null")
        } else {
            summaryStringsProvider.dreamOff(context)
        }
    }

    interface AmbientModeSuppressionProvider {
        fun isSuppressedByBedtime(context: Context) : Boolean
    }

    interface SummaryStringsProvider {
        fun dreamOff(context: Context) : CharSequence
        fun dreamOn(context: Context, activeDreamName: CharSequence) : CharSequence
        fun dreamOffBedtime(context: Context) : CharSequence
    }

    companion object {
        const val KEY = "screensaver"
    }
}
// LINT.ThenChange(DreamSettings.java)
