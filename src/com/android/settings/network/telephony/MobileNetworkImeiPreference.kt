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

/*
 * Changes from Qualcomm Technologies, Inc. are provided under the following license:
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.settings.network.telephony

import android.annotation.SuppressLint
import android.content.Context
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.telephony.SubscriptionManager
import android.telephony.SubscriptionManager.INVALID_SIM_SLOT_INDEX
import android.util.Log
import android.util.Pair
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.deviceinfo.imei.ImeiInfoDialogFragment
import com.android.settings.flags.Flags
import com.android.settings.network.SubscriptionUtil
import com.android.settings.network.telephony.TelephonyUtils
import com.android.settings.wifi.utils.isAdminUser
import com.android.settings.wifi.utils.telephonyManager
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.preference.PreferenceBinding

import com.qti.extphone.QtiImeiInfo

// LINT.IfChange
@SuppressLint("MissingPermission")
class MobileNetworkImeiPreference(
    private val context: Context,
    private val subId: Int,
    private val imeiList: List<String> = listOf<String>(),
) :
    PreferenceMetadata,
    PreferenceBinding,
    PreferenceLifecycleProvider,
    PreferenceTitleProvider,
    PreferenceSummaryProvider,
    PreferenceAvailabilityProvider {

    private val isAvailable =
        context.isAdminUser == true &&
            (Utils.isMobileDataCapable(context) || Utils.isVoiceCapable(context)) &&
            (Flags.isDualSimOnboardingEnabled() && SubscriptionManager.isValidSubscriptionId(subId))
    private var imei: String? = if (isAvailable) context.getImei() else ""
    private var indexing: Int = imeiList.indexOf(imei)
    private val formattedTitle: String = getFormattedTitle()

    override val key: String
        get() = KEY

    private val Context.isMinHalVersion2_1: Boolean
        private get() {
            val radioVersion: Pair<Int, Int> = telephonyManager?.getHalVersion(
                    TelephonyManager.HAL_SERVICE_MODEM)?: Pair(0, 0)
            val halVersion = makeRadioVersion(radioVersion.first, radioVersion.second)
            return halVersion > makeRadioVersion(2, 0)
        }

    override fun getSummary(context: Context): CharSequence? = imei

    override fun isAvailable(context: Context) = isAvailable

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.isCopyingEnabled = true
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        context.requirePreference<Preference>(key).onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                getSlotIndex()
                    .takeIf { it != INVALID_SIM_SLOT_INDEX }
                    ?.run {
                        ImeiInfoDialogFragment.show(
                            context.childFragmentManager,
                            this,
                            formattedTitle,
                        )
                    }
                return@OnPreferenceClickListener true
            }
    }

    override fun getTitle(context: Context): CharSequence? = formattedTitle

    private fun getFormattedTitle(): String =
        if (indexing != -1 && imeiList.size >= 2) {
            context.getString(R.string.imei_multi_sim, indexing + 1)
        } else {
            context.getString(R.string.status_imei)
        }

    private fun getSlotIndex(): Int {
        val subscription =
            SubscriptionUtil.getActiveSubscriptions(context.subscriptionManager).firstOrNull {
                it.subscriptionId == subId
            }
        return if (subscription != null) {
            Log.d(TAG, "getSlotIndex(), simSlotIndex=${subscription.simSlotIndex}")
            subscription.simSlotIndex
        } else {
            Log.e(TAG, "getSlotIndex(), simSlotIndex=INVALID_SIM_SLOT_INDEX")
            INVALID_SIM_SLOT_INDEX
        }
    }

    private fun Context.getImei(): String {
        val slot = getSlotIndex()
        var imei = String()
        var qtiImeiInfo: Array<QtiImeiInfo?>? = null
        try {
            if (isMinHalVersion2_1 && !TelephonyUtils.isDsdsToSsConfigValid(this)) {
                imei = telephonyManager?.getImei(slot) ?: String()
            } else {
                qtiImeiInfo = TelephonyUtils.getImeiInfo()
                if (qtiImeiInfo != null) {
                    for (i in qtiImeiInfo.indices) {
                        if (qtiImeiInfo[i] != null && qtiImeiInfo[i]!!.getSlotId() == slot) {
                            imei = qtiImeiInfo[i]!!.getImei()
                            break
                        }
                    }
                }
                if (TextUtils.isEmpty(imei)) {
                    imei = telephonyManager?.getImei(slot) ?: String()
                }
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Imei not available. " + exception)
        }
        return imei
    }

    private fun makeRadioVersion(major: Int, minor: Int): Int {
        if (major < 0 || minor < 0) return 0
        return major * 100 + minor
    }

    companion object {
        private const val TAG = "MobileNetworkImeiPreference"
        const val KEY = "network_mode_imei_info"
    }
}
// LINT.ThenChange(MobileNetworkImeiPreferenceController.kt)
