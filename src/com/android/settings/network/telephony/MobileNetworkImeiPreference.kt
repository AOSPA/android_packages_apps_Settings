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
import android.telephony.SubscriptionManager.INVALID_SIM_SLOT_INDEX
import android.util.Log
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.deviceinfo.imei.ImeiInfoDialogFragment
import com.android.settings.network.SubscriptionUtil
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.preference.PreferenceBinding
import kotlinx.coroutines.launch

// LINT.IfChange
@SuppressLint("MissingPermission")
class MobileNetworkImeiPreference(private val data: MobileNetworkData) :
    PreferenceMetadata,
    PreferenceBinding,
    PreferenceLifecycleProvider,
    PreferenceTitleProvider,
    PreferenceSummaryProvider,
    PreferenceAvailabilityProvider {

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.network_mode_imei_info_purpose

    override fun getSummary(context: Context): CharSequence? = data.imeiInfoDataFlow.value.summary

    override val availabilityDescription =
        "The user must be an admin user, and the device must have mobile data or voice capability, and the subscription ID must be valid."

    override fun isAvailable(context: Context) = data.imeiInfoDataFlow.value.isAvailable

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.isCopyingEnabled = true
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        data.coroutineScope?.launch {
            data.imeiInfoDataFlow.collect {
                context.notifyPreferenceChange(KEY)
                Log.d(TAG, "imeiDataFlow collect")
            }
        }
        context.requirePreference<Preference>(key).onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                val title = getTitle(context) ?: ""
                getSlotIndex()
                    .takeIf { it != INVALID_SIM_SLOT_INDEX }
                    ?.run {
                        ImeiInfoDialogFragment.show(
                            context.childFragmentManager,
                            this,
                            title.toString(),
                        )
                    }
                return@OnPreferenceClickListener true
            }
    }

    override fun getTitle(context: Context): CharSequence? = data.imeiInfoDataFlow.value.title

    private fun getSlotIndex(): Int {
        val subscription =
            SubscriptionUtil.getActiveSubscriptions(data.context.subscriptionManager).firstOrNull {
                it.subscriptionId == data.subId
            }
        return if (subscription != null) {
            Log.d(TAG, "getSlotIndex(), simSlotIndex=${subscription.simSlotIndex}")
            subscription.simSlotIndex
        } else {
            Log.e(TAG, "getSlotIndex(), simSlotIndex=INVALID_SIM_SLOT_INDEX")
            INVALID_SIM_SLOT_INDEX
        }
    }

    companion object {
        private const val TAG = "MobileNetworkImeiPreference"
        const val KEY = "network_mode_imei_info"
    }
}
// LINT.ThenChange(MobileNetworkImeiPreferenceController.kt)
