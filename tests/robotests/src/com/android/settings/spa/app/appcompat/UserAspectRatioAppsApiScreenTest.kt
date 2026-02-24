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
package com.android.settings.spa.app.appcompat

import android.content.Context
import android.content.res.Resources
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.flags.Flags
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.FailedPreconditionException
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when` as whenever
import org.mockito.Spy
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule

@RunWith(AndroidJUnit4::class)
class UserAspectRatioAppsApiScreenTest {
    @JvmField @Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()
    @get:Rule val setFlagsRule = SetFlagsRule()
    @Spy private var context: Context = ApplicationProvider.getApplicationContext()

    @Mock private lateinit var resources: Resources

    private lateinit var tester: ApiTester

    @Before
    fun setUp() {
        tester = ApiTester(UserAspectRatioAppsApiScreen(), context)
        whenever(context.resources).thenReturn(resources)
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
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getLaunchIntent_featureDisabled_throwsException() {
        whenever(
                resources.getBoolean(
                    com.android.internal.R.bool.config_appCompatUserAppAspectRatioSettingsIsEnabled
                )
            )
            .thenReturn(false)

        assertThrows(FailedPreconditionException::class.java) { tester.getLaunchIntent() }
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getLaunchIntent_featureEnabled_isNotNull() {
        whenever(
                resources.getBoolean(
                    com.android.internal.R.bool.config_appCompatUserAppAspectRatioSettingsIsEnabled
                )
            )
            .thenReturn(true)

        assertThat(tester.getLaunchIntent()).isNotNull()
    }
}
