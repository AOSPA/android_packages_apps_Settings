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
package com.android.settings.datausage

import android.content.pm.PackageInfo
import android.os.Bundle
import androidx.core.net.toUri
import com.android.settings.flags.Flags
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.android.settingslib.metadata.PreferenceMetadata
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.robolectric.Shadows.shadowOf

class DataUsageAppDetailScreenTest : SettingsCatalystTestCase() {
    init {
        shadowOf(appContext.packageManager)
            .installPackage(PackageInfo().apply { packageName = "com.test.package" })
    }

    private val preferenceScreenCreatorWithInvalidPackage =
        DataUsageAppDetailScreen(
            appContext,
            Bundle().apply { putString("app", "com.invalid.package") },
        )

    override val preferenceScreenCreator =
        DataUsageAppDetailScreen(
            appContext,
            Bundle().apply { putString("app", "com.test.package") },
        )

    override val flagName: String
        get() = Flags.FLAG_DEEPLINK_APPS_25Q4

    // TODO(b/427787504): enable when AppDataUsage fragment doesn't crash in a Robolectric test.
    override fun migration() {}

    @Test
    fun key() {
        assertThat(preferenceScreenCreator.key).isEqualTo(DataUsageAppDetailScreen.KEY)
    }

    @Test
    fun getLaunchIntent_correctActivity() {
        val underTest = preferenceScreenCreator.getLaunchIntent(appContext, null)

        assertThat(underTest.getComponent()?.getClassName())
            .isEqualTo(AppDataUsageActivity::class.java.getName())
        assertThat(underTest.data).isEqualTo("package:com.test.package".toUri())
    }

    @Test
    fun getLaunchIntent_correctActivityWithExtras() {
        val underTest =
            preferenceScreenCreator.getLaunchIntent(appContext, TestMetadata("testBindingKey"))

        assertThat(underTest.getBundleExtra("settingslib:binding_screen_args")?.getString("app"))
            .isEqualTo("com.test.package")
        assertThat(underTest.getStringExtra(":settings:fragment_args_key"))
            .isEqualTo("testBindingKey")
    }

    @Test
    fun isAvailable_whenPackageIsValid_isTrue() {
        assertThat(preferenceScreenCreator.isAvailable(appContext)).isTrue()
    }

    @Test
    fun isAvailable_whenPackageIsInvalid_isFalse() {
        assertThat(preferenceScreenCreatorWithInvalidPackage.isAvailable(appContext)).isFalse()
    }
}

private data class TestMetadata(
    override val bindingKey: String,
    override val key: String = "testKey",
) : PreferenceMetadata
