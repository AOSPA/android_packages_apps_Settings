/*
 * Copyright (C) 2023 The Android Open Source Project
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

// QTI_BEGIN: 2024-03-14: Telephony: CAG and SNPN feature
/*
 * Changes from Qualcomm Innovation Center are provided under the following license:
 *
 * Copyright (c) 2024 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

// QTI_END: 2024-03-14: Telephony: CAG and SNPN feature
package com.android.settings.network.telephony.gsm

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.telephony.ServiceState
import android.telephony.TelephonyManager
// QTI_BEGIN: 2024-03-17: Telephony: UI requirement in CU domestic roaming
import android.util.Log
// QTI_END: 2024-03-17: Telephony: UI requirement in CU domestic roaming
import androidx.lifecycle.LifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.Settings.NetworkSelectActivity
// QTI_BEGIN: 2024-03-17: Telephony: UI requirement in CU domestic roaming
import com.android.settings.network.telephony.DomesticRoamUtils
// QTI_END: 2024-03-17: Telephony: UI requirement in CU domestic roaming
import com.android.settings.network.telephony.MobileNetworkUtils
import com.android.settings.network.telephony.TelephonyBasePreferenceController
import com.android.settings.network.telephony.allowedNetworkTypesFlow
import com.android.settings.network.telephony.serviceStateFlow
import com.android.settingslib.spa.framework.util.collectLatestWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

// QTI_BEGIN: 2024-03-14: Telephony: CAG and SNPN feature
import com.qti.extphone.ExtTelephonyManager

// QTI_END: 2024-03-14: Telephony: CAG and SNPN feature
/** Preference controller for "Open network select" */
class OpenNetworkSelectPagePreferenceController
@JvmOverloads
constructor(
    context: Context,
    key: String,
    private val allowedNetworkTypesFlowFactory: (subId: Int) -> Flow<Long> =
        context::allowedNetworkTypesFlow,
    private val serviceStateFlowFactory: (subId: Int) -> Flow<ServiceState> =
        context::serviceStateFlow,
) :
    TelephonyBasePreferenceController(context, key),
    AutoSelectPreferenceController.OnNetworkSelectModeListener {

    private var preference: Preference? = null
    private var networkSelectionMode: Int = TelephonyManager.NETWORK_SELECTION_MODE_AUTO
// QTI_BEGIN: 2024-03-14: Telephony: CAG and SNPN feature
    private val telephonyManager = context.getSystemService(TelephonyManager::class.java)!!
// QTI_END: 2024-03-14: Telephony: CAG and SNPN feature

    /** Initialization based on given subscription id. */
    fun init(subId: Int): OpenNetworkSelectPagePreferenceController {
        mSubId = subId
// QTI_BEGIN: 2024-03-14: Telephony: CAG and SNPN feature
        telephonyManager.createForSubscriptionId(mSubId)
// QTI_END: 2024-03-14: Telephony: CAG and SNPN feature
        return this
    }

    override fun updateState(preference: Preference?) {
        super.updateState(preference)
        preference?.isEnabled =
            !mIsAirplaneModeOn &&
                networkSelectionMode != TelephonyManager.NETWORK_SELECTION_MODE_AUTO
    }

    override fun getAvailabilityStatus(subId: Int) =
        if (MobileNetworkUtils.shouldDisplayNetworkSelectOptions(mContext, subId)) AVAILABLE
        else CONDITIONALLY_UNAVAILABLE

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)
        preference?.intent =
            Intent().apply {
                setClass(mContext, NetworkSelectActivity::class.java)
                putExtra(Settings.EXTRA_SUB_ID, mSubId)
            }
    }

    override fun onViewCreated(viewLifecycleOwner: LifecycleOwner) {
        allowedNetworkTypesFlowFactory(mSubId).collectLatestWithLifecycle(viewLifecycleOwner) {
            preference?.isVisible =
                withContext(Dispatchers.Default) {
                    MobileNetworkUtils.shouldDisplayNetworkSelectOptions(mContext, mSubId)
                }
        }

        serviceStateFlowFactory(mSubId).collectLatestWithLifecycle(viewLifecycleOwner) {
            serviceState ->
            preference?.summary =
                if (serviceState.state == ServiceState.STATE_IN_SERVICE ||
// QTI_BEGIN: 2024-03-14: Telephony: CAG and SNPN feature
                        isSnpnInService(serviceState)) {
// QTI_END: 2024-03-14: Telephony: CAG and SNPN feature
                    withContext(Dispatchers.Default) {
// QTI_BEGIN: 2024-03-17: Telephony: UI requirement in CU domestic roaming
                        if (DomesticRoamUtils.isFeatureEnabled(mContext)) {
                            val registeredOperatorName : String = DomesticRoamUtils
                                    .getRegisteredOperatorName(mContext, mSubId)
                            if (DomesticRoamUtils.EMPTY_OPERATOR_NAME != registeredOperatorName) {
                                registeredOperatorName
// QTI_END: 2024-03-17: Telephony: UI requirement in CU domestic roaming
// QTI_BEGIN: 2024-03-19: Telephony: UI requirement in CU domestic roaming
                            } else {
                                MobileNetworkUtils.getCurrentCarrierNameForDisplay(mContext, mSubId)
// QTI_END: 2024-03-19: Telephony: UI requirement in CU domestic roaming
// QTI_BEGIN: 2024-03-17: Telephony: UI requirement in CU domestic roaming
                            }
// QTI_END: 2024-03-17: Telephony: UI requirement in CU domestic roaming
// QTI_BEGIN: 2024-03-19: Telephony: UI requirement in CU domestic roaming
                        } else {
                            MobileNetworkUtils.getCurrentCarrierNameForDisplay(mContext, mSubId)
// QTI_END: 2024-03-19: Telephony: UI requirement in CU domestic roaming
// QTI_BEGIN: 2024-03-17: Telephony: UI requirement in CU domestic roaming
                        }
// QTI_END: 2024-03-17: Telephony: UI requirement in CU domestic roaming
                    }
                } else {
                    mContext.getString(R.string.network_disconnected)
                }
        }
    }

// QTI_BEGIN: 2024-03-14: Telephony: CAG and SNPN feature
    private fun isSnpnInService(ss: ServiceState): Boolean {
        return ((MobileNetworkUtils.getAccessMode(mContext, telephonyManager.getSlotIndex())
                == ExtTelephonyManager.ACCESS_MODE_SNPN)
                && (ss.getDataRegState() == ServiceState.STATE_IN_SERVICE))
    }

// QTI_END: 2024-03-14: Telephony: CAG and SNPN feature
    override fun onNetworkSelectModeUpdated(mode: Int) {
        this.networkSelectionMode = mode
        updateState(preference)
    }
}
