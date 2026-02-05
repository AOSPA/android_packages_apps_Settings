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

package com.android.settings.spa.network

import android.content.Context
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.internal.telephony.flags.Flags as TelephonyFlags
import com.android.settings.R
import com.android.settings.flags.Flags
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy

@RunWith(AndroidJUnit4::class)
class NetworkCellularGroupProviderTest {
    @get:Rule val composeTestRule = createComposeRule()

    @get:Rule val setFlagsRule = SetFlagsRule()

    private val mockSubscriptionManager =
        mock<SubscriptionManager> {
            on { getPhoneNumber(SUB_ID_1) } doReturn NUMBER_1
            on { getPhoneNumber(SUB_ID_2) } doReturn NUMBER_2
        }

    private val context: Context =
        spy(ApplicationProvider.getApplicationContext()) {
            on { getSystemService(SubscriptionManager::class.java) } doReturn
                mockSubscriptionManager
        }

    @Test
    @EnableFlags(TelephonyFlags.FLAG_ENABLE_IS_PRIVATE_NETWORK_API)
    fun primarySimSectionImpl_twoNormalSims_displayed() {
        val simList = listOf(SUB_INFO_1, SUB_INFO_2)

        composeTestRule.setContent {
            PrimarySimSectionImpl(
                subscriptionInfoListFlow = flowOf(simList),
                callsSelectedId = mutableIntStateOf(SubscriptionManager.INVALID_SUBSCRIPTION_ID),
                textsSelectedId = mutableIntStateOf(SubscriptionManager.INVALID_SUBSCRIPTION_ID),
                mobileDataSelectedId =
                    mutableIntStateOf(SubscriptionManager.INVALID_SUBSCRIPTION_ID),
            )
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.primary_sim_title))
            .assertIsDisplayed()
    }

    @Test
    @EnableFlags(TelephonyFlags.FLAG_ENABLE_IS_PRIVATE_NETWORK_API)
    fun primarySimSectionImpl_oneNormalOneOpportunistic_notDisplayed() {
        val simList = listOf(SUB_INFO_1, SUB_INFO_OPPORTUNISTIC)

        composeTestRule.setContent {
            PrimarySimSectionImpl(
                subscriptionInfoListFlow = flowOf(simList),
                callsSelectedId = mutableIntStateOf(SubscriptionManager.INVALID_SUBSCRIPTION_ID),
                textsSelectedId = mutableIntStateOf(SubscriptionManager.INVALID_SUBSCRIPTION_ID),
                mobileDataSelectedId =
                    mutableIntStateOf(SubscriptionManager.INVALID_SUBSCRIPTION_ID),
            )
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.primary_sim_title))
            .assertIsNotDisplayed()
    }

    private companion object {
        const val SUB_ID_1 = 1
        const val SUB_ID_2 = 2
        const val DISPLAY_NAME_1 = "Sub 1"
        const val DISPLAY_NAME_2 = "Sub 2"
        const val NUMBER_1 = "000000001"
        const val NUMBER_2 = "000000002"
        const val MCC = "310"

        val SUB_INFO_1: SubscriptionInfo =
            SubscriptionInfo.Builder()
                .apply {
                    setId(SUB_ID_1)
                    setDisplayName(DISPLAY_NAME_1)
                    setMcc(MCC)
                    setSimSlotIndex(0)
                }
                .build()

        val SUB_INFO_2: SubscriptionInfo =
            SubscriptionInfo.Builder()
                .apply {
                    setId(SUB_ID_2)
                    setDisplayName(DISPLAY_NAME_2)
                    setMcc(MCC)
                    setSimSlotIndex(1)
                }
                .build()

        val SUB_INFO_OPPORTUNISTIC: SubscriptionInfo =
            SubscriptionInfo.Builder()
                .apply {
                    setId(SUB_ID_2)
                    setDisplayName(DISPLAY_NAME_2)
                    setMcc(MCC)
                    setSimSlotIndex(1)
                    setOpportunistic(true)
                }
                .build()
    }
}
