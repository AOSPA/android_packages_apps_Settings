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
import com.android.settings.accessibility.setupwizard.items.TwoTargetItem
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests for [MagnificationShortcutTwoTargetItemController]. */
@RunWith(RobolectricTestRunner::class)
class MagnificationShortcutTwoTargetItemControllerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val item = TwoTargetItem()
    private val controller: MagnificationShortcutTwoTargetItemController =
        MagnificationShortcutTwoTargetItemController.create(context, item)

    @Test
    fun bindData_setsItemPropertiesFromMetadata() {
        controller.bindData(item)

        assertThat(item.title.toString())
            .isEqualTo(
                context.getString(R.string.accessibility_screen_magnification_shortcut_title)
            )
        assertThat(item.summary.toString()).isNotNull()
    }
}
