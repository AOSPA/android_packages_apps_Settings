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

package com.android.settings.nfc

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import com.android.settings.R
import com.android.settings.connecteddevice.NfcAndPaymentFragment
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.HardwareUnsupported
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean

@ProvidePreferenceScreen(NfcAndPaymentApiScreen.KEY) // Matches the existing screen key
class NfcAndPaymentApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.CONNECTED_DEVICES,
        fragment = NfcAndPaymentFragment::class,
        purpose = R.string.nfc_and_payment_screen_purpose,
    ) {
    init {
        flag { Flags.catalystMigration26q2() }
        preference(
            key = PREFERENCE_KEY_USE_NFC,
            type = AnyBoolean,
            purpose = R.string.use_nfc_purpose,
        ) {
            sensitivityLevel(SensitivityLevel.REQUIRES_CONFIRMATION)
            preconditions(R.string.use_nfc_preconditions) {
                if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC_ANY)) {
                    Allowed
                } else {
                    HardwareUnsupported(R.string.use_nfc_unavailable)
                }
            }
            get {
                execute {
                    val adapter = NfcAdapter.getDefaultAdapter(context)
                    adapter?.isEnabled ?: false
                }
            }
            set {
                permissions(WRITE_SECURE_SETTINGS)
                execute { value ->
                    val adapter = NfcAdapter.getDefaultAdapter(context)
                    adapter?.let {
                        if (value) {
                            it.enable()
                        } else {
                            it.disable()
                        }
                    }
                }
            }
        }

        preference(
            key = PREFERENCE_KEY_SECURE_NFC,
            type = AnyBoolean,
            purpose = R.string.require_device_unlock_for_nfc_purpose,
        ) {
            sensitivityLevel(SensitivityLevel.REQUIRES_CONFIRMATION)
            preconditions(R.string.require_device_unlock_for_nfc_preconditions) {
                if (
                    !context.packageManager.hasSystemFeature(
                        PackageManager.FEATURE_NFC_HOST_CARD_EMULATION
                    )
                ) {
                    HardwareUnsupported(R.string.require_device_unlock_for_nfc_unavailable)
                } else {
                    val adapter = NfcAdapter.getDefaultAdapter(context)
                    if (adapter == null) {
                        HardwareUnsupported(R.string.use_nfc_unavailable)
                    } else if (!adapter.isSecureNfcSupported) {
                        HardwareUnsupported(R.string.require_device_unlock_for_nfc_unavailable)
                    } else {
                        Allowed
                    }
                }
            }
            get {
                execute {
                    val adapter = NfcAdapter.getDefaultAdapter(context)
                    adapter?.isSecureNfcEnabled ?: false
                }
            }
            set {
                permissions(WRITE_SECURE_SETTINGS)
                execute { value ->
                    val adapter = NfcAdapter.getDefaultAdapter(context)
                    adapter?.enableSecureNfc(value)
                }
            }
        }
    }

    companion object {
        const val KEY = "nfc_and_payment_screen"
        const val PREFERENCE_KEY_USE_NFC = "use_nfc"
        const val PREFERENCE_KEY_SECURE_NFC = "require_device_unlock_for_nfc"
        private const val TAG = "NfcAndPaymentApiScreen"
    }
}
