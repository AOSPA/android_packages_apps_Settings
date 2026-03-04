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

package com.android.settings.applications.specialaccess.notificationaccess

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.applications.specialaccess.notificationaccess.AppInfoNotificationAccessScreen.Companion.KEY_SERVICE
import com.android.settingslib.metadata.CatalystFlagProviderFactory
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class AppInfoNotificationAccessScreenTest {
    private val mockPackageManager: PackageManager = mock()
    private lateinit var context: Context

    @Before
    fun setUp() {
        context =
            spy(ApplicationProvider.getApplicationContext<Context>()) {
                on { packageManager } doReturn mockPackageManager
            }
    }

    @Test
    fun isAvailable_whenAppInfoIsNull_returnsFalse() {
        // Arrange: Configure the mock to throw an exception for the given package name.
        mockPackageManager.stub {
            on { getApplicationInfo("app.not.found", 0) } doThrow
                PackageManager.NameNotFoundException()
        }

        // Act: Create the screen, which will fail to find the app info.
        val screen = createScreen(packageName = "app.not.found")

        // Assert: isAvailable should be false.
        assertThat(screen.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_whenAppInfoIsNotNull_returnsTrue() {
        // Arrange: Configure the mock to return a valid ApplicationInfo object.
        mockPackageManager.stub {
            on { getApplicationInfo("app.found", 0) } doReturn ApplicationInfo()
        }

        // Act: Create the screen AFTER the mock has been configured.
        val screen = createScreen(packageName = "app.found")

        // Assert: isAvailable should be true.
        assertThat(screen.isAvailable(context)).isTrue()
    }

    private fun createScreen(packageName: String): AppInfoNotificationAccessScreen {
        return if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
            AppInfoNotificationAccessScreen(
                context,
                AppInfoNotificationAccessScreen.parametersSchema.prepare(
                    KEY_SERVICE to packageName + "/service"
                ),
            )
        } else {
            AppInfoNotificationAccessScreen(
                context,
                Bundle().apply {
                    putString("app", packageName)
                    putString("serviceName", "service")
                },
            )
        }
    }
}
