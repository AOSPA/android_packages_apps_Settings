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

import android.Manifest
import android.Manifest.permission.ACCESS_NOTIFICATION_POLICY
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.ApplicationInfoFlags
import android.os.UserManager
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.testutils.shadow.ShadowUserManager
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.preference.PreferenceFragment
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow

@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowUserManager::class])
class InstalledPackageNameTest {

    @get:Rule val setFlagsRule = SetFlagsRule()

    private lateinit var context: Context
    private val packageManager = mock<PackageManager>()
    private lateinit var shadowUserManager: ShadowUserManager

    @Before
    fun setUp() {
        context = spy(ApplicationProvider.getApplicationContext()) {
            on { packageManager } doReturn packageManager
        }
        val userManager = context.getSystemService(UserManager::class.java)!!
        shadowUserManager = Shadow.extract(userManager)
    }

    @After
    fun cleanUp() {
        ShadowUserManager.reset()
    }

    @Test
    fun whenNoParametersAreGiven_returnsKeyWithNoPermissionsAndNonSystemApps() {
        assertThat(InstalledPackageName.getKey()).isEqualTo("InstalledPackageName:no-permissions:non-system")
    }

    @Test
    fun whenExcluseSystemIsFalse_returnsKeyWithNoPermissionsAndAllApps() {
        assertThat(InstalledPackageName(excludeSystemApps = false).getKey()).isEqualTo("InstalledPackageName:no-permissions:all")
    }

    @Test
    fun whenExcluseSystemIsTrueAndPermissionsAreGiven_returnsKeyWithCorrectPermissionsAndAllApps() {
        assertThat(
            InstalledPackageName(
                heldPermissions = arrayOf(
                    Manifest.permission.READ_SYSTEM_PREFERENCES,
                    Manifest.permission.WRITE_SETTINGS
                ), excludeSystemApps = true
            ).getKey()
        ).isEqualTo("InstalledPackageName:android.permission.READ_SYSTEM_PREFERENCES,android.permission.WRITE_SETTINGS:non-system")
    }

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
                        parameter("package", R.string.parameter_purpose, true, InstalledPackageName)
                    }
                }
            }

        packageManager.stub {
            on {
                getInstalledApplicationsAsUser(
                    any<ApplicationInfoFlags>(),
                    anyInt()
                )
            } doReturn listOf(buildAppInfo("a.b.package1"), buildAppInfo("a.d.package2"))
        }

        val allPossibleParameters = runBlocking {
            screen.getAllPossibleParameters(context).toList()
        }
        val possibleParameterPairs =
            allPossibleParameters.flatMap { it.values.entries }.map { it.key to it.value }

        allPossibleParameters.forEach { assertThat(it.values).hasSize(1) }
        assertThat(possibleParameterPairs)
            .containsExactly("package" to "a.b.package1", "package" to "a.d.package2")
    }

    @Test
    fun getOptions_hasHeldPermissions_returnsCorrectParameters() {
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
                        parameter("package", R.string.parameter_purpose, true, InstalledPackageName(heldPermissions = PERM))
                    }
                }
            }

        packageManager.stub {
            on {
                getInstalledApplicationsAsUser(
                    any<ApplicationInfoFlags>(),
                    anyInt()
                )
            } doReturn listOf(buildAppInfo("a.b.package1"), buildAppInfo("a.d.package2"))
            on {
                getPackageInfo(
                    ("a.b.package1"),
                    PackageManager.GET_PERMISSIONS
                )
            } doReturn PackageInfo().apply {
                requestedPermissions = null
            }
            on {
                getPackageInfo(
                    ("a.d.package2"),
                    PackageManager.GET_PERMISSIONS
                )
            } doReturn PackageInfo().apply {
                requestedPermissions = PERM
            }

        }

        val allPossibleParameters = runBlocking {
            screen.getAllPossibleParameters(context).toList()
        }
        val possibleParameterPairs =
            allPossibleParameters.flatMap { it.values.entries }.map { it.key to it.value }

        allPossibleParameters.forEach { assertThat(it.values).hasSize(1) }
        assertThat(possibleParameterPairs)
            .containsExactly("package" to "a.d.package2")
    }

    private fun buildAppInfo(packageNameString: String): ApplicationInfo =
        ApplicationInfo().apply { packageName = packageNameString }

    companion object {
        private val PERM = arrayOf(ACCESS_NOTIFICATION_POLICY)
    }
}
