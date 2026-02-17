/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.datausage

import android.content.Context
import android.os.Bundle
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.telephony.SubscriptionManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.Settings
import com.android.settings.flags.Flags
import com.android.settings.utils.putSubId
import com.android.settingslib.metadata.CatalystFlagProvider
import com.android.settingslib.metadata.CatalystFlagProviderFactory
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.util.ReflectionHelpers

@RunWith(AndroidJUnit4::class)
class DataUsageListScreenTest {
    @get:Rule val platformFlags = SetFlagsRule()

    private val appContext: Context = ApplicationProvider.getApplicationContext()

    private lateinit var originalProvider: CatalystFlagProvider
    private lateinit var preferenceScreenCreator: DataUsageListScreen

    private val testSubId = 2737
    private val invalidSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID

    @Before
    fun setUp() {
        originalProvider = CatalystFlagProviderFactory.getInstance()
        preferenceScreenCreator = createScreen(Bundle(1).also { it.putSubId(android.provider.Settings.EXTRA_SUB_ID, 1) })
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
    @EnableFlags(Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun subId_flagTrue_validString_parsedCorrectly() {
        setCatalystUseKeyParameters(true)
        val args =
            Bundle().apply {
                putString(android.provider.Settings.EXTRA_SUB_ID, testSubId.toString())
            }
        val screen = createScreen(args)

        assertThat(screen.getSubId()).isEqualTo(testSubId)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun subId_flagTrue_invalidString_returnsDefault() {
        setCatalystUseKeyParameters(true)
        val args = Bundle().apply { putString(android.provider.Settings.EXTRA_SUB_ID, "invalid") }
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
    fun subId_flagTrue_subIdIsInt_returnsTheSubIdFromInt() {
        setCatalystUseKeyParameters(true)
        val args = Bundle().apply { putSubId(android.provider.Settings.EXTRA_SUB_ID, testSubId) }
        val screen = createScreen(args)

        assertThat(screen.getSubId()).isEqualTo(testSubId)
    }

    @Test
    @DisableFlags(
        Flags.FLAG_CATALYST_USE_STRING_BUNDLE
    )
    fun subId_flagFalse_validInt_parsedCorrectly() {
        setCatalystUseKeyParameters(false)
        val args = Bundle().apply { putInt(android.provider.Settings.EXTRA_SUB_ID, testSubId) }
        val screen = createScreen(args)

        assertThat(screen.getSubId()).isEqualTo(testSubId)
    }

    @Test
    @DisableFlags(
        Flags.FLAG_CATALYST_USE_STRING_BUNDLE
    )
    fun subId_flagFalse_subIdIsString_returnsTheSubIdFromString() {
        setCatalystUseKeyParameters(false)
        val args =
            Bundle().apply {
                putString(android.provider.Settings.EXTRA_SUB_ID, testSubId.toString())
            }
        val screen = createScreen(args)

        assertThat(screen.getSubId()).isEqualTo(testSubId)
    }

    @Test
    @DisableFlags(
        Flags.FLAG_CATALYST_USE_STRING_BUNDLE
    )
    fun subId_flagFalse_missingKey_returnsDefault() {
        setCatalystUseKeyParameters(false)
        val args = Bundle()
        val screen = createScreen(args)

        assertThat(screen.getSubId()).isEqualTo(invalidSubId)
    }

    @Test
    fun getLaunchIntent_createsCorrectIntent() {
        val intent = preferenceScreenCreator.getLaunchIntent(appContext, null)

        assertThat(intent).isNotNull()
        assertThat(intent?.component?.className)
            .isEqualTo(Settings.MobileDataUsageListActivity::class.java.name)
        assertThat(intent?.extras?.getInt(android.provider.Settings.EXTRA_SUB_ID)).isEqualTo(1)
    }

    private fun createScreen(args: Bundle): DataUsageListScreen {
        return if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
            DataUsageListScreen(DataUsageListScreen.parametersSchema.prepare(args))
        } else {
            DataUsageListScreen(args)
        }
    }

    private fun DataUsageListScreen.getSubId(): Int {
        return ReflectionHelpers.getField(this, "subId")
    }
}
