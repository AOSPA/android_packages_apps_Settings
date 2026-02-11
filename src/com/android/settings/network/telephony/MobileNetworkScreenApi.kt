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

package com.android.settings.network.telephony

import android.provider.Settings
import android.util.Log
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.types.GeneratedParameterType
import com.android.settingslib.metadata.preferencesapi.types.GeneratedValue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

// LINT.IfChange
@ProvidePreferenceScreen(MobileNetworkScreenApi.KEY, parameterized = true)
class MobileNetworkScreenApi :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.NETWORK,
        fragment = MobileNetworkSettings::class,
        // TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
        purpose = R.string.mobile_network_pref_screen_purpose,
        alreadyPartiallyMigrated = MobileNetworkScreen::class,
    ) {

    init {
        flag { Flags.catalystMigration26q2() }

        val subscriptionRepository = featureFactory.telephonyFeatureProvider.subscriptionRepository
        parameters {
            parameter(
                name = Settings.EXTRA_SUB_ID,
                purpose = R.string.mobile_network_pref_screen_parameter_sub_id_purpose,
                required = true,
                type =
                    GeneratedParameterType(
                        R.string.mobile_network_pref_screen_parameter_sub_id_description
                    ) {
                        runBlocking {
                            subscriptionRepository.activeSubscriptionListInfoFlow().first()?.map {
                                info ->
                                GeneratedValue(
                                    info.subscriptionId.toString(),
                                    info.displayName.toString(),
                                )
                            } ?: emptyList()
                        }
                    },
            )

            // Maps the API parameter to the Intent Extra for the Fragment
            prepareScreenExtras { parameters, extras ->
                val subIdString = parameters.values[Settings.EXTRA_SUB_ID]
                Log.d(TAG, "subIdString: $subIdString")
                if (subIdString != null) {
                    val subId = subIdString.toInt()
                    extras.putInt(Settings.EXTRA_SUB_ID, subId)
                }
            }
        }
    }

    companion object {
        private const val TAG = "MobileNetworkScreenApi"
        const val KEY = "api_mobile_network_pref_screen"
    }
}
// LINT.ThenChange(MobileNetworkSettings.java,
//                 MobileNetworkScreen.kt)
