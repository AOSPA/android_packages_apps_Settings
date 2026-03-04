/**
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.settings.regionalpreferences

import android.app.IActivityManager
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.internal.app.LocalePicker
import com.android.internal.app.LocaleStore
import com.android.settings.flags.Flags
import com.android.settings.testutils.shadow.ShadowActivityManager
import com.android.settings.testutils2.ApiTester
import com.google.common.truth.Truth.assertThat
import java.util.Locale
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowTelephonyManager

@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowActivityManager::class])
class RegionPickerApiFirstScreenTest {
    @get:Rule val setFlagsRule = SetFlagsRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val iActivityManager = mock<IActivityManager>()
    private val tester = ApiTester(RegionPickerApiFirstScreen())
    private val cacheLocaleList: LocaleList = LocaleList.getDefault()
    private val cacheLocale: Locale = Locale.getDefault(Locale.Category.FORMAT)

    @Before
    @Throws(java.lang.Exception::class)
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        setupLocaleInfoList(
            Locale.US,
            "en-US" to "English (United States)",
            "es-US" to "Español (Estados Unidos)",
        )
        val shadowTelephonyManager: ShadowTelephonyManager =
            Shadows.shadowOf(context.getSystemService(TelephonyManager::class.java))
        shadowTelephonyManager.setSimCountryIso("us")
        shadowTelephonyManager.setNetworkCountryIso("us")
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
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
    fun systemLocaleRegion_getDefault_returnDefaultLanguageTag() {
        setupLocaleInfoList(
            Locale.forLanguageTag("fr-FR"),
            "fr-FR" to "Français (France)",
            "en-US" to "English (United States)",
        )

        assertThat(tester.get<String>(KEY_PREFERENCE)).isEqualTo("FR")
    }

    @Test
    fun setDefaultSystemLanguageRegion_isSet() {
        tester.set(KEY_PREFERENCE, "GB")

        assertThat(Locale.getDefault().toLanguageTag()).isEqualTo("en-GB")
        assertThat(LocaleStore.getLocaleInfo(Locale.getDefault()).fullCountryNameNative)
            .isEqualTo("United Kingdom")

        Locale.setDefault(Locale.forLanguageTag("fr-FR"))
        tester.set(KEY_PREFERENCE, "FR")

        assertThat(Locale.getDefault().toLanguageTag()).isEqualTo("fr-FR")
        assertThat(LocaleStore.getLocaleInfo(Locale.getDefault()).fullCountryNameNative)
            .isEqualTo("France")
    }

    @Test
    fun setDefaultSystemLanguageRegion_isSet_withExtension() {
        RegionalPreferenceTestUtils.setSettingsProviderContent(context, "")
        Locale.setDefault(Locale.forLanguageTag("en-US-u-ms-ussystem"))
        tester.set(KEY_PREFERENCE, "IN")

        assertThat(Locale.getDefault().toLanguageTag()).isEqualTo("en-IN-u-ms-ussystem")
        assertThat(LocaleStore.getLocaleInfo(Locale.getDefault()).fullCountryNameNative)
            .isEqualTo("India")
    }

    @Test
    fun systemLocaleRegion_optionsAreInSupportedList() {
        runBlocking {
            Locale.setDefault(Locale.US)

            assertThat(tester.getPreferenceOptions<String>(KEY_PREFERENCE))
                .containsAtLeast(
                    ("US" to "United States"),
                    ("GB" to "United Kingdom"),
                    ("IN" to "India"),
                )
        }
    }

    private fun setupLocaleInfoList(defaultLocale: Locale, vararg languages: Pair<String, String>) {
        val config = Configuration()
        config.setLocales(getLocaleList(*languages))
        whenever(iActivityManager.getConfiguration()).thenReturn(config)
        ShadowActivityManager.setService(iActivityManager)
        Locale.setDefault(defaultLocale)
    }

    private fun getLocaleList(vararg languages: Pair<String, String>): LocaleList {
        val localeInfoList =
            languages.map { (languageTag, languageName) ->
                mock<LocaleStore.LocaleInfo>().apply {
                    whenever(fullNameNative).thenReturn(languageName)
                    whenever(isTranslated).thenReturn(true)
                    whenever(locale).thenReturn(Locale.forLanguageTag(languageTag))
                }
            }

        val locales = localeInfoList.map { it.locale }.toTypedArray()
        return LocaleList(*locales)
    }

    companion object {
        const val KEY_PREFERENCE = "default_system_language_region_preference"
    }
}
