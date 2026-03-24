/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.UserHandle
import android.os.UserManager
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settingslib.spa.testutils.delay
import com.android.settingslib.spaprivileged.model.app.userHandle
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class AppPermissionPreferenceTest {
    @get:Rule
    val setFlagsRule = SetFlagsRule()

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        doNothing().whenever(mock).startActivityAsUser(any(), any())
        doNothing().whenever(mock).startActivity(any())
    }

    @Test
    fun title_display() {
        val userManager = mock<UserManager>()
        doReturn(userManager).whenever(context).getSystemService(UserManager::class.java)

        setContent()

        composeTestRule.onNodeWithText(context.getString(R.string.permissions_label))
            .assertIsDisplayed()
    }

    @Test
    @DisableFlags(android.multiuser.Flags.FLAG_HSU_APP_MANAGEMENT)
    fun whenClick_startActivity_flagDisabled() {
        setContent()
        composeTestRule.onRoot().performClick()
        composeTestRule.delay()

        val intent = argumentCaptor {
            verify(context).startActivityAsUser(capture(), eq(APP.userHandle))
        }.firstValue
        assertThat(intent.action).isEqualTo(Intent.ACTION_MANAGE_APP_PERMISSIONS)
        assertThat(intent.getStringExtra(Intent.EXTRA_PACKAGE_NAME)).isEqualTo(PACKAGE_NAME)
        assertThat(intent.getBooleanExtra(EXTRA_HIDE_INFO_BUTTON, false)).isEqualTo(true)
    }


    @Test
    @EnableFlags(android.multiuser.Flags.FLAG_HSU_APP_MANAGEMENT)
    fun whenClick_startActivity_flagEnabled_normalApp() {
        setContent(APP)
        composeTestRule.onRoot().performClick()
        composeTestRule.delay()

        val intent = argumentCaptor {
            verify(context).startActivityAsUser(capture(), eq(APP.userHandle))
        }.firstValue
        assertThat(intent.action).isEqualTo(Intent.ACTION_MANAGE_APP_PERMISSIONS)
        assertThat(intent.getStringExtra(Intent.EXTRA_PACKAGE_NAME)).isEqualTo(PACKAGE_NAME)
        assertThat(intent.getBooleanExtra(EXTRA_HIDE_INFO_BUTTON, false)).isEqualTo(true)
    }

    @Test
    @EnableFlags(android.multiuser.Flags.FLAG_HSU_APP_MANAGEMENT)
    fun whenClick_startActivity_flagEnabled_hsuApp() {
        // HSU app behavior is only relevant on devices running in Headless System User Mode.
        Assume.assumeTrue(
            "Feature supported only on Headless System User Mode devices",
            UserManager.isHeadlessSystemUserMode()
        )

        val userManager = mock<UserManager> {
            on { isAdminUser } doReturn true
        }
        doReturn(userManager).whenever(context).getSystemService(UserManager::class.java)

        setContent(HSU_APP)
        composeTestRule.onRoot().performClick()
        composeTestRule.delay()

        val intent = argumentCaptor {
            verify(context).startActivity(capture())
        }.firstValue
        assertThat(intent.action).isEqualTo(Intent.ACTION_MANAGE_APP_PERMISSIONS)
        assertThat(intent.getStringExtra(Intent.EXTRA_PACKAGE_NAME)).isEqualTo(PACKAGE_NAME)
        assertThat(intent.getBooleanExtra(EXTRA_HIDE_INFO_BUTTON, false)).isEqualTo(true)
        assertThat(intent.getParcelableExtra(Intent.EXTRA_USER, UserHandle::class.java))
            .isEqualTo(HSU_APP.userHandle)
    }

    private fun setContent(app: ApplicationInfo = APP) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                AppPermissionPreference(
                    app = app,
                    summaryFlow = flowOf(
                        AppPermissionSummaryState(summary = SUMMARY, enabled = true)
                    ),
                )
            }
        }
        composeTestRule.delay()
    }

    private companion object {
        const val PACKAGE_NAME = "package.name"
        const val SUMMARY = "Summary"
        private const val EXTRA_HIDE_INFO_BUTTON = "hideInfoButton"

        // Represents an app running under the Headless System User (User 0).
        val HSU_APP = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            uid = UserHandle.getUid(UserHandle.USER_SYSTEM, 1000)
        }

        // Represents a standard app running under a typical human user (e.g., User 10).
        val APP = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            uid = UserHandle.getUid(10, 1000)
        }
    }
}
