/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.wifi.details2

import android.content.Context
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.core.BasePreferenceController.AVAILABLE
import com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE
import com.android.wifitrackerlib.WifiEntry
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class CertificateDetailsPreferenceControllerTest {
    @get:Rule
    val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        doNothing().whenever(mock).startActivity(any())
    }
    private val controller = CertificateDetailsPreferenceController(context, TEST_KEY)

    private val mockCertificateInfo = mock<WifiEntry.CertificateInfo>()
    private val mockWifiEntry =
        mock<WifiEntry> { on { certificateInfo } doReturn mockCertificateInfo }

    @Before
    fun setUp() {
        // Default setup for a network with one installed CA certificate
        mockCertificateInfo.apply {
            validationMethod =
                WifiEntry.CertificateInfo.CERTIFICATE_VALIDATION_METHOD_USING_INSTALLED_ROOTCA
            caCertificateAliases = arrayOf(MOCK_CA)
        }
        controller.setWifiEntry(mockWifiEntry)
    }

    @Test
    @RequiresFlagsEnabled(com.android.wifi.flags.Flags.FLAG_ANDROID_V_WIFI_API)
    fun title_isDisplayed() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                controller.Content()
            }
        }

        composeTestRule.onNodeWithText(
            context.getString(com.android.internal.R.string.ssl_certificate)
        ).assertIsDisplayed()
    }

    @Test
    @RequiresFlagsEnabled(com.android.wifi.flags.Flags.FLAG_ANDROID_V_WIFI_API)
    fun one_caCertificate_summary() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                controller.Content()
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.one_cacrt)).assertIsDisplayed()
    }

    @Test
    @RequiresFlagsEnabled(com.android.wifi.flags.Flags.FLAG_ANDROID_V_WIFI_API)
    fun summary_withMultipleCaCertificates_isCorrect() {
        // GIVEN a wifi entry with multiple installed CA certificates
        mockCertificateInfo.caCertificateAliases = arrayOf(MOCK_CA, "MOCK_CA_2")

        // WHEN the content is composed
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                controller.Content()
            }
        }

        // THEN the summary indicates the number of certificates
        val expectedSummary = context.getString(R.string.wifi_certificate_summary_Certificates, 2)
        composeTestRule.onNodeWithText(expectedSummary).assertIsDisplayed()
    }

    @Test
    @RequiresFlagsEnabled(com.android.wifi.flags.Flags.FLAG_ANDROID_V_WIFI_API)
    fun summary_withSystemCertificate_isCorrect() {
        // GIVEN a wifi entry using a system certificate
        mockCertificateInfo.validationMethod =
            WifiEntry.CertificateInfo.CERTIFICATE_VALIDATION_METHOD_USING_SYSTEM_CERTIFICATE

        // WHEN the content is composed
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                controller.Content()
            }
        }

        // THEN the correct summary is displayed
        composeTestRule.onNodeWithText(context.getString(R.string.wifi_certificate_summary_system))
            .assertIsDisplayed()
    }

    @Test
    @RequiresFlagsEnabled(com.android.wifi.flags.Flags.FLAG_ANDROID_V_WIFI_API)
    fun summary_withCertificatePinning_isCorrect() {
        // GIVEN a wifi entry using certificate pinning
        mockCertificateInfo.validationMethod =
            WifiEntry.CertificateInfo.CERTIFICATE_VALIDATION_METHOD_USING_CERTIFICATE_PINNING

        // WHEN the content is composed
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                controller.Content()
            }
        }

        // THEN the correct summary is displayed
        composeTestRule.onNodeWithText(context.getString(R.string.wifi_certificate_summary_pinning))
            .assertIsDisplayed()
    }

    @Test
    @RequiresFlagsEnabled(com.android.wifi.flags.Flags.FLAG_ANDROID_V_WIFI_API)
    fun summary_whenCertificateInfoIsNull_isCorrect() {
        // GIVEN a wifi entry with null certificate info (verifies NPE fix)
        whenever(mockWifiEntry.certificateInfo).thenReturn(null)

        // WHEN the content is composed
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                controller.Content()
            }
        }

        // THEN the default summary for pinning is shown, as validationMethod is null
        composeTestRule.onNodeWithText(context.getString(R.string.wifi_certificate_summary_pinning))
            .assertIsDisplayed()
    }

    @Test
    @RequiresFlagsEnabled(com.android.wifi.flags.Flags.FLAG_ANDROID_V_WIFI_API)
    fun getAvailabilityStatus_isAvailableForAllValidMethods() {
        // System certificate
        mockCertificateInfo.validationMethod =
            WifiEntry.CertificateInfo.CERTIFICATE_VALIDATION_METHOD_USING_SYSTEM_CERTIFICATE
        assertThat(controller.availabilityStatus).isEqualTo(AVAILABLE)

        // Installed root CA
        mockCertificateInfo.validationMethod =
            WifiEntry.CertificateInfo.CERTIFICATE_VALIDATION_METHOD_USING_INSTALLED_ROOTCA
        assertThat(controller.availabilityStatus).isEqualTo(AVAILABLE)

        // Certificate pinning
        mockCertificateInfo.validationMethod =
            WifiEntry.CertificateInfo.CERTIFICATE_VALIDATION_METHOD_USING_CERTIFICATE_PINNING
        assertThat(controller.availabilityStatus).isEqualTo(AVAILABLE)
    }

    @Test
    @RequiresFlagsEnabled(com.android.wifi.flags.Flags.FLAG_ANDROID_V_WIFI_API)
    fun getAvailabilityStatus_isUnavailableForInvalidMethod() {
        // GIVEN an unknown validation method
        mockCertificateInfo.validationMethod = -1

        // THEN the controller is unavailable
        assertThat(controller.availabilityStatus).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    @RequiresFlagsEnabled(com.android.wifi.flags.Flags.FLAG_ANDROID_V_WIFI_API)
    fun getAvailabilityStatus_whenCertificateInfoIsNull_isUnavailable() {
        // GIVEN a wifi entry with null certificate info (verifies NPE fix)
        whenever(mockWifiEntry.certificateInfo).thenReturn(null)

        // THEN the controller is unavailable
        assertThat(controller.availabilityStatus).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    private companion object {
        const val TEST_KEY = "test_key"
        const val MOCK_CA = "mock_ca"
    }
}