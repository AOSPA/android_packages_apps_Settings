/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.datausage

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.telephony.SubscriptionManager
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.BillingCycleActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.network.telephony.MobileNetworkScreen
import com.android.settings.network.telephony.subscriptionManager
import com.android.settings.utils.getSubId
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.catalyst.flags.Flags as CatalystFlags
import com.android.settingslib.metadata.KeyParameters
import com.android.settingslib.metadata.ParameterizedPreferenceScreenArgumentsFactory
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.preference.PreferenceBindingPlaceholder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// LINT.IfChange
/** Preference screen for setting the billing cycle, data warning and limit. */
@ProvidePreferenceScreen(BillingCycleScreen.KEY, parameterized = true)
open class BillingCycleScreen
private constructor(
    @Deprecated(
        "This property will be removed once the catalyst framework stops passing the arguments as a bundle. Use the keyParameters instead."
    )
    final override val arguments: Bundle?,
    final override val keyParameters: KeyParameters?,
) :
    PreferenceScreenMixin,
    PreferenceAvailabilityProvider,
    PreferenceBinding,
    PreferenceBindingPlaceholder {

    private val subId: Int =
        if (CatalystFlags.catalystUseKeyParameters()) {
            keyParameters!![Settings.EXTRA_SUB_ID]?.toInt()
                ?: SubscriptionManager.INVALID_SUBSCRIPTION_ID
        } else {
            arguments!!.getSubId(Settings.EXTRA_SUB_ID, SubscriptionManager.INVALID_SUBSCRIPTION_ID)
        }

    @Deprecated(
        "This constructor will be removed once the catalyst framework stops passing the arguments as a bundle. Use the other constructor instead."
    )
    constructor(args: Bundle) : this(args, null)

    constructor(keyParameters: KeyParameters) : this(null, keyParameters)

    override val key: String
        get() = KEY

    //TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.billing_preference_catalyst_purpose

    override val title: Int
        get() = R.string.billing_cycle

    override val highlightMenuKey: Int
        get() = R.string.menu_key_network

    override fun getMetricsCategory() = SettingsEnums.BILLING_CYCLE

    override fun isFlagEnabled(context: Context) = Flags.deeplinkNetworkAndInternet25q4()

    override fun fragmentClass(): Class<out Fragment> = BillingCycleSettings::class.java

    override fun hasCompleteHierarchy() = false

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {}

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?): Intent? =
        if (CatalystFlags.catalystUseKeyParameters()) {
            makeLaunchIntent(
                context,
                BillingCycleActivity::class.java,
                keyParameters!!,
                metadata?.key,
            )
        } else {
            makeLaunchIntent(context, BillingCycleActivity::class.java, arguments!!, metadata?.key)
        }

    override fun isEnabled(context: Context) = DataUsageUtils.hasMobileData(context)

    override fun isAvailable(context: Context) =
        context.subscriptionManager?.isActiveSubscriptionId(subId) ?: false

    companion object : ParameterizedPreferenceScreenArgumentsFactory {
        const val KEY = "billing_preference_catalyst"

        @JvmStatic override val parametersSchema = MobileNetworkScreen.parametersSchema

        @JvmStatic
        override fun keyParameters(context: Context): Flow<KeyParameters> {
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
            return MobileNetworkScreen.parameters(context)
        }
    }
}
// LINT.ThenChange(BillingCycleSettings.java, BillingCyclePreferenceController.java)
