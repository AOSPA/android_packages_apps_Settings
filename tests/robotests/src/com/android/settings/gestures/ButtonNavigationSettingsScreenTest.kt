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
import android.view.accessibility.Flags
import androidx.test.core.app.ApplicationProvider
import com.android.settings.Settings
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.TestScope
import org.junit.Test

class ButtonNavigationSettingsScreenTest() : SettingsCatalystTestCase() {
    override val preferenceScreenCreator = ButtonNavigationSettingsScreen()
    override val flagName
        get() = Flags.FLAG_NAVBAR_FLIP_ORDER_OPTION

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val testScope = TestScope()

    @Test override fun migration() {}

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
            ComponentName(appContext, Settings.ButtonNavigationSettingsActivity::class.java)
        val intent = preferenceScreenCreator.getLaunchIntent(appContext, null)
        assertThat(intent).isNotNull()
        assertThat(intent!!.component).isEqualTo(expectedComponent)
    }
}
