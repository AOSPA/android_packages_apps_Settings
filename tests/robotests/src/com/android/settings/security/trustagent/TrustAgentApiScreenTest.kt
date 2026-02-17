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

package com.android.settings.security.trustagent

import android.content.Context
import android.os.UserHandle
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.internal.widget.LockPatternUtils
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.security.SecurityFeatureProvider
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settings.testutils2.ApiTester
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.robolectric.annotation.Config

@Config(shadows = [SettingsShadowResources::class])
@RunWith(AndroidJUnit4::class)
class TrustAgentApiScreenTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private val mockLockPatternUtils = mock<LockPatternUtils>()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val tester = ApiTester(TrustAgentApiScreen())

    private val provider: SecurityFeatureProvider =
        FakeFeatureFactory.setupForTest().securityFeatureProvider

    @Before
    fun setUp() {
        SettingsShadowResources.overrideResource(R.bool.config_show_manage_trust_agents, true)
        provider.stub { on { getLockPatternUtils(context) } doReturn mockLockPatternUtils }
        mockLockPatternUtils.stub { on { isSecure(UserHandle.myUserId()) } doReturn true }
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagEnabled_isNotNull() {
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
}
