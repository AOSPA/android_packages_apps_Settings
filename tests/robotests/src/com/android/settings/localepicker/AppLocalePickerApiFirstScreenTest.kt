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

package com.android.settings.localepicker

import android.app.LocaleManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.ApplicationInfoFlags
import android.content.pm.PackageManager.ResolveInfoFlags
import android.content.pm.ResolveInfo
import android.content.pm.UserInfo
import android.content.res.Resources
import android.os.LocaleList
import android.os.UserManager
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.internal.R
import com.android.internal.app.AppLocaleCollector
import com.android.internal.app.LocaleStore
import com.android.settings.flags.Flags
import com.android.settings.localepicker.AppLocalePickerApiFirstScreenTest.ShadowAppLocaleCollector
import com.android.settings.localepicker.AppLocalePickerApiFirstScreenTest.ShadowAppLocaleCollector.Companion.setLocaleInfos
import com.android.settings.localepicker.AppLocalePickerApiFirstScreenTest.ShadowLocaleUtils
import com.android.settings.localepicker.AppLocalePickerApiFirstScreenTest.ShadowLocaleUtils.Companion.setDisplayLocaleUi
import com.android.settings.localepicker.AppLocalePickerFragment.ARG_PACKAGE_NAME
import com.android.settings.testutils.shadow.ShadowActivityManager
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.FailedPreconditionException
import com.android.settings.testutils2.Parameters
import com.google.common.truth.Truth.assertThat
import java.util.Locale
import kotlin.test.assertFailsWith
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowLocaleManager

@RunWith(AndroidJUnit4::class)
@Config(
    shadows =
        [
            ShadowActivityManager::class,
            ShadowLocaleManager::class,
            ShadowLocaleUtils::class,
            ShadowAppLocaleCollector::class,
        ]
)
class AppLocalePickerApiFirstScreenTest {
    @get:Rule val setFlagsRule = SetFlagsRule()
    private lateinit var tester: ApiTester
    private val resources =
        mock<Resources> {
            on { getStringArray(R.array.config_hideWhenDisabled_packageNames) } doReturn
                emptyArray()
        }
    private val packageManager =
        mock<PackageManager> {
            on { getInstalledModules(any()) } doReturn emptyList()
            on { getHomeActivities(any()) } doAnswer
                {
                    @Suppress("UNCHECKED_CAST")
                    val resolveInfos = it.arguments[0] as MutableList<ResolveInfo>
                    resolveInfos += resolveInfoOf(packageName = HOME_APP.packageName)
                    null
                }
            on { queryIntentActivitiesAsUser(any(), any<ResolveInfoFlags>(), any<Int>()) } doReturn
                listOf(resolveInfoOf(packageName = IN_LAUNCHER_APP.packageName))
            on { getInstalledApplications(any<ApplicationInfoFlags>()) } doReturn
                listOf(CHROME_APP, CLOCK_APP)
        }

    private val mockUserManager =
        mock<UserManager> {
            on { getUserInfo(MAIN_USER_ID) } doReturn UserInfo(0, "admin", UserInfo.FLAG_ADMIN)
            on { getProfileIdsWithDisabled(MAIN_USER_ID) } doReturn
                intArrayOf(MAIN_USER_ID, MANAGED_PROFILE_USER_ID)
        }
    private val context: Context =
        spy(ApplicationProvider.getApplicationContext()) {
            on { resources } doReturn resources
            on { packageManager } doReturn packageManager
            on { getSystemService(UserManager::class.java) } doReturn mockUserManager
        }

    @Before
    @Throws(java.lang.Exception::class)
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        whenever(context.packageManager).thenReturn(packageManager)

