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
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.CardPreference
import kotlinx.coroutines.launch

/**
 * A bottom banner promoting other supervision features offered by the supervision app.
 */
class SupervisionPromoFooterPreference :
    PreferenceMetadata,
    PreferenceBinding,
    PreferenceLifecycleProvider,
    PreferenceTitleProvider,
    PreferenceSummaryProvider {

    //TODO(b/399497788): Remove this and get title from supervision app
    override fun getTitle(context: Context) = "Full parental controls"

    //TODO(b/399497788): Remove this and get summary from supervision app
    override fun getSummary(context: Context) =
        "Set up an account for your kid & help them manage it (required for kids under [AOC])"

    override val key: String
        get() = KEY

    override fun createWidget(context: Context) = CardPreference(context)

    override fun onResume(context: PreferenceLifecycleContext) {
        super.onResume(context)
        val preference = context.findPreference<CardPreference>(KEY)
        if (preference != null) {
            context.lifecycleScope.launch {
                // TODO(b/399497788) Get title & summary from supervision app.
            }
        }
    }

    companion object {
        const val KEY = "supervision_promo_footer"
    }
}
