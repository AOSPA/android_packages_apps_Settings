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
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_3BUTTON
import android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragment
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.accessibility.AutoclickNavigationSuggestionDialogFragment
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper

/** Tests for [AutoclickMainSwitchPreference]. */
@RunWith(RobolectricTestRunner::class)
class AutoclickMainSwitchPreferenceTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val secureStore by lazy { SettingsSecureStore.get(context) }
    private lateinit var preference: AutoclickMainSwitchPreference

    @Before
    fun setUp() {
        preference = AutoclickMainSwitchPreference()
    }

    @Test
    fun key_isAutoClickEnabledSettingKey() {
        assertThat(preference.key).isEqualTo(Settings.Secure.ACCESSIBILITY_AUTOCLICK_ENABLED)
    }

    @Test
    fun title_isAutoclickMainSwitchTitle() {
        assertThat(context.getString(preference.title))
            .isEqualTo(context.getString(R.string.accessibility_autoclick_main_switch_title))
    }

    @Test
    fun purpose_isAutoclickMainSwitchPurpose() {
        assertThat(context.getString(preference.purpose))
            .isEqualTo(context.getString(R.string.a11y_autoclick_main_switch_purpose))
    }

    @Test
    fun storage_returnsSettingsSecureStore() {
        assertThat(preference.storage(context)).isInstanceOf(SettingsSecureStore::class.java)
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
    fun onStart_turnOffAutoclick_disablesAutoclickAndDoesNotShowDialog() {
        secureStore.setBoolean(Settings.Secure.ACCESSIBILITY_AUTOCLICK_ENABLED, true)
        secureStore.setInt(Settings.Secure.NAVIGATION_MODE, NAV_BAR_MODE_GESTURAL)

        launchPreferenceClick { fragment ->
            assertThat(secureStore.getBoolean(Settings.Secure.ACCESSIBILITY_AUTOCLICK_ENABLED))
                .isFalse()
            assertThat(fragment.childFragmentManager.fragments).isEmpty()
        }
    }

    @Test
    fun onStart_turnOnAutoclickWhenGesturalNavDisabled_enablesAutoclickAndDoesNotShowDialog() {
        secureStore.setBoolean(Settings.Secure.ACCESSIBILITY_AUTOCLICK_ENABLED, false)
        secureStore.setInt(Settings.Secure.NAVIGATION_MODE, NAV_BAR_MODE_3BUTTON)

        launchPreferenceClick { fragment ->
            assertThat(secureStore.getBoolean(Settings.Secure.ACCESSIBILITY_AUTOCLICK_ENABLED))
                .isTrue()
            assertThat(fragment.childFragmentManager.fragments).isEmpty()
        }
    }

    @Test
    fun onStart_turnOnAutoclickOnLaptop_enablesAutoclickAndDoesNotShowDialog() {
        secureStore.setBoolean(Settings.Secure.ACCESSIBILITY_AUTOCLICK_ENABLED, false)
        secureStore.setInt(Settings.Secure.NAVIGATION_MODE, NAV_BAR_MODE_GESTURAL)
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_PC, true)

        launchPreferenceClick { fragment ->
            assertThat(secureStore.getBoolean(Settings.Secure.ACCESSIBILITY_AUTOCLICK_ENABLED))
                .isTrue()
            assertThat(fragment.childFragmentManager.fragments).isEmpty()
        }
    }

    @Test
    fun onStart_turnOnAutoclickWhenGesturalNavEnabled_enablesAutoclickAndShowsNavigationSuggestionDialog() {
        secureStore.setBoolean(Settings.Secure.ACCESSIBILITY_AUTOCLICK_ENABLED, false)
        secureStore.setInt(Settings.Secure.NAVIGATION_MODE, NAV_BAR_MODE_GESTURAL)
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_PC, false)

        launchPreferenceClick { fragment ->
            assertThat(secureStore.getBoolean(Settings.Secure.ACCESSIBILITY_AUTOCLICK_ENABLED))
                .isTrue()
            assertDialogShown(fragment, AutoclickNavigationSuggestionDialogFragment::class.java)
        }
    }

    private fun launchPreferenceClick(onVerify: (Fragment) -> Unit) {
        launchFragment<Fragment>(themeResId = androidx.appcompat.R.style.Theme_AppCompat)
            .onFragment { fragment ->
                val preferenceScreen = PreferenceManager(context).createPreferenceScreen(context)
                val preferenceWidget =
                    preference.createAndBindWidget<Preference>(context, preferenceScreen)
                preferenceScreen.addPreference(preferenceWidget)
                val mockLifecycleContext: PreferenceLifecycleContext = mock {
                    on { applicationContext } doReturn context
                    on { packageManager } doReturn context.packageManager
                    on { findPreference<Preference>(preference.bindingKey) } doReturn
                        preferenceWidget
                    on { childFragmentManager } doReturn fragment.childFragmentManager
                }

                preference.onStart(mockLifecycleContext)
                preferenceWidget.inflateViewHolder()
                preferenceWidget.performClick()
                ShadowLooper.idleMainLooper()

                onVerify(fragment)
            }
    }
}
