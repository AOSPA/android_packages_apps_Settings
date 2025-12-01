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

package com.android.settings.utils

import android.content.Context
import android.platform.test.flag.junit.SetFlagsRule
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [SettingsShadowResources::class])
class DesktopSettingsUtilsTest {

    @get:Rule
    val mSetFlagsRule = SetFlagsRule()

    private lateinit var mContext: Context

    @Before
    fun setUp() {
        mContext = RuntimeEnvironment.application
    }

    @Test
    fun shouldShowTopLevelDeviceCategory_flagOnAndConfigTrue_returnsTrue() {
        mSetFlagsRule.enableFlags(Flags.FLAG_SHOW_TOP_LEVEL_DEVICE_CATEGORY)
        SettingsShadowResources.overrideResource(R.bool.config_show_top_level_device_category, true)

        assertThat(DesktopSettingsUtils.shouldShowTopLevelDeviceCategory(mContext)).isTrue()
    }

    @Test
    fun shouldShowTopLevelDeviceCategory_flagOffAndConfigTrue_returnsFalse() {
        mSetFlagsRule.disableFlags(Flags.FLAG_SHOW_TOP_LEVEL_DEVICE_CATEGORY)
        SettingsShadowResources.overrideResource(R.bool.config_show_top_level_device_category, true)

        assertThat(DesktopSettingsUtils.shouldShowTopLevelDeviceCategory(mContext)).isFalse()
    }

    @Test
    fun shouldShowTopLevelDeviceCategory_flagOnAndConfigFalse_returnsFalse() {
        mSetFlagsRule.enableFlags(Flags.FLAG_SHOW_TOP_LEVEL_DEVICE_CATEGORY)
        SettingsShadowResources.overrideResource(R.bool.config_show_top_level_device_category, false)

        assertThat(DesktopSettingsUtils.shouldShowTopLevelDeviceCategory(mContext)).isFalse()
    }

    @Test
    fun shouldShowTopLevelDeviceCategory_flagOffAndConfigFalse_returnsFalse() {
        mSetFlagsRule.disableFlags(Flags.FLAG_SHOW_TOP_LEVEL_DEVICE_CATEGORY)
        SettingsShadowResources.overrideResource(R.bool.config_show_top_level_device_category, false)

        assertThat(DesktopSettingsUtils.shouldShowTopLevelDeviceCategory(mContext)).isFalse()
    }
}
