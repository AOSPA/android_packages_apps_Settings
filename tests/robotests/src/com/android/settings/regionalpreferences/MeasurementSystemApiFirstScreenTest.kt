/**
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

package com.android.settings.regionalpreferences

import android.app.IActivityManager
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.internal.app.LocalePicker
import com.android.settings.flags.Flags
import com.android.settings.testutils.shadow.ShadowActivityManager
import com.android.settings.testutils2.ApiTester
import com.android.settingslib.datastore.SettingsSystemStore
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class MeasurementSystemApiFirstScreenTest {
    @get:Rule
    val setFlagsRule = SetFlagsRule()
    private val tester = ApiTester(MeasurementSystemApiFirstScreen())
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val iActivityManager = mock<IActivityManager>()
    private val cacheLocaleList: LocaleList = LocaleList.getDefault()
    private val cacheLocale: Locale = Locale.getDefault(Locale.Category.FORMAT)
    private var cacheProviderContent: String? = null

    @Before
    @Throws(java.lang.Exception::class)
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        ShadowActivityManager.setService(iActivityManager)
        cacheProviderContent =
            SettingsSystemStore.get(context).getString(Settings.System.LOCALE_PREFERENCES)
        val config = Configuration()
        config.setLocales(RegionalPreferenceTestUtils.setLocaleConditions())
        whenever(iActivityManager.getConfiguration()).thenReturn(config)
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        cacheProviderContent?.let { value ->
            RegionalPreferenceTestUtils.setSettingsProviderContent(context, cacheProviderContent!!)
        }
        Locale.setDefault(cacheLocale)
        LocalePicker.updateLocales(cacheLocaleList)
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
    fun getPreference_defaultIsUssystem_returnUssystem() {
        RegionalPreferenceTestUtils.setSettingsProviderContent(context, "")
        Locale.setDefault(Locale.forLanguageTag("en-US-u-ms-ussystem"))

        assertThat(tester.get<String>(MeasurementSystemApiFirstScreen.KEY_MEASUREMENT_SYSTEM_ITEM))
            .isEqualTo("ussystem")
    }

    @Test
    fun getPreference_defaultIsDefault_returnUseDefault() {
        RegionalPreferenceTestUtils.setSettingsProviderContent(context, "")
        Locale.setDefault(Locale.forLanguageTag("en-US"))

        assertThat(tester.get<String>(MeasurementSystemApiFirstScreen.KEY_MEASUREMENT_SYSTEM_ITEM))
            .isEqualTo("default")
    }

    @Test
    fun setPreference_valueIsDefault_returnUseDefault() {
        tester.set(MeasurementSystemApiFirstScreen.KEY_MEASUREMENT_SYSTEM_ITEM, "default")

        assertThat(tester.get<String>(MeasurementSystemApiFirstScreen.KEY_MEASUREMENT_SYSTEM_ITEM))
            .isEqualTo("default")
    }

    @Test
    fun setPreference_valueIsUk_returnUksystem() {
        tester.set(MeasurementSystemApiFirstScreen.KEY_MEASUREMENT_SYSTEM_ITEM, "uksystem")

        assertThat(tester.get<String>(MeasurementSystemApiFirstScreen.KEY_MEASUREMENT_SYSTEM_ITEM))
            .isEqualTo("uksystem")
    }
}
