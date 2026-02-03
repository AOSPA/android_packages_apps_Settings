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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settingslib.widget.IllustrationPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [CaptioningIllustrationPreference]. */
@RunWith(AndroidJUnit4::class)
class CaptioningIllustrationPreferenceTest {

    private lateinit var context: Context
    private lateinit var preference: CaptioningIllustrationPreference

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        preference = CaptioningIllustrationPreference()
    }

    @Test
    fun key_returnsCorrectValue() {
        assertThat(preference.key).isEqualTo(CaptioningIllustrationPreference.KEY)
    }

    @Test
    fun purpose_returnsCorrectValue() {
        assertThat(preference.purpose).isEqualTo(R.string.caption_preferences_illustration_purpose)
    }

    @Test
    fun indexable_isFalse() {
        assertThat(preference.indexable).isFalse()
    }

    @Test
    fun createWidget_returnsIllustrationPreference() {
        val widget = preference.createWidget(context)
        assertThat(widget).isInstanceOf(IllustrationPreference::class.java)
    }

    @Test
    fun createWidget_setsLottieAnimation() {
        val widget = preference.createWidget(context) as IllustrationPreference
        assertThat(widget.lottieAnimationResId)
            .isEqualTo(R.drawable.accessibility_captioning_banner)
    }

    @Test
    fun createWidget_isNotSelectable() {
        val widget = preference.createWidget(context)
        assertThat(widget.isSelectable).isFalse()
    }
}
