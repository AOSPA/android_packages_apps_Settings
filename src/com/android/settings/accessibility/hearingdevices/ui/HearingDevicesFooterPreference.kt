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

package com.android.settings.accessibility.hearingdevices.ui

import android.content.Context
import android.text.Html
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.accessibility.HearingAidHelper
import com.android.settings.accessibility.shared.ui.AccessibilityFooterPreferenceBinding
import com.android.settings.accessibility.shared.ui.AccessibilityFooterPreferenceMetadata
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.widget.FooterPreference

class HearingDevicesFooterPreference(
    private val context: Context,
    private val helper: HearingAidHelper = HearingAidHelper(context),
) :
    AccessibilityFooterPreferenceMetadata,
    AccessibilityFooterPreferenceBinding,
    PreferenceTitleProvider,
    PreferenceAvailabilityProvider {
    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.hearing_device_footer_purpose

    override val introductionTitle: Int
        get() = R.string.accessibility_hearing_device_about_title

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)

        val footerPreference = preference as FooterPreference
        footerPreference.isSelectable = false

        val aboutTts = context.getString(introductionTitle)
        val summaryTts = context.getString(getSummaryResId(true))
        footerPreference.contentDescription = "$aboutTts\n$summaryTts"
    }

    override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)

    override fun getTitle(context: Context): CharSequence? {
        // We use html tag inside footer string, so it is better to load from html to have better
        // html tag support.
        return Html.fromHtml(
            context.getString(getSummaryResId(false)),
            Html.FROM_HTML_MODE_COMPACT,
            /* imageGetter= */ null,
            /* tagHandler= */ null,
        )
    }

    private fun getSummaryResId(isTts: Boolean): Int {
        val isAshaProfileSupported = helper.isAshaProfileSupported()
        val isHapClientProfileSupported = helper.isHapClientProfileSupported()
        val (htmlResId, ttsResId) =
            when {
                isAshaProfileSupported && isHapClientProfileSupported ->
                    Pair(
                        R.string.accessibility_hearing_device_footer_summary,
                        R.string.accessibility_hearing_device_footer_summary_tts,
                    )

                isAshaProfileSupported ->
                    Pair(
                        R.string.accessibility_hearing_device_footer_asha_only_summary,
                        R.string.accessibility_hearing_device_footer_asha_only_summary_tts,
                    )

                isHapClientProfileSupported ->
                    Pair(
                        R.string.accessibility_hearing_device_footer_hap_only_summary,
                        R.string.accessibility_hearing_device_footer_hap_only_summary_tts,
                    )

                else -> Pair(0, 0)
            }
        return if (isTts) ttsResId else htmlResId
    }

    override val availabilityDescription = UI_ONLY_PREFERENCE

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context): Boolean = helper.isHearingAidSupported

    companion object {
        const val KEY = "hearing_device_footer"
    }
}
