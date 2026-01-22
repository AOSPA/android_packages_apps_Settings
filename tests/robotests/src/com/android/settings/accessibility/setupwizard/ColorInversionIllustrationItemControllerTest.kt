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

package com.android.settings.accessibility.setupwizard

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.accessibility.colorinversion.ui.ColorInversionIllustrationPreference
import com.android.settings.accessibility.setupwizard.items.IllustrationItem
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

/** Tests for [ColorInversionIllustrationItemController]. */
@RunWith(RobolectricTestRunner::class)
class ColorInversionIllustrationItemControllerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val illustrationItem: IllustrationItem = IllustrationItem()
    private val controller = ColorInversionIllustrationItemController(context, illustrationItem)

    @Test
    fun bindData_setsIllustrationImageResIdAndContentDescription() {
        val metadata = ColorInversionIllustrationPreference()
        val expectedImageResId = R.raw.accessibility_color_inversion_banner
        val expectedContentDescription = metadata.getContentDescription(context)

        controller.bindData(illustrationItem)
        ShadowLooper.idleMainLooper()

        assertThat(illustrationItem.imageResId).isEqualTo(expectedImageResId)
        assertThat(illustrationItem.contentDescription.toString())
            .isEqualTo(expectedContentDescription.toString())
    }
}
