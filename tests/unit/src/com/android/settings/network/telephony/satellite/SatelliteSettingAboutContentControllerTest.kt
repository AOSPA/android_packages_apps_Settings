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

package com.android.settings.network.telephony.satellite

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.network.telephony.TelephonyFeatureProvider
import com.android.settings.network.telephony.TelephonySettingsRepository
import com.android.settings.network.telephony.satellite.SatelliteSettingAboutContentController.Companion.PREF_KEY_ABOUT_SATELLITE_CONNECTIVITY
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settings.testutils.ResourcesUtils
import com.android.settingslib.widget.TopIntroPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class SatelliteSettingAboutContentControllerTest {
    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @Mock private lateinit var mockTelephonyFeatureProvider: TelephonyFeatureProvider
    @Mock private lateinit var mockTelephonySettingsRepository: TelephonySettingsRepository

    private lateinit var context: Context
    private lateinit var controller: SatelliteSettingAboutContentController
    private lateinit var screen: PreferenceScreen
    private lateinit var preference: TopIntroPreference
    private lateinit var fakeFeatureFactory: FakeFeatureFactory

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        fakeFeatureFactory = FakeFeatureFactory.setupForTest()
        val telephonyFeatureProvider = fakeFeatureFactory.mTelephonyFeatureProvider
        whenever(telephonyFeatureProvider.telephonyRepository)
            .thenReturn(mockTelephonySettingsRepository)

        whenever(mockTelephonySettingsRepository.getSimOperatorName(TEST_SUB_ID))
            .thenReturn(TEST_SIM_OPERATOR_NAME)

        context = ApplicationProvider.getApplicationContext<Context>()

        controller =
            SatelliteSettingAboutContentController(
                context = context,
                key = PREF_KEY_ABOUT_SATELLITE_CONNECTIVITY,
            )

        preference =
            TopIntroPreference(context).apply { key = PREF_KEY_ABOUT_SATELLITE_CONNECTIVITY }
        screen = PreferenceManager(context).createPreferenceScreen(context)
        screen.addPreference(preference)
    }

    @Test
    fun displayPreference_preferenceTitle_hasSimOperatorName() {
        controller.init(TEST_SUB_ID)

        controller.displayPreference(screen)

        assertThat(preference.title.toString())
            .isEqualTo(
                ResourcesUtils.getResourcesString(
                    context,
                    "description_about_satellite_setting",
                    TEST_SIM_OPERATOR_NAME,
                )
            )
    }

    private companion object {
        const val TEST_SUB_ID = 1
        const val TEST_SIM_OPERATOR_NAME = "Test Carrier"
    }
}
