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

import android.os.Bundle
import android.provider.Settings
import com.android.settings.flags.Flags
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MobileNetworkScreenTest : SettingsCatalystTestCase() {
    private val subId = 123

    override val preferenceScreenCreator =
        MobileNetworkScreen(Bundle().apply { putInt(Settings.EXTRA_SUB_ID, subId) })

    override val flagName: String
        get() = Flags.FLAG_DEEPLINK_NETWORK_AND_INTERNET_25Q4

    @Test
    fun getKey_returnsCorrectKey() {
        assertThat(preferenceScreenCreator.key).isEqualTo(MobileNetworkScreen.KEY)
    }

    @Test
    fun getBindingKey_returnsCorrectBindingKey() {
        assertThat(preferenceScreenCreator.bindingKey).isEqualTo("${MobileNetworkScreen.KEY}-123")
    }

    // TODO(b/419310279): Migration test fails when instantiating a BillingCycleRepository due to a
    // null networkService -- some setup needs to be added to provide the necessary interfaces.
    @Test override fun migration() {}
}
