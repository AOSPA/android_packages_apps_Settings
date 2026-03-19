/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.settings.wifi.calling

import android.app.settings.SettingsEnums
import android.content.Context
import android.os.Bundle
import android.telephony.SubscriptionManager.getDefaultSubscriptionId
import android.telephony.SubscriptionManager.isValidSubscriptionId
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.network.SubscriptionUtil
import com.android.settings.network.telephony.wificalling.WifiCallingRepository
import com.android.settings.utils.getSubId
import com.android.settings.utils.putSubId
import com.android.settings.wifi.calling.WifiCallingSettingsForSub.EXTRA_SUB_ID
import com.android.settingslib.metadata.CatalystFlagProviderFactory
import com.android.settingslib.metadata.KeyParametersSchema
import com.android.settingslib.metadata.ParameterizedPreferenceScreenArgumentsFactory
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.ValidatedKeyParameters
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.metadata.preferencesapi.types.SubscriptionId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_MOBILE_DATA

@ProvidePreferenceScreen(WifiCallingScreen.KEY, parameterized = true, parameterizedMigration = true)
open class WifiCallingScreen
private constructor(
    @Deprecated(
        "This property will be removed once the catalyst framework stops passing the arguments as a bundle. Use the keyParameters instead."
    )
    final override val arguments: Bundle?,
    final override val keyParameters: ValidatedKeyParameters?,
) : PreferenceScreenMixin, PreferenceAvailabilityProvider {
    override fun tags(context: Context) = arrayOf(APP_FUNCTION_MOBILE_DATA)


    private val subId: Int =
        if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
            keyParameters!![EXTRA_SUB_ID]?.toIntOrNull() ?: getDefaultSubscriptionId()
        } else {
            arguments!!.getSubId(EXTRA_SUB_ID, getDefaultSubscriptionId())
        }

    @Deprecated(
        "This constructor will be removed once the catalyst framework stops passing the arguments as a bundle. Use the other constructor instead."
    )
    constructor(args: Bundle) : this(args, null)

    constructor(keyParameters: ValidatedKeyParameters) : this(null, keyParameters)

    override val key: String
        get() = KEY

    override val keyParametersSchema: KeyParametersSchema
        get() = parametersSchema

    // TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.wifi_calling_purpose

    override val title: Int
        get() = R.string.wifi_calling_settings_title

    override val summary: Int
        get() = R.string.wifi_calling_summary

    override val highlightMenuKey: Int
        get() = R.string.menu_key_network

    override fun getMetricsCategory() = SettingsEnums.WIFI_CALLING_FOR_SUB

    override val availabilityDescription = "The subscription ID must be valid."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override fun isAvailable(context: Context) = isValidSubscriptionId(subId)

    override fun isFlagEnabled(context: Context) = Flags.catalystWifiCalling()

    override fun fragmentClass(): Class<out Fragment>? = WifiCallingSettingsForSub::class.java

    override fun hasCompleteHierarchy() = false

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) { +WifiCallingMainSwitchPreference(subId) }

    companion object : ParameterizedPreferenceScreenArgumentsFactory {
        const val KEY = "wifi_calling"

        @JvmStatic
        override val parametersSchema = KeyParametersSchema {
            parameter(EXTRA_SUB_ID, "The subscription ID. If it's not provided, the default system subscription will be used.", required=false, type=SubscriptionId(includeInactive = false))
        }

        @JvmStatic
        override fun keyParameters(context: Context): Flow<ValidatedKeyParameters> {
            // TODO (b/457649430): when the catalyst framework stops passing the arguments as a
            // bundle: replace the parameters(context) call to the actual implementation,
            // or make this function the primary implementation and the legacy parameters() should
            // call this one.
            return parameters(context).map { bundle ->
                if (bundle.isEmpty) {
                    parametersSchema.prepareEmpty()
                } else {
                    parametersSchema.prepare(bundle)
                }
            }
        }

        @Deprecated(
            "This method will be removed once the catalyst framework stops passing the arguments as a bundle. Use keyParameters instead."
        )
        @JvmStatic
        fun parameters(context: Context): Flow<Bundle> {
            fun Int.toArguments() = Bundle(1).also { it.putSubId(EXTRA_SUB_ID, this) }
            // handle backward compatibility with default subscription id
            val defaultSubId = getDefaultSubscriptionId()
            val flow =
                SubscriptionUtil.getSelectableSubscriptionInfoList(context)
                    .asFlow()
                    .filter {
                        val subId = it.subscriptionId
                        subId != defaultSubId &&
                            WifiCallingRepository(context, subId).wifiCallingReadyFlow().first()
                    }
                    .map { it.subscriptionId.toArguments() }
            // Bundle.EMPTY is for backward compatibility
            return when {
                isValidSubscriptionId(defaultSubId) -> merge(flowOf(Bundle.EMPTY), flow)
                else -> flow
            }
        }
    }
}
