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
import com.android.settingslib.widget.TopIntroPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AutoclickIntroPreferenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preference = AutoclickIntroPreference()

    @Test
    fun getKey_returnCorrectKey() {
        assertThat(preference.key).isEqualTo(AutoclickIntroPreference.KEY)
    }

    @Test
    fun getPurpose_returnCorrectPurpose() {
        assertThat(preference.purpose).isEqualTo(R.string.a11y_autoclick_intro_purpose)
    }

    @Test
    fun getTitle_returnCorrectTitle() {
        assertThat(preference.title).isEqualTo(R.string.accessibility_autoclick_intro_text)
    }

    @Test
    fun indexable_returnFalse() {
        assertThat(preference.indexable).isFalse()
    }

    @Test
    fun createWidget_returnTopIntroPreference() {
        val widget = preference.createWidget(context)

        assertThat(widget).isInstanceOf(TopIntroPreference::class.java)
    }
}
