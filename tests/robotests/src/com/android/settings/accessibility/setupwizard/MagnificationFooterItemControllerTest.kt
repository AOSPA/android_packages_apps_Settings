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

package com.android.settings.accessibility.setupwizard

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.settings.accessibility.screenmagnification.ui.MagnificationFooterPreference
import com.google.android.setupdesign.items.Item
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

/** Tests for [MagnificationFooterItemController]. */
@RunWith(RobolectricTestRunner::class)
class MagnificationFooterItemControllerTest {

    private val mockItem = mock<Item>()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val controller = MagnificationFooterItemController(context, mockItem)

    @Test
    fun bindData_setsFooterSummaryAndContentDescription() {
        // Use 0 to hide help links, matching the implementation logic.
        val metadata = MagnificationFooterPreference(helpResource = 0)
        val expectedTitle = metadata.getTitle(context)
        val intro = context.getString(metadata.introductionTitle)
        val expectedContentDescription = metadata.getContentDescription(intro, expectedTitle)

        controller.bindData(mockItem)
        ShadowLooper.idleMainLooper()

        val summaryCaptor = argumentCaptor<CharSequence>()
        verify(mockItem).summary = summaryCaptor.capture()
        assertThat(summaryCaptor.firstValue.toString()).isEqualTo(expectedTitle.toString())
        val contentDescCaptor = argumentCaptor<CharSequence>()
        verify(mockItem).contentDescription = contentDescCaptor.capture()
        assertThat(contentDescCaptor.firstValue.toString())
            .isEqualTo(expectedContentDescription.toString())
    }
}
