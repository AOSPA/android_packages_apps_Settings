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

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.res.Resources
import android.service.personalcontext.PersonalContextManager
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.personalcontext.PersonalContextAppPreference
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class PersonalContextAppPreferenceTest {
    val testPackageName = "com.foo.baz"

    @get:Rule val rule = createComposeRule()

    val targetApp =
        ApplicationInfo().apply {
            packageName = testPackageName
            uid = 123
        }

    private val resources = mock<Resources>()

    private val personalContextManager = mock<PersonalContextManager>()

    private val context =
        mock<Context> {
            on { getSystemService(eq(PersonalContextManager::class.java)) } doReturn
                personalContextManager
            on { resources } doReturn resources
            on { getString(any()) } doReturn "string"
        }

    private fun setContent() {
        rule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                PersonalContextAppPreference(targetApp)
            }
        }
    }

    private fun configure(
        serviceAvailable: Boolean,
        serviceEnabled: Boolean,
        perAppEnabled: Boolean,
    ) {
        resources.stub {
            on {
                getBoolean(
                    eq(com.android.internal.R.bool.config_enablePersonalContextManagerService)
                )
            } doReturn serviceAvailable
        }

        personalContextManager.stub {
            on { isEnabled } doReturn serviceEnabled
            on { isPersonalContextModeEnabled(eq(testPackageName)) } doReturn perAppEnabled
        }
    }

    @Test
    fun `PersonalContext service unavailable - no display`() {
        configure(false, false, false)
        setContent()
        rule.onRoot().assertIsNotDisplayed()
    }

    @Test
    fun `PersonalContext service available and disabled - no display`() {
        configure(true, false, false)
        setContent()
        rule.onRoot().assertIsNotDisplayed()
    }

    @Test
    fun `PersonalContext service available and enabled - displayed`() {
        configure(true, true, false)
        setContent()
        rule.onRoot().assertIsDisplayed()
    }

    @Test
    fun `onClick when disabled - enables personal context`() {
        configure(true, true, false)
        setContent()
        rule.onRoot().performClick()
        verify(personalContextManager).setPersonalContextModeEnabled(eq(testPackageName), eq(true))
    }

    @Test
    fun `onClick when enabled - disables personal context`() {
        configure(true, true, true)
        setContent()
        rule.onRoot().performClick()
        verify(personalContextManager).setPersonalContextModeEnabled(eq(testPackageName), eq(false))
    }

    @Test
    fun `onClick when disabled and not available - no interaction`() {
        configure(true, false, false)
        setContent()
        rule.onRoot().performClick()
        verify(personalContextManager, never())
            .setPersonalContextModeEnabled(eq(testPackageName), eq(true))
    }
}
