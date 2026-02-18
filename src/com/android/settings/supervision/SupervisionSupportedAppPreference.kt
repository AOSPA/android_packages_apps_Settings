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
package com.android.settings.supervision

import android.app.supervision.flags.Flags
import android.content.Context
import android.content.pm.PackageManager.MATCH_UNINSTALLED_PACKAGES
import android.util.Log
import com.android.settings.CatalystFragment
import com.android.settings.R
import com.android.settings.widget.NonClickablePreference
import com.android.settingslib.HelpUtils
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.supervision.SupervisionLog.TAG

class SupervisionSupportedAppPreference(
    private val titleString: CharSequence?,
    private val summaryString: CharSequence?,
    private val packageName: String,
    private val preferenceKey: String? = null,
    private val learnMoreLink: String? = null,
) : PreferenceMetadata, PreferenceBinding, PreferenceLifecycleProvider {
    private var lifecycleContext: PreferenceLifecycleContext? = null

    override val key: String
        get() = if (Flags.enableSupervisionSettingsUiUpdates()) preferenceKey ?: KEY else KEY

    override val purpose: Int
        get() = R.string.web_content_filters_supported_app_purpose

    override val indexable
        get() = false

    override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)

    override fun onCreate(context: PreferenceLifecycleContext) {
        if (Flags.enableSupportedAppsRetrievalUpdates()) {
            this.lifecycleContext = context
            attachFooter(learnMoreLink)
        }
    }

    override fun createWidget(context: Context) =
        NonClickablePreference(context, /* attrs= */ null).apply {
            if (!Flags.enableSupportedAppsRetrievalUpdates()) {
                val packageManager = context.packageManager

                val applicationInfo =
                    packageManager.getApplicationInfo(packageName, MATCH_UNINSTALLED_PACKAGES)
                val icon = packageManager.getApplicationIcon(applicationInfo)

                title = titleString
                summary = summaryString
                if (Flags.enableSupervisionSettingsUiUpdates()) {
                    key = preferenceKey ?: KEY
                }
                setIcon(icon)
            }
        }

    override fun bind(preference: androidx.preference.Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        if (Flags.enableSupportedAppsRetrievalUpdates() && (preference is NonClickablePreference)) {
            val packageManager = preference.context.packageManager

            val applicationInfo =
                packageManager.getApplicationInfo(packageName, MATCH_UNINSTALLED_PACKAGES)
            val icon = packageManager.getApplicationIcon(applicationInfo)

            preference.title = titleString
            preference.summary = summaryString
            preference.key = preferenceKey ?: KEY
            preference.icon = icon
        }
    }

    private fun attachFooter(learnMoreLink: String?) {
        val fragmentInstance = lifecycleContext?.lifecycleOwner
        if (learnMoreLink != null && fragmentInstance is CatalystFragment) {
            fragmentInstance.attachFooter(
                key,
                lifecycleContext!!.getString(
                    R.string.supervision_web_content_filters_learn_more_title
                ),
            ) {
                val intent =
                    HelpUtils.getHelpIntent(
                        lifecycleContext,
                        learnMoreLink,
                        lifecycleContext!!::class.java.name,
                    )
                if (intent != null) {
                    lifecycleContext?.startActivity(intent)
                } else {
                    Log.w(TAG, "HelpIntent is null")
                }
            }
        }
    }

    companion object {
        const val KEY = "web_content_filters_supported_app"
    }
}
