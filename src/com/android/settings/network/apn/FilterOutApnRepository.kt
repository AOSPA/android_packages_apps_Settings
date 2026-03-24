/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */
package com.android.settings.network.apn

import android.content.Context
import android.os.Looper
import android.os.PersistableBundle;
import android.os.RemoteException
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionManager
import android.telephony.data.ApnSetting
import android.util.Log

import com.qti.extphone.Client
import com.qti.extphone.ExtPhoneCallbackListener
import com.qti.extphone.ExtTelephonyManager
import com.qti.extphone.ServiceCallback
import com.qti.extphone.Status
import com.qti.extphone.Token

import org.codeaurora.telephony.utils.EnhancedRadioCapabilityResponse

import java.lang.ref.WeakReference

class FilterOutApnRepository
constructor(
    private val appContext: Context,
    private val subId: Int
) {

    interface ApnFilteredOutListener {
        fun onApnFilteredOut()
    }

    @Volatile private var enhancedRadioCapability: EnhancedRadioCapabilityResponse? = null
    private var client: Client? = null
    private var extTelephonyManager: ExtTelephonyManager? = null
    @Volatile private var extTelServiceConnected: Boolean = false
    private var apnFilteredOutListener: WeakReference<ApnFilteredOutListener>? = null
    private var isApnFilterRequired: Boolean = false
    private var apnFilterPattern: Array<String>? =  null

    init {
        try {
            extTelephonyManager = ExtTelephonyManager.getInstance(appContext)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get ExtTelephonyManager instance", e)
        }
        enhancedRadioCapability = EnhancedRadioCapabilityResponse()
    }

    private val extPhoneCallbackListener = object : ExtPhoneCallbackListener(
            Looper.getMainLooper()) {
        override fun getQtiRadioCapabilityResponse(slotId: Int, token: Token,
                status: Status, raf: Int) {
            enhancedRadioCapability?.updateEnhancedRadioCapability(raf)
            Log.d(TAG, "getQtiRadioCapabilityResponse: slotId=$slotId, raf=$raf")
            apnFilteredOutListener?.get()?.onApnFilteredOut()
        }
    }

    private val extTelServiceCallback = object : ServiceCallback {
        override fun onConnected() {
            Log.d(TAG, "ExtTelephony service connected")
            extTelServiceConnected = true
            val events = intArrayOf()
            try {
                client = extTelephonyManager?.registerCallbackWithEvents(
                    appContext.packageName, extPhoneCallbackListener, events
                )

                val phoneId = SubscriptionManager.getPhoneId(subId)
                extTelephonyManager?.getQtiRadioCapability(phoneId, client)
            } catch (e: RemoteException) {
                Log.e(TAG, "Failed to register callback or query radio capability", e)
            }
        }

        override fun onDisconnected() {
            Log.d(TAG, "ExtTelephony service disconnected")
            try {
                extTelephonyManager?.unregisterCallback(extPhoneCallbackListener)
            } catch (e: RemoteException) {
                Log.w(TAG, "unregisterCallback failed", e)
            }
            extTelServiceConnected = false
            client = null
        }
    }

    fun attach(listener: ApnFilteredOutListener?) {
        if (listener != null && apnFilteredOutListener?.get() == null) {
            apnFilteredOutListener = WeakReference(listener)
            extTelephonyManager?.connectService(extTelServiceCallback)
            Log.d(TAG, "Attached for subId: $subId")
        }
    }

    fun detach(listener: ApnFilteredOutListener?) {
        if (listener != null && apnFilteredOutListener?.get() == listener) {
            extTelephonyManager?.disconnectService(extTelServiceCallback)
            apnFilteredOutListener = null
            Log.d(TAG, "Detached for subId: $subId")
        }
    }

    /**
     * Check if the given APN should be filtered out based on radio capability.
     * This overload accepts individual APN parameters from a database cursor.
     *
     * @param apnName The APN name
     * @param apnType The APN type string (e.g., "default", "mms", "ims")
     * @param mvnoMatchData The MVNO match data used to filter APNs
     * @return true if the APN should be filtered out, false otherwise
     */
    fun isApnFilteredOut(apnName: String, apnType: String,
            mvnoType: String?, mvnoMatchData: String?): Boolean {
        // Early return if filtering is not required or service not connected
        if (!isApnFilterRequired || !extTelServiceConnected) {
            return false
        }

        if (apnType.isEmpty()) {
            return false
        }

        Log.d(TAG, "isApnFilteredOut: apn=$apnName, apnType=$apnType, "
                + " mvnoType=$mvnoType, mvnoMatchData=$mvnoMatchData")
        // Normalize apn types to align with framework
        val apnTypeBitmask = ApnSetting.getApnTypesBitmaskFromString(apnType)
        val normalizedApnTypes = ApnSetting.getApnTypesStringFromBitmask(apnTypeBitmask)

        // Check if filtering is required for this specific APN type
        if (!isApnFilteringRequired(normalizedApnTypes, mvnoType, mvnoMatchData)) {
            return false
        }

        val deviceCapability = enhancedRadioCapability?.getEnhancedRadioCapability()
        if (deviceCapability == null) {
            Log.w(TAG, "isApnFilteredOut: deviceCapability is null, skipping filter")
            return false
        }

        // Check if the APN name matches the expected one for the device capability
        val expectedApnName = getApnBasedOnRadioCapability(normalizedApnTypes,
                mvnoMatchData, deviceCapability)
        if (expectedApnName != null && expectedApnName != apnName) {
            Log.d(TAG, "isApnFilteredOut: filtering out apn=$apnName, " +
                    "expected=$expectedApnName for capability=$deviceCapability")
            return true
        }

        return false
    }

    /**
     * Check if APN filtering is required for the given APN type.
     *
     * @param apnType APN type string
     * @param mvnoType MVNO type string (e.g., "gid")
     * @param mvnoMatchData MVNO match data of the MVNO type
     * @return true if filtering is required
     */
    private fun isApnFilteringRequired(apnType: String, mvnoType: String?,
            mvnoMatchData: String?): Boolean {
        if (!"gid".equals(mvnoType, ignoreCase = true)) return false
        val apnConfig = apnFilterPattern ?: return false

        for (apnEntry in apnConfig) {
            val split = apnEntry.split(":")
            // Validate entry format and content
            if (split.size == KEY_MULTI_APN_ARRAY_FOR_SAME_GID_ENTRY_LENGTH &&
                split.all { it.isNotBlank() }) {
                if (mvnoMatchData == split[GID] && apnType == split[APN_TYPE]) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Return APN name based on the device capability if the corresponding
     * entry is present in the carrier config.
     *
     * @param apnType APN type string
     * @param gid The GID1 of the SIM
     * @param deviceCapability Device capability (e.g., "SA", "NSA", "LTE")
     * @return APN name from the entry that matches the parameters, or null if not found
     */
    private fun getApnBasedOnRadioCapability(apnType: String, gid: String?,
            deviceCapability: String?): String? {
        if (deviceCapability == null) {
            return null
        }

        val apnConfig = apnFilterPattern ?: return null

        for (apnEntry in apnConfig) {
            val split = apnEntry.split(":")
            // Validate entry format and content
            if (split.size == KEY_MULTI_APN_ARRAY_FOR_SAME_GID_ENTRY_LENGTH &&
                split.all { it.isNotBlank() }) {
                if (gid == split[GID] && apnType == split[APN_TYPE]
                        && deviceCapability == split[DEVICE_CAPABILITY]) {
                    return split[APN_NAME]
                }
            }
        }
        return null
    }

    fun injectCarrierConfig(pb: PersistableBundle?) {
        // Reset to defaults if config is null
        apnFilterPattern = pb?.getStringArray(
            CarrierConfigManager.KEY_MULTI_APN_ARRAY_FOR_SAME_GID)
        isApnFilterRequired = pb?.getBoolean(
            CarrierConfigManager.KEY_REQUIRE_APN_FILTERING_WITH_RADIO_CAPABILITY, false) ?: false

        Log.d(TAG, "injectCarrierConfig: isApnFilterRequired=$isApnFilterRequired, " +
                "patternCount=${apnFilterPattern?.size ?: 0}")
    }

    companion object {
        private const val TAG = "FilterOutApnRepository"

        // Constants for parsing KEY_MULTI_APN_ARRAY_FOR_SAME_GID entries
        // Entry format: "GID:APN_TYPE:DEVICE_CAPABILITY:APN_NAME"
        private const val GID = 0
        private const val APN_TYPE = 1
        private const val DEVICE_CAPABILITY = 2
        private const val APN_NAME = 3
        private const val KEY_MULTI_APN_ARRAY_FOR_SAME_GID_ENTRY_LENGTH = 4
    }
}
