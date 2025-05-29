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
import android.provider.Settings.Secure.BROWSER_CONTENT_FILTERS_ENABLED
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
class SupervisionSafeSitesPreferenceTest {
    @get:Rule val metricsRule = MetricsRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dataStore = SupervisionSafeSitesDataStore(context)
    private val allowAllSitesPreference = SupervisionAllowAllSitesPreference(dataStore)
    private val blockExplicitSitesPreference = SupervisionBlockExplicitSitesPreference(dataStore)

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

    @Before
    fun setUp() {
        allowAllSitesPreference.onCreate(mockLifeCycleContext)
        blockExplicitSitesPreference.onCreate(mockLifeCycleContext)
    }

    @Test
    fun getTitle_allowAllSites() {
        assertThat(allowAllSitesPreference.title)
            .isEqualTo(R.string.supervision_web_content_filters_browser_allow_all_sites_title)
    }

    @Test
    fun getTitle_blockExplicitSites() {
        assertThat(blockExplicitSitesPreference.title)
            .isEqualTo(R.string.supervision_web_content_filters_browser_block_explicit_sites_title)
    }

    @Test
    fun getSummary_blockExplicitSites() {
        assertThat(blockExplicitSitesPreference.summary)
            .isEqualTo(
                R.string.supervision_web_content_filters_browser_block_explicit_sites_summary
            )
    }

    @Test
    fun allowAllSitesIsChecked_whenNoValueIsSet() {
        setDataStoreValue(null)
        assertThat(blockExplicitSitesPreference.createWidget().isChecked).isFalse()
        assertThat(allowAllSitesPreference.createWidget().isChecked).isTrue()
    }

    @Test
    fun blockExplicitSitesIsChecked_whenPreviouslyEnabled() {
        setDataStoreValue(1)
        assertThat(allowAllSitesPreference.createWidget().isChecked).isFalse()
        assertThat(blockExplicitSitesPreference.createWidget().isChecked).isTrue()
    }

    @Test
    fun clickBlockExplicitSites_credentialFailed() {
        setDataStoreValue(0)
        val blockExplicitSitesWidget = blockExplicitSitesPreference.createWidget()
        assertThat(blockExplicitSitesWidget.isChecked).isFalse()

        blockExplicitSitesWidget.performClick()

        verifyConfirmSupervisionCredentialsActivity()
        blockExplicitSitesPreference.onConfirmCredentials(
            ActivityResult(Activity.RESULT_CANCELED, null)
        )

        verifyNoInteractions(metricsRule.metricsFeatureProvider)
        assertThat(blockExplicitSitesWidget.isChecked).isFalse()
        assertThat(getDataStoreValue()).isFalse()
    }

    @Test
    fun clickBlockExplicitSites_unresolvedIntent_activityNotLaunched() {
        mockPackageManager.stub {
            on { queryIntentActivitiesAsUser(any<Intent>(), anyInt(), anyInt()) } doReturn listOf()
        }

        setDataStoreValue(0)
        val blockExplicitSitesWidget = blockExplicitSitesPreference.createWidget()
        assertThat(blockExplicitSitesWidget.isChecked).isFalse()

        blockExplicitSitesWidget.performClick()

        verify(mockActivityResultLauncher, never()).launch(any())
        verifyNoInteractions(metricsRule.metricsFeatureProvider)
        assertThat(getDataStoreValue()).isFalse()
        assertThat(blockExplicitSitesWidget.isChecked).isFalse()
    }

    @Test
    fun clickBlockExplicitSites_enablesFilter() {
        setDataStoreValue(-1)
        val blockExplicitSitesWidget = blockExplicitSitesPreference.createWidget()
        assertThat(blockExplicitSitesWidget.isChecked).isFalse()

        blockExplicitSitesWidget.performClick()

        verifyConfirmSupervisionCredentialsActivity()
        blockExplicitSitesPreference.onConfirmCredentials(ActivityResult(Activity.RESULT_OK, null))

        assertThat(blockExplicitSitesWidget.isChecked).isTrue()
        assertThat(getDataStoreValue()).isTrue()
        verify(metricsRule.metricsFeatureProvider).changed(0, blockExplicitSitesPreference.key, 1)
    }

    @Test
    fun clickAllowAllSites_disablesFilter() {
        setDataStoreValue(1)
        val allowAllSitesWidget = allowAllSitesPreference.createWidget()
        assertThat(allowAllSitesWidget.isChecked).isFalse()
        allowAllSitesWidget.performClick()

        verifyConfirmSupervisionCredentialsActivity()
        allowAllSitesPreference.onConfirmCredentials(ActivityResult(Activity.RESULT_OK, null))

        assertThat(allowAllSitesWidget.isChecked).isTrue()
        assertThat(getDataStoreValue()).isFalse()
        verify(metricsRule.metricsFeatureProvider).changed(0, allowAllSitesPreference.key, 1)
    }

    private fun getDataStoreValue() =
        SettingsSecureStore.get(context).getBoolean(BROWSER_CONTENT_FILTERS_ENABLED)

    private fun setDataStoreValue(value: Int?) {
        SettingsSecureStore.get(context).setInt(BROWSER_CONTENT_FILTERS_ENABLED, value)
    }

    private fun SupervisionSafeSitesPreference.createWidget() =
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
