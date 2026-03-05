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
import com.android.settings.accessibility.autoclick.dialogs.AutoclickCursorAreaSizeDialogFragment
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

/** Tests for [AutoclickCursorAreaSizePreference]. */
@RunWith(RobolectricTestRunner::class)
class AutoclickCursorAreaSizePreferenceTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val secureStore by lazy { SettingsSecureStore.get(context) }
    private lateinit var preference: AutoclickCursorAreaSizePreference

    @Before
    fun setUp() {
        preference = AutoclickCursorAreaSizePreference()
    }

    @Test
    fun key_isAccessibilityAutoclickCursorAreaSizeSettingKey() {
        assertThat(preference.key)
            .isEqualTo(Settings.Secure.ACCESSIBILITY_AUTOCLICK_CURSOR_AREA_SIZE)
    }

    @Test
    fun title_isAutoclickCursorAreaSizeTitle() {
        assertThat(context.getString(preference.title))
            .isEqualTo(context.getString(R.string.autoclick_cursor_area_size_title))
    }

    @Test
    fun purpose_isAutoclickCursorAreaSizePurpose() {
        assertThat(context.getString(preference.purpose))
            .isEqualTo(context.getString(R.string.a11y_autoclick_cursor_area_size_purpose))
    }

    @Test
    fun getSummary_autoclickCursorAreaSizeSet_returnsFormattedSummary() {
        val size = 40
        secureStore.setInt(Settings.Secure.ACCESSIBILITY_AUTOCLICK_CURSOR_AREA_SIZE, size)

        assertThat(preference.getSummary(context).toString())
            .isEqualTo(context.getString(R.string.autoclick_cursor_area_size_dialog_option_small))
    }

    @Test
    fun getSummary_autoclickCursorAreaSizeNotSet_returnsDefaultSizeSummary() {
        secureStore.setInt(Settings.Secure.ACCESSIBILITY_AUTOCLICK_CURSOR_AREA_SIZE, null)

        assertThat(preference.getSummary(context).toString())
            .isEqualTo(context.getString(R.string.autoclick_cursor_area_size_dialog_option_default))
    }

    @Test
    fun storage_returnsSettingsSecureStoreWithDefault() {
        val store = preference.storage(context)
        assertThat(store).isInstanceOf(SettingsSecureStore::class.java)
        assertThat(store.getDefaultValue(preference.key, Int::class.javaObjectType))
            .isEqualTo(AccessibilityManager.AUTOCLICK_CURSOR_AREA_SIZE_DEFAULT)
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
    fun onStart_validPreference_showsCursorAreaSizeDialogOnClick() {
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

                assertDialogShown(fragment, AutoclickCursorAreaSizeDialogFragment::class.java)
            }
            .close()
    }
}
