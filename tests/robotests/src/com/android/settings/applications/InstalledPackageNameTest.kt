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
import android.content.ContextWrapper
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.ApplicationInfoFlags
import android.multiuser.Flags
import android.os.UserManager
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
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
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowPackageManager

@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowUserManager::class])
class InstalledPackageNameTest {

    @get:Rule val setFlagsRule = SetFlagsRule()

    private lateinit var context: Context
    private lateinit var spyPackageManager: PackageManager
    private lateinit var shadowUserManager: ShadowUserManager

    @Before
    fun setUp() {
        val appContext: Context = ApplicationProvider.getApplicationContext()
        spyPackageManager = spy(appContext.packageManager)
        context =
            object : ContextWrapper(appContext) {
                override fun getPackageManager() = spyPackageManager
            }
        val userManager = context.getSystemService(UserManager::class.java)!!
        shadowUserManager = Shadow.extract(userManager)
    }

    @After
    fun cleanUp() {
        ShadowUserManager.reset()
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
                        parameter(
                            "package",
                            R.string.parameter_purpose,
                            true,
                            InstalledPackageName(),
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

    @Test
    fun getOptions_appFlagsAssigned_useAssignedFlags() {
        val assignedFlags = ApplicationInfoFlags.of(0)

        InstalledPackageName(assignedFlags).getOptions(context)

        verify(spyPackageManager).getInstalledApplications(eq(assignedFlags))
    }

    @Test
    @EnableFlags(Flags.FLAG_DONT_SHOW_OTHER_USERS_APPS_TO_ADMIN)
    fun getOptions_flagEnabled_admin_useDefaultFlagsSelf() {
        shadowUserManager.setIsAdminUser(true)

        InstalledPackageName().getOptions(context)

        val captor = argumentCaptor<ApplicationInfoFlags>()
        verify(spyPackageManager).getInstalledApplications(captor.capture())
        assertThat(captor.firstValue.value).isEqualTo(FLAGS_SELF.toLong())
    }

    @Test
    @DisableFlags(Flags.FLAG_DONT_SHOW_OTHER_USERS_APPS_TO_ADMIN)
    fun getOptions_flagDisabled_admin_useDefaultFlagsAll() {
        shadowUserManager.setIsAdminUser(true)

        InstalledPackageName().getOptions(context)

        val captor = argumentCaptor<ApplicationInfoFlags>()
        verify(spyPackageManager).getInstalledApplications(captor.capture())
        assertThat(captor.firstValue.value).isEqualTo(FLAGS_ALL.toLong())
    }

    @Test
    @DisableFlags(Flags.FLAG_DONT_SHOW_OTHER_USERS_APPS_TO_ADMIN)
    fun getOptions_flagDisabled_user_useDefaultFlagsSelf() {
        shadowUserManager.setIsAdminUser(false)

        InstalledPackageName().getOptions(context)

        val captor = argumentCaptor<ApplicationInfoFlags>()
        verify(spyPackageManager).getInstalledApplications(captor.capture())
        assertThat(captor.firstValue.value).isEqualTo(FLAGS_SELF.toLong())
    }

    private fun buildPackageInfo(packageName: String): PackageInfo {
        val packageInfo = PackageInfo()
        packageInfo.packageName = packageName
        return packageInfo
    }

    companion object {
        private const val FLAGS_SELF =
            PackageManager.MATCH_DISABLED_COMPONENTS or
                PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS
        private const val FLAGS_ALL = FLAGS_SELF or PackageManager.MATCH_ANY_USER
    }
}
