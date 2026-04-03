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

package com.android.settings.network.tether

import android.Manifest
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.network.tether.TetheringRepository.TetherType
import com.android.settings.overlay.FeatureFactory
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean

// LINT.IfChange
@ProvidePreferenceScreen(TetherApiScreen.KEY)
class TetherApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.NETWORK,
        fragment = TetherSettings::class,
        purpose = R.string.tether_settings_purpose_api,
        alreadyPartiallyMigrated = TetherScreen::class,
    ) {
    private val tetheringRepository =
        FeatureFactory.featureFactory.wifiFeatureProvider.tetheringRepository

    init {
        flag { Flags.catalystMigration26q2() }

        preference(USB_TETHER_KEY, USB_TETHER_PURPOSE, AnyBoolean) {
            sensitivityLevel(SensitivityLevel.DEEP_LINK_ONLY)

            get { execute { tetheringRepository.isEnabled(TetherType.USB) } }
            set {
                permissions(Manifest.permission.TETHER_PRIVILEGED)
                execute { value -> tetheringRepository.setEnabled(TetherType.USB, value) }
            }
        }

        preference(ETH_TETHER_KEY, ETH_TETHER_PURPOSE, AnyBoolean) {
            sensitivityLevel(SensitivityLevel.DEEP_LINK_ONLY)

            get { execute { tetheringRepository.isEnabled(TetherType.ETHERNET) } }
            set {
                permissions(Manifest.permission.TETHER_PRIVILEGED)
                execute { value -> tetheringRepository.setEnabled(TetherType.ETHERNET, value) }
            }
        }
    }

    companion object {
        const val KEY = "api_tether_settings"
        const val USB_TETHER_KEY = "usb_tether_settings"
        private val USB_TETHER_PURPOSE = R.string.usb_tether_switch_purpose
        const val ETH_TETHER_KEY = "enable_ethernet_tethering"
        private val ETH_TETHER_PURPOSE = R.string.ether_tether_switch_purpose
    }
}
// LINT.ThenChange(TetherSettings.java, TetherScreen.kt)
