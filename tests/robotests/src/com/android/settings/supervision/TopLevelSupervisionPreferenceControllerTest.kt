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
package com.android.settings.supervision

import android.app.Activity
import android.content.Intent
import androidx.preference.Preference
import com.android.settings.core.BasePreferenceController.AVAILABLE
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argThat
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TopLevelSupervisionPreferenceControllerTest {
    private val context =
        spy(Robolectric.buildActivity(Activity::class.java).get())

    private val preference = Preference(context)

    @Before
    fun setUp() {
        preference.key = PREFERENCE_KEY
    }

    @Test
    fun navigateToDashboard() {
        val preferenceController = TopLevelSupervisionPreferenceController(context, PREFERENCE_KEY)

        assertThat(preferenceController.availabilityStatus).isEqualTo(AVAILABLE)

        preferenceController.handlePreferenceTreeClick(preference)
        verify(context)
            .startActivity(componentIntentMatcher(SupervisionDashboardActivity::class.java))
    }

    private fun componentIntentMatcher(cls: Class<*>) =
        argThat<Intent> { this.component?.className == cls.name }

    private companion object {
        const val PREFERENCE_KEY = "test_key"
    }
}
