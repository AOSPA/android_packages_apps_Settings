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
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests for [AutoclickFooterPreference]. */
@RunWith(RobolectricTestRunner::class)
class AutoclickFooterPreferenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var preference: AutoclickFooterPreference

    @Before
    fun setUp() {
        preference = AutoclickFooterPreference()
    }

    @Test
    fun key_returnsCorrectValue() {
        assertThat(preference.key).isEqualTo(AutoclickFooterPreference.KEY)
    }

    @Test
    fun purpose_returnsAutoclickFooterPurpose() {
        assertThat(context.getString(preference.purpose))
            .isEqualTo(context.getString(R.string.a11y_autoclick_footer_purpose))
    }

    @Test
    fun title_returnsAutoclickDescription() {
        assertThat(context.getString(preference.title))
            .isEqualTo(context.getString(R.string.accessibility_autoclick_description))
    }

    @Test
    fun introductionTitle_returnsAutoclickAboutTitle() {
        assertThat(context.getString(preference.introductionTitle))
            .isEqualTo(context.getString(R.string.accessibility_autoclick_about_title))
    }

    @Test
    fun learnMoreText_returnsAutoclickFooterLearnMoreContentDescription() {
        assertThat(context.getString(preference.learnMoreText))
            .isEqualTo(
                context.getString(
                    R.string.accessibility_autoclick_footer_learn_more_content_description
                )
            )
    }

    @Test
    fun helpResource_returnsHelpUrlAutoclick() {
        assertThat(context.getString(preference.helpResource))
            .isEqualTo(context.getString(R.string.help_url_autoclick))
    }
}