        // Mock labels for apps
        whenever(packageManager.getApplicationLabel(CHROME_APP)).thenReturn("Chrome")
        whenever(packageManager.getApplicationLabel(CLOCK_APP)).thenReturn("Clock")
        tester = ApiTester(AppLocalePickerApiFirstScreen(), context)
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        ShadowLocaleUtils.reset()
        ShadowAppLocaleCollector.reset()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagEnabled_isNotNull() {
        assertThat(tester.getScreen()).isNotNull()
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagDisabled_isNull() {
        assertThat(tester.getScreen()).isNull()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun setAppLocale_preconditionFails_throwsException() {
        setDisplayLocaleUi(false)

        assertFailsWith<FailedPreconditionException> { tester.getLaunchIntent() }
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getAppLanguage_returnsCurrentLocale() {
        setDisplayLocaleUi(true)
        packageManager.stub {
            on {
                getInstalledApplicationsAsUser(
                    any<ApplicationInfoFlags>(),
                    anyInt()
                )
            } doReturn listOf(CHROME_APP, CLOCK_APP)
        }

        val currentLocaleInfo = mock<LocaleStore.LocaleInfo>()
        whenever(currentLocaleInfo.isAppCurrentLocale).thenReturn(true)
        whenever(currentLocaleInfo.locale).thenReturn(Locale.US)
        whenever(currentLocaleInfo.fullNameNative).thenReturn("English (United States)")
        setLocaleInfos(setOf(currentLocaleInfo))
        val secondLocaleInfo = mock<LocaleStore.LocaleInfo>()
        whenever(secondLocaleInfo.isAppCurrentLocale).thenReturn(false)
        whenever(secondLocaleInfo.locale).thenReturn(Locale.forLanguageTag("es-US"))
        whenever(secondLocaleInfo.fullNameNative).thenReturn("Español (Estados Unidos)")

        tester.initializeScreenParameters(Parameters(ARG_PACKAGE_NAME to PACKAGE_CHROME))

        assertThat(tester.get<String>(KEY_PREFERENCE)).isEqualTo("en-US")
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun setAppLanguage_setOtherLocale_isSet() {
        setDisplayLocaleUi(true)
        packageManager.stub {
            on {
                getInstalledApplicationsAsUser(
                    any<ApplicationInfoFlags>(),
                    anyInt()
                )
            } doReturn listOf(CHROME_APP, CLOCK_APP)
        }

        val currentLocaleInfo = mock<LocaleStore.LocaleInfo>()
        whenever(currentLocaleInfo.isAppCurrentLocale).thenReturn(true)
        whenever(currentLocaleInfo.locale).thenReturn(Locale.US)
        whenever(currentLocaleInfo.fullNameNative).thenReturn("English (United States)")
        val secondLocaleInfo = mock<LocaleStore.LocaleInfo>()
        whenever(secondLocaleInfo.isAppCurrentLocale).thenReturn(false)
        whenever(secondLocaleInfo.locale).thenReturn(Locale.forLanguageTag("es-US"))
        whenever(secondLocaleInfo.fullNameNative).thenReturn("Español (Estados Unidos)")
        setLocaleInfos(setOf(currentLocaleInfo, secondLocaleInfo))

        tester.initializeScreenParameters(Parameters(ARG_PACKAGE_NAME to PACKAGE_CHROME))
        tester.set(KEY_PREFERENCE, "es-US")

        val localeManager = context.getSystemService(LocaleManager::class.java)
        val appLocales = localeManager.getApplicationLocales(PACKAGE_CHROME)

        assertThat(appLocales.toLanguageTags()).isEqualTo("es-US")
    }

    @Implements(LocaleUtils::class)
    class ShadowLocaleUtils {
        companion object {
            private var appList: List<ApplicationInfo> = emptyList()
            var canDisplayLocaleUi = false

            @Implementation
            @JvmStatic
            fun canDisplayLocaleUi(context: Context, packageName: String?): Boolean {
                return canDisplayLocaleUi
            }

            fun setDisplayLocaleUi(canDisplay: Boolean) {
                canDisplayLocaleUi = canDisplay
            }

            @Implementation
            @JvmStatic
            fun getAppList(
                context: Context,
                packageManager: PackageManager,
            ): List<ApplicationInfo> {
                return appList
            }

            @Implementation
            @JvmStatic
            fun onLocaleSelected(
                context: Context,
                localeInfo: LocaleStore.LocaleInfo,
                packageName: String,
            ) {
                context
                    .getSystemService(LocaleManager::class.java)
                    .setApplicationLocales(packageName, LocaleList(localeInfo.locale))
            }

            fun setAppList(list: List<ApplicationInfo>) {
                appList = list
            }

            fun reset() {
                appList = emptyList()
            }
        }
    }

    @Implements(AppLocaleCollector::class)
    class ShadowAppLocaleCollector {
        @Implementation
        fun getSupportedLocaleList(
            parent: LocaleStore.LocaleInfo?,
            translatedOnly: Boolean,
            isForCountryMode: Boolean,
        ): Set<LocaleStore.LocaleInfo> {
            return locales
        }

        companion object {
            private var locales: Set<LocaleStore.LocaleInfo> = emptySet()

            fun setLocaleInfos(list: Set<LocaleStore.LocaleInfo>) {
                locales = list
            }

            fun reset() {
                locales = emptySet()
            }
        }
    }

    companion object {
        const val KEY_PREFERENCE: String = "app_language_picker_preference"
        const val PACKAGE_CHROME = "com.android.chrome"
        const val PACKAGE_CLOCK = "com.google.android.deskclock"
        const val MAIN_USER_ID = 0
        const val MANAGED_PROFILE_USER_ID = 11

        val CHROME_APP =
            ApplicationInfo().apply {
                packageName = PACKAGE_CHROME
                enabled = true
            }

        val CLOCK_APP =
            ApplicationInfo().apply {
                packageName = PACKAGE_CLOCK
                enabled = true
            }

        val HOME_APP =
            ApplicationInfo().apply {
                packageName = "home.app"
                flags = ApplicationInfo.FLAG_SYSTEM
            }

        val IN_LAUNCHER_APP =
            ApplicationInfo().apply {
                packageName = "app.in.launcher"
                flags = ApplicationInfo.FLAG_SYSTEM
            }

        fun resolveInfoOf(packageName: String) =
            ResolveInfo().apply {
                activityInfo = ActivityInfo().apply { this.packageName = packageName }
            }
    }
}
