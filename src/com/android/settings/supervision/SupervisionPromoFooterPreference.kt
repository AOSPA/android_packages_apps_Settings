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

import android.content.Context
import android.content.Intent
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.CardPreference
import kotlinx.coroutines.launch

/** A bottom banner promoting other supervision features offered by the supervision app. */
class SupervisionPromoFooterPreference(private val preferenceDataProvider: PreferenceDataProvider) :
    PreferenceMetadata,
    PreferenceBinding,
    PreferenceLifecycleProvider,
    PreferenceTitleProvider,
    PreferenceSummaryProvider {

    // Operation to be performed when the preference is clicked
    private var intent: Intent? = null

    // TODO(b/399497788): Remove this and get title from supervision app
    override fun getTitle(context: Context) = "Full parental controls"

    // TODO(b/399497788): Remove this and get summary from supervision app
    override fun getSummary(context: Context) =
        "Set up an account for your kid & help them manage it (required for kids under [AOC])"

    override fun intent(context: Context): Intent? = intent

    override val key: String
        get() = KEY

    override fun createWidget(context: Context) = CardPreference(context)

    override fun onResume(context: PreferenceLifecycleContext) {
        super.onResume(context)
        val preference = context.findPreference<CardPreference>(KEY)
        if (preference != null) {
            context.lifecycleScope.launch {
                // TODO(b/399497788) Get title & summary from supervision app.
                val preferenceData =
                    preferenceDataProvider.getPreferenceData(listOf(KEY)).await()[KEY]
                intent = intent ?: Intent()
                intent?.setAction(preferenceData?.action)
                intent?.setPackage(preferenceData?.targetPackage)
                // Hide the preference if the target package can not respond to the action
                if (!canTargetPackageRespondToAction(preference.context)) {
                    preference.isVisible = false
                }
            }
        }
    }

    private fun canTargetPackageRespondToAction(context: Context): Boolean {
        if (intent?.action == null || intent?.`package` == null) {
            return false
        }

        intent!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val activities =
            context.packageManager.queryIntentActivitiesAsUser(intent!!, 0, context.userId)
        return activities.isNotEmpty()
    }

    companion object {
        const val KEY = "supervision_promo_footer"
    }
}
