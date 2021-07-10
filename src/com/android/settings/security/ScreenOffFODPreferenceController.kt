/*
 * Copyright (C) 2021 Paranoid Android
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
package com.android.settings.security

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.fingerprint.FingerprintManager
import android.provider.Settings
import android.provider.Settings.Secure.DOZE_ALWAYS_ON
import android.provider.Settings.System.SCREEN_OFF_FOD
import com.android.settings.Utils
import com.android.settings.core.TogglePreferenceController

class ScreenOffFODPreferenceController(context: Context, key: String?) :
    TogglePreferenceController(context, key) {

    private val fingerprintManager: FingerprintManager?
    private val packageManager: PackageManager = context.packageManager

    companion object {
        private const val FOD = "vendor.aospa.biometrics.fingerprint.inscreen"
    }

    init {
        fingerprintManager = Utils.getFingerprintManagerOrNull(context)
    }

    override fun getAvailabilityStatus(): Int {
        return if (fingerprintManager != null && fingerprintManager.isHardwareDetected
            && fingerprintManager.hasEnrolledFingerprints() && packageManager.hasSystemFeature(FOD)
        ) AVAILABLE else UNSUPPORTED_ON_DEVICE
    }

    override fun isChecked(): Boolean {
        return Settings.System.getInt(mContext.getContentResolver(), SCREEN_OFF_FOD, 0) == 1
    }

    override fun setChecked(isChecked: Boolean): Boolean {
        val enabled: Int = if (isChecked) 1 else 0
        Settings.System.putInt(mContext.getContentResolver(), SCREEN_OFF_FOD, enabled)
        Settings.Secure.putInt(mContext.getContentResolver(), DOZE_ALWAYS_ON, enabled)
        return true
    }
}
