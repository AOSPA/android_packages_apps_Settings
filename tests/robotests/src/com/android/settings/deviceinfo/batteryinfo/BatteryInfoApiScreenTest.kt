/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.android.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.deviceinfo.batteryinfo

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.fuelgauge.BatterySettingsFeatureProvider
import com.android.settings.fuelgauge.BatteryUtils
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settings.testutils2.ApiTester
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowBatteryManager

@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowBatteryManagerOverride::class])
class BatteryInfoApiScreenTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private lateinit var apiTester: ApiTester
    private lateinit var context: Context
    private lateinit var shadowBatteryManager: ShadowBatteryManagerOverride
    private lateinit var batterySettingsFeatureProvider: BatterySettingsFeatureProvider

    @Before
    fun setUp() {
        context = spy(RuntimeEnvironment.getApplication())
        batterySettingsFeatureProvider =
            FakeFeatureFactory.setupForTest().batterySettingsFeatureProvider
        doReturn(true).whenever(batterySettingsFeatureProvider).isBatteryInfoEnabled(any())

        apiTester = ApiTester(BatteryInfoApiScreen(), context)

        val batteryManager = context.getSystemService(BatteryManager::class.java)!!
        shadowBatteryManager = shadowOf(batteryManager) as ShadowBatteryManagerOverride
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagEnabled_isNotNull() {
        assertThat(apiTester.getScreen()).isNotNull()
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagDisabled_isNull() {
        assertThat(apiTester.getScreen()).isNull()
    }

    @Test
    fun getLaunchIntent_hasIntent() {
        assertThat(apiTester.getLaunchIntent()).isNotNull()
    }

    @Test
    fun getManufactureDatePreference_returnsFormattedDate() {
        val timestampSeconds = 1715856000L
        val expectedDate = "2024-05-16"
        shadowBatteryManager.setProperty(
            BatteryManager.BATTERY_PROPERTY_MANUFACTURING_DATE,
            timestampSeconds,
        )

        assertThat(apiTester.get<String>(BatteryInfoApiScreen.MANUFACTURE_DATE_KEY))
            .isEqualTo(expectedDate)
    }

    @Test
    fun getFirstUseDatePreference_returnsFormattedDate() {
        val timestampSeconds = 1715857000L
        val expectedDate = "2024-05-16"
        shadowBatteryManager.setProperty(
            BatteryManager.BATTERY_PROPERTY_FIRST_USAGE_DATE,
            timestampSeconds,
        )

        assertThat(apiTester.get<String>(BatteryInfoApiScreen.FIRST_USE_DATE_KEY))
            .isEqualTo(expectedDate)
    }

    @Test
    fun getCycleCountPreference_hasValue_returnsValue() {
        val cycleCount = 42
        val batteryIntent =
            Intent(Intent.ACTION_BATTERY_CHANGED).apply {
                putExtra(BatteryManager.EXTRA_CYCLE_COUNT, cycleCount)
            }
        doReturn(batteryIntent).whenever(context).registerReceiver(eq(null), any())

        assertThat(apiTester.get<Integer>(BatteryInfoApiScreen.CYCLE_COUNT_KEY))
            .isEqualTo(cycleCount)
    }

    @Test
    fun getCycleCountPreference_noValue_errors() {
        val batteryIntent = Intent(Intent.ACTION_BATTERY_CHANGED)
        doReturn(batteryIntent).whenever(context).registerReceiver(eq(null), any())

        assertFailsWith<IllegalStateException> {
            apiTester.get<Integer>(BatteryInfoApiScreen.CYCLE_COUNT_KEY)
        }
    }
}

/* ShadowBatteryManager does not expose BATTERY_PROPERTY_MANUFACTURING_DATE and
 * BATTERY_PROPERTY_FIRST_USAGE_DATE. Creating an override for the shadow.
 */
@Implements(BatteryManager::class)
class ShadowBatteryManagerOverride : ShadowBatteryManager() {
    private val properties = mutableMapOf<Int, Long>()

    @Implementation
    override fun getLongProperty(property: Int): Long {
        return properties[property] ?: -1L
    }

    fun setProperty(property: Int, value: Long) {
        properties[property] = value
    }
}
