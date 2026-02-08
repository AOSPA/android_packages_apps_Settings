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
import com.android.settingslib.widget.TopIntroPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [CaptioningPropertiesTopIntro]. */
@RunWith(AndroidJUnit4::class)
class CaptioningPropertiesTopIntroTest {

    private lateinit var context: Context
    private lateinit var preference: CaptioningPropertiesTopIntro

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        preference = CaptioningPropertiesTopIntro()
    }

    @Test
    fun key_returnsCorrectValue() {
        assertThat(preference.key).isEqualTo(CaptioningPropertiesTopIntro.KEY)
    }

    @Test
    fun purpose_returnsCorrectValue() {
        assertThat(preference.purpose).isEqualTo(R.string.caption_preferences_top_intro_purpose)
    }

    @Test
    fun title_returnsCorrectValue() {
        assertThat(preference.title).isEqualTo(R.string.accessibility_captioning_preference_intro)
    }

    @Test
    fun indexable_isFalse() {
        assertThat(preference.indexable).isFalse()
    }

    @Test
    fun createWidget_returnsTopIntroPreference() {
        val widget = preference.createWidget(context)
        assertThat(widget).isInstanceOf(TopIntroPreference::class.java)
    }
}
