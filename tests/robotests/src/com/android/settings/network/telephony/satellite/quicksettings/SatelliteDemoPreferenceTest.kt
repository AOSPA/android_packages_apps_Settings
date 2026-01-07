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

package com.android.settings.network.telephony.satellite.quicksettings

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.preference.PreferenceViewHolder
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SatelliteDemoPreferenceTest {

    private lateinit var context: Context
    private lateinit var preference: SatelliteDemoPreference
    private lateinit var holder: PreferenceViewHolder
    private lateinit var iconView: ImageView
    private lateinit var iconFrame: FrameLayout

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        preference = SatelliteDemoPreference(context, null)

        // Create a mock view hierarchy similar to what default Preference uses
        // Standard preference layout usually has an icon_frame containing the icon
        val container = LinearLayout(context)

        iconFrame =
            FrameLayout(context).apply {
                id = android.R.id.icon_frame
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
            }

        iconView =
            ImageView(context).apply {
                id = android.R.id.icon
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
            }

        iconFrame.addView(iconView)
        container.addView(iconFrame)

        holder = PreferenceViewHolder.createInstanceForTests(container)
    }

    @Test
    fun onBindViewHolder_modifiesIconViewProperties() {
        preference.onBindViewHolder(holder)

        assertThat(iconView.adjustViewBounds).isTrue()
        assertThat(iconView.maxWidth).isEqualTo(Int.MAX_VALUE)
        assertThat(iconView.maxHeight).isEqualTo(Int.MAX_VALUE)
        assertThat(iconView.scaleType).isEqualTo(ImageView.ScaleType.FIT_CENTER)
        assertThat(iconView.layoutParams.width).isEqualTo(ViewGroup.LayoutParams.WRAP_CONTENT)
        assertThat(iconView.layoutParams.height).isEqualTo(ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    @Test
    fun onBindViewHolder_modifiesIconFrameProperties() {
        preference.onBindViewHolder(holder)

        assertThat(iconFrame.layoutParams.width).isEqualTo(ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}
