/**
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.settings.localepicker

import android.app.GrammaticalInflectionManager
import android.content.Context
import android.os.LocaleList
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.internal.app.LocalePicker
import com.android.settings.flags.Flags
import com.android.settings.localepicker.TermsOfAddressApiFirstScreen.Companion.KEY_PREFERENCE
import com.android.settings.testutils2.ApiTester
import com.google.common.truth.Truth.assertThat
import java.util.Locale
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@RunWith(AndroidJUnit4::class)
class TermsOfAddressApiFirstScreenTest {
    @get:Rule val setFlagsRule = SetFlagsRule()
    private var context: Context = ApplicationProvider.getApplicationContext()
    private val tester = ApiTester(TermsOfAddressApiFirstScreen())
    private val cacheLocaleList: LocaleList = LocaleList.getDefault()
    private val cacheLocale: Locale = Locale.getDefault(Locale.Category.FORMAT)

    @After
    @Throws(Exception::class)
    fun tearDown() {
        Locale.setDefault(cacheLocale)
        LocalePicker.updateLocales(cacheLocaleList)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2, Flags.FLAG_TERMS_OF_ADDRESS_ENABLED)
    fun getScreen_allFlagsEnabled_isNotNull() {
        assertThat(tester.getScreen()).isNotNull()
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    @EnableFlags(Flags.FLAG_TERMS_OF_ADDRESS_ENABLED)
    fun getScreen_catalystFlagDisabled_isNull() {
        assertThat(tester.getScreen()).isNull()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    @DisableFlags(Flags.FLAG_TERMS_OF_ADDRESS_ENABLED)
    fun getScreen_termsOfAddressFlagDisabled_isNull() {
        assertThat(tester.getScreen()).isNull()
    }

    @Test
    fun termOfAddress_optionsAreCorrect() {
        runBlocking {
            assertThat(tester.getPreferenceOptions<Int>(KEY_PREFERENCE))
                .containsExactly(
                    (0 to "Not specified"),
                    (1 to "Neutral"),
                    (2 to "Feminine"),
                    (3 to "Masculine"),
                )
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2, Flags.FLAG_TERMS_OF_ADDRESS_ENABLED)
    @Config(shadows = [ShadowGrammaticalInflectionManager::class, ShadowLocaleUtils::class])
    fun termOfAddress_getDefaultSystemGrammaticalGender_return_1() {
        ShadowGrammaticalInflectionManager.setSystemWideGrammaticalGender(1)

        assertThat(tester.get<Int>(KEY_PREFERENCE)).isEqualTo(1)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2, Flags.FLAG_TERMS_OF_ADDRESS_ENABLED)
    @Config(shadows = [ShadowGrammaticalInflectionManager::class, ShadowLocaleUtils::class])
    fun setDefaultSystemGrammaticalGender_isSet() {
        tester.set(KEY_PREFERENCE, 2)
        val grammaticalInflectionManager =
            context.getSystemService(GrammaticalInflectionManager::class.java)
        assertThat(grammaticalInflectionManager.systemGrammaticalGender).isEqualTo(2)

        tester.set(KEY_PREFERENCE, 3)
        assertThat(grammaticalInflectionManager.systemGrammaticalGender).isEqualTo(3)
    }

    @Implements(GrammaticalInflectionManager::class)
    class ShadowGrammaticalInflectionManager {
        companion object {
            @JvmStatic var sSystemGrammaticalGender = 0

            @Implementation
            @JvmStatic
            fun getSystemGrammaticalGender(): Int = sSystemGrammaticalGender

            @Implementation
            @JvmStatic
            fun setSystemWideGrammaticalGender(gender: Int) {
                sSystemGrammaticalGender = gender
            }
        }
    }

    @Implements(LocaleUtils::class)
    class ShadowLocaleUtils {
        companion object {
            @Implementation
            @JvmStatic
            fun isTermsOfAddressAvailable(context: Context): Boolean = true
        }
    }
}
