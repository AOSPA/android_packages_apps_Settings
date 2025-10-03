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

package com.android.settings.accessibility.shortcuts.ui

import android.content.Context
import android.icu.text.ListFormatter
import androidx.test.core.app.ApplicationProvider
import com.android.internal.accessibility.AccessibilityShortcutController.COLOR_INVERSION_COMPONENT_NAME
import com.android.internal.accessibility.AccessibilityShortcutController.REDUCE_BRIGHT_COLORS_COMPONENT_NAME
import com.android.settings.R
import com.android.settings.accessibility.shared.utils.FRAMEWORK_ACCESSIBILITY_FEATURE_NAME
import com.android.settingslib.widget.TopIntroPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IntroPreferenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var preference: IntroPreference

    @Before
    fun setUp() {
        preference = IntroPreference(emptySet())
    }

    @Test
    fun key() {
        assertThat(preference.key).isEqualTo(IntroPreference.KEY)
    }

    @Test
    fun getTitle_noFeature_returnNull() {
        assertThat(preference.getTitle(context)).isNull()
    }

    @Test
    fun getTitle_oneFeature_returnNull() {
        preference = IntroPreference(setOf(COLOR_INVERSION_COMPONENT_NAME.flattenToString()))
        assertThat(preference.getTitle(context)).isNull()
    }

    @Test
    fun getTitle_twoFeatures_returnIntroText() {
        val feature1 = COLOR_INVERSION_COMPONENT_NAME.flattenToString()
        val feature2 = REDUCE_BRIGHT_COLORS_COMPONENT_NAME.flattenToString()

        val expectedTitle =
            context.getString(
                R.string.accessibility_shortcut_edit_screen_prompt,
                ListFormatter.getInstance()
                    .format(
                        listOf(
                            context.getString(FRAMEWORK_ACCESSIBILITY_FEATURE_NAME[feature1]!!),
                            context.getString(FRAMEWORK_ACCESSIBILITY_FEATURE_NAME[feature2]!!),
                        )
                    ),
            )
        preference = IntroPreference(setOf(feature1, feature2))
        assertThat(preference.getTitle(context).toString()).isEqualTo(expectedTitle)
    }

    @Test
    fun isIndexable_returnFalse() {
        assertThat(preference.indexable).isFalse()
    }

    @Test
    fun createWidget_returnTopIntroPreference() {
        assertThat(preference.createWidget(context)).isInstanceOf(TopIntroPreference::class.java)
    }

    @Test
    fun isAvailable_oneFeature_returnFalse() {
        preference = IntroPreference(setOf(COLOR_INVERSION_COMPONENT_NAME.flattenToString()))
        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_twoFeatures_returnTrue() {
        val feature1 = COLOR_INVERSION_COMPONENT_NAME.flattenToString()
        val feature2 = REDUCE_BRIGHT_COLORS_COMPONENT_NAME.flattenToString()
        preference = IntroPreference(setOf(feature1, feature2))

        assertThat(preference.isAvailable(context)).isTrue()
    }
}
