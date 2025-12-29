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

package com.android.settings.accessibility.timingcontrols.ui

import android.content.Context
import android.provider.Settings
import android.view.ViewConfiguration
import androidx.annotation.IntegerRes
import androidx.annotation.StringRes
import androidx.preference.ListPreference
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.accessibility.shared.data.StringToIntDataStoreWrapper
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameters
import com.google.testing.junit.testparameterinjector.TestParametersValuesProvider
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.shadows.ShadowLooper

/** Test for [LongPressTimeoutPreference] */
@RunWith(RobolectricTestParameterInjector::class)
class LongPressTimeoutPreferenceTest {

    @get:Rule val settingsStoreRule = SettingsStoreRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preference = LongPressTimeoutPreference(context)

    @Test
    fun key() {
        assertThat(preference.key).isEqualTo(Settings.Secure.LONG_PRESS_TIMEOUT)
    }

    @Test
    fun title() {
        assertThat(preference.title)
            .isEqualTo(R.string.accessibility_long_press_timeout_preference_title)
    }

    @Test
    fun purpose() {
        assertThat(preference.purpose).isEqualTo(R.string.a11y_touch_hold_delay_purpose)
    }

    @Test
    fun values() {
        assertThat(preference.values).isEqualTo(R.array.long_press_timeout_selector_int_values)
    }

    @Test
    fun valuesDescription() {
        assertThat(preference.valuesDescription)
            .isEqualTo(R.array.long_press_timeout_selector_list_titles)
    }

    @Test
    fun createWidget_returnsListPreference() {
        val widget = preference.createWidget(context)
        assertThat(widget).isInstanceOf(ListPreference::class.java)
    }

    @TestParameters(valuesProvider = TimeoutOptionsProvider::class)
    @Test
    fun bindWidget_verifySummary(
        @IntegerRes timeoutValueRes: Int,
        @StringRes timeoutDescriptionRes: Int,
    ) {
        val longTimeout = context.resources.getInteger(timeoutValueRes)
        setTimeout(longTimeout)

        val widget = preference.createAndBindWidget(context) as ListPreference

        assertThat(widget.summary.toString()).isEqualTo(context.getString(timeoutDescriptionRes))
    }

    @Test
    fun valueType() {
        assertThat(preference.valueType).isEqualTo(Int::class.javaObjectType)
    }

    @Test
    fun storage_returnsStringToIntDataStoreWrapper() {
        assertThat(preference.storage(context))
            .isInstanceOf(StringToIntDataStoreWrapper::class.java)
    }

    @Test
    fun storage_getDefaultValue_matchesDefaultTimeoutInViewConfiguration() {
        assertThat(
                preference
                    .storage(context)
                    .getDefaultValue(LongPressTimeoutPreference.KEY, Int::class.javaObjectType)
            )
            .isEqualTo(ViewConfiguration.DEFAULT_LONG_PRESS_TIMEOUT)
    }

    private fun setTimeout(timeout: Int) {
        SettingsSecureStore.get(context).setInt(Settings.Secure.LONG_PRESS_TIMEOUT, timeout)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    }

    private object TimeoutOptionsProvider : TestParametersValuesProvider() {
        override fun provideValues(context: Context): List<TestParameters.TestParametersValues> {
            return listOf<TestParameters.TestParametersValues>(
                createTestParameter(
                    "Short",
                    R.integer.long_press_timeout_short,
                    R.string.accessibility_long_press_timeout_option_short,
                ),
                createTestParameter(
                    "Medium",
                    R.integer.long_press_timeout_medium,
                    R.string.accessibility_long_press_timeout_option_medium,
                ),
                createTestParameter(
                    "Long",
                    R.integer.long_press_timeout_long,
                    R.string.accessibility_long_press_timeout_option_long,
                ),
            )
        }

        private fun createTestParameter(
            testName: String,
            @IntegerRes timeoutValueRes: Int,
            @StringRes timeoutDescriptionRes: Int,
        ): TestParameters.TestParametersValues {
            return TestParameters.TestParametersValues.builder()
                .name("LongPress Timeout: $testName")
                .addParameter("timeoutValueRes", timeoutValueRes)
                .addParameter("timeoutDescriptionRes", timeoutDescriptionRes)
                .build()
        }
    }
}
