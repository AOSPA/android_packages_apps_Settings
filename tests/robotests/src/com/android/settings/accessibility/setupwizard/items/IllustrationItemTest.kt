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
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.airbnb.lottie.LottieAnimationView
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests for [IllustrationItem]. */
@RunWith(RobolectricTestRunner::class)
class IllustrationItemTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val illustrationItem: IllustrationItem = IllustrationItem()
    private val rootView: View =
        LayoutInflater.from(context).inflate(R.layout.setup_illustration_item, null)

    @Test
    fun onBindView_setsImageDrawable() {
        val drawable = ColorDrawable(Color.RED)
        illustrationItem.imageDrawable = drawable

        illustrationItem.onBindView(rootView)

        val illustrationView =
            rootView.findViewById<LottieAnimationView>(R.id.sud_item_illustration)
        assertThat(illustrationView.drawable).isEqualTo(drawable)
    }

    @Test
    fun onBindView_setsContentDescription() {
        val testDescription = "Test Description"
        illustrationItem.contentDescription = testDescription

        illustrationItem.onBindView(rootView)

        assertThat(rootView.contentDescription).isEqualTo(testDescription)
    }

    @Test
    fun isGroupDivider_returnsTrue() {
        assertThat(illustrationItem.isGroupDivider).isTrue()
    }

    @Test
    fun isEnabled_returnsFalse() {
        assertThat(illustrationItem.isEnabled).isFalse()
    }

    @Test
    fun setImageUri_clearsOtherResources() {
        illustrationItem.imageDrawable = ColorDrawable(Color.RED)

        val uri = Uri.parse("content://test/image.png")
        illustrationItem.imageUri = uri

        assertOnlyThisResourceIsSet(expectedUri = uri)
    }

    @Test
    fun setImageDrawable_clearsOtherResources() {
        illustrationItem.imageUri = Uri.parse("content://test")

        val drawable = ColorDrawable(Color.BLUE)
        illustrationItem.imageDrawable = drawable

        assertOnlyThisResourceIsSet(expectedDrawable = drawable)
    }

    /** Helper to verify that only one resource is active at a time. */
    private fun assertOnlyThisResourceIsSet(
        expectedDrawable: Drawable? = null,
        expectedUri: Uri? = null,
    ) {
        assertThat(illustrationItem.imageDrawable).isEqualTo(expectedDrawable)
        assertThat(illustrationItem.imageUri).isEqualTo(expectedUri)
    }
}
