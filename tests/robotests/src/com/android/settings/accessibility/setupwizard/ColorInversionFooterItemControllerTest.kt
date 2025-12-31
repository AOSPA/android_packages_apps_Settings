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
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.android.settings.accessibility.colorinversion.ui.FooterPreference
import com.google.android.setupdesign.items.Item
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.verify
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/** Tests for [ColorInversionFooterItemController]. */
@RunWith(RobolectricTestRunner::class)
class ColorInversionFooterItemControllerTest {

    private val mockItem = mock<Item>()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val controller = ColorInversionFooterItemController(context, mockItem)

    @Test
    fun bindData_setsFooterSummary() {
        // Use 0 to hide help links, matching the implementation logic.
        val metadata = FooterPreference(helpResource = 0)
        val expectedValue = metadata.getTitle(context)

        controller.bindData(mockItem)
        shadowOf(Looper.getMainLooper()).idle()

        val captor = argumentCaptor<CharSequence>()
        verify(mockItem).summary = captor.capture()
        assertThat(captor.firstValue.toString()).isEqualTo(expectedValue.toString())
    }
}
