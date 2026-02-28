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
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.airbnb.lottie.LottieAnimationView
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

/** Tests for [IllustrationCheckBoxItem]. */
@RunWith(RobolectricTestRunner::class)
class IllustrationCheckBoxItemTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val item = IllustrationCheckBoxItem()
    private val rootView: View = LayoutInflater.from(context).inflate(item.layoutResource, null)

    @Test
    fun onBindView_noResources_hidesImageView() {
        item.onBindView(rootView)

        val lottieView = rootView.findViewById<LottieAnimationView>(R.id.sud_items_image)
        assertThat(lottieView.visibility).isEqualTo(View.GONE)
    }

    @Test
    fun onBindView_withResId_showsImageViewAndSetsImage() {
        item.imageResId = R.drawable.accessibility_shortcut_type_volume_keys

        item.onBindView(rootView)

        val lottieView = rootView.findViewById<LottieAnimationView>(R.id.sud_items_image)
        assertThat(lottieView.visibility).isEqualTo(View.VISIBLE)
    }

    @Test
    fun onBindView_withRawResId_showsImageViewAndStartsAnimation() {
        val lottieView = LottieAnimationView(context).apply { id = R.id.sud_items_image }
        val spyLottieView = spy(lottieView)
        val spyRootView =
            spy(rootView).stub {
                on { findViewById<LottieAnimationView>(R.id.sud_items_image) } doReturn
                    spyLottieView
            }
        item.imageRawResId = R.raw.accessibility_shortcut_type_fab

        item.onBindView(spyRootView)

        verify(spyLottieView).visibility = View.VISIBLE
        verify(spyLottieView).setAnimation(R.raw.accessibility_shortcut_type_fab)
        verify(spyLottieView).repeatCount = 0
        verify(spyLottieView).playAnimation()
    }

    @Test
    fun setIntroImageResId_updatesInternalStateAndClearsRaw() {
        item.imageRawResId = R.raw.accessibility_shortcut_type_fab
        item.imageResId = R.drawable.accessibility_shortcut_type_volume_keys

        assertThat(item.imageResId).isEqualTo(R.drawable.accessibility_shortcut_type_volume_keys)
        assertThat(item.imageRawResId).isEqualTo(Resources.ID_NULL)
    }

    @Test
    fun setIntroImageRawResId_updatesInternalStateAndClearsResId() {
        item.imageResId = R.drawable.accessibility_shortcut_type_volume_keys
        item.imageRawResId = R.raw.accessibility_shortcut_type_fab

        assertThat(item.imageResId).isEqualTo(Resources.ID_NULL)
        assertThat(item.imageRawResId).isEqualTo(R.raw.accessibility_shortcut_type_fab)
    }
}
