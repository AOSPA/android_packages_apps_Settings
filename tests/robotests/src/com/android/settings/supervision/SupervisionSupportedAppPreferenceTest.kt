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
package com.android.settings.supervision

import android.app.supervision.flags.Flags
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.MATCH_UNINSTALLED_PACKAGES
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.CatalystFragment
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class SupervisionSupportedAppPreferenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val title = "title"
    private val summary = "summary"
    private val appPackageName = "packageName"
    private val preferenceKey = "preferenceKey"
    private val appIcon = ColorDrawable(Color.RED)
    private val mockPackageManager = mock(PackageManager::class.java)

    @get:Rule(order = 0) val setFlagsRule = SetFlagsRule()

    @Before
    fun setUp() {
        val fakeApplicationInfo = ApplicationInfo().apply { packageName = appPackageName }

        mockPackageManager.stub {
            on { getApplicationInfo(eq(appPackageName), any<Int>()) } doReturn fakeApplicationInfo
            on { getApplicationIcon(fakeApplicationInfo) } doReturn appIcon
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPPORTED_APPS_RETRIEVAL_UPDATES)
    fun getTitle() {
        val preference = getPreference(preferenceKey)
        assertThat(preference.title).isEqualTo(title)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPPORTED_APPS_RETRIEVAL_UPDATES)
    fun getSummary() {
        val preference = getPreference(preferenceKey)
        assertThat(preference.summary).isEqualTo(summary)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPPORTED_APPS_RETRIEVAL_UPDATES)
    fun getIcon() {
        val preference = getPreference(preferenceKey)
        assertThat(preference.icon).isEqualTo(appIcon)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun getKey() {
        val preference = getPreference(preferenceKey)
        assertThat(preference.key).isEqualTo(preferenceKey)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun getDefaultKey() {
        val preference = getPreference()
        assertThat(preference.key).isEqualTo(SupervisionSupportedAppPreference.KEY)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPPORTED_APPS_RETRIEVAL_UPDATES)
    fun attachFooter() {
        val mockLifecycleContext = mock(PreferenceLifecycleContext::class.java)
        val learnMoreLink = "learn more link"
        val spyFragment = spy(CatalystFragment())

        whenever(mockLifecycleContext.getString(any())).thenReturn(learnMoreLink)
        whenever(mockLifecycleContext.lifecycleOwner).thenReturn(spyFragment)

        val metadata =
            SupervisionSupportedAppPreference(
                title,
                summary,
                appPackageName,
                preferenceKey,
                learnMoreLink,
            )

        metadata.onCreate(mockLifecycleContext)
        assertThat(spyFragment.footerDataMap.containsKey(preferenceKey)).isTrue()
    }

    @Test
    fun createWidget_callsGetApplicationInfoWithCorrectFlags() {
        getPreference()
        val expectedFlags = MATCH_UNINSTALLED_PACKAGES
        verify(mockPackageManager, times(1))
            .getApplicationInfo(eq(appPackageName), eq(expectedFlags))
    }

    private fun getPreference(key: String? = null): Preference {
        val spyContext = spy(context) { on { packageManager } doReturn mockPackageManager }
        return SupervisionSupportedAppPreference(title, summary, appPackageName, key)
            .createAndBindWidget(spyContext)
    }
}
