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

package com.android.settings.gestures

import android.content.ComponentName
import android.content.Context
import android.testing.TestableContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.settings.Settings
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.TestScope
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ButtonNavigationSettingsScreenTest {
    val preferenceScreenCreator = ButtonNavigationSettingsScreen()

    private val context: Context =
        TestableContext(InstrumentationRegistry.getInstrumentation().context)
    private val testScope = TestScope()

    @Test
    fun getPreferenceHierarchy_returnsHierarchy() {
        val hierarchy = preferenceScreenCreator.getPreferenceHierarchy(context, testScope)

        assertThat(hierarchy.find(DefaultButtonNavigationSettingsOrderPreference.KEY)).isNotNull()
        assertThat(hierarchy.find(ReverseButtonNavigationSettingsOrderPreference.KEY)).isNotNull()
    }

    @Test
    fun isIndexable_returnTrue() {
        assertThat(preferenceScreenCreator.indexable).isTrue()
    }

    @Test
    fun hasCompleteHierarchy() {
        assertThat(preferenceScreenCreator.hasCompleteHierarchy()).isTrue()
    }

    @Test
    fun getFragmentClass() {
        assertThat(preferenceScreenCreator.fragmentClass())
            .isEqualTo(ButtonNavigationSettingsFragment::class.java)
    }

    @Test
    fun getLaunchIntent_returnButtonNavigationSettingsActivityIntent() {
        val expectedComponent =
            ComponentName(context, Settings.ButtonNavigationSettingsActivity::class.java)
        val intent = preferenceScreenCreator.getLaunchIntent(context, null)
        assertThat(intent).isNotNull()
        assertThat(intent!!.component).isEqualTo(expectedComponent)
    }
}
