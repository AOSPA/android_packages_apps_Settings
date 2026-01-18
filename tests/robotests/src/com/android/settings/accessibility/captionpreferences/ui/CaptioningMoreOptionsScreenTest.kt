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

package com.android.settings.accessibility.captionpreferences.ui

import android.app.settings.SettingsEnums
import android.content.Context
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import androidx.fragment.app.testing.FragmentScenario
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.test.core.app.ApplicationProvider
import com.android.internal.app.LocalePicker
import com.android.settings.R
import com.android.settings.accessibility.CaptioningMoreOptionsFragment
import com.android.settings.accessibility.Flags
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.datastore.SettingsSecureStore
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameters
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.shadows.ShadowLooper

/** Test for [CaptioningMoreOptionsScreen] */
@RunWith(RobolectricTestParameterInjector::class)
class CaptioningMoreOptionsScreenTest {
    @get:Rule val setFlagsRule = SetFlagsRule()
    private val appContext = ApplicationProvider.getApplicationContext<Context>()
    private val preferenceScreenCreator = CaptioningMoreOptionsScreen()

    @get:Rule val settingsStoreRule = SettingsStoreRule()

    @Test
    fun key_returnsCorrectValue() {
        assertThat(preferenceScreenCreator.key).isEqualTo(CaptioningMoreOptionsScreen.KEY)
    }

    @Test
    fun title_returnsCorrectValue() {
        assertThat(preferenceScreenCreator.title).isEqualTo(R.string.captioning_more_options_title)
    }

    @Test
    fun purpose_returnsCorrectValue() {
        assertThat(preferenceScreenCreator.purpose)
            .isEqualTo(R.string.caption_preferences_more_options_screen_purpose)
    }

    @Test
    fun highlightMenuKey_returnsCorrectValue() {
        assertThat(preferenceScreenCreator.highlightMenuKey)
            .isEqualTo(R.string.menu_key_accessibility)
    }

    @Test
    fun isFlagEnabled_returnsCorrectValue() {
        assertThat(preferenceScreenCreator.isFlagEnabled(appContext))
            .isEqualTo(Flags.catalystCaptionPreferencesScreen())
    }

    @Test
    fun fragmentClass_returnsCorrectValue() {
        assertThat(preferenceScreenCreator.fragmentClass())
            .isEqualTo(CaptioningMoreOptionsFragment::class.java)
    }

    @Test
    fun getMetricsCategory_returnsCorrectValue() {
        assertThat(preferenceScreenCreator.getMetricsCategory())
            .isEqualTo(SettingsEnums.ACCESSIBILITY_CAPTION_MORE_OPTIONS)
    }

    @Test
    fun isIndexable_whenCaptioningEnabled_returnsTrue() {
        SettingsSecureStore.get(appContext)
            .setBoolean(Settings.Secure.ACCESSIBILITY_CAPTIONING_ENABLED, true)

        assertThat(preferenceScreenCreator.isIndexable(appContext)).isTrue()
    }

    @Test
    fun isIndexable_whenCaptioningDisabled_returnsFalse() {
        SettingsSecureStore.get(appContext)
            .setBoolean(Settings.Secure.ACCESSIBILITY_CAPTIONING_ENABLED, false)

        assertThat(preferenceScreenCreator.isIndexable(appContext)).isFalse()
    }

    @TestParameters("{flagEnabled: false}", "{flagEnabled: true}")
    @Test
    fun launchFragment_verifyOptionFooterOrder(flagEnabled: Boolean) {
        setFlagValue(flagEnabled)
        launchFragment { fragment ->
            val localePreference = requireNotNull(getLocalePreference(fragment))
            val footerPreference = requireNotNull(getFooterPreference(fragment))

            assertThat(localePreference.order).isLessThan(footerPreference.order)
        }
    }

    @TestParameters("{flagEnabled: false}", "{flagEnabled: true}")
    @Test
    fun launchFragment_verifyFooterTitle(flagEnabled: Boolean) {
        setFlagValue(flagEnabled)
        launchFragment { fragment ->
            val footerPreference = requireNotNull(getFooterPreference(fragment))

            assertThat(footerPreference.title.toString())
                .isEqualTo(
                    appContext.getString(R.string.accessibility_captioning_preference_summary)
                )
        }
    }

    @TestParameters("{flagEnabled: false}", "{flagEnabled: true}")
    @Test
    fun launchFragment_verifyDefaultLanguageTitleAndSummary(flagEnabled: Boolean) {
        setFlagValue(flagEnabled)
        launchFragment { fragment ->
            val localePreference = requireNotNull(getLocalePreference(fragment))

            assertThat(localePreference.isPersistent).isTrue()
            assertThat(localePreference.title.toString())
                .isEqualTo(appContext.getString(R.string.captioning_locale))
            assertThat(localePreference.summary.toString())
                .isEqualTo(appContext.getString(R.string.locale_default))
        }
    }

    @TestParameters("{flagEnabled: false}", "{flagEnabled: true}")
    @Test
    fun launchFragment_changeLocale_verifySettingSavedAndSummaryUpdated(flagEnabled: Boolean) {
        setFlagValue(flagEnabled)
        launchFragment { fragment ->
            val localePreference = requireNotNull(getLocalePreference(fragment)) as ListPreference
            val localeInfos =
                LocalePicker.getAllAssetLocales(appContext, /* isInDeveloperMode= */ false)
            assumeTrue(
                "There should be at least one locale to make this test effective",
                localeInfos.isNotEmpty(),
            )
            val newLocaleInfo = localeInfos[0]
            val newLocale = newLocaleInfo.locale.toString()
            localePreference.callChangeListener(newLocale)
            localePreference.value = newLocale
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

            assertThat(localePreference.summary.toString()).isEqualTo(newLocaleInfo.label)
            assertThat(
                    Settings.Secure.getString(
                        appContext.contentResolver,
                        Settings.Secure.ACCESSIBILITY_CAPTIONING_LOCALE,
                    )
                )
                .isEqualTo(newLocaleInfo.locale.toString())
        }
    }

    private fun getFooterPreference(fragment: PreferenceFragmentCompat): Preference? {
        return fragment.findPreference("captioning_more_options_footer")
    }

    private fun getLocalePreference(fragment: PreferenceFragmentCompat): Preference? {
        return fragment.findPreference(
            if (Flags.catalystCaptionPreferencesScreen())
                Settings.Secure.ACCESSIBILITY_CAPTIONING_LOCALE
            else "captioning_locale"
        )
    }

    private fun launchFragment(action: (PreferenceFragmentCompat) -> Unit) =
        @Suppress("UNCHECKED_CAST")
        FragmentScenario.launch(
                preferenceScreenCreator.fragmentClass() as Class<PreferenceFragmentCompat>
            )
            .use { it.onFragment(action) }

    private val flagName: String = Flags.FLAG_CATALYST_CAPTION_PREFERENCES_SCREEN

    @Suppress("DEPRECATION")
    private fun setFlagValue(enabled: Boolean) {
        if (enabled) {
            setFlagsRule.enableFlags(flagName)
        } else {
            setFlagsRule.disableFlags(flagName)
        }
    }
}
