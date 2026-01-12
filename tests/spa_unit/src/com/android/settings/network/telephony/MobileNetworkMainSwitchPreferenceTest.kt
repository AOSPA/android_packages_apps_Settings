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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.spy
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

class MobileNetworkMainSwitchPreferenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val mockSubscriptionActivationRepository = mock<SubscriptionActivationRepository>()

    private val mockSubscriptionRepository = mock<SubscriptionRepository>()

    private lateinit var preference: MobileNetworkMainSwitchPreference
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val preferenceLifecycleContext: PreferenceLifecycleContext = mock {
        on { lifecycleScope }.thenReturn(testScope)
    }

    @Before
    fun setUp() {
        preference =
            spy(
                MobileNetworkMainSwitchPreference(
                    context,
                    TEST_SUB_ID,
                    testScope,
                    mockSubscriptionActivationRepository,
                    mockSubscriptionRepository,
                )
            )
    }

    @Test
    fun isEnabled_subIsActive_returnTrue() = runBlocking {
        mockSubscriptionActivationRepository.stub {
            on { isActivationChangeableFlow() }.thenReturn(flowOf(true))
        }

        preference.onStart(preferenceLifecycleContext)
        preferenceLifecycleContext.notifyPreferenceChange(preference.key)

        delay(100)

        val result = preference.isEnabled(context)
        assertThat(result).isTrue()
    }

    @Test
    fun isEnabled_subIsNotActive_returnFalse() = runBlocking {
        mockSubscriptionActivationRepository.stub {
            on { isActivationChangeableFlow() }.thenReturn(flowOf(false))
        }

        preference.onStart(preferenceLifecycleContext)
        preferenceLifecycleContext.notifyPreferenceChange(preference.key)

        delay(100)

        val result = preference.isEnabled(context)
        assertThat(result).isFalse()
    }

    @Test
    fun setValue_setActive() = runBlocking {
        preference.storage(context).setBoolean(preference.key, true)

        verify(mockSubscriptionActivationRepository).setActive(TEST_SUB_ID, true)
    }

    @Test
    fun setValue_setInActive() = runBlocking {
        preference.storage(context).setBoolean(preference.key, false)

        verify(mockSubscriptionActivationRepository).setActive(TEST_SUB_ID, false)
    }

    @Test
    fun getValue_subActive_returnTrue() {
        mockSubscriptionRepository.stub {
            on { isSubscriptionEnabledFlow(TEST_SUB_ID) }.thenReturn(flowOf(true))
        }

        val result = preference.storage(context).getBoolean(preference.key)

        assertThat(result).isTrue()
    }

    @Test
    fun getValue_subInactive_returnFalse() {
        mockSubscriptionRepository.stub {
            on { isSubscriptionEnabledFlow(TEST_SUB_ID) }.thenReturn(flowOf(false))
        }

        val result = preference.storage(context).getBoolean(preference.key)

        assertThat(result).isFalse()
    }

    companion object {
        private const val TEST_SUB_ID = 5
    }
}
