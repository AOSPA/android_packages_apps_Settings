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

package com.android.settings.datetime.timezone

import android.content.Context
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import android.timezone.flags.Flags
import com.android.settings.core.BasePreferenceController
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnit
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ClearHomeTimeZonePreferenceControllerTest {

    @get:Rule val mSetFlagsRule = SetFlagsRule()
    @get:Rule val mMockitoRule = MockitoJUnit.rule()

    @Mock private lateinit var mFragment: HomeTimeZoneSettings

    private lateinit var mContext: Context
    private lateinit var mController: ClearHomeTimeZonePreferenceController

    @Before
    fun setUp() {
        mContext = RuntimeEnvironment.getApplication()
        mController = ClearHomeTimeZonePreferenceController(mContext, "test_key")
        mController.setParentFragment(mFragment)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_HOME_TIME_ZONE_API)
    fun getAvailabilityStatus_flagOn_shouldBeAvailable() {
        assertThat(mController.getAvailabilityStatus())
            .isEqualTo(BasePreferenceController.AVAILABLE)
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_HOME_TIME_ZONE_API)
    fun getAvailabilityStatus_flagOff_shouldBeUnsupported() {
        assertThat(mController.getAvailabilityStatus())
            .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE)
    }

    @Test
    fun isChecked_homeZoneNotCleared_shouldReturnTrue() {
        Settings.Global.putString(
            mContext.contentResolver,
            Settings.Global.HOME_TIME_ZONE_ID,
            "America/Los_Angeles",
        )
        assertThat(mController.isChecked).isTrue()

        // Null signals that the home zone has not been set yet.
        Settings.Global.putString(mContext.contentResolver, Settings.Global.HOME_TIME_ZONE_ID, null)
        assertThat(mController.isChecked).isTrue()
    }

    @Test
    fun isChecked_homeZoneCleared_shouldReturnFalse() {
        // Empty string signals that the home zone has been cleared by the user.
        Settings.Global.putString(mContext.contentResolver, Settings.Global.HOME_TIME_ZONE_ID, "")
        assertThat(mController.isChecked).isFalse()
    }

    @Test
    fun setChecked_true_shouldUpdatePreference() {
        mController.setChecked(true)
        verify(mFragment, never()).setHomeTimeZone("")
    }

    @Test
    fun setChecked_false_shouldClearZoneAndupdatePreference() {
        mController.setChecked(false)
        verify(mFragment).setHomeTimeZone("")
    }
}
