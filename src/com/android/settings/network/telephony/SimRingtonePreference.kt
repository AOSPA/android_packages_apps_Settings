/*
 * Copyright (C) 2026 The Android Open Source Project
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
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.media.audio.Flags as AudioFlags
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import com.android.settings.DefaultRingtonePreference
import com.android.settings.R
import com.android.settings.network.SubscriptionUtil
import com.android.settings.network.telephony.MobileNetworkSettingsSearchIndex.MobileNetworkSettingsSearchItem
import com.android.settings.network.telephony.MobileNetworkSettingsSearchIndex.MobileNetworkSettingsSearchResult
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.preference.PreferenceBindingPlaceholder

/** Preference for Network & Internet > SIMs > [SIM] > Ringtone */
// LINT.IfChange
@SuppressLint("MissingPermission")
class SimRingtonePreference(private val context: Context, private val subId: Int) :
    PreferenceMetadata,
    PreferenceBinding,
    PreferenceLifecycleProvider,
    PreferenceSummaryProvider,
    PreferenceBindingPlaceholder,
    PreferenceAvailabilityProvider {

    private lateinit var ringtonePreference: DefaultRingtonePreference
    private val telecomManager by lazy {
        context.applicationContext.getSystemService(TelecomManager::class.java)
    }

    override val title: Int
        get() = R.string.sim_ringtone_title

    override val key: String
        get() = KEY

    override fun getSummary(context: Context): CharSequence? = getRingtoneSummary(context)

    override val availabilityDescription =
        "The device must have more than 1 active SIM and have the corresponding config enabled."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override fun isAvailable(context: Context) =
        context.resources.getBoolean(R.bool.config_show_sim_specific_ringtone) &&
            SubscriptionUtil.getAvailableSubscriptions(context).size > 1 &&
            SubscriptionManager.isValidSubscriptionId(subId)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.isCopyingEnabled = true
    }

    override fun onResume(context: PreferenceLifecycleContext) {
        context.notifyPreferenceChange(KEY)
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        val ringtonePickerLauncher =
            context.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    ringtonePreference.onActivityResult(
                        RINGTONE_PICKER_REQUEST_CODE,
                        result.resultCode,
                        result.data,
                    )
                }
            }

        context.requirePreference<Preference>(key).onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                ringtonePreference = DefaultRingtonePreference(context, null)
                val phoneAccountHandle = getCurrentPhoneAccountHandle()
                ringtonePreference.phoneAccountHandle = phoneAccountHandle
                val ringtonePickerIntent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                ringtonePreference.onPrepareRingtonePickerIntent(ringtonePickerIntent)

                ringtonePickerLauncher?.launch(ringtonePickerIntent)
                return@OnPreferenceClickListener true
            }
    }

    private fun getCurrentPhoneAccountHandle(): PhoneAccountHandle? {
        val accountHandles = getPhoneAccountHandles()
        val id = subId

        // Iterates through accountHandles and returns the one where the ID matches
        return accountHandles.find { it.id == id.toString() }
    }

    // This method is to get the available phone account handles of the SIMs.
    private fun getPhoneAccountHandles(): List<PhoneAccountHandle> {
        val tm = telecomManager ?: return emptyList()

        val accountHandles = tm.getCallCapablePhoneAccounts(true)
        return accountHandles.filter { accountHandle ->
            val phoneAccount = tm.getPhoneAccount(accountHandle)

            val hasSubscriptionCapability =
                phoneAccount?.hasCapabilities(PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION) == true
            val isNotEmergency = accountHandle.id != EMERGENCY_PHONE_ACCOUNT_HANDLE_ID

            hasSubscriptionCapability && isNotEmergency
        }
    }

    private fun getRingtoneSummary(context: Context): CharSequence {
        var simRingtoneUri =
            RingtoneManager.getRingtoneUriForPhoneAccountHandle(
                context,
                getCurrentPhoneAccountHandle(),
            )

        // Use 'let' and the Elvis operator to resolve the title or return the default string
        return simRingtoneUri?.let { uri ->
            RingtoneManager.getRingtone(context, uri)?.getTitle(context)
        } ?: context.getString(R.string.owner_info_settings_summary)
    }

    override val purpose: Int
        get() = R.string.sim_ringtone_pref_purpose

    companion object {
        private const val TAG = "SimRingtonePreference"
        const val KEY = "sim_ringtone_info"
        const val EMERGENCY_PHONE_ACCOUNT_HANDLE_ID = "E"
        private const val RINGTONE_PICKER_REQUEST_CODE = 9001

        class SimRingtoneSearchItem(private val context: Context) :
            MobileNetworkSettingsSearchItem {
            override fun getSearchResult(subId: Int): MobileNetworkSettingsSearchResult? {
                if (
                    !context.resources.getBoolean(R.bool.config_show_sim_specific_ringtone) ||
                        !AudioFlags.supportPerPhoneAccountRingtone()
                )
                    return null
                return MobileNetworkSettingsSearchResult(
                    key = "sim_ringtone_info",
                    title = context.getString(R.string.sim_ringtone_title),
                )
            }
        }
    }
}
// LINT.ThenChange(DefaultRingtonePreference.java)
