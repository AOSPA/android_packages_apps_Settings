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

package com.android.settings.accessibility.captionpreferences.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [CaptioningFooterPreference]. */
@RunWith(AndroidJUnit4::class)
class CaptioningFooterPreferenceTest {

    private val preference = CaptioningFooterPreference("test_key")

    @Test
    fun key_returnsConstructorKey() {
        assertThat(preference.key).isEqualTo("test_key")
    }

    @Test
    fun purpose_returnsCorrectResource() {
        assertThat(preference.purpose).isEqualTo(R.string.caption_preferences_footer_purpose)
    }

    @Test
    fun title_returnsCorrectResource() {
        assertThat(preference.title).isEqualTo(R.string.accessibility_captioning_preference_summary)
    }

    @Test
    fun introductionTitle_returnsCorrectResource() {
        assertThat(preference.introductionTitle)
            .isEqualTo(R.string.accessibility_captioning_about_title)
    }

    @Test
    fun learnMoreText_returnsCorrectResource() {
        assertThat(preference.learnMoreText)
            .isEqualTo(R.string.accessibility_captioning_footer_learn_more_content_description)
    }

    @Test
    fun helpResource_returnsCorrectResource() {
        assertThat(preference.helpResource).isEqualTo(R.string.help_url_caption)
    }
}
