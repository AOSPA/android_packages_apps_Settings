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

import android.os.Bundle
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.telephony.SubscriptionManager
import com.android.settings.Settings
import com.android.settings.flags.Flags
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.android.settings.utils.putSubId
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.robolectric.util.ReflectionHelpers

class DataUsageListScreenTest : SettingsCatalystTestCase() {

    @get:Rule val platformFlags = SetFlagsRule()

    override val preferenceScreenCreator =
        DataUsageListScreen(
            Bundle(1).also { it.putSubId(android.provider.Settings.EXTRA_SUB_ID, 1) }
        )

    override val flagName: String
        get() = Flags.FLAG_DEEPLINK_NETWORK_AND_INTERNET_25Q4

    private val testSubId = 2737
    private val invalidSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun subId_flagTrue_validString_parsedCorrectly() {
        val args =
            Bundle().apply {
                putString(android.provider.Settings.EXTRA_SUB_ID, testSubId.toString())
            }
        val screen = DataUsageListScreen(args)

        assertThat(screen.getSubId()).isEqualTo(testSubId)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun subId_flagTrue_invalidString_returnsDefault() {
        val args = Bundle().apply { putString(android.provider.Settings.EXTRA_SUB_ID, "invalid") }
        val screen = DataUsageListScreen(args)

        assertThat(screen.getSubId()).isEqualTo(invalidSubId)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun subId_flagTrue_missingKey_returnsDefault() {
        val args = Bundle()
        val screen = DataUsageListScreen(args)

        assertThat(screen.getSubId()).isEqualTo(invalidSubId)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun subId_flagTrue_subIdIsInt_returnsDefault() {
        val args = Bundle().apply { putInt(android.provider.Settings.EXTRA_SUB_ID, testSubId) }
        val screen = DataUsageListScreen(args)

        assertThat(screen.getSubId()).isEqualTo(invalidSubId)
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun subId_flagFalse_validInt_parsedCorrectly() {
        val args = Bundle().apply { putInt(android.provider.Settings.EXTRA_SUB_ID, testSubId) }
        val screen = DataUsageListScreen(args)

        assertThat(screen.getSubId()).isEqualTo(testSubId)
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun subId_flagFalse_subIdIsString_returnsDefault() {
        val args =
            Bundle().apply {
                putString(android.provider.Settings.EXTRA_SUB_ID, testSubId.toString())
            }
        val screen = DataUsageListScreen(args)

        assertThat(screen.getSubId()).isEqualTo(invalidSubId)
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun subId_flagFalse_missingKey_returnsDefault() {
        val args = Bundle()
        val screen = DataUsageListScreen(args)

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

    private fun DataUsageListScreen.getSubId(): Int {
        return ReflectionHelpers.getField(this, "subId")
    }

    // TODO(b/419311082): Migration test fails as a lot of telephony infra is not mocked.
    override fun migration() {}
}
