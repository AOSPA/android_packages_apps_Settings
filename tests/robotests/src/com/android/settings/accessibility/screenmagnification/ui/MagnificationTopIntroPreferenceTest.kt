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

package com.android.settings.accessibility.screenmagnification.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.fragment.app.testing.EmptyFragmentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_SETUP_FLOW
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameters
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowPackageManager

@RunWith(RobolectricTestParameterInjector::class)
class MagnificationTopIntroPreferenceTest {

    private lateinit var context: Context
    private lateinit var shadowPackageManager: ShadowPackageManager
    private var activityScenario: ActivityScenario<EmptyFragmentActivity>? = null
    private val preference = MagnificationTopIntroPreference()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        shadowPackageManager = shadowOf(context.packageManager)
    }

    @After
    fun tearDown() {
        activityScenario?.close()
    }

    @Test
    fun key() {
        assertThat(preference.key).isEqualTo("top_intro")
    }

    @Test
    fun getTitle() {
        assertThat(preference.title)
            .isEqualTo(R.string.accessibility_screen_magnification_intro_text)
    }

    @Test
    fun isIndexable() {
        assertThat(preference.indexable).isFalse()
    }

    @Test
    @TestParameters(
        value =
            [
                "{inSetupWizard: false, expectedValue: true}",
                "{inSetupWizard: true, expectedValue: false}",
            ]
    )
    fun isAvailable_returnExpectedValue(inSetupWizard: Boolean, expectedValue: Boolean) {
        val newContext = createContext(inSetupWizard)

        assertThat(preference.isAvailable(newContext)).isEqualTo(expectedValue)
    }

    private fun createContext(inSetupWizard: Boolean): Context {
        shadowPackageManager.addActivityIfNotPresent(
            ComponentName(context, EmptyFragmentActivity::class.java)
        )
        var startedActivity: Context? = null
        val intent = Intent(context, EmptyFragmentActivity::class.java)
        if (inSetupWizard) {
            intent.putExtra(EXTRA_IS_SETUP_FLOW, inSetupWizard)
        }
        activityScenario = ActivityScenario.launch(intent)
        activityScenario!!.onActivity { activity -> startedActivity = activity }
        return startedActivity!!
    }
}
