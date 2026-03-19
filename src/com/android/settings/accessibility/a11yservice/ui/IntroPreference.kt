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
import android.text.TextUtils
import com.android.settings.R
import com.android.settings.accessibility.extensions.isInSetupWizard
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.TopIntroPreference

/** Handles fetching and display the introduction text of an [AccessibilityServiceInfo]. */
class IntroPreference(private val serviceInfo: AccessibilityServiceInfo) :
    PreferenceMetadata, PreferenceBinding, PreferenceTitleProvider, PreferenceAvailabilityProvider {

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.a11y_service_detail_screen_top_intro_purpose

    override val indexable
        get() = false

    override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)

    override fun getTitle(context: Context): CharSequence? =
        serviceInfo.loadIntro(context.packageManager)

    override val availabilityDescription = UI_ONLY_PREFERENCE

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context): Boolean {
        return !context.isInSetupWizard() && !TextUtils.isEmpty(getTitle(context))
    }

    override fun createWidget(context: Context) = TopIntroPreference(context)

    companion object {
        internal const val KEY = "top_intro"
    }
}
