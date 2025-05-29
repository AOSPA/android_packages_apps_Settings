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
import android.provider.Settings.Secure.SEARCH_CONTENT_FILTERS_ENABLED
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.testutils.MetricsRule
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.SelectorWithWidgetPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@RunWith(AndroidJUnit4::class)
class SupervisionSafeSearchPreferenceTest {
    @get:Rule val metricsRule = MetricsRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dataStore = SupervisionSafeSearchDataStore(context)
    private val searchFilterOffPreference = SupervisionSearchFilterOffPreference(dataStore)
    private val searchFilterOnPreference = SupervisionSearchFilterOnPreference(dataStore)

    private val mockActivityResultLauncher: ActivityResultLauncher<Intent> = mock()
    private val mockPackageManager: PackageManager = mock {
        on { queryIntentActivitiesAsUser(any<Intent>(), anyInt(), anyInt()) } doReturn
            listOf(ResolveInfo())
    }
    private val mockLifeCycleContext: PreferenceLifecycleContext = mock {
        on { packageManager } doReturn mockPackageManager
        on { registerForActivityResult(any<StartActivityForResult>(), any()) } doReturn
            mockActivityResultLauncher
    }

    private var dataStoreValue: Int?
        get() = SettingsSecureStore.get(context).getInt(SEARCH_CONTENT_FILTERS_ENABLED)
        set(value) = SettingsSecureStore.get(context).setInt(SEARCH_CONTENT_FILTERS_ENABLED, value)

    @Before
    fun setUp() {
        searchFilterOffPreference.onCreate(mockLifeCycleContext)
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
    fun filterOffIsChecked_whenNoValueIsSet() {
        dataStoreValue = null
        assertThat(searchFilterOnPreference.createWidget().isChecked).isFalse()
        assertThat(searchFilterOffPreference.createWidget().isChecked).isTrue()
    }

    @Test
    fun filterOnIsChecked_whenPreviouslyEnabled() {
        dataStoreValue = 1
        assertThat(searchFilterOnPreference.createWidget().isChecked).isTrue()
        assertThat(searchFilterOffPreference.createWidget().isChecked).isFalse()
    }

    @Test
    fun clickFilterOn_failedToEnablesFilter_activityFailed() {
        dataStoreValue = 0
        val filterOnWidget = searchFilterOnPreference.createWidget()
        assertThat(filterOnWidget.isChecked).isFalse()

        filterOnWidget.performClick()
        verifyConfirmSupervisionCredentialsActivity()
        searchFilterOnPreference.onConfirmCredentials(
            ActivityResult(Activity.RESULT_CANCELED, null)
        )

        verifyNoInteractions(metricsRule.metricsFeatureProvider)
        assertThat(dataStoreValue).isEqualTo(0)
        assertThat(filterOnWidget.isChecked).isFalse()
    }

    @Test
    fun clickFilterOn_unresolvedIntent_activityNotLaunched() {
        mockPackageManager.stub {
            on { queryIntentActivitiesAsUser(any<Intent>(), anyInt(), anyInt()) } doReturn listOf()
        }

        dataStoreValue = 0
        val filterOnWidget = searchFilterOnPreference.createWidget()
        assertThat(filterOnWidget.isChecked).isFalse()

        filterOnWidget.performClick()

        verify(mockActivityResultLauncher, never()).launch(any())
        verifyNoInteractions(metricsRule.metricsFeatureProvider)
        assertThat(dataStoreValue).isEqualTo(0)
        assertThat(filterOnWidget.isChecked).isFalse()
    }

    @Test
    fun clickFilterOn_enablesFilter() {
        dataStoreValue = -1
        val filterOnWidget = searchFilterOnPreference.createWidget()
        assertThat(filterOnWidget.isChecked).isFalse()

        filterOnWidget.performClick()
        verifyConfirmSupervisionCredentialsActivity()
        searchFilterOnPreference.onConfirmCredentials(ActivityResult(Activity.RESULT_OK, null))

        assertThat(dataStoreValue).isEqualTo(1)
        assertThat(filterOnWidget.isChecked).isTrue()
        verify(metricsRule.metricsFeatureProvider).changed(0, searchFilterOnPreference.key, 1)
    }

    @Test
    fun clickFilterOff_disablesFilter() {
        dataStoreValue = 1
        val filterOffWidget = searchFilterOffPreference.createWidget()
        assertThat(filterOffWidget.isChecked).isFalse()

        filterOffWidget.performClick()
        verifyConfirmSupervisionCredentialsActivity()
        searchFilterOffPreference.onConfirmCredentials(ActivityResult(Activity.RESULT_OK, null))

        assertThat(dataStoreValue).isEqualTo(0)
        assertThat(filterOffWidget.isChecked).isTrue()
        verify(metricsRule.metricsFeatureProvider).changed(0, searchFilterOffPreference.key, 1)
    }

    private fun SupervisionSafeSearchPreference.createWidget() =
        createAndBindWidget<SelectorWithWidgetPreference>(context).also { widget ->
            mockLifeCycleContext.stub { on { requirePreference<Preference>(key) } doReturn widget }
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
