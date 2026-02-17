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

package com.android.settings.network.apn

import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.os.PersistableBundle
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.Settings.ApnSettingsActivity
import com.android.settings.flags.Flags
import com.android.settings.network.CarrierConfigCache
import com.android.settings.utils.putSubId
import com.android.settingslib.metadata.CatalystFlagProvider
import com.android.settingslib.metadata.CatalystFlagProviderFactory
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.robolectric.util.ReflectionHelpers

@RunWith(AndroidJUnit4::class)
class ApnSettingsScreenTest {
    private val subId = 42
    private val invalidSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID

    @get:Rule val platformFlags = SetFlagsRule()

    private val appContext: Context = ApplicationProvider.getApplicationContext()

    private lateinit var originalProvider: CatalystFlagProvider
    private lateinit var preferenceScreenCreator: ApnSettingsScreen

    private val mockTelephonyManager = mock<TelephonyManager>()

    private val mockCarrierConfigCache = mock<CarrierConfigCache>()

    private val context =
        object : ContextWrapper(appContext) {
            override fun getSystemService(name: String): Any =
                when (name) {
                    TELEPHONY_SERVICE -> mockTelephonyManager
                    else -> super.getSystemService(name)
                }
        }

    @Before
    fun setup() {
        originalProvider = CatalystFlagProviderFactory.getInstance()
        mockTelephonyManager.stub {
            on { createForSubscriptionId(subId) } doReturn mockTelephonyManager
        }
        CarrierConfigCache.setTestInstance(context, mockCarrierConfigCache)
        preferenceScreenCreator = createScreen(Bundle().apply { putSubId(ApnSettings.SUB_ID, subId) })
    }

    @After
    fun tearDown() {
        CatalystFlagProviderFactory.setProvider(originalProvider)
    }

    private fun setCatalystUseKeyParameters(value: Boolean) {
        CatalystFlagProviderFactory.setProvider(object : CatalystFlagProvider {
            override fun catalystUseKeyParameters() = value
        })
    }

    @Test
    fun key_isEqualToStatic() {
        assertThat(preferenceScreenCreator.key).isEqualTo(ApnSettingsScreen.KEY)
    }

    @Test
    fun getLaunchIntent_correctActivity() {
        val underTest = preferenceScreenCreator.getLaunchIntent(appContext, null)

        assertThat(underTest.component?.className)
            .isEqualTo(ApnSettingsActivity::class.java.getName())
    }

    @Test
    fun isAvailable_apnSettingsSupportedWithGsm_returnTrue() {
        mockTelephonyManager.stub { on { phoneType } doReturn TelephonyManager.PHONE_TYPE_GSM }
        val bundle = PersistableBundle()
        bundle.putBoolean(CarrierConfigManager.KEY_APN_EXPAND_BOOL, true)
        mockCarrierConfigCache.stub { on { getConfigForSubId(subId) } doReturn bundle }

        assertThat(preferenceScreenCreator.isAvailable(context)).isEqualTo(true)
    }

    @Test
    fun isAvailable_carrierConfigNull_returnFalse() {
        mockTelephonyManager.stub { on { phoneType } doReturn TelephonyManager.PHONE_TYPE_GSM }
        mockCarrierConfigCache.stub { on { getConfigForSubId(subId) } doReturn null }

        assertThat(preferenceScreenCreator.isAvailable(context)).isEqualTo(false)
    }

    @Test
    fun isAvailable_hideCarrierNetworkSettings_returnFalse() {
        mockTelephonyManager.stub { on { phoneType } doReturn TelephonyManager.PHONE_TYPE_GSM }
        val bundle = PersistableBundle()
        bundle.putBoolean(CarrierConfigManager.KEY_APN_EXPAND_BOOL, true)
        bundle.putBoolean(CarrierConfigManager.KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL, true)
        mockCarrierConfigCache.stub { on { getConfigForSubId(subId) } doReturn bundle }

        assertThat(preferenceScreenCreator.isAvailable(context)).isEqualTo(false)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun subId_flagTrue_validString_parsedCorrectly() {
        setCatalystUseKeyParameters(true)
        val args = Bundle().apply { putString(ApnSettings.SUB_ID, subId.toString()) }
        val screen = createScreen(args)

        assertThat(screen.getSubId()).isEqualTo(subId)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun subId_flagTrue_invalidString_returnsDefault() {
        setCatalystUseKeyParameters(true)
        val args = Bundle().apply { putString(ApnSettings.SUB_ID, "invalid") }
        val screen = createScreen(args)

        assertThat(screen.getSubId()).isEqualTo(invalidSubId)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun subId_flagTrue_missingKey_returnsDefault() {
        setCatalystUseKeyParameters(true)
        val args = Bundle()
        val screen = createScreen(args)

        assertThat(screen.getSubId()).isEqualTo(invalidSubId)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun subId_flagTrue_subIdIsInt_returnsTheSubId() {
        setCatalystUseKeyParameters(true)
        val args = Bundle().apply { putSubId(ApnSettings.SUB_ID, subId) }
        val screen = createScreen(args)

        assertThat(screen.getSubId()).isEqualTo(subId)
    }

    @Test
    @DisableFlags(
        Flags.FLAG_CATALYST_USE_STRING_BUNDLE
    )
    fun subId_flagFalse_validInt_parsedCorrectly() {
        setCatalystUseKeyParameters(false)
        val args = Bundle().apply { putSubId(ApnSettings.SUB_ID, subId) }
        val screen = createScreen(args)

        assertThat(screen.getSubId()).isEqualTo(subId)
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun subId_flagFalse_missingKey_returnsDefault() {
        setCatalystUseKeyParameters(false)
        val args = Bundle()
        val screen = createScreen(args)

        assertThat(screen.getSubId()).isEqualTo(invalidSubId)
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun subId_flagFalse_subIdIsString_returnsTheSubId() {
        setCatalystUseKeyParameters(false)
        val args = Bundle().apply { putString(ApnSettings.SUB_ID, subId.toString()) }
        val screen = createScreen(args)

        assertThat(screen.getSubId()).isEqualTo(subId)
    }

    private fun ApnSettingsScreen.getSubId(): Int {
        return ReflectionHelpers.getField(this, "subId")
    }

    private fun createScreen(args: Bundle): ApnSettingsScreen {
        return if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
            ApnSettingsScreen(ApnSettingsScreen.parametersSchema.prepare(args))
        } else {
            ApnSettingsScreen(args)
        }
    }
}
