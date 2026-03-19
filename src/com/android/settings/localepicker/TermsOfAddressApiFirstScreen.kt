/**
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.settings.localepicker

import android.app.GrammaticalInflectionManager
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.localepicker.LocaleUtils.isTermsOfAddressAvailable
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Custom
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.preferencesapi.safe
import com.android.settingslib.metadata.preferencesapi.types.GeneratedType
import com.android.settingslib.metadata.preferencesapi.types.EType
import com.android.settingslib.metadata.preferencesapi.types.GeneratedValue

// LINT.IfChange
@ProvidePreferenceScreen(TermsOfAddressApiFirstScreen.KEY)
class TermsOfAddressApiFirstScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.SYSTEM,
        fragment = TermsOfAddressFragment::class,
        purpose = R.string.terms_of_address_purpose,
    ) {

    init {
        flag { Flags.catalystMigration26q2() && Flags.termsOfAddressEnabled() }

        preconditions(R.string.terms_of_address_screen_preconditions) {
            if (isTermsOfAddressAvailable(context)) {
                Allowed
            } else {
                Custom(
                    R.string.terms_of_address_screen_unavailable,
                    stability = PreconditionStability.UNSTABLE,
                )
            }
        }

        preference(
            key = KEY_PREFERENCE,
            purpose = R.string.terms_of_address_preference_purpose,
            type =
                GeneratedType(
                    description = R.string.default_system_grammatical_gender_description
                ) {
                    val termOfAddress = context.resources.getStringArray(R.array.terms_of_address)
                    termOfAddress.map {
                        GeneratedValue(termOfAddress.indexOf(it).safe(), it.safe())
                    }
                },
        ) {
            get {
                execute {
                    val grammaticalInflectionManager =
                        context.getSystemService(GrammaticalInflectionManager::class.java)
                    grammaticalInflectionManager.systemGrammaticalGender
                }
            }
            set {
                execute { value ->
                    val grammaticalInflectionManager =
                        context.getSystemService(GrammaticalInflectionManager::class.java)
                    grammaticalInflectionManager.setSystemWideGrammaticalGender(value)
                }
            }
        }
    }

    companion object {
        const val KEY = "terms_of_address"
        const val KEY_PREFERENCE = "terms_of_address_preference"
    }
}
// LINT.ThenChange(TermsOfAddressFragment.java)
