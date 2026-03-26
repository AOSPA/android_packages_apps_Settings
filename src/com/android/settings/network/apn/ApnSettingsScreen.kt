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

package com.android.settings.network.apn

import android.app.settings.SettingsEnums
import android.content.Context
import android.os.Bundle
import android.os.PersistableBundle
import android.os.UserManager
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.ApnSettingsActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.network.CarrierConfigCache
import com.android.settings.network.SubscriptionUtil
import com.android.settings.network.telephony.MobileNetworkUtils
import com.android.settings.restriction.PreferenceRestrictionMixin
import com.android.settings.utils.getSubId
import com.android.settings.utils.makeLaunchIntent
import com.android.settings.utils.putSubId
import com.android.settingslib.RestrictedPreference
import com.android.settingslib.metadata.CatalystFlagProviderFactory
import com.android.settingslib.metadata.KeyParametersSchema
import com.android.settingslib.metadata.ParameterizedPreferenceScreenArgumentsFactory
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.ValidatedKeyParameters
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.metadata.preferencesapi.types.AnyInt
import com.android.settingslib.metadata.preferencesapi.types.SubscriptionId
import com.android.settingslib.preference.PreferenceBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_MOBILE_DATA

// LINT.IfChange
@ProvidePreferenceScreen(ApnSettingsScreen.KEY, parameterized = true)
open class ApnSettingsScreen
private constructor(
    @Deprecated(
        "This property will be removed once the catalyst framework stops passing the arguments as a bundle. Use the keyParameters instead."
    )
    final override val arguments: Bundle?,
    final override val keyParameters: ValidatedKeyParameters?,
) :
    PreferenceScreenMixin,
    PreferenceRestrictionMixin,
    PreferenceAvailabilityProvider,
    PreferenceBinding {
    override fun tags(context: Context) = arrayOf(APP_FUNCTION_MOBILE_DATA)


    private val subId: Int =
        if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
            keyParameters!![ApnSettings.SUB_ID]?.toIntOrNull() ?: INVALID_SUBSCRIPTION_ID
        } else {
            arguments!!.getSubId(ApnSettings.SUB_ID, INVALID_SUBSCRIPTION_ID)
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
        get() = R.string.telephony_apn_key_purpose

    override val title: Int
        get() = R.string.mobile_network_apn_title

    override val screenTitle: Int
        get() = R.string.apn_settings

    override val keywords: Int
        get() = R.string.keywords_access_point_names

    override val restrictionKeys
        get() = arrayOf(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)

    // TODO (b/441290203) - migrate ApnPreferenceController.updateState to catalyst
    override fun isEnabled(context: Context): Boolean =
        super<PreferenceRestrictionMixin>.isEnabled(context)

    override fun getMetricsCategory() = SettingsEnums.APN

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass(): Class<out Fragment>? = ApnSettings::class.java

    override val highlightMenuKey: Int
        get() = R.string.menu_key_network

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {}

    override fun createWidget(context: Context) = RestrictedPreference(context)

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
            makeLaunchIntent(
                context,
                ApnSettingsActivity::class.java,
                keyParameters!!,
                metadata?.bindingKey,
            )
        } else {
            makeLaunchIntent(
                context,
                ApnSettingsActivity::class.java,
                arguments!!,
                metadata?.bindingKey,
            )
        }

    override val availabilityDescription =
        "The subscription must have a GSM APN."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override fun isAvailable(context: Context): Boolean {
        val carrierConfig: PersistableBundle? =
            CarrierConfigCache.getInstance(context).getConfigForSubId(subId)
        val isGsmApn =
            MobileNetworkUtils.isGsmOptions(context, subId) &&
                carrierConfig != null &&
                carrierConfig.getBoolean(CarrierConfigManager.KEY_APN_EXPAND_BOOL)
        val hideCarrierNetwork =
            carrierConfig == null ||
                carrierConfig.getBoolean(
                    CarrierConfigManager.KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL
                )

        return !hideCarrierNetwork && isGsmApn
    }

    companion object : ParameterizedPreferenceScreenArgumentsFactory {
        const val KEY = "telephony_apn_key"

        @JvmStatic
        override val parametersSchema = KeyParametersSchema {
            parameter(ApnSettings.SUB_ID, "The subscription ID", type = SubscriptionId)
        }

        @JvmStatic
        override fun keyParameters(context: Context): Flow<ValidatedKeyParameters> {
            // TODO (b/457649430): when the catalyst framework stops passing the arguments as a
            // bundle: replace the parameters(context) call to the actual implementation,
            // or make this function the primary implementation and the legacy parameters() should
            // call this one.
            return parameters(context).map { bundle -> parametersSchema.prepare(bundle) }
        }

        @Deprecated(
            "This method will be removed once the catalyst framework stops passing the arguments as a bundle. Use keyParameters instead."
        )
        @JvmStatic
        fun parameters(context: Context): Flow<Bundle> {
            fun Int.toArguments() = Bundle(1).also { it.putSubId(ApnSettings.SUB_ID, this) }
            return SubscriptionUtil.getSelectableSubscriptionInfoList(context).asFlow().map {
                it.subscriptionId.toArguments()
            }
        }
    }
}
// LINT.ThenChange(ApnSettings.java, ../telephony/ApnPreferenceController.java)
