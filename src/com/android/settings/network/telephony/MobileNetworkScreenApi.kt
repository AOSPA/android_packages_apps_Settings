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
import android.telephony.SubscriptionManager
import android.util.Log
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Disallowed
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean
import com.android.settingslib.metadata.preferencesapi.types.GeneratedParameterType
import com.android.settingslib.metadata.preferencesapi.types.GeneratedValue

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
        val telephonyRepository = featureFactory.telephonyFeatureProvider.telephonyRepository

        parameters {
            parameter(
                name = Settings.EXTRA_SUB_ID,
                purpose = R.string.mobile_network_pref_screen_parameter_sub_id_purpose,
                required = true,
                type =
                    GeneratedParameterType(
                        R.string.mobile_network_pref_screen_parameter_sub_id_description
                    ) {
                        subscriptionRepository.visibleActiveSubscriptionInfoList.map { info ->
                            GeneratedValue(
                                info.subscriptionId.toString(),
                                info.displayName.toString(),
                            )
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

        preference(
            key = KEY_AUTO_DATA_SWITCHING,
            purpose = R.string.mobile_network_auto_data_switching_purpose,
            type = AnyBoolean,
        ) {
            preconditions(R.string.auto_data_switching_preconditions) {
                val subId =
                    keyParameters?.get(Settings.EXTRA_SUB_ID)?.toInt()
                        ?: SubscriptionManager.INVALID_SUBSCRIPTION_ID
                if (!SubscriptionManager.isValidSubscriptionId(subId)) {
                    Disallowed("This is not a valid subscription id")
                } else if (subscriptionRepository.getDefaultDataSubscriptionId() == subId) {
                    Disallowed("This is the default data subscription")
                } else if (!telephonyRepository.isMobileDataCable()) {
                    Disallowed("Mobile data is not available")
                } else {
                    Allowed
                }
            }
            get {
                execute {
                    val subId = keyParameters?.get(Settings.EXTRA_SUB_ID)!!.toInt()
                    telephonyRepository.isMobileDataSwitchPolicyEnabled(subId)
                }
            }
            set {
                execute { value ->
                    val subId = keyParameters?.get(Settings.EXTRA_SUB_ID)!!.toInt()
                    telephonyRepository.setMobileDataSwitchPolicyEnabled(subId, value)
                }
            }
        }
    }

    companion object {
        private const val TAG = "MobileNetworkScreenApi"
        const val KEY = "api_mobile_network_pref_screen"
        const val KEY_AUTO_DATA_SWITCHING = "auto_data_switch"
    }
}
// LINT.ThenChange(MobileNetworkSettings.java,
//                 MobileNetworkScreen.kt)
