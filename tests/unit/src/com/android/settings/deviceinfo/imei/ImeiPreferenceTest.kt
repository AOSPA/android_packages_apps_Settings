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

package com.android.settings.deviceinfo.imei

import android.content.ContextWrapper
import android.os.UserManager
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class ImeiPreferenceTest {
    private val mockUserManager = mock<UserManager>()
    private val mockTelephonyManager = mock<TelephonyManager>()

    private val context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getApplicationContext() = this

            override fun getSystemService(name: String): Any? =
                when (name) {
                    getSystemServiceName(UserManager::class.java) -> mockUserManager
                    getSystemServiceName(TelephonyManager::class.java) -> mockTelephonyManager
                    else -> super.getSystemService(name)
                }
        }

    private lateinit var preference: ImeiPreference

    @Before
    fun setUp() {
        mockUserManager.stub { on { isAdminUser } doReturn true }
        mockTelephonyManager.stub {
            on { isDataCapable } doReturn true
            on { isDeviceVoiceCapable } doReturn true
            on { getImei(0) } doReturn IMEI_1
            on { getImei(1) } doReturn IMEI_2
            on { primaryImei } doReturn IMEI_1
            on { activeModemCount } doReturn MULTI_SLOT
        }
        preference = ImeiPreference(context, 0, 1, imeiList_oneImei)
    }

    @Test
    fun initUi_noImei_doNotCrash() {
        preference = ImeiPreference(context, 0, 2, listOf<String>())
    }

    @Test
    fun initUi_oneImei_doNotCrash() {
        preference = ImeiPreference(context, 0, 2, imeiList_oneImei)
    }

    @Test
    fun initUi_twoImei_doNotCrash() {
        preference = ImeiPreference(context, 0, 2, imeiList)
    }

    @Test
    fun getKey_slotIndex0_returnImeiWithIndex() {
        preference = ImeiPreference(context, 0, 2, imeiList)

        assertThat(preference.key).isEqualTo(ImeiPreference.KEY_PREFIX + "1")
    }

    @Test
    fun getKey_slotIndex1_returnImeiWithIndex() {
        preference = ImeiPreference(context, 1, 2, imeiList)

        assertThat(preference.key).isEqualTo(ImeiPreference.KEY_PREFIX + "2")
    }

    @Test
    fun isAvailable_byDefault_returnTrue() {
        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_isNotAdminUser_returnFalse() {
        mockUserManager.stub { on { isAdminUser } doReturn false }

        preference = ImeiPreference(context, 0, 1, imeiList_oneImei)

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_noDataNorVoiceCapable_returnFalse() {
        mockTelephonyManager.stub {
            on { isDataCapable } doReturn false
            on { isDeviceVoiceCapable } doReturn false
        }

        preference = ImeiPreference(context, 0, 1, imeiList_oneImei)

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun getSummary_oneImei_index0_returnImei1() {
        assertThat(preference.getSummary(context)).isEqualTo(IMEI_1)
    }

    @Test
    fun getSummary_twoImei_index0_returnImei1() {
        preference = ImeiPreference(context, 0, 2, imeiList)

        assertThat(preference.getSummary(context)).isEqualTo(IMEI_1)
    }

    @Test
    fun getSummary_twoImei_index1_returnImei2() {
        preference = ImeiPreference(context, 1, 2, imeiList)

        assertThat(preference.getSummary(context)).isEqualTo(IMEI_2)
    }

    @Test
    fun getSummary_index0_bothSlotsAreSameImei_returnImei1() {
        preference = ImeiPreference(context, 0, 2, imeiList_sameImei)

        assertThat(preference.getSummary(context)).isEqualTo(IMEI_1)
    }

    @Test
    fun getSummary_index1_bothSlotsAreSameImei_returnImei1() {
        preference = ImeiPreference(context, 1, 2, imeiList_sameImei)

        assertThat(preference.getSummary(context)).isEqualTo(IMEI_1)
    }

    @Test
    fun getSummary_index2_indexNotInList_returnEmptyString() {
        preference = ImeiPreference(context, 2, 2, imeiList)

        assertThat(preference.getSummary(context)).isEqualTo("")
    }

    @Test
    fun getImeiList_singleActiveSlot_getOneImei() {
        mockTelephonyManager.stub {
            on { activeModemCount } doReturn SINGLE_SLOT
            on { getImei(0) } doReturn IMEI_1
            on { primaryImei } doReturn IMEI_1
        }

        assertThat(context.getImeiList).hasSize(1)
        assertThat(context.getImeiList[0]).isEqualTo(IMEI_1)
    }

    @Test
    fun getImeiList_multiActiveSlot_getTwoImei() {
        assertThat(context.getImeiList).hasSize(2)
        assertThat(context.getImeiList[0]).isEqualTo(IMEI_1)
        assertThat(context.getImeiList[1]).isEqualTo(IMEI_2)
    }

    @Test
    fun getImeiList_multiActiveSlot_primaryImei_getTwoImei() {
        mockTelephonyManager.stub { on { primaryImei } doReturn IMEI_2 }

        assertThat(context.getImeiList[0]).isEqualTo(IMEI_2)
        assertThat(context.getImeiList[1]).isEqualTo(IMEI_1)
    }

    @Test
    fun getImeiList_multiActiveSlot_sameImei_getTwoImei() {
        mockTelephonyManager.stub {
            on { getImei(0) } doReturn IMEI_1
            on { getImei(1) } doReturn IMEI_1
        }
        mockTelephonyManager.stub { on { primaryImei } doReturn IMEI_1 }

        assertThat(context.getImeiList[0]).isEqualTo(IMEI_1)
        assertThat(context.getImeiList[1]).isEqualTo(IMEI_1)
    }

    @Test
    fun getImeiList_getPrimaryImeiThrowException_doNotCrash() {
        mockTelephonyManager.stub { on { primaryImei } doThrow (IllegalStateException()) }

        context.getImeiList
    }

    companion object {
        const val SINGLE_SLOT = 1
        const val MULTI_SLOT = 2
        const val IMEI_1 = "111111111111115"
        const val IMEI_2 = "222222222222225"
        val imeiList = listOf(IMEI_1, IMEI_2)
        val imeiList_sameImei = listOf(IMEI_1, IMEI_1)
        val imeiList_oneImei = listOf(IMEI_1)
    }
}
