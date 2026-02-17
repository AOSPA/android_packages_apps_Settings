/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.app.AppOpsManager
import android.app.AppOpsManager.MODE_ALLOWED
import android.app.AppOpsManager.MODE_DEFAULT
import android.app.AppOpsManager.MODE_IGNORED
import android.app.AppOpsManager.OP_CONTINUE_ACROSS_DEVICES
import android.companion.CompanionDeviceManager.FLAG_TASK_CONTINUITY
import android.companion.datatransfer.continuity.TaskContinuityManager
import android.companion.datatransfer.continuity.TaskContinuityManager.HANDOFF_AVAILABILITY_STATUS_AVAILABLE
import android.companion.datatransfer.continuity.TaskContinuityManager.HANDOFF_AVAILABILITY_STATUS_DISABLED_BY_POLICY
import android.companion.datatransfer.continuity.TaskContinuityManager.HandoffFeatureStateListener
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.testutils.mockAsUser
import com.android.settingslib.spaprivileged.framework.common.appOpsManager
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenever
import org.mockito.Spy
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule

@RunWith(AndroidJUnit4::class)
class ContinueAcrossDevicesSwitchPreferenceTest {

    @get:Rule val mockito: MockitoRule = MockitoJUnit.rule()

    @get:Rule val composeTestRule = createComposeRule()

    @get:Rule val setFlagsRule: SetFlagsRule = SetFlagsRule()

    @Spy private val context: Context = ApplicationProvider.getApplicationContext()

    @Mock private lateinit var appOpsManager: AppOpsManager

    @Mock private lateinit var taskContinuityManager: TaskContinuityManager

    private val isContinueAcrossDevicesSwitchEnabledStateFlow = MutableStateFlow(true)

    @Before
    fun setUp() {
        context.mockAsUser()
        whenever(context.appOpsManager).thenReturn(appOpsManager)
        whenever(context.getSystemService(TaskContinuityManager::class.java))
            .thenReturn(taskContinuityManager)
        mockHandoffEnablementStatus(TaskContinuityManager.HANDOFF_AVAILABILITY_STATUS_AVAILABLE)
    }

    private fun mockHandoffEnablementStatus(enablementStatus: Int) {
        doAnswer {
                @Suppress("UNCHECKED_CAST")
                (it.arguments[1] as HandoffFeatureStateListener).onHandoffFeatureStateChanged(
                    enablementStatus,
                    true,
                )
            }
            .`when`(taskContinuityManager)
            .registerHandoffFeatureStateListener(any(), any())
    }

    private fun mockOpsMode(mode: Int) {
        whenever(appOpsManager.checkOpNoThrow(OP_CONTINUE_ACROSS_DEVICES, UID, PACKAGE_NAME))
            .thenReturn(mode)
    }

    @Test
    @DisableFlags(android.companion.Flags.FLAG_TASK_CONTINUITY)
    fun `Task continuity disabled - not display`() {
        setContent()

        composeTestRule.onRoot().assertIsNotDisplayed()
    }

    @Test
    @EnableFlags(android.companion.Flags.FLAG_TASK_CONTINUITY)
    fun `Task continuity enabled - display`() {
        setContent()

        composeTestRule.onRoot().assertIsDisplayed()
    }

    @Test
    @EnableFlags(android.companion.Flags.FLAG_TASK_CONTINUITY)
    fun `Not eligible - displayed but disabled`() {
        mockHandoffEnablementStatus(
            TaskContinuityManager.HANDOFF_AVAILABILITY_STATUS_DISABLED_BY_POLICY
        )

        setContent()

        val text = context.getString(R.string.continue_across_devices_switch)
        composeTestRule.onNodeWithText(text).assertIsDisplayed().assertIsNotEnabled().assertIsOff()
    }

    @Test
    @EnableFlags(android.companion.Flags.FLAG_TASK_CONTINUITY)
    fun `on click - changes state`() {
        mockOpsMode(MODE_ALLOWED)

        setContent()
        composeTestRule.onRoot().performClick()
        verify(appOpsManager).setUidMode(OP_CONTINUE_ACROSS_DEVICES, UID, MODE_IGNORED)
    }

    @Test
    @EnableFlags(android.companion.Flags.FLAG_TASK_CONTINUITY)
    fun `AppOpp ignored - switch is off`() {
        mockOpsMode(MODE_IGNORED)

        setContent()

        composeTestRule.onNode(isToggleable()).assertIsEnabled().assertIsOff()
    }

    @Test
    @EnableFlags(android.companion.Flags.FLAG_TASK_CONTINUITY)
    fun `AppOpp allowed - switch is on`() {
        mockOpsMode(MODE_ALLOWED)

        setContent()

        composeTestRule.onNode(isToggleable()).assertIsEnabled().assertIsOn()
    }

    @Test
    @EnableFlags(android.companion.Flags.FLAG_TASK_CONTINUITY)
    fun `AppOpp default - switch is on`() {
        mockOpsMode(MODE_DEFAULT)

        setContent()

        composeTestRule.onNode(isToggleable()).assertIsEnabled().assertIsOn()
    }

    private fun setContent(app: ApplicationInfo = TARGET_APP) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                ContinueAcrossDevicesSwitchPreference(
                    app,
                    isContinueAcrossDevicesSwitchEnabledStateFlow,
                )
            }
        }
    }

    private companion object {
        const val PACKAGE_NAME = "package name"
        const val UID = 123

        val TARGET_APP =
            ApplicationInfo().apply {
                packageName = PACKAGE_NAME
                uid = UID
                targetSdkVersion = Build.VERSION_CODES.R
            }
    }
}
