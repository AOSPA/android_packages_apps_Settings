/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.content.Context
import android.os.UserManager
import android.provider.Settings
import com.android.settings.Utils
import com.android.settingslib.spaprivileged.framework.common.userManager
import com.android.settingslib.spaprivileged.settingsprovider.settingsGlobalBoolean

class SimRepository(private val context: Context) {
    private val userManager = context.userManager

    /** Gets whether show mobile network settings page entrance to the current user. */
    fun showMobileNetworkPageEntrance(): Boolean =
        (Utils.isMobileDataCapable(context) || Utils.isVoiceCapable(context)) &&
            userManager.isAdminUser

    /** Gets whether current user can enter mobile network settings page. */
    fun canEnterMobileNetworkPage(): Boolean {
        val isAirplaneMode by context.settingsGlobalBoolean(Settings.Global.AIRPLANE_MODE_ON)
        return showMobileNetworkPageEntrance() &&
            !isAirplaneMode &&
            !userManager.hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)
    }
}
