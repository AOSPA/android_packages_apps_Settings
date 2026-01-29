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
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.internal.app.LocalePicker
import com.android.internal.app.LocaleStore
import com.android.settings.flags.Flags
import com.android.settings.regionalpreferences.NumberingSystemItemController.KEY_SELECTED_LANGUAGE
import com.android.settings.regionalpreferences.NumberingSystemLocaleListApiFirstScreenTest.ShadowRegionalPreferencesDataUtils.Companion.setNumberingSystemLocales
import com.android.settings.testutils.shadow.ShadowActivityManager
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.Parameters
import com.google.common.truth.Truth.assertThat
import java.util.Locale
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
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowTelephonyManager

@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowActivityManager::class])
class NumberingSystemLocaleListApiFirstScreenTest {
    @get:Rule
    val setFlagsRule = SetFlagsRule()
    private var context: Context = ApplicationProvider.getApplicationContext()
    private val tester = ApiTester(NumberingSystemLocaleListApiFirstScreen())
    private val iActivityManager = mock<IActivityManager>()
    private val cacheLocaleList: LocaleList = LocaleList.getDefault()
    private val cacheLocale: Locale = Locale.getDefault(Locale.Category.FORMAT)

    @Before
    @Throws(java.lang.Exception::class)
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        LocaleStore.fillCache(context)
        setupLocaleInfoList(
            Locale.US,
            "mzn-IR",
            "en-US" to "English (United States)",
            "mzn-IR" to "مازرونی (ایران)",
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
        LocaleList.setDefault(cacheLocaleList)
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
    @Config(shadows = [ShadowRegionalPreferencesDataUtils::class])
    fun setLanguageNumberingSystem_setLatn_isSet() {
        setNumberingSystemLocales("en-US", "mzn-IR")
        setupLocaleInfoList(
            Locale.US,
            "mzn-IR",
            "en-US" to "English (United States)",
            "mzn-IR" to "مازرونی (ایران)",
        )
        tester.initializeScreenParameters(Parameters(KEY_SELECTED_LANGUAGE to "mzn-IR"))
        tester.set(KEY_PREFERENCE, "mzn-IR-u-nu-latn")

        assertThat(tester.get<String>(KEY_PREFERENCE)).isEqualTo("mzn-IR-u-nu-latn")
    }

    @Test
    @Config(shadows = [ShadowRegionalPreferencesDataUtils::class])
    fun setLanguageNumberingSystem_setDefault_isSet() {
        setNumberingSystemLocales("en-US", "mzn-IR")
        setupLocaleInfoList(
            Locale.US,
            "mzn-IR",
            "en-US" to "English (United States)",
            "mzn-IR-u-nu-latn" to "مازرونی (ایران)",
        )
        tester.initializeScreenParameters(Parameters(KEY_SELECTED_LANGUAGE to "mzn-IR"))
        tester.set(KEY_PREFERENCE, "mzn-IR")

        assertThat(tester.get<String>(KEY_PREFERENCE)).isEqualTo("mzn-IR")
    }

    @Test
    @Config(shadows = [ShadowRegionalPreferencesDataUtils::class])
    fun setLanguageNumberingSystem_allHasNumberingSystem_isSet() {
        setNumberingSystemLocales("ar-EG", "mzn-IR")
        setupLocaleInfoList(
            Locale.forLanguageTag("ar-EG"),
            "mzn-IR",
            "ar-EG" to "العربية (مصر)",
            "mzn-IR" to "مازرونی (ایران)",
        )
        tester.initializeScreenParameters(Parameters(KEY_SELECTED_LANGUAGE to "ar-EG"))
        tester.set(KEY_PREFERENCE, "ar-EG-u-nu-latn")

        assertThat(tester.get<String>(KEY_PREFERENCE)).isEqualTo("ar-EG-u-nu-latn")
    }

    private fun setupLocaleInfoList(
        defaultLocale: Locale,
        parameterLanguage: String,
        vararg languages: Pair<String, String>,
    ) {
        ShadowActivityManager.setService(iActivityManager)
        val config = Configuration()
        val localeList = getLocaleList(parameterLanguage, *languages)
        config.setLocales(localeList)
        whenever(iActivityManager.getConfiguration()).thenReturn(config)
        Locale.setDefault(defaultLocale)
        LocaleList.setDefault(localeList)
        LocalePicker.updateLocales(localeList)
    }

    private fun getLocaleList(
        parameterLanguage: String,
        vararg languages: Pair<String, String>,
    ): LocaleList {
        val localeInfoList =
            languages.map { (languageTag, languageName) ->
                mock<LocaleStore.LocaleInfo>().apply {
                    whenever(fullNameNative).thenReturn(languageName)
                    whenever(isTranslated).thenReturn(true)
                    whenever(locale).thenReturn(Locale.forLanguageTag(languageTag))
                    if (languageTag.startsWith(parameterLanguage)) {
                        whenever(hasNumberingSystems()).thenReturn(true)
                    }
                }
            }
        val locales = localeInfoList.map { it.locale }.toTypedArray()
        return LocaleList(*locales)
    }

    @Implements(RegionalPreferencesDataUtils::class)
    class ShadowRegionalPreferencesDataUtils {
        companion object {
            @JvmStatic var firstLanguageCode = ""
            @JvmStatic var secondLanguageCode = ""

            @Implementation
            @JvmStatic
            fun getNumberingSystemLocales(): Set<Locale> =
                setOf(
                    Locale.forLanguageTag(firstLanguageCode),
                    Locale.forLanguageTag(secondLanguageCode),
                )

            fun setNumberingSystemLocales(firstLanguageCode: String, secondLanguageCode: String) {
                this.firstLanguageCode = firstLanguageCode
                this.secondLanguageCode = secondLanguageCode
            }
        }
    }

    companion object {
        const val KEY_PREFERENCE: String = "numbering_system_preference"
    }
}
