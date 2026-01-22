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
import com.android.settings.accessibility.screenmagnification.ui.MagnificationIllustrationPreference
import com.android.settings.accessibility.setupwizard.items.IllustrationItem
import com.android.settings.testutils.shadow.ShadowThemeHelper
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/** Tests for [MagnificationIllustrationItemController]. */
@Config(shadows = [ShadowThemeHelper::class])
@RunWith(RobolectricTestRunner::class)
class MagnificationIllustrationItemControllerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val illustrationItem: IllustrationItem = IllustrationItem()
    private val controller = MagnificationIllustrationItemController(context, illustrationItem)

    @Test
    fun bindData_whenGlifExpressiveStyleApplied_setsCorrectImage() {
        ShadowThemeHelper.setShouldApplyGlifExpressiveStyle(true)
        val expectedImageResId = R.raw.accessibility_magnification_banner_expressive

        controller.bindData(illustrationItem)
        ShadowLooper.idleMainLooper()

        assertThat(illustrationItem.imageResId).isEqualTo(expectedImageResId)
    }

    @Test
    fun bindData_whenGlifExpressiveStyleNotApplied_setsCorrectImage() {
        ShadowThemeHelper.setShouldApplyGlifExpressiveStyle(false)
        val expectedImageResId = R.raw.accessibility_magnification_banner

        controller.bindData(illustrationItem)
        ShadowLooper.idleMainLooper()

        assertThat(illustrationItem.imageResId).isEqualTo(expectedImageResId)
    }

    @Test
    fun bindData_setsIllustrationContentDescription() {
        val metadata = MagnificationIllustrationPreference()
        val expectedContentDescription = metadata.getContentDescription(context)

        controller.bindData(illustrationItem)
        ShadowLooper.idleMainLooper()

        assertThat(illustrationItem.contentDescription.toString())
            .isEqualTo(expectedContentDescription.toString())
    }
}
