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

import android.content.ContextWrapper
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

class RoamingPreferenceTest {
    private val mockTelephonyManager = mock<TelephonyManager>()
    private val mockTelephonyManagerForSubId = mock<TelephonyManager>()

    private val context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getApplicationContext() = this

            override fun getSystemService(name: String): Any? =
                when (name) {
                    getSystemServiceName(TelephonyManager::class.java) -> mockTelephonyManager
                    else -> super.getSystemService(name)
                }
        }

    private lateinit var preference: RoamingPreference

    @Before
    fun setUp() {
        mockTelephonyManager.stub {
            on { createForSubscriptionId(anyInt()) } doReturn mockTelephonyManagerForSubId
        }
        preference = RoamingPreference(context, 0)
    }

    @Test
    fun storage_setValue_success() {
        preference.storage(context).setBoolean(RoamingPreference.KEY, true)

        verify(mockTelephonyManagerForSubId).isDataRoamingEnabled = true
    }

    @Test
    fun storage_getValueIsTrue_returnTrue() {
        mockTelephonyManagerForSubId.stub { on { isDataRoamingEnabled } doReturn true }

        val result = preference.storage(context).getBoolean(RoamingPreference.KEY)

        assertThat(result).isTrue()
    }

    @Test
    fun storage_getValueIsFalse_returnTrue() {
        mockTelephonyManagerForSubId.stub { on { isDataRoamingEnabled } doReturn false }

        val result = preference.storage(context).getBoolean(RoamingPreference.KEY)

        assertThat(result).isFalse()
    }
}
