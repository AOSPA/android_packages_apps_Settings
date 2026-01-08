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

package com.android.settings.accessibility.buttonshortcutsetting.ui

import android.content.Context
import android.icu.text.MessageFormat
import android.text.Html
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector

/** Test for [ButtonShortcutSettingFooterPreference] */
@RunWith(RobolectricTestParameterInjector::class)
class ButtonShortcutSettingFooterPreferenceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preference = ButtonShortcutSettingFooterPreference()

    @Test
    fun key() {
        assertThat(preference.key).isEqualTo(ButtonShortcutSettingFooterPreference.KEY)
    }

    @Test
    fun purpose_returnsCorrectResourceId() {
        assertThat(preference.purpose)
            .isEqualTo(R.string.a11y_button_shortcut_setting_footer_purpose)
    }

    @Test
    fun introductionTitle_returnsCorrectResourceId() {
        assertThat(preference.introductionTitle)
            .isEqualTo(R.string.accessibility_button_about_title)
    }

    @Test
    fun getTitle_returnsFormattedHtml() {
        val expectedHtml =
            Html.fromHtml(
                    MessageFormat.format(
                        context.getString(R.string.accessibility_button_description),
                        1,
                        2,
                        3,
                    ),
                    Html.FROM_HTML_MODE_COMPACT,
                    /* imageGetter= */ null,
                    /* tagHandler= */ null,
                )
                .toString()

        assertThat(preference.getTitle(context).toString()).isEqualTo(expectedHtml)
    }
}
