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

package com.android.settings.accessibility.textreading.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FocusTargetViewModelTest {

    private lateinit var viewModel: FocusTargetViewModel

    @Before
    fun setUp() {
        viewModel = FocusTargetViewModel()
    }

    @Test
    fun defaultFocusTarget_isNull() {
        assertThat(viewModel.focusTarget.value).isNull()
    }

    @Test
    fun setFocusTarget_updatesValue() {
        val testKey = "test_preference_key"
        val testViewId = 12345

        viewModel.setFocusTarget(testKey, testViewId)

        val target = viewModel.focusTarget.value
        assertThat(target).isNotNull()
        assertThat(target?.preferenceKey).isEqualTo(testKey)
        assertThat(target?.viewId).isEqualTo(testViewId)
    }

    @Test
    fun clearFocusTarget_resetsToNull() {
        // First set a target
        viewModel.setFocusTarget("some_key", 987)
        assertThat(viewModel.focusTarget.value).isNotNull()

        // Then clear it
        viewModel.clearFocusTarget()

        assertThat(viewModel.focusTarget.value).isNull()
    }
}
