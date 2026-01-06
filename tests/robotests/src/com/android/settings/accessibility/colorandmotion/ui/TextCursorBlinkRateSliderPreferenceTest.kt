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

package com.android.settings.accessibility.colorandmotion.ui

import android.content.Context
import android.view.View
import androidx.preference.PreferenceViewHolder
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TextCursorBlinkRateSliderPreferenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var preference: TextCursorBlinkRateSliderPreference
    private lateinit var holder: PreferenceViewHolder
    private val clickListener = View.OnClickListener { preference.value = 6 }

    @Before
    fun setUp() {
        preference = TextCursorBlinkRateSliderPreference(context)
        val rootView = View.inflate(context, preference.layoutResource, null /* parent */)
        holder = PreferenceViewHolder.createInstanceForTests(rootView)
    }

    @Test
    fun resetToDefault() {
        preference.setResetClickListener(clickListener)
        preference.onBindViewHolder(holder)
        val button = preference.getButton()
        assertThat(button).isNotNull()
        assertThat(preference.value).isNotEqualTo(6)
        button!!.callOnClick()

        assertThat(preference.value).isEqualTo(6)
    }
}
