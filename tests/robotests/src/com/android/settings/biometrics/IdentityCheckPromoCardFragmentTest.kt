/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.biometrics

import android.os.Looper
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
class IdentityCheckPromoCardFragmentTest {
    private lateinit var activity: FragmentActivity

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
    }

    @Test
    fun launchFragment_showDialog() {
        val bottomSheetDialog = setUpFragment()
        val behavior = bottomSheetDialog.behavior

        assertThat(behavior.skipCollapsed).isTrue()
        assertThat(behavior.isDraggable).isFalse()
        assertThat(behavior.isFitToContents).isTrue()
        assertThat(behavior.state).isEqualTo(BottomSheetBehavior.STATE_EXPANDED)
    }

    private fun setUpFragment(): BottomSheetDialog {
        val fragment = IdentityCheckPromoCardFragment()
        fragment.show(activity.supportFragmentManager, null)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val dialog = ShadowDialog.getLatestDialog() as BottomSheetDialog
        assertThat(dialog).isNotNull()

        return dialog
    }
}