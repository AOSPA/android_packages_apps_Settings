/**
 * Copyright (c) 2025 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.settings.network.telephony

import android.content.Context
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach

class RoamingPreferenceRepository(
    private val context: Context,
    private val subscriptionRepository: SubscriptionRepository = SubscriptionRepository(context),
    private val dataSubscriptionRepository: DataSubscriptionRepository =
            DataSubscriptionRepository(context),
    private val callStateRepository: CallStateRepository = CallStateRepository(context),
) {

   /**
    * When there is a nDDS voice call, it is disallowed to turn off data roaming of
    * DDS sub after temp DDS is happened.
    *
    * @return Flow<true> if option needs to get greyed out
    */
    fun isDisallowedFlow(subId: Int): Flow<Boolean> = combine(
        dataSubscriptionRepository.defaultDataSubscriptionIdFlow(),
        dataSubscriptionRepository.activeDataSubscriptionIdFlow(),
        callStateOnNddsSubFlow(),
    ){ defaultDataSubId, activeDataSubId, callState ->
        defaultDataSubId == subId && defaultDataSubId != activeDataSubId
                && callState != TelephonyManager.CALL_STATE_IDLE
    }

    private fun callStateOnNddsSubFlow(): Flow<Int> = combine(
        subscriptionRepository.activeSubscriptionIdListFlow(),
        dataSubscriptionRepository.defaultDataSubscriptionIdFlow(),
    ){ activeSubIds, defaultDataSubId -> getNddsCallStateFlow(activeSubIds, defaultDataSubId) }
    .flatMapLatest { it }
    .distinctUntilChanged()
    .conflate()
    .onEach {  Log.d(TAG, "nDDS sub call state : $it") }
    .flowOn(Dispatchers.Default)

    private fun getNddsCallStateFlow(
        activeSubIdList: List<Int>,
        defaultDataSubId: Int,
    ): Flow<Int> {
        if (activeSubIdList.size <= 1) return flowOf(TelephonyManager.CALL_STATE_IDLE)
        if (!SubscriptionManager.isValidSubscriptionId(defaultDataSubId))
                return flowOf(TelephonyManager.CALL_STATE_IDLE)
        val callStateFlows = activeSubIdList
                .filter { subId -> subId != defaultDataSubId }
                .map { subId -> callStateRepository.callStateFlow(subId)}
        return combine(callStateFlows){ states -> states.first() }
                .distinctUntilChanged()
                .conflate()
    }

    companion object {
        private const val TAG = "RoamingPreferenceRepository"
    }
}
