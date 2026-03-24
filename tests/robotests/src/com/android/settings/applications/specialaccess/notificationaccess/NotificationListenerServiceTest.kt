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

package com.android.settings.applications.specialaccess.notificationaccess

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.service.notification.NotificationListenerService as AndroidNotificationListenerService
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowPackageManager
import com.android.settingslib.metadata.preferencesapi.safe

@RunWith(RobolectricTestRunner::class)
class NotificationListenerServiceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var shadowPackageManager: ShadowPackageManager

    @Before
    fun setUp() {
        shadowPackageManager = Shadows.shadowOf(context.packageManager)
    }

    @Test
    fun getOptions_returnsExpectedList() = runTest {
        val packageName = "com.example"
        val className = "com.example.NotificationListener"
        val flattened = "$packageName/$className"
        val serviceInfo = ServiceInfo().apply {
            this.packageName = packageName
            this.name = className
            this.permission = Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE
        }
        val resolveInfo = ResolveInfo().apply {
            this.serviceInfo = serviceInfo
        }
        val intent = Intent(AndroidNotificationListenerService.SERVICE_INTERFACE)
        shadowPackageManager.addResolveInfoForIntent(intent, resolveInfo)

        val options = NotificationListenerService.getOptions(context)

        assertThat(options).hasSize(1)
        assertThat(options[0].first).isEqualTo(flattened.safe())
        assertThat(options[0].second).isEqualTo(flattened.safe())
    }

    @Test
    fun getType_returnsStringClass() {
        assertThat(NotificationListenerService.getType()).isEqualTo(String::class.java)
    }

    @Test
    fun getKey_returnsCorrectKey() {
        assertThat(NotificationListenerService.getKey()).isEqualTo("NotificationListenerService")
    }

    @Test
    fun getDescription_returnsCorrectDescription() {
        assertThat(NotificationListenerService.getDescription(context))
            .isEqualTo("The flattened string representation of a NotificationListenerService")
    }
}
