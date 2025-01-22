/**
 * Copyright (c) 2025 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */
package com.android.settings.network.telephony

import android.content.Context
import android.provider.Settings
import android.sysprop.TelephonyProperties;
import android.telephony.TelephonyManager
import android.util.Log
import com.android.settingslib.spaprivileged.settingsprovider.settingsGlobalChangeFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class DdsPreferenceRepository(
    private val context: Context,
    private val callStateRepository: CallStateRepository = CallStateRepository(context),
) {

    fun isDdsPreferenceSelectableFlow(): Flow<Boolean> = combine (
        callStateRepository.isInCallFlow(),
        isInEcbModeFlow(),
        isSmartDdsEnabledFlow()
    ) { isInCall, isInEcbMode, isSmartDdsEnabled ->
            !isSmartDdsEnabled && !isInEcbMode && !isInCall
    }

    private fun isInEcbModeFlow(): Flow<Boolean> {
        return flow {
            val telephonyManager = context.getSystemService(TelephonyManager::class.java)
            val isEcbmEnabled = telephonyManager!!.getEmergencyCallbackMode()
            val isScbmEnabled = TelephonyProperties.in_scbm().orElse(false)
            emit(isEcbmEnabled || isScbmEnabled)
        }.onEach {  Log.d(TAG, "isInEcbMode: $it") }
        .flowOn(Dispatchers.Default)
    }

    private fun isSmartDdsEnabledFlow(): Flow<Boolean> {
        val smartDdsSwitchflow =
                context.settingsGlobalChangeFlow(Settings.Global.SMART_DDS_SWITCH,
                sendInitialValue = true)
        return smartDdsSwitchflow.map {
                Settings.Global.getInt(context.getContentResolver(),
                        Settings.Global.SMART_DDS_SWITCH, 0) == 1 }
                .distinctUntilChanged()
                .conflate()
                .onEach {  Log.d(TAG, "isSmartDdsEnabledFlow: $it") }
                .flowOn(Dispatchers.Default)
    }

    private companion object {
        private const val TAG = "DdsPreferenceRepository"
    }
}
