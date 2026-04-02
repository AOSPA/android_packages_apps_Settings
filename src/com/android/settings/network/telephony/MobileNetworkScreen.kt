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

package com.android.settings.network.telephony

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.media.audio.Flags as AudioFlags
import android.os.Bundle
import android.os.UserManager
import android.provider.Settings
import android.telephony.SubscriptionManager
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.MobileNetworkActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.datausage.BillingCycleScreen
import com.android.settings.datausage.DataUsageListScreen
import com.android.settings.network.SubscriptionUtil
import com.android.settings.network.apn.ApnSettings
import com.android.settings.network.apn.ApnSettingsScreen
import com.android.settings.restriction.PreferenceRestrictionMixin
import com.android.settings.utils.getSubId
import com.android.settings.utils.makeLaunchIntent
import com.android.settings.utils.putSubId
import com.android.settingslib.metadata.CatalystFlagProviderFactory
import com.android.settingslib.metadata.KeyParametersSchema
import com.android.settingslib.metadata.ParameterizedPreferenceScreenArgumentsFactory
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceIndexableProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.ValidatedKeyParameters
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_MOBILE_DATA
import com.android.settingslib.metadata.preferencesapi.types.SubscriptionId
import com.android.settingslib.widget.UntitledPreferenceCategoryMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map

/** Preference screen for Network & Internet > SIMs > [SIM] */
// LINT.IfChange
@ProvidePreferenceScreen(MobileNetworkScreen.KEY, parameterized = true)
open class MobileNetworkScreen
private constructor(
    @Deprecated(
        "This property will be removed once the catalyst framework stops passing the arguments as a bundle. Use the keyParameters instead."
    )
    final override val arguments: Bundle?,
    final override val keyParameters: ValidatedKeyParameters?,
) :
    PreferenceScreenMixin,
    PreferenceAvailabilityProvider,
    PreferenceTitleProvider,
    PreferenceIndexableProvider,
    PreferenceRestrictionMixin {
    override fun tags(context: Context) = arrayOf(APP_FUNCTION_MOBILE_DATA)

    private val subId: Int =
        if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
            keyParameters!![Settings.EXTRA_SUB_ID]?.toIntOrNull()
                ?: SubscriptionManager.INVALID_SUBSCRIPTION_ID
        } else {
            arguments!!.getSubId(Settings.EXTRA_SUB_ID, SubscriptionManager.INVALID_SUBSCRIPTION_ID)
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

    override val purpose: Int
        get() = R.string.mobile_network_pref_screen_purpose

    override val bindingKey
        get() = "$KEY-$subId"

    override fun getTitle(context: Context): CharSequence? {
        val info = SubscriptionUtil.getSubscriptionOrDefault(context, subId)
        return SubscriptionUtil.getUniqueSubscriptionDisplayName(info, context)
    }

    override val highlightMenuKey: Int
        get() = R.string.menu_key_network

    override fun getMetricsCategory() = SettingsEnums.MOBILE_NETWORK

    override val availabilityDescription = "The subscription id must be valid."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override fun isAvailable(context: Context): Boolean =
        SubscriptionManager.isValidSubscriptionId(subId)

    override fun fragmentClass(): Class<out Fragment>? = MobileNetworkSettings::class.java

    override fun hasCompleteHierarchy() = false

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +MobileNetworkMainSwitchPreference(context, subId, coroutineScope) order +0
            val data = MobileNetworkData(context, coroutineScope, subId)
            +EnabledStateUntitledCategory(subId) += {
                +MobileNetworkDataUsagePreference(context, coroutineScope, subId)
                +MobileNetworkSpnPreference(context, subId)
                +MobileNetworkPhoneNumberPreference(data)
                +EnabledNetworkModePreference(data)
                +MobileNetworkImeiPreference(data)
                if (AudioFlags.supportPerPhoneAccountRingtone()) {
                    +SimRingtonePreference(context, subId) order 110
                }
                if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
                    +(DataUsageListScreen.KEY withParameters keyParameters!!)
                } else {
                    +(DataUsageListScreen.KEY args arguments!!)
                }
                if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
                    +(BillingCycleScreen.KEY withParameters keyParameters!!) order 115
                } else {
                    +(BillingCycleScreen.KEY args arguments!!) order 115
                }
                +UntitledPreferenceCategoryMetadata(
                    "apn_and_protection_container",
                    R.string.mobile_network_apn_and_protection_purpose,
                ) +=
                    {
                        if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
                            val newParameters =
                                ApnSettingsScreen.parametersSchema.prepare(
                                    ApnSettings.SUB_ID to subId.toString()
                                )
                            +(ApnSettingsScreen.KEY withParameters newParameters)
                        } else {
                            val bundle = Bundle(1).also { it.putSubId(ApnSettings.SUB_ID, subId) }
                            +(ApnSettingsScreen.KEY args bundle)
                        }
                    }
            }
        }

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?): Intent? {
        val intent =
            if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
                makeLaunchIntent(
                    context,
                    MobileNetworkActivity::class.java,
                    keyParameters!!,
                    metadata?.bindingKey,
                )
            } else {
                makeLaunchIntent(
                    context,
                    MobileNetworkActivity::class.java,
                    arguments!!,
                    metadata?.bindingKey,
                )
            }
        intent.putExtra(Settings.EXTRA_SUB_ID, subId)
        return intent
    }

    override val restrictionKeys
        get() = arrayOf(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)

    override val useAdminDisabledSummary
        get() = true

    override fun isEnabled(context: Context) = super<PreferenceRestrictionMixin>.isEnabled(context)

    override fun getEnabledDescription() = "There must be no restrictions on configuring mobile networks imposed by device administrators."

    override fun getEnabledStability() = PreconditionStability.UNSTABLE

    override fun isIndexable(context: Context) =
        // Shoudln't be indexable on ARC (Android on Chrome OS) and this seems to be the way to
        // detect that. See https://stackoverflow.com/a/44868935 or isArc() implementation in
        // cts/common/device-side/util-axt/src/com/android/compatibility/common/util/FeatureUtil.java
        !(context.packageManager.hasSystemFeature("org.chromium.arc") ||
            context.packageManager.hasSystemFeature("org.chromium.arc.device_management"))

    companion object : ParameterizedPreferenceScreenArgumentsFactory {
        const val KEY = "mobile_network_pref_screen"

        @JvmStatic
        override val parametersSchema = KeyParametersSchema {
            parameter(Settings.EXTRA_SUB_ID, "The subscription ID", type = SubscriptionId)
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
            fun Int.toArguments() = Bundle(1).also { it.putSubId(Settings.EXTRA_SUB_ID, this) }
            return SubscriptionUtil.getSelectableSubscriptionInfoList(context).asFlow().map {
                it.subscriptionId.toArguments()
            }
        }
    }
}
// LINT.ThenChange(MobileNetworkSettings.java)
