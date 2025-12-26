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

package com.android.settings.accessibility.actiontimeout.ui

import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ActionTimeoutFooterPreferenceTest {
    private val preference = ActionTimeoutFooterPreference()

    @Test
    fun key_returnsCorrectKey() {
        assertThat(preference.key).isEqualTo(ActionTimeoutFooterPreference.KEY)
    }

    @Test
    fun title_returnsCorrectTitleRes() {
        assertThat(preference.title)
            .isEqualTo(R.string.accessibility_control_timeout_preference_summary)
    }

    @Test
    fun introductionTitle_returnsCorrectTitleRes() {
        assertThat(preference.introductionTitle)
            .isEqualTo(R.string.accessibility_control_timeout_about_title)
    }

    @Test
    fun helpResource_returnsCorrectHelpResourceRes() {
        assertThat(preference.helpResource).isEqualTo(R.string.help_url_timeout)
    }

    @Test
    fun learnMoreText_returnsCorrectLearnMoreTextRes() {
        assertThat(preference.learnMoreText)
            .isEqualTo(R.string.accessibility_control_timeout_footer_learn_more_content_description)
    }

    @Test
    fun purpose_returnsCorrectPurposeRes() {
        assertThat(preference.purpose).isEqualTo(R.string.a11y_action_timeout_footer_purpose)
    }
}
