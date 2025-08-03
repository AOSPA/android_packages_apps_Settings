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

package com.android.settings.accessibility

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.core.net.toUri
import androidx.fragment.app.testing.EmptyFragmentActivity
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.android.settings.core.BasePreferenceController.AVAILABLE
import com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settings.testutils.inflateViewHolder
import com.android.settingslib.widget.ButtonPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when` as whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class DisabilitySupportButtonPreferenceControllerTest {
    @get:Rule val setFlagsRule = SetFlagsRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val shadowPackageManager = shadowOf(context.packageManager)
    private var activityScenario: ActivityScenario<EmptyFragmentActivity>? = null
    private val preferenceManager = PreferenceManager(context)
    private val preferenceScreen = preferenceManager.createPreferenceScreen(context)
    private lateinit var preference: ButtonPreference
    private lateinit var controller: DisabilitySupportButtonPreferenceController
    private lateinit var accessibilityDisabilitySupportFeatureProvider:
        AccessibilityDisabilitySupportFeatureProvider

    @Before
    fun setUp() {
        accessibilityDisabilitySupportFeatureProvider =
            FakeFeatureFactory.setupForTest().accessibilityDisabilitySupportFeatureProvider
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DISABILITY_SUPPORT)
    fun getAvailabilityStatus_disabilitySupportUrlEmpty_returnUnavailable() {
        setUp("")

        assertThat(controller.availabilityStatus).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DISABILITY_SUPPORT)
    fun getAvailabilityStatus_disabilitySupportUrlNotEmpty_returnAvailable() {
        setUp(URL)

        assertThat(controller.availabilityStatus).isEqualTo(AVAILABLE)
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_DISABILITY_SUPPORT)
    fun getAvailabilityStatus_disableDisabilitySupport_returnUnavailable() {
        setUp(URL)

        assertThat(controller.availabilityStatus).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DISABILITY_SUPPORT)
    fun performClick_shouldStartBugReportIntent() {
        setUp(URL)

        preference.button.performClick()

        val startedIntent = Shadows.shadowOf(context as Application?).nextStartedActivity
        assertThat(startedIntent.getAction()).isEqualTo(Intent.ACTION_VIEW)
        assertThat(startedIntent.categories.contains(Intent.CATEGORY_BROWSABLE)).isTrue()
        assertThat(startedIntent.data).isEqualTo(URL.toUri())
    }

    private fun setUp(url: String) {
        whenever(accessibilityDisabilitySupportFeatureProvider.url).thenReturn(url)
        var newContext: Context = createContext()
        controller = DisabilitySupportButtonPreferenceController(newContext, KEY)
        preference = ButtonPreference(newContext)
        preference.setKey(KEY)
        preferenceScreen.addPreference(preference)
        controller.displayPreference(preferenceScreen)
        preference.onBindViewHolder(preference.inflateViewHolder())
    }

    private fun createContext(): Context {
        shadowPackageManager.addActivityIfNotPresent(
            ComponentName(context, EmptyFragmentActivity::class.java)
        )
        var startedActivity: Context? = null
        val intent = Intent(context, EmptyFragmentActivity::class.java)
        activityScenario = ActivityScenario.launch(intent)
        activityScenario!!.onActivity { activity -> startedActivity = activity }
        return startedActivity!!
    }

    companion object {
        private const val KEY = "test_key"
        private const val URL = "url"
    }
}
