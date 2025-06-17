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
package com.android.settings.network

import android.content.Context
import android.content.pm.PackageManager.FEATURE_TELEPHONY
import android.platform.test.annotations.DisableFlags
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.test.core.app.ApplicationProvider
import com.android.settings.flags.Flags
import com.android.settings.testutils2.SettingsCatalystTestCase
import org.mockito.kotlin.mock
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSubscriptionManager

class MobileNetworkListScreenTest : SettingsCatalystTestCase() {
    override val preferenceScreenCreator = MobileNetworkListScreen()

    override val flagName: String
        get() = Flags.FLAG_CATALYST_MOBILE_NETWORK_LIST

    @DisableFlags(Flags.FLAG_IS_DUAL_SIM_ONBOARDING_ENABLED)
    @Config(shadows = [ShadowSubscriptionManager::class])
    override fun migration() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val subscriptionManager =
            shadowOf(context.getSystemService(SubscriptionManager::class.java))
        val subscriptionInfo: SubscriptionInfo = mock()
        subscriptionManager.setAvailableSubscriptionInfos(subscriptionInfo)
        // make screen available
        shadowOf(context.packageManager).setSystemFeature(FEATURE_TELEPHONY, true)
        super.migration()
    }
}
