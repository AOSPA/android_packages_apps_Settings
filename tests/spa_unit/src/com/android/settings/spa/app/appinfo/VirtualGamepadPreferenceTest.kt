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

package com.android.settings.spa.app.appinfo

import android.app.AppGlobals
import android.app.compat.CompatChanges
import android.content.Context
import android.content.pm.ActivityInfo.OVERRIDE_ENABLE_VIRTUAL_GAMEPAD
import android.content.pm.ApplicationInfo
import android.content.pm.IPackageManager
import android.content.pm.PackageManager
import android.os.UserHandle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.android.settings.R
import com.android.settingslib.spa.testutils.delay
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when` as whenever
import org.mockito.MockitoSession
import org.mockito.Spy
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.verify
import org.mockito.quality.Strictness

/** To run this test: atest SettingsSpaUnitTests:VirtualGamepadPreferenceTest */
@RunWith(AndroidJUnit4::class)
class VirtualGamepadPreferenceTest {
    @get:Rule val composeTestRule = createComposeRule()

    private lateinit var mockSession: MockitoSession

    @Spy private val context: Context = ApplicationProvider.getApplicationContext()

    @Spy private val resources = context.resources

    @Mock private lateinit var packageManager: IPackageManager

    @Before
    fun setUp() {
        mockSession =
            ExtendedMockito.mockitoSession()
                .initMocks(this)
                .mockStatic(AppGlobals::class.java)
                .mockStatic(CompatChanges::class.java)
                .strictness(Strictness.LENIENT)
                .startMocking()
        whenever(context.resources).thenReturn(resources)
        whenever(AppGlobals.getPackageManager()).thenReturn(packageManager)
        whenever(packageManager.getVirtualGamepadUserOption(PACKAGE_NAME, context.userId))
            .thenReturn(PackageManager.VIRTUAL_GAMEPAD_USER_OPTION_UNSET)
    }

    @After
    fun tearDown() {
        mockSession.finishMocking()
    }

    @Test
    fun whenNotAvailableOnDevice_notDisplayed() {
        setVirtualGamepadAvailableOnDevice(false)
        setVirtualGamepadEnabledForApp(true)

        setContent()

        composeTestRule.onRoot().assertIsNotDisplayed()
    }

    @Test
    fun whenNotEnabledForApp_notDisplayed() {
        setVirtualGamepadAvailableOnDevice(true)
        setVirtualGamepadEnabledForApp(false)

        setContent()

        composeTestRule.onRoot().assertIsNotDisplayed()
    }

    @Test
    fun whenCanDisplay_Displayed() {
        setVirtualGamepadAvailableOnDevice(true)
        setVirtualGamepadEnabledForApp(true)

        setContent()

        composeTestRule
            .onNodeWithText(context.getString(R.string.virtual_gamepad_title))
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun onClick_changeUserOptOut() {
        setVirtualGamepadAvailableOnDevice(true)
        setVirtualGamepadEnabledForApp(true)

        setContent()

        // Click once to opt out
        composeTestRule
            .onNodeWithText(context.getString(R.string.virtual_gamepad_title))
            .performClick()
        verify(packageManager)
            .setVirtualGamepadUserOption(
                PACKAGE_NAME,
                context.userId,
                PackageManager.VIRTUAL_GAMEPAD_USER_OPTION_OPT_OUT,
            )

        clearInvocations(packageManager)

        // Click again to opt in
        composeTestRule
            .onNodeWithText(context.getString(R.string.virtual_gamepad_title))
            .performClick()
        verify(packageManager)
            .setVirtualGamepadUserOption(
                PACKAGE_NAME,
                context.userId,
                PackageManager.VIRTUAL_GAMEPAD_USER_OPTION_UNSET,
            )
    }

    private fun setVirtualGamepadAvailableOnDevice(available: Boolean) {
        whenever(
                resources.getString(
                    com.android.internal.R.string.config_virtual_gamepad_package_name
                )
            )
            .thenReturn(if (available) "gamepad_package_name" else "")

        whenever(
                resources.getString(
                    com.android.internal.R.string.config_virtual_gamepad_activity_name
                )
            )
            .thenReturn(if (available) "gamepad_activity_name" else "")
    }

    private fun setVirtualGamepadEnabledForApp(enabled: Boolean) {
        whenever(
                CompatChanges.isChangeEnabled(
                    OVERRIDE_ENABLE_VIRTUAL_GAMEPAD,
                    PACKAGE_NAME,
                    UserHandle.CURRENT,
                )
            )
            .thenReturn(enabled)
    }

    private fun setContent() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                VirtualGamepadPreference(APP)
            }
        }
        composeTestRule.delay()
    }

    private companion object {
        const val PACKAGE_NAME = "com.test.mypackage"
        const val UID = 123
        val APP =
            ApplicationInfo().apply {
                packageName = PACKAGE_NAME
                uid = UID
            }
    }
}
