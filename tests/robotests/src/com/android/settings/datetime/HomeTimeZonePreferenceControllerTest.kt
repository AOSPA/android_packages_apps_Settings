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

import android.content.Context
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import android.timezone.flags.Flags
import com.android.settings.R
import com.android.settings.core.BasePreferenceController.AVAILABLE
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.spy
import org.mockito.Spy
import org.mockito.junit.MockitoJUnit
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
@EnableFlags(Flags.FLAG_ENABLE_HOME_TIME_ZONE_API)
class HomeTimeZonePreferenceControllerTest {

    @get:Rule val mMockitoRule = MockitoJUnit.rule()
    @get:Rule val mSetFlagsRule = SetFlagsRule()

    @Spy private lateinit var mContext: Context
    private lateinit var mController: HomeTimeZonePreferenceController

    @Before
    fun setUp() {
        mContext = spy(RuntimeEnvironment.getApplication())
        mController = HomeTimeZonePreferenceController(mContext, "test_key")
    }

    @Test
    fun getAvailabilityStatus_shouldBeAvailable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE)
    }

    @Test
    fun getSummary_homeTimeZoneNotSet_shouldReturnDefaultSummary() {
        Settings.Global.putString(mContext.contentResolver, Settings.Global.HOME_TIME_ZONE_ID, null)
        assertThat(mController.getSummary())
            .isEqualTo(mContext.getString(R.string.home_time_zone_summary))
    }

    @Test
    fun getSummary_homeTimeZoneSet_shouldReturnFormattedTimeZone() {
        val timeZoneId = "America/Los_Angeles"
        Settings.Global.putString(
            mContext.contentResolver,
            Settings.Global.HOME_TIME_ZONE_ID,
            timeZoneId,
        )

        // The expected summary is locale-dependent, so we just check that it's not the default.
        assertThat(mController.getSummary())
            .isNotEqualTo(mContext.getString(R.string.home_time_zone_summary))
    }
}
