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

package com.android.settings.datetime.timezone

import android.content.Context
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import com.google.common.truth.Truth.assertThat
import java.util.TimeZone
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.`when`
import org.mockito.Spy
import org.mockito.junit.MockitoJUnit
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class HomeTimeZoneSuggestorTest {

    @get:Rule val mMockitoRule = MockitoJUnit.rule()

    @Mock private lateinit var mSubscriptionManager: SubscriptionManager

    @Spy private lateinit var mContext: Context
    private lateinit var mSuggestor: HomeTimeZoneSuggestor

    @Before
    fun setUp() {
        mContext = spy(RuntimeEnvironment.getApplication())
        `when`(mContext.getSystemService(SubscriptionManager::class.java))
            .thenReturn(mSubscriptionManager)
        mSuggestor = HomeTimeZoneSuggestor(mContext)
    }

    @Test
    fun getTimeZoneSuggestions_noSim_shouldReturnCurrentTimeZone() {
        `when`(mSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(null)
        assertThat(mSuggestor.getTimeZoneSuggestions().size).isEqualTo(1)
        assertThat(mSuggestor.getTimeZoneSuggestions()[0].getId())
            .isEqualTo(TimeZone.getDefault().getID())
    }

    @Test
    fun getTimeZoneSuggestions_withSim_shouldReturnSuggestions() {
        val subscriptionInfo =
            SubscriptionInfo.Builder()
                .setId(0)
                .setSimSlotIndex(0)
                .setDisplayName("Google Fi")
                .setCarrierName("Google Fi")
                .setMcc("310")
                .setMnc("110")
                .setCountryIso("US")
                .build()

        `when`(mSubscriptionManager.getActiveSubscriptionInfoList())
            .thenReturn(listOf(subscriptionInfo))
        val suggestions = mSuggestor.getTimeZoneSuggestions()
        assertThat(suggestions).isNotEmpty()

        // Check that one of the suggestions is in the US
        assertThat(suggestions.any { "America/Los_Angeles" == it.getId() }).isTrue()
    }

    @Test
    fun getTimeZoneSuggestions_differentTimeZone_shouldReturnBoth() {
        val originalTimeZone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))

            val subscriptionInfo =
                SubscriptionInfo.Builder()
                    .setId(0)
                    .setSimSlotIndex(0)
                    .setDisplayName("Vodafone UK")
                    .setCarrierName("Vodafone UK")
                    .setMcc("234")
                    .setMnc("15")
                    .setCountryIso("GB")
                    .build()

            `when`(mSubscriptionManager.getActiveSubscriptionInfoList())
                .thenReturn(listOf(subscriptionInfo))

            val suggestions = mSuggestor.getTimeZoneSuggestions()

            // It should contain both the current default timezone and the SIM-based suggestions.
            assertThat(suggestions.size).isEqualTo(2) // 1 UK suggestion + current timezone
            assertThat(suggestions.any { "Europe/London" == it.getId() }).isTrue()
            assertThat(suggestions.any { "America/Los_Angeles" == it.getId() }).isTrue()
        } finally {
            TimeZone.setDefault(originalTimeZone)
        }
    }

    @Test
    fun getTimeZoneSuggestions_sameTimeZone_shouldNotHaveDuplicates() {
        val originalTimeZone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))

            val subscriptionInfo =
                SubscriptionInfo.Builder()
                    .setId(0)
                    .setSimSlotIndex(0)
                    .setDisplayName("Google Fi")
                    .setCarrierName("Google Fi")
                    .setMcc("310")
                    .setMnc("110")
                    .setCountryIso("US")
                    .build()

            `when`(mSubscriptionManager.getActiveSubscriptionInfoList())
                .thenReturn(listOf(subscriptionInfo))

            val suggestions = mSuggestor.getTimeZoneSuggestions()

            // Check that it only appears once to ensure deduplication works.
            assertThat(suggestions.size).isEqualTo(8) // 8 US suggestions
            assertThat(suggestions.any { "America/Los_Angeles" == it.getId() }).isTrue()
        } finally {
            TimeZone.setDefault(originalTimeZone)
        }
    }
}
