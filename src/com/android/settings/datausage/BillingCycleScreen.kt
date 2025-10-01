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
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.preference.PreferenceBindingPlaceholder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

// LINT.IfChange
/** Preference screen for setting the billing cycle, data warning and limit. */
@ProvidePreferenceScreen(BillingCycleScreen.KEY, parameterized = true)
open class BillingCycleScreen(override val arguments: Bundle) :
    PreferenceScreenMixin,
    PreferenceAvailabilityProvider,
    PreferenceBinding,
    PreferenceBindingPlaceholder {

    private val subId =
        arguments.getSubId(Settings.EXTRA_SUB_ID, SubscriptionManager.INVALID_SUBSCRIPTION_ID)

    override val key: String
        get() = KEY

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
        makeLaunchIntent(context, BillingCycleActivity::class.java, metadata?.key)

    override fun isEnabled(context: Context) = DataUsageUtils.hasMobileData(context)

    override fun isAvailable(context: Context) =
        context.subscriptionManager?.isActiveSubscriptionId(subId) ?: false

    companion object {
        const val KEY = "billing_preference_catalyst"

        @JvmStatic
        fun parameters(context: Context): Flow<Bundle> {
            return MobileNetworkScreen.parameters(context)
        }
    }
}
// LINT.ThenChange(BillingCycleSettings.java, BillingCyclePreferenceController.java)
