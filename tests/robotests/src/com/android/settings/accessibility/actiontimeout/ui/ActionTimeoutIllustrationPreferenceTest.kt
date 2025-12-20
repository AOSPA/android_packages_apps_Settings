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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ActionTimeoutIllustrationPreferenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preference = ActionTimeoutIllustrationPreference()

    @Test
    fun key_returnsCorrectKey() {
        assertThat(preference.key).isEqualTo(ActionTimeoutIllustrationPreference.KEY)
    }

    @Test
    fun indexable_isFalse() {
        assertThat(preference.indexable).isFalse()
    }

    @Test
    fun createWidget_verifyIllustrationResId() {
        val widget = preference.createWidget(context)

        assertThat(widget.lottieAnimationResId).isEqualTo(R.raw.accessibility_timeout_banner)
    }

    @Test
    fun purpose_returnsCorrectPurposeRes() {
        assertThat(preference.purpose).isEqualTo(R.string.a11y_action_timeout_illustration_purpose)
    }
}
