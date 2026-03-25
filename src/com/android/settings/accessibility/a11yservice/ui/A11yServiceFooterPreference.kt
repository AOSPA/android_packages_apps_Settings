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

package com.android.settings.accessibility.a11yservice.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.text.Html
import com.android.settings.R
import com.android.settings.accessibility.extensions.getFeatureName
import com.android.settings.accessibility.extensions.isServiceEnabled
import com.android.settings.accessibility.shared.ui.AccessibilityFooterPreferenceBinding
import com.android.settings.accessibility.shared.ui.AccessibilityFooterPreferenceIntroductionTitleProvider
import com.android.settings.accessibility.shared.ui.AccessibilityFooterPreferenceMetadata
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE

/** Footer metadata for [AccessibilityServiceInfo]'s description. */
class A11yServiceFooterPreference(
    override val key: String,
    override val purpose: Int,
    private val serviceInfo: AccessibilityServiceInfo,
    private val loadHtmlFooter: Boolean,
) :
    AccessibilityFooterPreferenceMetadata,
    AccessibilityFooterPreferenceBinding,
    PreferenceTitleProvider,
    AccessibilityFooterPreferenceIntroductionTitleProvider,
    PreferenceAvailabilityProvider {

    override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)

    override fun getTitle(context: Context): CharSequence? {
        if (loadHtmlFooter) {
            return serviceInfo.loadHtmlDescription(context.packageManager)?.let {
                Html.fromHtml(
                    it,
                    Html.FROM_HTML_MODE_COMPACT,
                    /* imageGetter= */ null,
                    /* tagHandler= */ null,
                )
            }
        } else {
            return if (shouldShowCrashDescription(context, serviceInfo)) {
                context.getText(R.string.accessibility_description_state_stopped)
            } else {
                serviceInfo.loadDescription(context.packageManager)
            }
        }
    }

    override fun getIntroductionTitle(context: Context): CharSequence? {
        return context.getString(
            R.string.accessibility_introduction_title,
            serviceInfo.getFeatureName(context),
        )
    }

    override val availabilityDescription = UI_ONLY_PREFERENCE

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context): Boolean {
        return getTitle(context)?.isNotBlank() == true
    }

    private fun shouldShowCrashDescription(
        context: Context,
        serviceInfo: AccessibilityServiceInfo,
    ): Boolean {
        return serviceInfo.isServiceEnabled(context) && serviceInfo.crashed
    }

    companion object {
        const val FOOTER_KEY = "footer_info"
        const val HTML_FOOTER_KEY = "html_footer_info"
    }
}
