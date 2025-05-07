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
package com.android.settings.deviceinfo.simstatus

import android.content.Context
import android.os.UserManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.telephony.euicc.EuiccManager
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.Utils
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.preference.PreferenceBindingPlaceholder

/**
 * Preference to show EID information.
 */
class SimEidPreference(
    private val context: Context,
) : PreferenceMetadata,
    PreferenceAvailabilityProvider,
    PreferenceBinding,
    PreferenceBindingPlaceholder,
    PreferenceLifecycleProvider,
    PreferenceTitleProvider,
    PreferenceSummaryProvider {

    private data class EidMetadata(val eid: String, val associatedSlotId: Int?)

    private val eidMetadata = getEidMetadata()
    private val formattedTitle = if (eidMetadata?.associatedSlotId == null) {
        context.getString(R.string.status_eid)
    } else {
        context.getString(R.string.eid_multi_sim, eidMetadata.associatedSlotId + 1)
    }

    override val key = "eid_info"

    override fun isAvailable(context: Context): Boolean =
        context.applicationContext.getSystemService(UserManager::class.java)?.isAdminUser == true &&
                (Utils.isMobileDataCapable(context) || Utils.isVoiceCapable(context)) &&
                eidMetadata?.eid?.isNotEmpty() == true

    override fun getTitle(context: Context): CharSequence? = formattedTitle

    override fun getSummary(context: Context): CharSequence? = eidMetadata?.eid

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.isCopyingEnabled = true
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        context.requirePreference<Preference>(key).onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                SimEidDialogFragment.show(
                    context.childFragmentManager, formattedTitle,
                    eidMetadata?.eid ?: ""
                )
                return@OnPreferenceClickListener true
            }
    }

    private fun getEidMetadata(): EidMetadata? {
        val metadata = getEidMetadataWithAssociatedSlotId()
        if (metadata != null) {
            return metadata
        }
        val euiccManager = context.getSystemService(EuiccManager::class.java)
        if (euiccManager != null && euiccManager.isEnabled) {
            val eid = euiccManager.eid
            if (eid != null) {
                return EidMetadata(eid, null)
            }
        }
        return null
    }

    private fun getEidMetadataWithAssociatedSlotId(): EidMetadata? {
        val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
        if (subscriptionManager == null) {
            return null
        }
        val telephonyManager = context.getSystemService(TelephonyManager::class.java)
        if (telephonyManager == null) {
            return null
        }
        val phoneCount = telephonyManager.phoneCount
        if (phoneCount <= MAX_PHONE_COUNT_SINGLE_SIM) {
            return null
        }

        val activeSubscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList()
        if (activeSubscriptionInfoList == null) {
            return null
        }
        val euiccCardInfoList = telephonyManager.uiccCardsInfo.filter { it.isEuicc }
        for (subInfo in activeSubscriptionInfoList.filter { it.isEmbedded }
            .sortedBy { it.simSlotIndex }) {
            for (euiccCardInfo in euiccCardInfoList) {
                if (subInfo.cardId == euiccCardInfo.cardId) {
                    val eid = euiccCardInfo.eid
                    if (!eid.isNullOrEmpty()) {
                        return EidMetadata(
                            eid,
                            subInfo.simSlotIndex
                        )
                    }
                    val euiccManager = context.getSystemService(EuiccManager::class.java)
                    if (euiccManager != null) {
                        val eid = euiccManager.createForCardId(euiccCardInfo.cardId).getEid()
                        if (eid != null) {
                            return EidMetadata(eid, subInfo.simSlotIndex)
                        }
                    }
                }
            }
        }

        return null
    }

    companion object {
        const val TAG = "SimEidPreference"
        const val MAX_PHONE_COUNT_SINGLE_SIM = 1
    }
}