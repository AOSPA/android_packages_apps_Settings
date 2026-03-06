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

package com.android.settings.accessibility.setupwizard

import android.content.Context
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.android.server.accessibility.Flags.FLAG_ENABLE_COLOR_INVERSION_IN_SUW
import com.android.settings.EmptySetupWizardActivity
import com.android.settings.R
import com.google.android.setupdesign.items.Item
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.robolectric.RobolectricTestRunner

/** Tests for [ColorInversionItemController]. */
@RunWith(RobolectricTestRunner::class)
class ColorInversionItemControllerTest {

    @get:Rule val setFlagsRule = SetFlagsRule()
    @get:Rule val activityScenarioRule = ActivityScenarioRule(EmptySetupWizardActivity::class.java)
    private lateinit var context: Context
    private val item = Item()
    private val configResId = com.android.internal.R.bool.config_enableColorInversionInSetupWizard

    @Before
    fun setUp() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        val spyResources = spy(baseContext.resources)
        context = spy(baseContext)
        context.stub { on { resources } doReturn spyResources }
    }

    @Test
    @EnableFlags(FLAG_ENABLE_COLOR_INVERSION_IN_SUW)
    fun bindData_showsItem() {
        context.resources.stub { on { getBoolean(configResId) } doReturn true }
        val controller = ColorInversionItemController(context, item)

        controller.bindData(item)

        assertThat(item.isVisible).isTrue()
    }

    @Test
    @DisableFlags(FLAG_ENABLE_COLOR_INVERSION_IN_SUW)
    fun bindData_flagOff_hidesItem() {
        context.resources.stub { on { getBoolean(configResId) } doReturn true }
        val controller = ColorInversionItemController(context, item)

        controller.bindData(item)

        assertThat(item.isVisible).isFalse()
    }

    @Test
    @EnableFlags(FLAG_ENABLE_COLOR_INVERSION_IN_SUW)
    fun bindData_configOff_hidesItem() {
        context.resources.stub { on { getBoolean(configResId) } doReturn false }
        val controller = ColorInversionItemController(context, item)

        controller.bindData(item)

        assertThat(item.isVisible).isFalse()
    }

    @Test
    fun onItemSelected_navigatesToTextReadingSetupWizardFragment() {
        activityScenarioRule.scenario.onActivity { activity ->
            activity.setContentView(R.layout.accessibility_suw_activity)
            val controller = ColorInversionItemController(context, item)

            controller.onItemSelected(activity)
            activity.supportFragmentManager.executePendingTransactions()

            val currentFragment =
                activity.supportFragmentManager.findFragmentById(R.id.fragment_container)
            assertThat(currentFragment).isInstanceOf(ColorInversionSetupWizardFragment::class.java)
        }
    }
}
