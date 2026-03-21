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

import android.annotation.SuppressLint
import android.content.Context
import android.telephony.SubscriptionManager
import android.util.Log
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.flags.Flags
import com.android.settings.network.SubscriptionUtil
import com.android.settings.wifi.utils.isAdminUser
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.SensitivityLevel

// LINT.IfChange
@SuppressLint("MissingPermission")
class MobileNetworkSpnPreference(private val context: Context, private val subId: Int) :
    PersistentPreference<String>, PreferenceMetadata, PreferenceSummaryProvider, PreferenceAvailabilityProvider {

    override val availabilityDescription = "The user must be an admin user, the device must be mobile data capable or voice capable, and the subscription id must be valid."

    private val isAvailable =
        context.isAdminUser == true &&
            (Utils.isMobileDataCapable(context) || Utils.isVoiceCapable(context)) &&
            (Flags.isDualSimOnboardingEnabled() && SubscriptionManager.isValidSubscriptionId(subId))
    private var carrierName = getCarrierName()

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.mobile_network_spn_purpose

    override val title: Int
        get() = R.string.mobile_network_spn_title

    override val supportsWrite = false

    override val valueType = String::class.javaObjectType

    override fun storage(context: Context): KeyValueStore = createSummaryStorage(context, key)

    override fun getSummary(context: Context): CharSequence? = carrierName

    override fun isAvailable(context: Context) = isAvailable

    private fun getCarrierName(): CharSequence {
        return SubscriptionUtil.getActiveSubscriptions(context.subscriptionManager)
            .firstOrNull { it.subscriptionId == subId }
            ?.run {
                Log.d(TAG, "getCarrierName(), subId=$subId, carrierName=$carrierName")
                carrierName
            } ?: "".also { Log.w(TAG, "getCarrierName(), subId=$subId, subscription is empty") }
    }

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    companion object {
        private const val TAG = "MobileNetworkSpnPreference"
        const val KEY = "mobile_network_spn"
    }
}
// LINT.ThenChange(MobileNetworkSpnPreferenceController.java)
