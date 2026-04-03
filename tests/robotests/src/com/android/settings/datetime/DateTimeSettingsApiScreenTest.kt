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
package com.android.settings.datetime

import android.Manifest
import android.app.Application
import android.app.time.Capabilities
import android.app.time.TimeConfiguration
import android.app.time.TimeManager
import android.app.time.TimeState
import android.app.time.TimeZoneState
import android.app.time.UnixEpochTime
import android.content.Context
import android.os.SystemClock
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.flags.Flags
import com.android.settings.testutils2.ApiTester
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowTimeManager

@RunWith(AndroidJUnit4::class)
class DateTimeSettingsApiScreenTest {
    @get:Rule val flagsRule = SetFlagsRule()

    private lateinit var context: Context
    private lateinit var tester: ApiTester
    private lateinit var timeManager: TimeManager
    private lateinit var shadowTimeManager: ShadowTimeManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        tester = ApiTester(DateTimeSettingsApiScreen(), context)
        timeManager = context.getSystemService(TimeManager::class.java)!!
        shadowTimeManager = Shadow.extract<ShadowTimeManager>(timeManager)

        // Grant permission
        Shadows.shadowOf(context.applicationContext as Application)
            .grantPermissions(Manifest.permission.MANAGE_TIME_AND_ZONE_DETECTION)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_isNotNull() {
        assertThat(tester.getScreen()).isNotNull()
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagDisabled_isNull() {
        assertThat(tester.getScreen()).isNull()
    }

    @Test
    fun getLaunchIntent_hasIntent() {
        assertThat(tester.getLaunchIntent()).isNotNull()
    }

    @Test
    fun autoDateTime_get_returnsValue() {
        shadowTimeManager.setTimeCapabilityState(
            ShadowTimeManager.CONFIGURE_TIME_AUTO_DETECTION_ENABLED_CAPABILITY,
            Capabilities.CAPABILITY_POSSESSED,
        )

        val value = tester.get<Boolean>("auto_time")

        assertThat(value).isTrue()
    }

    @Test
    fun autoDateTime_set_updatesConfiguration() {
        shadowTimeManager.setTimeCapabilityState(
            ShadowTimeManager.CONFIGURE_TIME_AUTO_DETECTION_ENABLED_CAPABILITY,
            Capabilities.CAPABILITY_POSSESSED,
        )

        tester.set("auto_time", false)

        assertThat(shadowTimeManager.timeConfiguration.isAutoDetectionEnabled).isFalse()
    }

    @Test
    fun date_get_returnsValue() {
        setMockTimeAndZone("2026-03-23T12:00:00Z")

        val value = tester.get<String>("date")

        assertThat(value).isEqualTo("2026-03-23")
    }

    @Test
    fun date_set_updatesConfiguration() {
        shadowTimeManager.setTimeCapabilityState(
            ShadowTimeManager.CONFIGURE_TIME_AUTO_DETECTION_ENABLED_CAPABILITY,
            Capabilities.CAPABILITY_POSSESSED,
        )
        timeManager.updateTimeConfiguration(
            TimeConfiguration.Builder().setAutoDetectionEnabled(false).build()
        )

        setMockTimeAndZone("2026-03-22T00:00:00Z")

        tester.set("date", "2026-03-23")

        val manualTime = shadowTimeManager.lastManualTime as UnixEpochTime?
        assertThat(manualTime).isNotNull()
        val manualInstant = java.time.Instant.ofEpochMilli(manualTime!!.unixEpochTimeMillis)
        val zoneId = java.time.ZoneId.of("UTC")
        val manualLocalDate = manualInstant.atZone(zoneId).toLocalDate()

        assertThat(manualLocalDate.year).isEqualTo(2026)
        assertThat(manualLocalDate.monthValue).isEqualTo(3)
        assertThat(manualLocalDate.dayOfMonth).isEqualTo(23)
        assertThat(shadowTimeManager.lastConfirmedTime).isEqualTo(manualTime)
    }

    @Test
    fun time_get_returnsValue() {
        setMockTimeAndZone("2026-03-23T13:37:00Z")

        val value = tester.get<String>("time")

        assertThat(value).isEqualTo("13:37")
    }

    @Test
    fun time_set_updatesConfiguration() {
        shadowTimeManager.setTimeCapabilityState(
            ShadowTimeManager.CONFIGURE_TIME_AUTO_DETECTION_ENABLED_CAPABILITY,
            Capabilities.CAPABILITY_POSSESSED,
        )
        timeManager.updateTimeConfiguration(
            TimeConfiguration.Builder().setAutoDetectionEnabled(false).build()
        )

        setMockTimeAndZone("2026-03-23T12:00:00Z")

        tester.set("time", "13:37")

        val manualTime = shadowTimeManager.lastManualTime as UnixEpochTime?
        assertThat(manualTime).isNotNull()
        val manualInstant = java.time.Instant.ofEpochMilli(manualTime!!.unixEpochTimeMillis)
        val zoneId = java.time.ZoneId.of("UTC")
        val manualLocalTime = manualInstant.atZone(zoneId).toLocalTime().withSecond(0).withNano(0)

        assertThat(manualLocalTime.hour).isEqualTo(13)
        assertThat(manualLocalTime.minute).isEqualTo(37)
        assertThat(shadowTimeManager.lastConfirmedTime).isEqualTo(manualTime)
    }

    private fun setMockTimeAndZone(timeIso: String, zoneId: String = "UTC") {
        val currentInstant = java.time.Instant.parse(timeIso)
        val unixEpochTime =
            UnixEpochTime(SystemClock.elapsedRealtime(), currentInstant.toEpochMilli())
        shadowTimeManager.setTimeState(TimeState(unixEpochTime, false))
        shadowTimeManager.setTimeZoneState(TimeZoneState(zoneId, false))
    }
}
