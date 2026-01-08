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

package com.android.settings.accessibility.setupwizard.items

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests for [FooterItem]. */
@RunWith(RobolectricTestRunner::class)
class FooterItemTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val footerItem: FooterItem = FooterItem()
    private val rootView: View =
        LayoutInflater.from(context).inflate(R.layout.setup_items_footer, null)

    @Test
    fun onBindView_backgroundIsExplicitlyRemoved() {
        rootView.setBackgroundColor(Color.BLUE)

        footerItem.onBindView(rootView)

        assertThat(rootView.background).isNull()
    }

    @Test
    fun onBindView_paddingStartAndEndAreZeroed() {
        rootView.setPadding(100, 20, 100, 20)

        footerItem.onBindView(rootView)

        assertThat(rootView.paddingLeft).isEqualTo(0)
        assertThat(rootView.paddingRight).isEqualTo(0)
        assertThat(rootView.paddingTop).isEqualTo(20)
        assertThat(rootView.paddingBottom).isEqualTo(20)
    }

    @Test
    fun isGroupDivider_returnsTrue() {
        assertThat(footerItem.isGroupDivider).isTrue()
    }
}
