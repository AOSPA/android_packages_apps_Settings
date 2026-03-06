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

package com.android.settings.deviceinfo.imei

import android.content.Context
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.util.Log
import android.util.Pair;
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.deviceinfo.PhoneNumberUtil
import com.android.settings.network.telephony.TelephonyUtils
import com.android.settings.wifi.utils.activeModemCount
import com.android.settings.wifi.utils.isAdminUser
import com.android.settings.wifi.utils.telephonyManager
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.preference.PreferenceBindingPlaceholder
import com.android.telephony.Rlog

import com.qti.extphone.QtiImeiInfo;

/** IMEI data class to store IMEI and slot ID. */
data class ImeiData(val imei: String, val slotId: Int)

/** Preference to show IMEI information for single and multi modem devices. */
class ImeiPreference(
    context: Context,
    private val index: Int,
    private val activeModemCount: Int,
    private val imeiList: List<ImeiData> = listOf(),
) :
    PreferenceMetadata,
    PreferenceBinding,
    PreferenceBindingPlaceholder,
    PreferenceLifecycleProvider,
    PreferenceTitleProvider,
    PreferenceSummaryProvider,
    PreferenceAvailabilityProvider {

    private val formattedTitle: String = context.getFormattedTitle()

    override val key: String
        get() = KEY_PREFIX + "${index + 1}"

    override val purpose: Int
        get() = R.string.imei_info_purpose

    init {
        Log.d(TAG, "init index = " + index)
        TelephonyUtils.connectExtTelephonyService(context)
    }
    override fun isAvailable(context: Context): Boolean =
        context.isAdminUser == true &&
            (Utils.isMobileDataCapable(context) || Utils.isVoiceCapable(context))

    override fun getTitle(context: Context): CharSequence? = formattedTitle

    override fun getSummary(context: Context): CharSequence? = getFormattedSummary()

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.isCopyingEnabled = true
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        context.requirePreference<Preference>(key).onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                val slotId = if (index < imeiList.size) imeiList[index].slotId else index
                ImeiInfoDialogFragment.show(context.childFragmentManager, slotId, formattedTitle)
                return@OnPreferenceClickListener true
            }
    }

    private fun Context.getFormattedTitle(): String {
        val slotCount = TelephonyUtils.getSlotsCount(this)
        if (slotCount <= 1) {
            return getString(R.string.status_imei)
        }
        return getString(R.string.imei_multi_sim, index + 1)
    }

    private fun getFormattedSummary(): CharSequence {
        return when {
            imeiList.isEmpty() || index >= imeiList.size -> String()
            else -> {
                PhoneNumberUtil.expandByTts(imeiList[index].imei)
            }
        }
    }

    companion object {
        const val TAG = "ImeiPreference"
        const val KEY_PREFIX = "imei_info"
    }
}

/**
 * As per GSMA specification TS37, below Primary IMEI requirements are mandatory to support
 * TS37_2.2_REQ_5 TS37_2.2_REQ_8 (Attached the document has description about this test cases)
 *
 * b/434700998, using the lower IMEI as the primary IMEI. IMEI 1 = primary IMEI i.e. lower IMEI IMEI
 * 2 = non-primary IMEI
 */
val Context.getImeiList: List<ImeiData>
    get() = buildList {
        telephonyManager?.let {
            var primaryImei = String()
            try {
                primaryImei = getPrimaryImei()
            } catch (exception: Exception) {
                Log.e(ImeiPreference.TAG, "PrimaryImei not available.", exception)
            }
            var imeiListFromSlot: MutableList<ImeiData> = mutableListOf()
            val slotCount = TelephonyUtils.getSlotsCount(this@getImeiList)
            for (slotIndex in 0..slotCount - 1) {
                try {
                    val slotImei = getImeiForSlot(slotIndex)
                    imeiListFromSlot.add(ImeiData(slotImei ?: String(), slotIndex))
                } catch (exception: Exception) {
                    Log.e(ImeiPreference.TAG, "Slot[$slotIndex] imei not available.", exception)
                }
            }

            imeiListFromSlot.sortBy { it.imei }
            if (primaryImei.isNotEmpty() && imeiListFromSlot.size >= 2) {
                val primaryImeiData = imeiListFromSlot.find { it.imei == primaryImei }
                if (primaryImeiData != null) {
                    imeiListFromSlot.remove(primaryImeiData)
                    add(primaryImeiData)
                }
            }
            addAll(imeiListFromSlot)
        }
    }

private val Context.isMinHalVersion2_1: Boolean
    private get() {
        val radioVersion: Pair<Int, Int> = telephonyManager?.getHalVersion(
                TelephonyManager.HAL_SERVICE_MODEM)?: Pair(0, 0)
        val halVersion = makeRadioVersion(radioVersion.first, radioVersion.second)
        return halVersion > makeRadioVersion(2, 0)
    }

private fun Context.getImeiForSlot(slot: Int): String {
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
        Log.e(ImeiPreference.TAG, "Imei not available. " + exception)
    }
    return imei
}

fun Context.getPrimaryImei(): String {
    var primaryImei = String()
    var qtiImeiInfo: Array<QtiImeiInfo?>? = null
    try {
        if (isMinHalVersion2_1 && !TelephonyUtils.isDsdsToSsConfigValid(this)) {
            primaryImei = telephonyManager?.primaryImei ?: String()
        } else {
            qtiImeiInfo = TelephonyUtils.getImeiInfo()
            if (qtiImeiInfo != null) {
                for (i in qtiImeiInfo.indices) {
                    if (qtiImeiInfo[i] != null
                            && qtiImeiInfo[i]!!.getImeiType() == QtiImeiInfo.IMEI_TYPE_PRIMARY) {
                        primaryImei = qtiImeiInfo[i]!!.getImei()
                        break
                    }
                }
            }
        }
    } catch (exception: Exception) {
        Log.e(ImeiPreference.TAG, "PrimaryImei not available. " + exception)
    }
    return primaryImei
}

private fun makeRadioVersion(major: Int, minor: Int): Int {
    if (major < 0 || minor < 0) return 0
    return major * 100 + minor
}