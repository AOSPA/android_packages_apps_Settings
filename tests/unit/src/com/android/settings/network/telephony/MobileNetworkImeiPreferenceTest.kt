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
import android.os.UserManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
import com.android.settings.deviceinfo.imei.ImeiData
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

class MobileNetworkImeiPreferenceTest {
    private val mockUserManager = mock<UserManager>()
    private val mockTelephonyManager = mock<TelephonyManager>()
    private val mockSubscriptionManager = mock<SubscriptionManager>()
    private lateinit var mockMobileNetworkData: MobileNetworkData
    private val context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getApplicationContext() = this

            override fun getSystemService(name: String): Any? =
                when (name) {
                    getSystemServiceName(UserManager::class.java) -> mockUserManager
                    getSystemServiceName(TelephonyManager::class.java) -> mockTelephonyManager
                    getSystemServiceName(SubscriptionManager::class.java) -> mockSubscriptionManager
                    else -> super.getSystemService(name)
                }
        }

    private lateinit var preference: MobileNetworkImeiPreference

    @Before
    fun setUp() {
        mockTelephonyManager.stub {
            on { isDataCapable } doReturn true
            on { isDeviceVoiceCapable } doReturn true
            on { createForSubscriptionId(anyInt()) } doReturn mockTelephonyManager
            on { primaryImei } doReturn IMEI_1
        }

        mockMobileNetworkData = MobileNetworkData(context, null, 0)
        mockMobileNetworkData._imeiInfoDataFlow.value =
            ImeiInfoData(
                isAvailable = true,
                imeiData = imeiData1,
                title = TITLE_1,
                summary = SUMMARY_1,
            )
        preference = MobileNetworkImeiPreference(mockMobileNetworkData)
    }

    @Test
    fun isAvailable_byDefault_returnTrue() {
        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_isNotAvailableInData_returnFalse() {
        mockMobileNetworkData._imeiInfoDataFlow.value = ImeiInfoData(isAvailable = false)
        preference = MobileNetworkImeiPreference(mockMobileNetworkData)

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun getSummary_returnSummaryFromData() {
        assertThat(preference.getSummary(context)).isEqualTo(SUMMARY_1)
    }

    @Test
    fun getTitle_returnTitleFromData() {
        assertThat(preference.getTitle(context)).isEqualTo(TITLE_1)
    }

    companion object {
        const val IMEI_1 = "111111111111115"
        const val TITLE_1 = "IMEI 1"
        const val SUMMARY_1 = "Summary 1"
        val imeiData1 = ImeiData(IMEI_1, 0)
    }
}
