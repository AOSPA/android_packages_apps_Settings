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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.provider.Settings
import android.provider.Settings.Secure.SEARCH_CONTENT_FILTERS_ENABLED
import android.provider.Settings.SettingNotFoundException
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.SelectorWithWidgetPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class SupervisionSafeSearchPreferenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var mockLifeCycleContext: PreferenceLifecycleContext
    private lateinit var mockActivityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var mockPackageManager: PackageManager
    private lateinit var dataStore: SupervisionSafeSearchDataStore
    private lateinit var searchFilterOffPreference: SupervisionSearchFilterOffPreference
    private lateinit var searchFilterOnPreference: SupervisionSearchFilterOnPreference

    @Before
    fun setUp() {
        dataStore = SupervisionSafeSearchDataStore(context)
        mockLifeCycleContext = mock(PreferenceLifecycleContext::class.java)
        mockActivityResultLauncher =
            mock(ActivityResultLauncher::class.java) as ActivityResultLauncher<Intent>
        mockPackageManager = mock(PackageManager::class.java)
        mockConfirmSupervisionCredentialsActivity()
        searchFilterOffPreference = SupervisionSearchFilterOffPreference(dataStore)
        searchFilterOffPreference.onCreate(mockLifeCycleContext)
        searchFilterOnPreference = SupervisionSearchFilterOnPreference(dataStore)
        searchFilterOnPreference.onCreate(mockLifeCycleContext)
    }

    @Test
    fun getTitle_filterOn() {
        assertThat(searchFilterOnPreference.title)
            .isEqualTo(R.string.supervision_web_content_filters_search_filter_on_title)
    }

    @Test
    fun getSummary_filterOn() {
        assertThat(searchFilterOnPreference.summary)
            .isEqualTo(R.string.supervision_web_content_filters_search_filter_on_summary)
    }

    @Test
    fun getTitle_filterOff() {
        assertThat(searchFilterOffPreference.title)
            .isEqualTo(R.string.supervision_web_content_filters_search_filter_off_title)
    }

    @Test
    fun getSummary_filterOff() {
        assertThat(searchFilterOffPreference.summary)
            .isEqualTo(R.string.supervision_web_content_filters_search_filter_off_summary)
    }

    @Test
    fun filterOffIsChecked_whenNoValueIsSet() {
        assertThrows(SettingNotFoundException::class.java) {
            Settings.Secure.getInt(context.getContentResolver(), SEARCH_CONTENT_FILTERS_ENABLED)
        }
        assertThat(getFilterOnWidget().isChecked).isFalse()
        assertThat(getFilterOffWidget().isChecked).isTrue()
    }

    @Test
    fun filterOnIsChecked_whenPreviouslyEnabled() {
        Settings.Secure.putInt(context.getContentResolver(), SEARCH_CONTENT_FILTERS_ENABLED, 1)
        assertThat(getFilterOffWidget().isChecked).isFalse()
        assertThat(getFilterOnWidget().isChecked).isTrue()
    }

    @Test
    fun clickFilterOn_failedToEnablesFilter_activityFailed() {
        Settings.Secure.putInt(context.getContentResolver(), SEARCH_CONTENT_FILTERS_ENABLED, 0)
        val filterOnWidget = getFilterOnWidget()
        assertThat(filterOnWidget.isChecked).isFalse()

        filterOnWidget.performClick()
        verifyConfirmSupervisionCredentialsActivity()
        searchFilterOnPreference.onConfirmCredentials(
            ActivityResult(Activity.RESULT_CANCELED, null)
        )

        assertThat(
                Settings.Secure.getInt(context.getContentResolver(), SEARCH_CONTENT_FILTERS_ENABLED)
            )
            .isEqualTo(0)
        assertThat(filterOnWidget.isChecked).isFalse()
    }

    @Test
    fun clickFilterOn_unresolvedIntent_activityNotLaunched() {
        `when`(mockPackageManager.queryIntentActivitiesAsUser(any<Intent>(), anyInt(), anyInt()))
            .thenReturn(emptyList<ResolveInfo>())

        Settings.Secure.putInt(context.getContentResolver(), SEARCH_CONTENT_FILTERS_ENABLED, 0)
        val filterOnWidget = getFilterOnWidget()
        assertThat(filterOnWidget.isChecked).isFalse()

        filterOnWidget.performClick()

        verify(mockActivityResultLauncher, never()).launch(any())
        assertThat(
                Settings.Secure.getInt(context.getContentResolver(), SEARCH_CONTENT_FILTERS_ENABLED)
            )
            .isEqualTo(0)
        assertThat(filterOnWidget.isChecked).isFalse()
    }

    @Test
    fun clickFilterOn_enablesFilter() {
        Settings.Secure.putInt(context.getContentResolver(), SEARCH_CONTENT_FILTERS_ENABLED, -1)
        val filterOnWidget = getFilterOnWidget()
        assertThat(filterOnWidget.isChecked).isFalse()

        filterOnWidget.performClick()
        verifyConfirmSupervisionCredentialsActivity()
        searchFilterOnPreference.onConfirmCredentials(ActivityResult(Activity.RESULT_OK, null))

        assertThat(
                Settings.Secure.getInt(context.getContentResolver(), SEARCH_CONTENT_FILTERS_ENABLED)
            )
            .isEqualTo(1)
        assertThat(filterOnWidget.isChecked).isTrue()
    }

    @Test
    fun clickFilterOff_disablesFilter() {
        Settings.Secure.putInt(context.getContentResolver(), SEARCH_CONTENT_FILTERS_ENABLED, 1)
        val filterOffWidget = getFilterOffWidget()
        assertThat(filterOffWidget.isChecked).isFalse()

        filterOffWidget.performClick()
        verifyConfirmSupervisionCredentialsActivity()
        searchFilterOffPreference.onConfirmCredentials(ActivityResult(Activity.RESULT_OK, null))

        assertThat(
                Settings.Secure.getInt(context.getContentResolver(), SEARCH_CONTENT_FILTERS_ENABLED)
            )
            .isEqualTo(0)
        assertThat(filterOffWidget.isChecked).isTrue()
    }

    private fun getFilterOnWidget(): SelectorWithWidgetPreference {
        val widget: SelectorWithWidgetPreference =
            searchFilterOnPreference.createAndBindWidget(context)
        mockLifeCycleContext.stub {
            on { findPreference<Preference>(SupervisionSearchFilterOnPreference.KEY) } doReturn
                widget
            on { requirePreference<Preference>(SupervisionSearchFilterOnPreference.KEY) } doReturn
                widget
        }
        return widget
    }

    private fun getFilterOffWidget(): SelectorWithWidgetPreference {
        val widget: SelectorWithWidgetPreference =
            searchFilterOffPreference.createAndBindWidget(context)
        mockLifeCycleContext.stub {
            on { findPreference<Preference>(SupervisionSearchFilterOffPreference.KEY) } doReturn
                widget
            on { requirePreference<Preference>(SupervisionSearchFilterOffPreference.KEY) } doReturn
                widget
        }
        return widget
    }

    private fun mockConfirmSupervisionCredentialsActivity() {
        `when`(mockPackageManager.queryIntentActivitiesAsUser(any<Intent>(), anyInt(), anyInt()))
            .thenReturn(listOf(ResolveInfo()))
        `when`(mockLifeCycleContext.packageManager).thenReturn(mockPackageManager)
        `when`(mockLifeCycleContext.registerForActivityResult(any<StartActivityForResult>(), any()))
            .thenReturn(mockActivityResultLauncher)
    }

    private fun verifyConfirmSupervisionCredentialsActivity() {
        val intentCaptor = argumentCaptor<Intent>()
        verify(mockActivityResultLauncher).launch(intentCaptor.capture())

        assertThat(intentCaptor.allValues.size).isEqualTo(1)
        val intent = intentCaptor.firstValue
        assertThat(intent.action)
            .isEqualTo("android.app.supervision.action.CONFIRM_SUPERVISION_CREDENTIALS")
        assertThat(intent.`package`).isEqualTo("com.android.settings")
    }
}
