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

package com.android.settings.wifi.calling

import android.content.Context
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import com.android.settingslib.metadata.preferencesapi.safe
import com.android.settingslib.metadata.preferencesapi.unsafe

@RunWith(AndroidJUnit4::class)
class SubscriptionIdTest {

    @Mock private lateinit var context: Context
    @Mock private lateinit var subscriptionManager: SubscriptionManager

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        `when`(context.getSystemService(SubscriptionManager::class.java))
            .thenReturn(subscriptionManager)
    }

    @Test
    fun constructor_includeActiveOnly_success() {
        SubscriptionId(includeActive = true, includeInactive = false)
    }

    @Test(expected = IllegalArgumentException::class)
    fun constructor_includeInactive_throwsException() {
        SubscriptionId(includeActive = true, includeInactive = true)
    }

    @Test(expected = IllegalArgumentException::class)
    fun constructor_notIncludeActive_throwsException() {
        SubscriptionId(includeActive = false, includeInactive = false)
    }

    @Test
    fun getType_returnsIntClass() {
        assertThat(SubscriptionId().getType()).isEqualTo(Int::class.java)
    }

    @Test
    fun getDescription_returnsCorrectString() {
        assertThat(SubscriptionId().getDescription(context))
            .isEqualTo("An ID of a network subscription")
    }

    @Test
    fun getKey_returnsCorrectKey() {
        assertThat(SubscriptionId().getKey()).isEqualTo("SubscriptionId:true:false")
    }

    @Test
    fun getOptions_returnsActiveSubscriptions() {
        runBlocking {
            val sub1 = mock(SubscriptionInfo::class.java)
            `when`(sub1.subscriptionId).thenReturn(1)
            `when`(sub1.displayName).thenReturn("Sub 1")

            val sub2 = mock(SubscriptionInfo::class.java)
            `when`(sub2.subscriptionId).thenReturn(2)
            `when`(sub2.displayName).thenReturn("Sub 2")

            `when`(subscriptionManager.activeSubscriptionInfoList).thenReturn(listOf(sub1, sub2))

            val options = SubscriptionId().getOptions(context)

            assertThat(options).containsExactly(1.safe() to "Sub 1".unsafe(), 2.safe() to "Sub 2".unsafe())
        }
    }

    @Test
    fun getOptions_nullSubscriptionList_returnsEmpty() {
        runBlocking {
            `when`(subscriptionManager.activeSubscriptionInfoList).thenReturn(null)

            val options = SubscriptionId().getOptions(context)

            assertThat(options).isEmpty()
        }
    }

    @Test
    fun getOptions_unsupportedOperationException_returnsEmpty() {
        runBlocking {
            `when`(subscriptionManager.activeSubscriptionInfoList)
                .thenThrow(UnsupportedOperationException("Not supported"))

            val options = SubscriptionId().getOptions(context)

            assertThat(options).isEmpty()
        }
    }
}
