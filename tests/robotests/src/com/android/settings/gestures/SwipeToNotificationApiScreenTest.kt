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

package com.android.settings.gestures

import android.content.Context
import android.content.pm.PackageManager
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.Utils
import com.android.settings.flags.Flags
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settings.testutils2.ApiTester
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowPackageManager

@Config(shadows = [SettingsShadowResources::class])
@RunWith(AndroidJUnit4::class)
class SwipeToNotificationApiScreenTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private val tester = ApiTester(SwipeToNotificationApiScreen())
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val shadowPackageManager: ShadowPackageManager = shadowOf(context.packageManager)

    @Before
    fun setUp() {
        SettingsShadowResources.overrideResource(
            com.android.internal.R.bool.config_supportSystemNavigationKeys,
            true,
        )
        shadowPackageManager.setSystemFeature(PackageManager.FEATURE_FINGERPRINT, true)
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
    @Config(shadows = [ShadowUtils::class])
    fun getLaunchIntent_hasIntent() {
        assertThat(tester.getLaunchIntent()).isNotNull()
    }
}

@Implements(Utils::class)
class ShadowUtils {

    companion object {
        @Implementation @JvmStatic fun hasFingerprintHardware(context: Context): Boolean = true
    }
}
