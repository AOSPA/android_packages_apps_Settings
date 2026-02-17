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

package com.android.settings.applications.specialaccess

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.applications.specialaccess.InteractAcrossProfilesAppDetailScreen.Companion.KEY_APP_PACKAGE_NAME
import com.android.settingslib.metadata.CatalystFlagProviderFactory
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class InteractAcrossProfilesAppDetailScreenTest {
    private val packageManager: PackageManager = mock()
    private val context: Context = mock { on { packageManager } doReturn packageManager }

    @Test
    fun isAvailable_whenAppInfoIsNull_returnsFalse() {
        packageManager.stub {
            on { getApplicationInfo("app.not.found", 0) } doThrow
                PackageManager.NameNotFoundException()
        }
        val screen = createScreen()

        assertThat(screen.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_whenAppInfoIsNotNull_returnsTrue() {
        packageManager.stub { on { getApplicationInfo("app.found", 0) } doReturn ApplicationInfo() }
        val screen = createScreen()

        assertThat(screen.isAvailable(context)).isTrue()
    }

    private fun createScreen(): InteractAcrossProfilesAppDetailScreen {
        return if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
            InteractAcrossProfilesAppDetailScreen(
                context,
                InteractAcrossProfilesAppDetailScreen.parametersSchema.prepare(
                    KEY_APP_PACKAGE_NAME to "app.found"
                ),
            )
        } else {
            InteractAcrossProfilesAppDetailScreen(
                context,
                Bundle().apply { putString("app", "app.found") },
            )
        }
    }
}
