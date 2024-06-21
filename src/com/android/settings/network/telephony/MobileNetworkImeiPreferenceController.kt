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

/*
 * Changes from Qualcomm Innovation Center are provided under the following license:
 *
 * Copyright (c) 2024 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.settings.network.telephony

import android.content.Context
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.text.TextUtils;
import android.util.Log
import android.util.Pair;
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.deviceinfo.imei.ImeiInfoDialogFragment
import com.android.settings.flags.Flags
import com.android.settings.network.SubscriptionInfoListViewModel
import com.android.settings.network.SubscriptionUtil
import com.android.settingslib.Utils
import com.android.settingslib.spa.framework.util.collectLatestWithLifecycle
import com.android.settingslib.spaprivileged.framework.common.userManager
import com.qti.extphone.QtiImeiInfo;
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * Preference controller for "IMEI"
 */
class MobileNetworkImeiPreferenceController(context: Context, key: String) :
    TelephonyBasePreferenceController(context, key) {

    private lateinit var lazyViewModel: Lazy<SubscriptionInfoListViewModel>
    private lateinit var preference: Preference
    private lateinit var fragment: Fragment
    private lateinit var mTelephonyManager: TelephonyManager
    private var simSlot = -1
    private var imei = String()
    private var title = String()
    private var qtiImeiInfo: Array<QtiImeiInfo?>? = null

    private val isMinHalVersion2_1: Boolean
        private get() {
            val radioVersion: Pair<Int, Int> = mTelephonyManager.getHalVersion(
                    TelephonyManager.HAL_SERVICE_MODEM)
            val halVersion = makeRadioVersion(radioVersion.first, radioVersion.second)
            return halVersion > makeRadioVersion(2, 0)
        }

    fun init(fragment: Fragment, subId: Int) {
        this.fragment = fragment
        lazyViewModel = fragment.viewModels()
        mSubId = subId
        mTelephonyManager = mContext.getSystemService(TelephonyManager::class.java)
            ?.createForSubscriptionId(mSubId)!!
        simSlot = mTelephonyManager.slotIndex
        TelephonyUtils.connectExtTelephonyService(mContext)
    }

    override fun getAvailabilityStatus(subId: Int): Int = when {
        !Flags.isDualSimOnboardingEnabled() -> CONDITIONALLY_UNAVAILABLE
        SubscriptionManager.isValidSubscriptionId(subId)
                && SubscriptionUtil.isSimHardwareVisible(mContext)
                && mContext.userManager.isAdminUser
                && !Utils.isWifiOnly(mContext) -> AVAILABLE
        else -> CONDITIONALLY_UNAVAILABLE
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)!!
    }

    override fun onViewCreated(viewLifecycleOwner: LifecycleOwner) {
        if (!this::lazyViewModel.isInitialized) {
            Log.e(
                this.javaClass.simpleName,
                "lateinit property lazyViewModel has not been initialized"
            )
            return
        }
        val viewModel by lazyViewModel
        val coroutineScope = viewLifecycleOwner.lifecycleScope

        viewModel.subscriptionInfoListFlow
                .collectLatestWithLifecycle(viewLifecycleOwner) { subscriptionInfoList ->
                    subscriptionInfoList
                            .firstOrNull { subInfo -> subInfo.subscriptionId == mSubId }
                            ?.let {
                                coroutineScope.launch {
                                    refreshData(it)
                                }
                            }
                }
    }

    @VisibleForTesting
    suspend fun refreshData(subscription:SubscriptionInfo){
        withContext(Dispatchers.Default) {
            title = getTitle()
            imei = getImei()
            simSlot = subscription.simSlotIndex
        }
        refreshUi()
    }

    private fun refreshUi(){
        preference.title = title
        preference.summary = imei
    }

    override fun handlePreferenceTreeClick(preference: Preference): Boolean {
        if (preference.key != preferenceKey) return false

        Log.d(TAG, "handlePreferenceTreeClick:")
        ImeiInfoDialogFragment.show(fragment, simSlot, preference.title.toString())
        return true
    }

    private fun getImei(): String {
        val phoneType = getPhoneType()
        var imei = String()

        if (phoneType == TelephonyManager.PHONE_TYPE_CDMA) {
            imei = mTelephonyManager.meid?: String()
        } else {
            try {
                if (isMinHalVersion2_1) {
                    imei = mTelephonyManager.imei
                } else {
                    if (qtiImeiInfo == null) {
                        qtiImeiInfo = TelephonyUtils.getImeiInfo()
                    }
                    if (qtiImeiInfo != null) {
                        for (i in qtiImeiInfo!!.indices) {
                            if (qtiImeiInfo!![i] != null
                                    && qtiImeiInfo!![i]!!.slotId == simSlot) {
                                imei = qtiImeiInfo!![i]!!.imei
                                break
                            }
                        }
                    }
                    if (TextUtils.isEmpty(imei)) {
                        imei = mTelephonyManager.imei
                    }
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Imei not available. $exception")
            }
        }
        return imei
    }

    private fun getTitleForGsmPhone(): String {
        return mContext.getString(
            if (isPrimaryImei()) R.string.imei_primary else R.string.status_imei)
    }

    private fun getTitleForCdmaPhone(): String {
        return mContext.getString(
            if (isPrimaryImei()) R.string.meid_primary else R.string.status_meid_number)
    }

    private fun getTitle(): String {
        val phoneType = getPhoneType()
        return if (phoneType == TelephonyManager.PHONE_TYPE_CDMA) getTitleForCdmaPhone()
                else getTitleForGsmPhone()
    }

    /**
     * As per GSMA specification TS37, below Primary IMEI requirements are mandatory to support
     *
     * TS37_2.2_REQ_5
     * TS37_2.2_REQ_8 (Attached the document has description about this test cases)
     */
    protected fun isPrimaryImei(): Boolean {
        val imei = getImei()
        var primaryImei = String()

        try {
            if (isMinHalVersion2_1) {
                primaryImei = mTelephonyManager.primaryImei
                return primaryImei != null && primaryImei == imei && isMultiSim()
            } else {
                if (qtiImeiInfo == null) {
                    qtiImeiInfo = TelephonyUtils.getImeiInfo()
                }
                if (qtiImeiInfo != null) {
                    for (i in qtiImeiInfo!!.indices) {
                        if (qtiImeiInfo!![i] != null
                                && qtiImeiInfo!![i]!!.slotId == simSlot
                                && qtiImeiInfo!![i]!!.imeiType == QtiImeiInfo.IMEI_TYPE_PRIMARY) {
                            return true
                        }
                    }
                }
            }
        } catch (exception: Exception) {
            Log.e(TAG, "PrimaryImei not available. $exception")
        }
        return false
    }

    private fun isMultiSim(): Boolean {
        return mTelephonyManager.activeModemCount > 1
    }

    private fun makeRadioVersion(major: Int, minor: Int): Int {
        return if (major < 0 || minor < 0) 0 else major * 100 + minor
    }

    fun getPhoneType(): Int {
        return mTelephonyManager.currentPhoneType
    }

    companion object {
        private const val TAG = "MobileNetworkImeiPreferenceController"
    }
}
