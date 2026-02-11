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

package com.android.settings.network.telephony

import android.content.Context
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.flags.Flags
import com.android.settings.network.telephony.MobileNetworkScreenApi.Companion.KEY_AUTO_DATA_SWITCHING
import com.android.settings.network.telephony.MobileNetworkScreenApi.Companion.KEY_ROAMING_SWITCHING
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.FailedPreconditionException
import com.android.settings.testutils2.Parameters
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class MobileNetworkScreenApiTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private lateinit var telephonyFeatureProvider: TelephonyFeatureProvider
    private lateinit var tester: ApiTester
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val subInfo1 =
        SubscriptionInfo.Builder().setId(TEST_SUB_ID).setDisplayName("test_1").build()
    private val subInfo2 =
        SubscriptionInfo.Builder().setId(TEST_DEFAULT_DATA_SUB_ID).setDisplayName("test_2").build()
    private val mockTelephonyRepository = mock<TelephonySettingsRepository>()
    private val mockSubscriptionRepository = mock<SubscriptionSettingsRepository>()

    @Before
    fun setUp() = runTest {
        telephonyFeatureProvider = FakeFeatureFactory.setupForTest().telephonyFeatureProvider
        telephonyFeatureProvider.stub {
            on { telephonyRepository } doReturn mockTelephonyRepository
            on { subscriptionRepository } doReturn mockSubscriptionRepository
            on { carrierConfigRepository } doReturn CarrierConfigRepository(context)
        }

        mockTelephonyRepository.stub { on { isMobileDataCable } doReturn true }

        mockSubscriptionRepository.stub {
            on { visibleActiveSubscriptionInfoList } doReturn listOf(subInfo1, subInfo2)
            on { defaultDataSubscriptionId } doReturn TEST_DEFAULT_DATA_SUB_ID
        }
        val mobileNetworkScreenApi = MobileNetworkScreenApi()
        val allParameters = mobileNetworkScreenApi.getAllPossibleParameters(context).first()
        mobileNetworkScreenApi.initializeParameters(allParameters)
        tester = ApiTester(mobileNetworkScreenApi)
    }

    @After
    fun tearDown() {
        CarrierConfigRepository.resetForTest()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagEnabled_isNotNull() {
        assertThat(tester.getScreen()).isNotNull()
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagDisabled_isNull() {
        assertThat(tester.getScreen()).isNull()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getBoolean_disallowedByInputDataSubId_failedPreconditionExceptionAndNoCrash() {
        val parameters = Parameters(Settings.EXTRA_SUB_ID to TEST_DEFAULT_DATA_SUB_ID.toString())

        tester.initializeScreenParameters(parameters)

        try {
            tester.get<Boolean>(KEY_AUTO_DATA_SWITCHING, parameters)
        } catch (e: FailedPreconditionException) {
            // no Crash
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getBoolean_policyEnabled_returnTrue() {
        mockTelephonyRepository.stub { on { isMobileDataSwitchPolicyEnabled(any()) } doReturn true }

        val parameters = Parameters(Settings.EXTRA_SUB_ID to TEST_SUB_ID.toString())

        tester.initializeScreenParameters(parameters)

        assertThat(tester.get<Boolean>(KEY_AUTO_DATA_SWITCHING, parameters)).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getBoolean_policyDisabled_returnFalse() {
        mockTelephonyRepository.stub {
            on { isMobileDataSwitchPolicyEnabled(any()) } doReturn false
        }

        val parameters = Parameters(Settings.EXTRA_SUB_ID to TEST_SUB_ID.toString())

        tester.initializeScreenParameters(parameters)

        assertThat(tester.get<Boolean>(KEY_AUTO_DATA_SWITCHING, parameters)).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getBoolean_roamingStateIsOn_returnTrue() {
        mockTelephonyRepository.stub { on { isDataRoamingEnabled(TEST_SUB_ID) } doReturn true }

        val parameters = Parameters(Settings.EXTRA_SUB_ID to TEST_SUB_ID.toString())

        tester.initializeScreenParameters(parameters)

        assertThat(tester.get<Boolean>(KEY_ROAMING_SWITCHING, parameters)).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getBoolean_roamingStateIsOff_returnFalse() {
        mockTelephonyRepository.stub { on { isDataRoamingEnabled(TEST_SUB_ID) } doReturn false }
        val parameters = Parameters(Settings.EXTRA_SUB_ID to TEST_SUB_ID.toString())

        tester.initializeScreenParameters(parameters)

        assertThat(tester.get<Boolean>(KEY_ROAMING_SWITCHING, parameters)).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun setBoolean_turnOnRoamingState_success() {
        mockTelephonyRepository.stub { on { isDataRoamingEnabled(TEST_SUB_ID) } doReturn false }
        CarrierConfigRepository.setBooleanForTest(
            TEST_SUB_ID,
            CarrierConfigManager.KEY_FORCE_HOME_NETWORK_BOOL,
            false,
        )
        val parameters = Parameters(Settings.EXTRA_SUB_ID to TEST_SUB_ID.toString())
        tester.initializeScreenParameters(parameters)

        tester.set(KEY_ROAMING_SWITCHING, true, parameters)

        verify(mockTelephonyRepository).setDataRoamingEnabled(TEST_SUB_ID, true)
    }

    companion object {
        const val TEST_SUB_ID = 1
        const val TEST_DEFAULT_DATA_SUB_ID = 2
    }
}
