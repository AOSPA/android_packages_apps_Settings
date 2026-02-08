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
package com.android.settings.spa.accessibility

import android.app.UiModeManager
import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class ForceDarkAppExceptionsRepositoryTest {
    @get:Rule val mockitoRule = MockitoJUnit.rule()
    @Mock lateinit var context: Context
    @Mock lateinit var uiModeManager: UiModeManager
    private lateinit var repository: ForceDarkAppExceptionsRepository
    private val testUserId = 0

    @Before
    fun setup() {
        whenever(context.getSystemService(UiModeManager::class.java)).thenReturn(uiModeManager)
        whenever(uiModeManager.getAllForceInvertAlwaysDisableApps(testUserId))
            .thenReturn(mutableListOf())
        repository = ForceDarkAppExceptionsRepository(context, testUserId)
    }

    @Test
    fun setAppForceDarkAlwaysDisableTrue_setsPackageNameForceInvertPackageAlwaysDisable() {
        val appInfo = ApplicationInfo()
        appInfo.packageName = MOCK_PACKAGE_NAME
        repository.setIsAppForceDarkAlwaysDisable(appInfo, true)

        verify(uiModeManager)
            .setForceInvertOverrideStateForApp(
                eq(MOCK_PACKAGE_NAME),
                eq(UiModeManager.FORCE_INVERT_PACKAGE_ALWAYS_DISABLE),
                eq(testUserId),
            )
    }

    @Test
    fun setAppForceDarkAlwaysDisableFalse_setsPackageNameForceInvertPackageAllowed() {
        val appInfo = ApplicationInfo()
        appInfo.packageName = MOCK_PACKAGE_NAME
        // Add the app to the always disable list
        repository.setIsAppForceDarkAlwaysDisable(appInfo, true)
        verify(uiModeManager)
            .setForceInvertOverrideStateForApp(
                eq(MOCK_PACKAGE_NAME),
                eq(UiModeManager.FORCE_INVERT_PACKAGE_ALWAYS_DISABLE),
                eq(testUserId),
            )

        // Exclude the app from the always disable list
        repository.setIsAppForceDarkAlwaysDisable(appInfo, false)

        verify(uiModeManager)
            .setForceInvertOverrideStateForApp(
                eq(MOCK_PACKAGE_NAME),
                eq(UiModeManager.FORCE_INVERT_PACKAGE_ALLOWED),
                eq(testUserId),
            )
    }

    companion object {
        private val MOCK_PACKAGE_NAME = "com.mock.example"
    }
}
