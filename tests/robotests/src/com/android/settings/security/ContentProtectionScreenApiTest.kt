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

package com.android.settings.security

import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.DeviceConfig
import android.view.contentcapture.ContentCaptureManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.internal.R.string.config_defaultContentProtectionService
import com.android.settings.flags.Flags.FLAG_CATALYST_MIGRATION_26Q2
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.FailedPreconditionException
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(shadows = [SettingsShadowResources::class])
class ContentProtectionScreenApiTest {

    private val tester: ApiTester = ApiTester(ContentProtectionScreenApi())

    @get:Rule val setFlagsRule = SetFlagsRule()

    @Before
    fun setUp() {
        SettingsShadowResources.overrideResource(
            config_defaultContentProtectionService,
            "com.test.package/TestClass",
        )
    }

    @After
    fun tearDown() {
        SettingsShadowResources.reset()
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagEnabled_isNotNull() {
        assertThat(tester.getScreen()).isNotNull()
    }

    @Test
    @DisableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagDisabled_isNull() {
        assertThat(tester.getScreen()).isNull()
    }

    @Test
    fun getIntent_contentProtectionEnabled_isNotNull() {
        setContentProtectionEnabled(true)

        assertThat(tester.getLaunchIntent()).isNotNull()
    }

    @Test(expected = FailedPreconditionException::class)
    fun getIntent_contentProtectionDisabled_throwsFailedPreconditionException() {
        setContentProtectionEnabled(false)

        tester.getLaunchIntent()
    }

    private fun setContentProtectionEnabled(enabled: Boolean) {
        DeviceConfig.setProperty(
            DeviceConfig.NAMESPACE_CONTENT_CAPTURE,
            ContentCaptureManager.DEVICE_CONFIG_PROPERTY_ENABLE_CONTENT_PROTECTION_RECEIVER,
            if (enabled) "true" else "false",
            /* makeDefault= */ false,
        )
    }
}
