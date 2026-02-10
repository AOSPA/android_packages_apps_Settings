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

package com.android.settings.accessibility.autoclick.ui

import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragment
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.accessibility.AutoclickDelayDialogFragment
import com.android.settings.accessibility.AutoclickUtils
import com.android.settings.accessibility.AutoclickUtils.MAX_AUTOCLICK_DELAY_MS
import com.android.settings.accessibility.AutoclickUtils.MIN_AUTOCLICK_DELAY_MS
import com.android.settings.testutils.AccessibilityTestUtils.assertDialogShown
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils.inflateViewHolder
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

/** Tests for [AutoclickDelayPreference]. */
@RunWith(RobolectricTestRunner::class)
class AutoclickDelayPreferenceTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val secureStore by lazy { SettingsSecureStore.get(context) }
    private lateinit var preference: AutoclickDelayPreference

    @Before
    fun setUp() {
        preference = AutoclickDelayPreference(context)
    }

    @Test
    fun key_isAccessibilityAutoclickDelaySettingKey() {
        assertThat(preference.key).isEqualTo(Settings.Secure.ACCESSIBILITY_AUTOCLICK_DELAY)
    }

    @Test
    fun title_isAutoclickDelayBeforeClickTitle() {
        assertThat(context.getString(preference.title))
            .isEqualTo(context.getString(R.string.autoclick_delay_before_click_title))
    }

    @Test
    fun purpose_isAutoclickDelayPurpose() {
        assertThat(context.getString(preference.purpose))
            .isEqualTo(context.getString(R.string.a11y_autoclick_delay_purpose))
    }

    @Test
    fun getMinValue_returnsAutoclickMin() {
        assertThat(preference.getMinValue(context)).isEqualTo(MIN_AUTOCLICK_DELAY_MS)
    }

    @Test
    fun getMaxValue_returnsAutoclickMax() {
        assertThat(preference.getMaxValue(context)).isEqualTo(MAX_AUTOCLICK_DELAY_MS)
    }

    @Test
    fun getSummary_autoclickDelaySet_returnsFormattedSummary() {
        val delay = 600
        secureStore.setInt(Settings.Secure.ACCESSIBILITY_AUTOCLICK_DELAY, delay)

        val expectedSummary =
            AutoclickUtils.getAutoclickDelaySummary(
                context,
                R.string.accessibility_autoclick_delay_unit_second,
                delay,
            )
        assertThat(preference.getSummary(context).toString()).isEqualTo(expectedSummary.toString())
    }

    @Test
    fun getSummary_autoclickDelayNotSet_returnsDefaultDelaySummary() {
        secureStore.setInt(Settings.Secure.ACCESSIBILITY_AUTOCLICK_DELAY, null)

        val expectedSummary =
            AutoclickUtils.getAutoclickDelaySummary(
                context,
                R.string.accessibility_autoclick_delay_unit_second,
                AccessibilityManager.AUTOCLICK_DELAY_WITH_INDICATOR_DEFAULT,
            )
        assertThat(preference.getSummary(context).toString()).isEqualTo(expectedSummary.toString())
    }

    @Test
    fun storage_returnsSettingsSecureStoreWithDefault() {
        val store = preference.storage(context)
        assertThat(store).isInstanceOf(SettingsSecureStore::class.java)
        assertThat(store.getDefaultValue(preference.key, Int::class.javaObjectType))
            .isEqualTo(AccessibilityManager.AUTOCLICK_DELAY_WITH_INDICATOR_DEFAULT)
    }

    @Test
    fun onStart_nullPreference_doesNotCrash() {
        val mockLifecycleContext =
            mock<PreferenceLifecycleContext> {
                on { applicationContext } doReturn context
                on { findPreference<Preference>(any()) } doReturn null
            }

        preference.onStart(mockLifecycleContext)

        verify(mockLifecycleContext, never()).childFragmentManager
    }

    @Test
    fun onStart_validPreference_showsDelayDialogOnClick() {
        launchFragment<Fragment>(themeResId = androidx.appcompat.R.style.Theme_AppCompat)
            .onFragment { fragment ->
                val preferenceScreen = PreferenceManager(context).createPreferenceScreen(context)
                val preferenceWidget =
                    preference.createAndBindWidget<Preference>(context, preferenceScreen)
                preferenceScreen.addPreference(preferenceWidget)
                val preferenceLifecycleContext: PreferenceLifecycleContext = mock {
                    on { findPreference<Preference>(preference.bindingKey) } doReturn
                        preferenceWidget
                    on { childFragmentManager } doReturn fragment.childFragmentManager
                }

                preference.onStart(preferenceLifecycleContext)
                preferenceWidget.inflateViewHolder()
                preferenceWidget.performClick()

                assertDialogShown(fragment, AutoclickDelayDialogFragment::class.java)
            }
    }

    @Test
    fun init_delayBelowMinimum_resetsToDefaultValue() {
        // Less than MIN
        secureStore.setInt(
            Settings.Secure.ACCESSIBILITY_AUTOCLICK_DELAY,
            MIN_AUTOCLICK_DELAY_MS - 1,
        )

        // Initialize AutoclickDelayPreference
        AutoclickDelayPreference(context)

        assertThat(secureStore.getInt(Settings.Secure.ACCESSIBILITY_AUTOCLICK_DELAY))
            .isEqualTo(AccessibilityManager.AUTOCLICK_DELAY_WITH_INDICATOR_DEFAULT)
    }
}
