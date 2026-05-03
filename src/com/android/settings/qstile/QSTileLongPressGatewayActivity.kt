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

package com.android.settings.qstile

import android.app.settings.SettingsEnums
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.android.settings.core.SubSettingLauncher
import com.android.settings.development.AdbWirelessDebuggingFragment
import com.android.settings.development.AdbWirelessDebuggingPreferenceController
import com.android.settings.development.qstile.AdbWirelessDebuggingDevelopmentTile
import com.android.settings.network.telephony.satellite.quicksettings.SatelliteLandingPageActivity
import com.android.settings.network.telephony.satellite.quicksettings.SatelliteTileService
import com.android.settings.network.telephony.satellite.quicksettings.SatelliteUtils
import com.android.settings.overlay.FeatureFactory

/** A gateway activity for handling long-press actions on Quick Settings tiles. */
class QSTileLongPressGatewayActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "QSTileLongPressGateway"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the component name of the tile that was long-pressed.
        val tileComponent = intent.getParcelableExtra<ComponentName>(Intent.EXTRA_COMPONENT_NAME)

        // Add more tiles here to launch specific activities. Otherwise, we will launch the
        // Developer Options page by default for tiles in the same package.
        when (tileComponent?.className) {
            SatelliteTileService::class.java.name -> {
                // If component is SatelliteTileService, launch the landing page.
                val intent = SatelliteUtils.resolveSatelliteSettingsIntent(this)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                finish()
                return
            }
            AdbWirelessDebuggingDevelopmentTile::class.java.name -> {
                if (AdbWirelessDebuggingPreferenceController.isAvailable(this)) {
                    Log.d(TAG, "Long press from wireless debugging qstile")
                    SubSettingLauncher(this)
                        .setDestination(AdbWirelessDebuggingFragment::class.java.name)
                        .setSourceMetricsCategory(SettingsEnums.SETTINGS_ADB_WIRELESS)
                        .launch()
                    finish()
                    return
                }
            }
        }

        if (tileComponent?.packageName == packageName) {
            // Default to launching Developer Options for other tiles in the package.
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }
        finish()
    }
}
