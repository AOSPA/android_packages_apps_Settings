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
import android.hardware.SensorPrivacyManager
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import android.safetycenter.SafetyCenterManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.flags.Flags
import com.android.settings.safetycenter.SafetyCenterTestUtils.EMPTY_SC_DATA
import com.android.settings.safetycenter.SafetyCenterTestUtils.createEntry
import com.android.settings.safetycenter.SafetyCenterTestUtils.createScData
import com.android.settings.safetycenter.ui.PrivacyControlsScreenApi
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.FailedPreconditionException
import com.android.settings.utils.SensorPrivacyManagerHelper
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowSafetyCenterManager
import org.robolectric.util.ReflectionHelpers

@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowSafetyCenterManager::class])
class PrivacyControlsScreenApiTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val tester = ApiTester(PrivacyControlsScreenApi())
    private lateinit var mockHelper: SensorPrivacyManagerHelper
    private lateinit var shadowSafetyCenterManager: ShadowSafetyCenterManager

    @Before
    fun setUp() {
        mockHelper = mock(SensorPrivacyManagerHelper::class.java)
        ReflectionHelpers.setStaticField(
            SensorPrivacyManagerHelper::class.java,
            "sInstance",
            mockHelper,
        )

        val safetyCenterManager = context.getSystemService(SafetyCenterManager::class.java)!!
        shadowSafetyCenterManager = Shadow.extract(safetyCenterManager)
        shadowSafetyCenterManager.setSafetyCenterEnabled(true)

        // Populate with at least one entry to ensure the screen is considered available
        shadowSafetyCenterManager.setSafetyCenterData(
            createScData(
                entries =
                    listOf(
                        createEntry(
                            id = "default_entry",
                            title = "Default Entry",
                            sourceId = "AndroidHealthConnect",
                        )
                    )
            )
        )
    }

    @After
    fun tearDown() {
        ReflectionHelpers.setStaticField(SensorPrivacyManagerHelper::class.java, "sInstance", null)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2, Flags.FLAG_ENABLE_SAFETY_CENTER_NEW_UI)
    fun getScreen_allFlagsEnabled_isNotNull() {
        assertThat(tester.getScreen()).isNotNull()
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    @EnableFlags(Flags.FLAG_ENABLE_SAFETY_CENTER_NEW_UI)
    fun getScreen_catalystFlagDisabled_isNull() {
        assertThat(tester.getScreen()).isNull()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2, Flags.FLAG_ENABLE_SAFETY_CENTER_NEW_UI)
    fun cameraToggle_getAndSet_works() {
        `when`(mockHelper.supportsSensorToggle(SensorPrivacyManager.Sensors.CAMERA))
            .thenReturn(true)
        // isSensorBlocked = false means privacy NOT enabled (toggle ON)
        `when`(mockHelper.isSensorBlocked(sensor = SensorPrivacyManager.Sensors.CAMERA))
            .thenReturn(false)

        assertThat(tester.get<Boolean>("privacy_camera_toggle")).isTrue()

        tester.set("privacy_camera_toggle", false)
        // setting toggle to false means blocking sensor (true)
        verify(mockHelper).setSensorBlocked(SensorPrivacyManager.Sensors.CAMERA, true)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2, Flags.FLAG_ENABLE_SAFETY_CENTER_NEW_UI)
    fun micToggle_getAndSet_works() {
        `when`(mockHelper.supportsSensorToggle(SensorPrivacyManager.Sensors.MICROPHONE))
            .thenReturn(true)
        // isSensorBlocked = false means privacy NOT enabled (toggle ON)
        `when`(mockHelper.isSensorBlocked(sensor = SensorPrivacyManager.Sensors.MICROPHONE))
            .thenReturn(false)

        assertThat(tester.get<Boolean>("privacy_mic_toggle")).isTrue()

        tester.set("privacy_mic_toggle", false)
        // setting toggle to false means blocking sensor (true)
        verify(mockHelper).setSensorBlocked(SensorPrivacyManager.Sensors.MICROPHONE, true)
        `when`(mockHelper.isSensorBlocked(sensor = SensorPrivacyManager.Sensors.MICROPHONE))
            .thenReturn(true)
        assertThat(tester.get<Boolean>("privacy_mic_toggle")).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2, Flags.FLAG_ENABLE_SAFETY_CENTER_NEW_UI)
    fun clipboardToggle_getAndSet_works() {
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.CLIPBOARD_SHOW_ACCESS_NOTIFICATIONS,
            0,
        )
        assertThat(tester.get<Boolean>("show_clip_access_notification")).isFalse()

        tester.set("show_clip_access_notification", true)
        assertThat(
                Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.CLIPBOARD_SHOW_ACCESS_NOTIFICATIONS,
                )
            )
            .isEqualTo(1)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2, Flags.FLAG_ENABLE_SAFETY_CENTER_NEW_UI)
    fun showPasswordToggle_getAndSet_works() {
        Settings.System.putInt(context.contentResolver, Settings.System.TEXT_SHOW_PASSWORD, 0)
        assertThat(tester.get<Boolean>("show_password")).isFalse()

        tester.set("show_password", true)
        assertThat(
                Settings.System.getInt(context.contentResolver, Settings.System.TEXT_SHOW_PASSWORD)
            )
            .isEqualTo(1)
    }

}
