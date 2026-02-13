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

package com.android.settings.safetycenter

import android.content.Context
import android.os.ServiceManager
import android.os.UserHandle
import android.os.UserManager
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.flags.Flags
import com.android.settings.safetycenter.ui.MoreSecurityPrivacyScreenApi
import com.android.settings.testutils.shadow.ShadowUserManager
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.FailedPreconditionException
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowUserManager::class])
class MoreSecurityPrivacyScreenApiTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val tester = ApiTester(MoreSecurityPrivacyScreenApi())
    private lateinit var userManager: UserManager

    @Before
    fun setUp() {
        userManager = context.getSystemService(UserManager::class.java)!!

        // Default ServiceManager to allow ContentCapture
        ServiceManager.addService(
            Context.CONTENT_CAPTURE_MANAGER_SERVICE,
            mock(android.os.IBinder::class.java),
        )
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun mediaControlsToggle_getAndSet_works() {
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.MEDIA_CONTROLS_LOCK_SCREEN,
            0,
        )
        assertThat(tester.get<Boolean>("privacy_media_controls_lockscreen")).isFalse()

        tester.set("privacy_media_controls_lockscreen", true)
        assertThat(
                Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.MEDIA_CONTROLS_LOCK_SCREEN,
                )
            )
            .isEqualTo(1)
        assertThat(tester.get<Boolean>("privacy_media_controls_lockscreen")).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun cameraExtensionsToggle_getAndSet_works() {
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.CAMERA_EXTENSIONS_FALLBACK,
            0,
        )
        assertThat(tester.get<Boolean>("privacy_camera_extensions_fallback")).isFalse()

        tester.set("privacy_camera_extensions_fallback", true)
        assertThat(
                Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.CAMERA_EXTENSIONS_FALLBACK,
                )
            )
            .isEqualTo(1)
        assertThat(tester.get<Boolean>("privacy_camera_extensions_fallback")).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun contentCapture_available_getAndSet_works() {
        // Ensure feature is available (mock service exists) and restriction is NOT set
        Shadows.shadowOf(userManager)
            .setUserRestriction(
                UserHandle.of(UserHandle.myUserId()),
                UserManager.DISALLOW_CONTENT_CAPTURE,
                false,
            )

        Settings.Secure.putInt(context.contentResolver, Settings.Secure.CONTENT_CAPTURE_ENABLED, 0)

        assertThat(tester.get<Boolean>("content_capture")).isFalse()

        tester.set("content_capture", true)
        assertThat(
                Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.CONTENT_CAPTURE_ENABLED,
                )
            )
            .isEqualTo(1)
        assertThat(tester.get<Boolean>("content_capture")).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun contentCapture_restricted_unavailable() {
        Shadows.shadowOf(userManager)
            .setUserRestriction(
                UserHandle.of(UserHandle.myUserId()),
                UserManager.DISALLOW_CONTENT_CAPTURE,
                true,
            )

        assertFailsWith<FailedPreconditionException> { tester.get<Boolean>("content_capture") }
    }
}
