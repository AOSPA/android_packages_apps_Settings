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

package com.android.settings.accessibility.screenmagnification

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.util.Consumer
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.android.internal.accessibility.common.NotificationConstants.ACTION_CANCEL_SURVEY_NOTIFICATION
import com.android.internal.accessibility.common.NotificationConstants.ACTION_SCHEDULE_SURVEY_NOTIFICATION
import com.android.internal.accessibility.common.NotificationConstants.EXTRA_PAGE_ID
import com.android.settings.Utils.SETTINGS_PACKAGE_NAME
import com.android.settings.accessibility.ShortcutPreference
import com.android.settings.accessibility.SurveyManager
import com.android.settings.overlay.SurveyFeatureProvider
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settings.testutils.inflateViewHolder
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.doAnswer
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

private const val PREFERENCE_KEY = "prefKey"
private const val SURVEY_TRIGGER_KEY = "surveyTriggerKey"
private const val PAGE_ID = 123

/** Tests for [ToggleMagnificationShortcutPreferenceController] */
@RunWith(RobolectricTestRunner::class)
class ToggleMagnificationShortcutPreferenceControllerTest {

    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()

    private lateinit var shortcutPreference: ShortcutPreference
    private lateinit var controller: ToggleMagnificationShortcutPreferenceController
    private lateinit var fragmentScenario: FragmentScenario<Fragment>
    private lateinit var surveyFeatureProvider: SurveyFeatureProvider
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val preferenceManager = PreferenceManager(context)

    @Before
    fun setUp() {
        surveyFeatureProvider =
            FakeFeatureFactory.setupForTest().getSurveyFeatureProvider(context)!!
        controller = ToggleMagnificationShortcutPreferenceController(context, PREFERENCE_KEY)
        controller.setSurveyManager(
            SurveyManager(TestLifecycleOwner(), context, SURVEY_TRIGGER_KEY, PAGE_ID)
        )

        fragmentScenario = launchFragment<Fragment>(initialState = Lifecycle.State.INITIALIZED)
        fragmentScenario.onFragment { fragment -> { fragment.lifecycle.addObserver(controller) } }
        shortcutPreference = ShortcutPreference(context, null)
        shortcutPreference.key = PREFERENCE_KEY
        val preferenceScreen = preferenceManager.createPreferenceScreen(context)
        preferenceScreen.addPreference(shortcutPreference)
        preferenceManager.setPreferences(preferenceScreen)
        shortcutPreference.inflateViewHolder()
    }

    @After
    fun cleanUp() {
        fragmentScenario.close()
    }

    @Test
    fun updateState_checkedAndSurveyAvailable_sendScheduleBroadcast() {
        setupSurveyAvailability(/* available= */ true)
        shortcutPreference.isChecked = true

        controller.updateState(shortcutPreference)

        val intents = Shadows.shadowOf(context as Application?).getBroadcastIntents()
        assertThat(intents).hasSize(1)
        val intent: Intent = intents.get(0)
        assertThat(intent.getAction()).isEqualTo(ACTION_SCHEDULE_SURVEY_NOTIFICATION)
        assertThat(intent.getPackage()).isEqualTo(SETTINGS_PACKAGE_NAME)
        assertThat(intent.getIntExtra(EXTRA_PAGE_ID, /* def= */ -1)).isEqualTo(PAGE_ID)
    }

    @Test
    fun updateState_checkedAndSurveyUnavailable_noBroadcast() {
        setupSurveyAvailability(/* available= */ false)
        shortcutPreference.isChecked = true

        controller.updateState(shortcutPreference)

        val intents = Shadows.shadowOf(context as Application?).getBroadcastIntents()
        assertThat(intents).isEmpty()
    }

    @Test
    fun updateState_notChecked_sendCancelBroadcast() {
        shortcutPreference.isChecked = false

        controller.updateState(shortcutPreference)

        val intents = Shadows.shadowOf(context as Application?).getBroadcastIntents()
        assertThat(intents).hasSize(1)
        val intent: Intent = intents.get(0)
        assertThat(intent.getAction()).isEqualTo(ACTION_CANCEL_SURVEY_NOTIFICATION)
        assertThat(intent.getPackage()).isEqualTo(SETTINGS_PACKAGE_NAME)
        assertThat(intent.getIntExtra(EXTRA_PAGE_ID, /* def= */ -1)).isEqualTo(PAGE_ID)
    }

    private fun setupSurveyAvailability(available: Boolean) {
        doAnswer { invocation ->
                val consumer: Consumer<Boolean?> = invocation.getArgument(2)
                consumer.accept(available)
                null
            }
            .`when`(surveyFeatureProvider)
            .checkSurveyAvailable(any(), anyString(), any())
    }
}
