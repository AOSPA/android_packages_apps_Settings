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

package com.android.settings.network.telephony

import android.Manifest
import android.content.Context
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.fragment.app.FragmentManager
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.SwitchPreferenceCompat
import com.android.settings.R
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.NoOpKeyedObservable
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.preference.PreferenceBindingPlaceholder
import com.android.settingslib.preference.SwitchPreferenceBinding
import kotlinx.coroutines.launch

/** A catalyst switch preference for switching data roaming on/off. */
// LINT.IfChange
class RoamingPreference(
    val context: Context,
    val subId: Int,
    private val mobileDataRepository: MobileDataRepository = MobileDataRepository(context),
) :
    BooleanValuePreference,
    SwitchPreferenceBinding,
    PreferenceBindingPlaceholder,
    PreferenceLifecycleProvider,
    PreferenceAvailabilityProvider,
    OnPreferenceChangeListener {

    private lateinit var fragmentManager: FragmentManager
    private lateinit var roamingPreference: SwitchPreferenceCompat
    private val carrierConfigRepository = CarrierConfigRepository(context)

    override val title: Int = R.string.roaming

    override val summary: Int = R.string.roaming_enable

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        fragmentManager = context.fragmentManager
        context.lifecycleScope.launch {
            mobileDataRepository.isDataRoamingEnabledFlow(subId).collect { it ->
                if (roamingPreference != null) {
                    roamingPreference.isChecked = it
                }
            }
        }
    }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        roamingPreference = preference as SwitchPreferenceCompat
        preference.onPreferenceChangeListener = this
    }

    override val key: String = KEY

    override fun getReadPermissions(context: Context) =
        Permissions.anyOf(
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_BASIC_PHONE_STATE,
        )

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.DISALLOW

    override fun isAvailable(context: Context): Boolean =
        SubscriptionManager.isValidSubscriptionId(subId) &&
            !carrierConfigRepository.getBoolean(
                subId,
                CarrierConfigManager.KEY_FORCE_HOME_NETWORK_BOOL,
            )

    override fun storage(context: Context) = DataRoamingStorage(context, subId) as KeyValueStore

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        if (newValue as Boolean && isDialogNeeded()) {
            showDialog()
        } else {
            // Update data directly if we don't need dialog
            storage(context).setBoolean(KEY, newValue)
        }
        return true
    }

    private fun isDialogNeeded(): Boolean {
        // Need dialog if we need to turn on roaming and the roaming charge indication is allowed
        return !carrierConfigRepository.getBoolean(
            subId,
            CarrierConfigManager.KEY_DISABLE_CHARGE_INDICATION_BOOL,
        )
    }

    private fun showDialog() {
        fragmentManager?.let { RoamingDialogFragment.newInstance(subId).show(it, DIALOG_TAG) }
    }

    @Suppress("UNCHECKED_CAST")
    private class DataRoamingStorage(
        private val context: Context,
        private val subId: Int,
        private val telephonyManager: TelephonyManager =
            context.getSystemService(TelephonyManager::class.java).createForSubscriptionId(subId),
    ) : NoOpKeyedObservable<String>(), KeyValueStore {

        override fun contains(key: String) = key == KEY

        override fun <T : Any> getValue(key: String, valueType: Class<T>): T {
            return telephonyManager.isDataRoamingEnabled as T
        }

        override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
            if (key.equals(KEY) && value is Boolean) {
                telephonyManager.isDataRoamingEnabled = value
            } else {
                throw IllegalArgumentException("Invalid key : $key or value type - $value")
            }
        }
    }

    companion object {
        const val KEY = "button_roaming_key_catalyst"
        private const val DIALOG_TAG = "MobileDataDialog"
    }
}
// LINT.ThenChange(RoamingPreferenceController.kt)
