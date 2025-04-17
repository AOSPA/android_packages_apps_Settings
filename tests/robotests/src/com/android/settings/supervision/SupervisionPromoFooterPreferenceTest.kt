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
package com.android.settings.supervision

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.preference.Preference
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.settings.supervision.ipc.PreferenceData
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.getPreferenceSummary
import com.android.settingslib.metadata.getPreferenceTitle
import com.android.settingslib.widget.CardPreference
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SupervisionPromoFooterPreferenceTest {

    @get:Rule val mocks: MockitoRule = MockitoJUnit.rule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var preference: CardPreference
    private lateinit var context: Context
    private lateinit var lifecycleCoroutineScope: LifecycleCoroutineScope

    @Mock private lateinit var preferenceLifecycleContext: PreferenceLifecycleContext

    @Mock private lateinit var preferenceDataProvider: PreferenceDataProvider

    @Mock private lateinit var mockPackageManager: PackageManager

    @Before
    fun setup() {
        context = spy(InstrumentationRegistry.getInstrumentation().context)
        whenever(context.packageManager).thenReturn(mockPackageManager)
        preference = CardPreference(context)
        lifecycleCoroutineScope = TestLifecycleOwner().lifecycleScope
        whenever(preferenceLifecycleContext.findPreference<Preference>(any()))
            .thenReturn(preference)
        whenever(preferenceLifecycleContext.lifecycleScope).thenReturn(lifecycleCoroutineScope)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun getTitle_returnsCorrectTitle() {
        val supervisionPromoFooterPreference =
            SupervisionPromoFooterPreference(preferenceDataProvider)
        assertThat(supervisionPromoFooterPreference.getPreferenceTitle(context))
            .isEqualTo("Full parental controls")
    }

    @Test
    fun getSummary_returnsCorrectSummary() {
        val supervisionPromoFooterPreference =
            SupervisionPromoFooterPreference(preferenceDataProvider)
        assertThat(supervisionPromoFooterPreference.getPreferenceSummary(context))
            .isEqualTo(
                "Set up an account for your kid & help them manage it (required for " +
                    "kids under [AOC])"
            )
    }

    @Test
    fun onResume_actionIsNull_preferenceIsHidden() =
        testScope.runTest {
            val promoPreference = SupervisionPromoFooterPreference(preferenceDataProvider)
            promoPreference.bind(preference, mock())

            whenever(preferenceDataProvider.getPreferenceData(any())).thenAnswer {
                CompletableDeferred(
                    mapOf(
                        SupervisionPromoFooterPreference.KEY to
                            PreferenceData(targetPackage = "test.package")
                    )
                )
            }

            promoPreference.onResume(preferenceLifecycleContext)

            assertFalse(preference.isVisible)
            verify(mockPackageManager, never())
                .queryIntentActivitiesAsUser(any<Intent>(), any<Int>(), any<Int>())
        }

    @Test
    fun onResume_packageIsNull_preferenceIsHidden() =
        testScope.runTest {
            val promoPreference = SupervisionPromoFooterPreference(preferenceDataProvider)
            promoPreference.bind(preference, mock())

            whenever(preferenceDataProvider.getPreferenceData(any())).thenAnswer {
                CompletableDeferred(
                    mapOf(
                        SupervisionPromoFooterPreference.KEY to
                            PreferenceData(action = "Test Action")
                    )
                )
            }

            promoPreference.onResume(preferenceLifecycleContext)

            assertFalse(preference.isVisible)
            verify(mockPackageManager, never())
                .queryIntentActivitiesAsUser(any<Intent>(), any<Int>(), any<Int>())
        }

    @Test
    fun onResume_emptyPreferenceData_preferenceIsHidden() =
        testScope.runTest {
            val promoPreference = SupervisionPromoFooterPreference(preferenceDataProvider)
            promoPreference.bind(preference, mock())

            whenever(preferenceDataProvider.getPreferenceData(any())).thenAnswer {
                CompletableDeferred(mapOf<String, PreferenceData>())
            }

            promoPreference.onResume(preferenceLifecycleContext)

            assertFalse(preference.isVisible)
            verify(mockPackageManager, never())
                .queryIntentActivitiesAsUser(any<Intent>(), any<Int>(), any<Int>())
        }

    @Test
    fun onResume_noActivitiesCanHandleIntent_preferenceIsHidden() =
        testScope.runTest {
            val promoPreference = SupervisionPromoFooterPreference(preferenceDataProvider)
            promoPreference.bind(preference, mock())

            whenever(preferenceDataProvider.getPreferenceData(any())).thenAnswer {
                CompletableDeferred(
                    mapOf(
                        SupervisionPromoFooterPreference.KEY to
                            PreferenceData(action = "Test Action", targetPackage = "test.package")
                    )
                )
            }

            whenever(
                    mockPackageManager.queryIntentActivitiesAsUser(
                        any<Intent>(),
                        any<Int>(),
                        any<Int>(),
                    )
                )
                .thenReturn(emptyList())

            promoPreference.onResume(preferenceLifecycleContext)

            assertFalse(preference.isVisible)
        }

    @Test
    fun onResume_validIntent_hasActivityToHandleIntent_preferenceIsVisible_validIntentCreated() =
        testScope.runTest {
            val promoPreference = SupervisionPromoFooterPreference(preferenceDataProvider)
            promoPreference.bind(preference, mock())

            whenever(preferenceDataProvider.getPreferenceData(any())).thenAnswer {
                CompletableDeferred(
                    mapOf(
                        SupervisionPromoFooterPreference.KEY to
                            PreferenceData(action = "Test Action", targetPackage = "test.package")
                    )
                )
            }

            whenever(
                    mockPackageManager.queryIntentActivitiesAsUser(
                        any<Intent>(),
                        any<Int>(),
                        any<Int>(),
                    )
                )
                .thenReturn(listOf(ResolveInfo()))

            promoPreference.onResume(preferenceLifecycleContext)

            assertTrue(preference.isVisible)
            assertEquals("Test Action", promoPreference.intent(context)?.action)
            assertEquals("test.package", promoPreference.intent(context)?.`package`)
        }
}
