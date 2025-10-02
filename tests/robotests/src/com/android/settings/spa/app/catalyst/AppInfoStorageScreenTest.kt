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

package com.android.settings.spa.app.catalyst

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowPackageManager

@RunWith(AndroidJUnit4::class)
class AppInfoStorageScreenTest {
    private lateinit var context: Context
    private lateinit var shadowPackageManager: ShadowPackageManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        shadowPackageManager = shadowOf(context.packageManager)
    }

    @Test
    fun isAvailable_whenAppIsNotFound_returnsFalse() {
        val screen =
            AppInfoStorageScreen(context, Bundle().apply { putString("app", "app.not.found") })

        assertThat(screen.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_whenAppIsFound_returnsTrue() {
        val packageInfo =
            PackageInfo().apply {
                packageName = "app.found"
                applicationInfo = ApplicationInfo().apply { packageName = "app.found" }
            }
        shadowPackageManager.installPackage(packageInfo)

        val screen = AppInfoStorageScreen(context, Bundle().apply { putString("app", "app.found") })

        assertThat(screen.isAvailable(context)).isTrue()
    }
}
