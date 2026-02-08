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
package com.android.settings.applications.intentpicker

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.applications.AppInfoBase.ARG_PACKAGE_NAME
import com.android.settings.flags.Flags
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.FailedPreconditionException
import com.android.settings.testutils2.Parameters
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowPackageManager
import org.robolectric.shadows.ShadowSystemProperties

@RunWith(AndroidJUnit4::class)
class AppLaunchApiScreenTest {

    @get:Rule val setFlagsRule = SetFlagsRule()

    private val tester = ApiTester(AppLaunchApiScreen())
    private lateinit var context: Context
    private lateinit var packageManager: ShadowPackageManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        packageManager = shadowOf(context.packageManager)
    }

    @After
    fun cleanUp() {
        ShadowPackageManager.reset()
        ShadowSystemProperties.reset()
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
    fun getLaunchIntent_installedApp_hasIntent() {
        installApp(FakeAppInfo())
        tester.initializeScreenParameters(Parameters(ARG_PACKAGE_NAME to PACKAGE_NAME))

        assertThat(tester.getLaunchIntent()).isNotNull()
    }

    @Test
    fun getLaunchIntent_nullApp_shouldThrowFailedPreconditionException() {
        installApp(null)
        tester.initializeScreenParameters(Parameters(ARG_PACKAGE_NAME to PACKAGE_NAME))

        assertThat(tester.getLaunchIntent()).isNotNull()
    }

    @Test
    fun getLaunchIntent_disabledApp_shouldThrowFailedPreconditionException() {
        installApp(FakeAppInfo(enabled = false))
        tester.initializeScreenParameters(Parameters(ARG_PACKAGE_NAME to PACKAGE_NAME))

        assertThrows(FailedPreconditionException::class.java) { tester.getLaunchIntent() }
    }

    @Test
    fun getLaunchIntent_instantApp_shouldThrowFailedPreconditionException() {
        ShadowSystemProperties.override("settingsdebug.instant.packages", PACKAGE_NAME)
        installApp(FakeAppInfo())
        tester.initializeScreenParameters(Parameters(ARG_PACKAGE_NAME to PACKAGE_NAME))

        assertThrows(FailedPreconditionException::class.java) { tester.getLaunchIntent() }
    }

    private fun installApp(appInfo: ApplicationInfo?) =
        packageManager.installPackage(
            PackageInfo().apply {
                packageName = PACKAGE_NAME
                applicationInfo = appInfo
            }
        )

    class FakeAppInfo(enabled: Boolean = true) : ApplicationInfo() {
        init {
            this.packageName = PACKAGE_NAME
            this.enabled = enabled
        }
    }

    companion object {
        const val PACKAGE_NAME = "com.abc"
    }
}
