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
import android.provider.Settings
import android.provider.Settings.Secure.BROWSER_CONTENT_FILTERS_ENABLED
import android.provider.Settings.SettingNotFoundException
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
import org.mockito.Mockito.mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class SupervisionSafeSitesPreferenceTest {
    private val mockLifeCycleContext = mock(PreferenceLifecycleContext::class.java)
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var dataStore: SupervisionSafeSitesDataStore
    private lateinit var allowAllSitesPreference: SupervisionAllowAllSitesPreference
    private lateinit var blockExplicitSitesPreference: SupervisionBlockExplicitSitesPreference

    @Before
    fun setUp() {
        dataStore = SupervisionSafeSitesDataStore(context)
        allowAllSitesPreference = SupervisionAllowAllSitesPreference(dataStore)
        allowAllSitesPreference.onCreate(mockLifeCycleContext)
        blockExplicitSitesPreference = SupervisionBlockExplicitSitesPreference(dataStore)
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
        assertThrows(SettingNotFoundException::class.java) {
            Settings.Secure.getInt(context.getContentResolver(), BROWSER_CONTENT_FILTERS_ENABLED)
        }
        assertThat(getBlockExplicitSitesWidget().isChecked).isFalse()
        assertThat(getAllowAllSitesWidget().isChecked).isTrue()
    }

    @Test
    fun blockExplicitSitesIsChecked_whenPreviouslyEnabled() {
        Settings.Secure.putInt(context.getContentResolver(), BROWSER_CONTENT_FILTERS_ENABLED, 1)
        assertThat(getAllowAllSitesWidget().isChecked).isFalse()
        assertThat(getBlockExplicitSitesWidget().isChecked).isTrue()
    }

    @Test
    fun clickBlockExplicitSites_credentialFailed() {
        Settings.Secure.putInt(context.getContentResolver(), BROWSER_CONTENT_FILTERS_ENABLED, 0)
        val blockExplicitSitesWidget = getBlockExplicitSitesWidget()
        assertThat(blockExplicitSitesWidget.isChecked).isFalse()

        blockExplicitSitesWidget.performClick()

        verifyConfirmSupervisionCredentialsActivityStarted(
            SupervisionBlockExplicitSitesPreference.REQUEST_CODE_SUPERVISION_CREDENTIALS
        )

        val result =
            blockExplicitSitesPreference.onActivityResult(
                mockLifeCycleContext,
                SupervisionBlockExplicitSitesPreference.REQUEST_CODE_SUPERVISION_CREDENTIALS,
                Activity.RESULT_CANCELED,
                null,
            )

        assertThat(result).isFalse()
    }

    @Test
    fun clickBlockExplicitSites_enablesFilter() {
        Settings.Secure.putInt(context.getContentResolver(), BROWSER_CONTENT_FILTERS_ENABLED, 0)
        val blockExplicitSitesWidget = getBlockExplicitSitesWidget()
        assertThat(blockExplicitSitesWidget.isChecked).isFalse()

        blockExplicitSitesWidget.performClick()

        verifyConfirmSupervisionCredentialsActivityStarted(
            SupervisionBlockExplicitSitesPreference.REQUEST_CODE_SUPERVISION_CREDENTIALS
        )

        val result =
            blockExplicitSitesPreference.onActivityResult(
                mockLifeCycleContext,
                SupervisionBlockExplicitSitesPreference.REQUEST_CODE_SUPERVISION_CREDENTIALS,
                Activity.RESULT_OK,
                null,
            )

        assertThat(result).isTrue()
        assertThat(blockExplicitSitesWidget.isChecked).isTrue()
        assertThat(
                Settings.Secure.getInt(
                    context.getContentResolver(),
                    BROWSER_CONTENT_FILTERS_ENABLED,
                )
            )
            .isEqualTo(1)
    }

    @Test
    fun clickAllowAllSites_disablesFilter() {
        Settings.Secure.putInt(context.getContentResolver(), BROWSER_CONTENT_FILTERS_ENABLED, 1)
        val allowAllSitesWidget = getAllowAllSitesWidget()
        assertThat(allowAllSitesWidget.isChecked).isFalse()
        allowAllSitesWidget.performClick()

        verifyConfirmSupervisionCredentialsActivityStarted(
            SupervisionAllowAllSitesPreference.REQUEST_CODE_SUPERVISION_CREDENTIALS
        )

        val result =
            allowAllSitesPreference.onActivityResult(
                mockLifeCycleContext,
                SupervisionAllowAllSitesPreference.REQUEST_CODE_SUPERVISION_CREDENTIALS,
                Activity.RESULT_OK,
                null,
            )

        assertThat(result).isTrue()
        assertThat(allowAllSitesWidget.isChecked).isTrue()
        assertThat(
                Settings.Secure.getInt(
                    context.getContentResolver(),
                    BROWSER_CONTENT_FILTERS_ENABLED,
                )
            )
            .isEqualTo(0)
    }

    private fun getBlockExplicitSitesWidget(): SelectorWithWidgetPreference {
        val widget: SelectorWithWidgetPreference =
            blockExplicitSitesPreference.createAndBindWidget(context)
        mockLifeCycleContext.stub {
            on { findPreference<Preference>(SupervisionBlockExplicitSitesPreference.KEY) } doReturn
                widget
            on {
                requirePreference<Preference>(SupervisionBlockExplicitSitesPreference.KEY)
            } doReturn widget
        }
        return widget
    }

    private fun getAllowAllSitesWidget(): SelectorWithWidgetPreference {
        val widget: SelectorWithWidgetPreference =
            allowAllSitesPreference.createAndBindWidget(context)
        mockLifeCycleContext.stub {
            on { findPreference<Preference>(SupervisionAllowAllSitesPreference.KEY) } doReturn
                widget
            on { requirePreference<Preference>(SupervisionAllowAllSitesPreference.KEY) } doReturn
                widget
        }
        return widget
    }

    private fun verifyConfirmSupervisionCredentialsActivityStarted(requestCode: Int) {
        val intentCaptor = argumentCaptor<Intent>()

        verify(mockLifeCycleContext)
            .startActivityForResult(intentCaptor.capture(), eq(requestCode), eq(null))

        assertThat(intentCaptor.allValues.size).isEqualTo(1)

        assertThat(intentCaptor.firstValue.component?.className)
            .isEqualTo(ConfirmSupervisionCredentialsActivity::class.java.name)
    }
}
