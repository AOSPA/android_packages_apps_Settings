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

package com.android.settings.deviceinfo.imei

import android.content.Context
import android.util.Log
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.Utils
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

/** Preference to show IMEI information for single and multi modem devices. */
class ImeiPreference(
    context: Context,
    private val slotIndex: Int,
    private val activeModemCount: Int,
) :
    PreferenceMetadata,
    PreferenceBinding,
    PreferenceBindingPlaceholder,
    PreferenceLifecycleProvider,
    PreferenceTitleProvider,
    PreferenceSummaryProvider,
    PreferenceAvailabilityProvider {

    private val formattedTitle: String = context.getFormattedTitle()
    private val formattedSummary: String = context.getFormattedSummary()

    override val key: String
        get() = KEY_PREFIX + "${slotIndex + 1}"

    override fun isAvailable(context: Context): Boolean =
        context.isAdminUser == true &&
            (Utils.isMobileDataCapable(context) || Utils.isVoiceCapable(context))

    override fun getTitle(context: Context): CharSequence? = formattedTitle

    override fun getSummary(context: Context): CharSequence? = formattedSummary

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.isCopyingEnabled = true
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        context.requirePreference<Preference>(key).onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                ImeiInfoDialogFragment.show(context.childFragmentManager, slotIndex, formattedTitle)
                return@OnPreferenceClickListener true
            }
    }

    private fun Context.getFormattedTitle(): String =
        if (activeModemCount <= 1) {
            getString(R.string.status_imei)
        } else {
            getString(R.string.imei_multi_sim, slotIndex + 1)
        }

    private fun Context.getFormattedSummary(): String {
        val imeiList = getImeiList()
        return when {
            imeiList.isEmpty() -> String()
            slotIndex > imeiList.size -> imeiList[0]
            else -> imeiList[slotIndex]
        }
    }

    /**
     * As per GSMA specification TS37, below Primary IMEI requirements are mandatory to support
     * TS37_2.2_REQ_5 TS37_2.2_REQ_8 (Attached the document has description about this test cases)
     *
     * b/434700998, using the lower IMEI as the primary IMEI. IMEI 1 = primary IMEI i.e. lower IMEI
     * IMEI 2 = non-primary IMEI
     */
    private fun Context.getImeiList(): List<String> = buildList {
        telephonyManager?.let {
            var primaryImei = String()
            try {
                primaryImei = it.primaryImei
            } catch (exception: Exception) {
                Log.e(TAG, "PrimaryImei not available.", exception)
            }
            val imeiListFromSlot: List<String> = buildList {
                for (slotIndex in 0..activeModemCount - 1) {
                    val slotImei = it.getImei(slotIndex)
                    if (slotImei != null && primaryImei != slotImei) {
                        add(slotImei)
                    }
                }
            }
            imeiListFromSlot.sorted()
            if (!primaryImei.isEmpty()) {
                add(primaryImei)
            }
            addAll(imeiListFromSlot)
        }
    }

    companion object {
        private const val TAG = "ImeiPreference"
        const val KEY_PREFIX = "imei_info"
    }
}
