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

package com.android.settings.deviceinfo.legal

import android.content.ContextWrapper
import android.content.pm.ApplicationInfo
import android.content.pm.ModuleInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.Resources
import com.android.settings.flags.Flags
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

class ModuleLicensesScreenTest : SettingsCatalystTestCase() {
    private val mockPackageManager = mock<PackageManager>()
    private val mockResources = mock<Resources>()
    private val mockAssetManager = mock<AssetManager>()

    private val moduleInfo = ModuleInfo().apply { packageName = "com.android.module" }
    private val appInfo = ApplicationInfo()
    private val packageInfo = PackageInfo().apply { applicationInfo = appInfo }

    override val preferenceScreenCreator = ModuleLicensesScreen()

    override val flagName: String
        get() = Flags.FLAG_CATALYST_MIGRATION_26Q2

    private val context =
        object : ContextWrapper(appContext) {
            override fun getPackageManager() = mockPackageManager
        }

    @Before
    fun setUp() {
        mockPackageManager.stub {
            on { getInstalledModules(anyInt()) } doReturn listOf(moduleInfo)
            on { getPackageInfo(eq("com.android.module"), anyInt()) } doReturn packageInfo
            on { getResourcesForApplication(appInfo) } doReturn mockResources
        }

        mockResources.stub { on { assets } doReturn mockAssetManager }
    }

    @Test
    fun isAvailable_hasModuleWithLicense_returnsTrue() {
        mockAssetManager.stub {
            on { list("") } doReturn arrayOf(ModuleLicenseProvider.GZIPPED_LICENSE_FILE_NAME)
        }

        assertThat(preferenceScreenCreator.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_noModuleWithLicense_returnsFalse() {
        mockAssetManager.stub { on { list("") } doReturn arrayOf("other_file") }

        assertThat(preferenceScreenCreator.isAvailable(context)).isFalse()
    }
}
