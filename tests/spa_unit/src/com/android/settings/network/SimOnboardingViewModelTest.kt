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

package com.android.settings.network

import android.app.Application
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import android.telephony.SubscriptionInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.flags.Flags
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class SimOnboardingViewModelTest {
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    private lateinit var application: Application
    private lateinit var viewModel: SimOnboardingViewModel
    private val mockService = mock<SimOnboardingService>()

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        viewModel = SimOnboardingViewModel(application)
    }

    @After fun tearDown() = runBlocking { viewModel.clearOnboardingSimSet() }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_SKIP_ONBOARDING_WHEN_SAME_SET)
    fun shouldSkipOnboarding_noSavedSet_returnFalse() = runBlocking {
        whenever(mockService.activeSubInfoList).thenReturn(listOf(SUB_INFO_1))
        whenever(mockService.targetSubInfo).thenReturn(SUB_INFO_2)

        assertThat(viewModel.shouldSkipOnboarding(mockService)).isFalse()
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_SKIP_ONBOARDING_WHEN_SAME_SET)
    fun shouldSkipOnboarding_notTheSameSet_returnFalse() = runBlocking {
        whenever(mockService.activeSubInfoList).thenReturn(listOf(SUB_INFO_1))
        whenever(mockService.targetSubInfo).thenReturn(SUB_INFO_2)
        whenever(mockService.userSelectedSubInfoList)
            .thenReturn(mutableListOf(SUB_INFO_1, SUB_INFO_2))
        viewModel.saveSimOnboardingResult(mockService)

        whenever(mockService.activeSubInfoList).thenReturn(listOf(SUB_INFO_1))
        whenever(mockService.targetSubInfo).thenReturn(SUB_INFO_3)

        assertThat(viewModel.shouldSkipOnboarding(mockService)).isFalse()
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_SKIP_ONBOARDING_WHEN_SAME_SET)
    fun shouldSkipOnboarding_savedSetMatches_returnTrue() = runBlocking {
        whenever(mockService.activeSubInfoList).thenReturn(listOf(SUB_INFO_1))
        whenever(mockService.targetSubInfo).thenReturn(SUB_INFO_2)
        whenever(mockService.userSelectedSubInfoList)
            .thenReturn(mutableListOf(SUB_INFO_1, SUB_INFO_2))

        viewModel.saveSimOnboardingResult(mockService)

        assertThat(viewModel.shouldSkipOnboarding(mockService)).isTrue()
    }

    private companion object {
        const val SUB_ID_1 = 1
        const val SUB_ID_2 = 2
        const val SUB_ID_3 = 3

        val SUB_INFO_1: SubscriptionInfo =
            SubscriptionInfo.Builder().apply { setId(SUB_ID_1) }.build()

        val SUB_INFO_2: SubscriptionInfo =
            SubscriptionInfo.Builder().apply { setId(SUB_ID_2) }.build()

        val SUB_INFO_3: SubscriptionInfo =
            SubscriptionInfo.Builder().apply { setId(SUB_ID_3) }.build()
    }
}
