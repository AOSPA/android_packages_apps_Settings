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
package com.android.settings.applications

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.preference.PreferenceFragment
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowPackageManager

@RunWith(AndroidJUnit4::class)
class InstalledPackageNameTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun getAllPossibleParameters_onParameterizedScreenWithInstalledPackageName_returnsCorrectParameters() {
        val screen =
            object :
                PreferencesApiScreen(
                    key = "ApiScreen",
                    topLevelSettingsCategory = Category.SYSTEM,
                    fragment = PreferenceFragment::class,
                    purpose = R.string.preference_screen_purpose,
                ) {
                init {
                    parameters {
                        parameter(
                            "package",
                            R.string.parameter_purpose,
                            true,
                            InstalledPackageName(PackageManager.PackageInfoFlags.of(0)),
                        )
                    }
                }
            }
        ShadowPackageManager.reset()
        val packageManager = shadowOf(context.packageManager)
        packageManager.installPackage(buildPackageInfo("a.b.package1"))
        packageManager.installPackage(buildPackageInfo("a.d.package2"))

        val allPossibleParameters = runBlocking {
            screen.getAllPossibleParameters(context).toList()
        }
        val possibleParameterPairs =
            allPossibleParameters.flatMap { it.values.entries }.map { it.key to it.value }

        allPossibleParameters.forEach { assertThat(it.values).hasSize(1) }
        assertThat(possibleParameterPairs)
            .containsExactly("package" to "a.b.package1", "package" to "a.d.package2")
    }

    private fun buildPackageInfo(packageName: String): PackageInfo {
        val packageInfo = PackageInfo()
        packageInfo.packageName = packageName
        return packageInfo
    }
}
