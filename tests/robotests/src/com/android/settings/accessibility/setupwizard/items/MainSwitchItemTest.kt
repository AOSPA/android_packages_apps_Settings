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

package com.android.settings.accessibility.setupwizard.items

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.google.android.setupdesign.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

/** Tests for [MainSwitchItem]. */
@RunWith(RobolectricTestRunner::class)
class MainSwitchItemTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val mainSwitchItemItem: MainSwitchItem = MainSwitchItem()
    private val rootView: View =
        LayoutInflater.from(context).inflate(mainSwitchItemItem.layoutResource, null)

    @Test
    fun onBindView_setsBackground() {
        rootView.background = null

        mainSwitchItemItem.onBindView(rootView)

        val shadowDrawable = Shadows.shadowOf(rootView.background)
        assertThat(shadowDrawable.createdFromResId)
            .isEqualTo(R.drawable.sud_card_view_container_background_selected)
    }

    @Test
    fun isGroupDivider_returnsTrue() {
        assertThat(mainSwitchItemItem.isGroupDivider).isTrue()
    }
}
