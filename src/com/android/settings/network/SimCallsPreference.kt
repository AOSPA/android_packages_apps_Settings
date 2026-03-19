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

package com.android.settings.network

import android.annotation.SuppressLint
import android.content.Context
import android.telephony.SubscriptionManager
import com.android.settings.R
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.metadata.MUSTPASS_SET
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.preference.PreferenceBindingPlaceholder

/** Show primary call's preference in dual active SIMs. */
@SuppressLint("MissingPermission")
class SimCallsPreference() :
    PersistentPreference<String>,
    PreferenceMetadata,
    PreferenceBinding,
    PreferenceBindingPlaceholder,
    PreferenceSummaryProvider,
    PreferenceAvailabilityProvider {

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.sim_calls_preference_key_purpose

    override val title: Int
        get() = R.string.primary_sim_calls_title

    override val icon: Int
        get() = R.drawable.ic_phone

    override val availabilityDescription =
        "The device must have more than one active subscription available."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override fun isAvailable(context: Context): Boolean {
        return context
            .getSystemService(SubscriptionManager::class.java)
            .activeSubscriptionIdList
            .size > 1
    }

    override fun tags(context: Context) = arrayOf(MUSTPASS_SET)

    override val supportsWrite = false

    override val valueType = String::class.javaObjectType

    override fun storage(context: Context): KeyValueStore = createSummaryStorage(context, key)

    override fun getSummary(context: Context): CharSequence? {
        val subInfo =
            context
                .getSystemService(SubscriptionManager::class.java)
                ?.getActiveSubscriptionInfo(SubscriptionManager.getDefaultVoiceSubscriptionId())
        if (subInfo == null) {
            return ""
        }
        return subInfo.displayName
    }

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    companion object {
        const val KEY = "sim_calls_preference_key"
        const val TAG = "SimCallsPreference"
    }
}
