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

import android.content.Intent
import android.hardware.display.BrightnessInfo
import android.hardware.display.DisplayManager
import android.view.Display
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.android.settings.EmptySetupWizardActivity
import com.android.settings.display.BrightnessLevelPreference
import com.android.settings.testutils.SystemProperty
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.google.android.setupdesign.items.Item
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameters
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.Shadows.shadowOf

/** Tests for [BrightnessLevelItemController]. */
@RunWith(RobolectricTestParameterInjector::class)
class BrightnessLevelItemControllerTest {

    @get:Rule val activityScenarioRule = ActivityScenarioRule(EmptySetupWizardActivity::class.java)

    private val mockItem = mock<Item>()
    private val mockDataStore = mock<KeyValueStore>()
    private val mockDisplay = mock<Display>()
    private val mockDisplayManager = mock<DisplayManager>()

    private lateinit var controller: BrightnessLevelItemController

    @Before
    fun setUp() {
        activityScenarioRule.scenario.onActivity { activity ->
            val spyActivity = spy(activity)
            mockDisplay.stub { on { displayId } doReturn Display.DEFAULT_DISPLAY }
            spyActivity.stub {
                on { display } doReturn mockDisplay
                on { getSystemService(DisplayManager::class.java) } doReturn mockDisplayManager
            }
            controller = BrightnessLevelItemController(spyActivity, mockItem, mockDataStore)
        }
    }

    @Test
    fun onStart_registersObserverWithCorrectKey() {
        controller.onStart()

        val captor = argumentCaptor<KeyedObserver<String>>()
        verify(mockDataStore)
            .addObserver(eq(BrightnessLevelPreference.KEY), captor.capture(), any())
    }

    @Test
    fun onStop_removesRegisteredObserver() {
        val captor = argumentCaptor<KeyedObserver<String>>()
        controller.onStart()
        verify(mockDataStore).addObserver(any(), captor.capture(), any())
        val capturedObserver = captor.firstValue

        controller.onStop()

        verify(mockDataStore)
            .removeObserver(eq(BrightnessLevelPreference.KEY), eq(capturedObserver))
    }

    @Test
    fun onStop_withoutStart_doesNotAttemptRemoval() {
        controller.onStop()
        verify(mockDataStore, never()).removeObserver(any(), any())
    }

    @Test
    @TestParameters(
        value =
            [
                "{brightnessPercentage: 100, expected: '100%'}",
                "{brightnessPercentage: 50, expected: '87%'}",
                "{brightnessPercentage: 0, expected: '0%'}",
            ]
    )
    fun dataStoreChange_updatesItemSummary(brightnessPercentage: Int, expected: String) {
        val brightnessValue = brightnessPercentage.toFloat() / 100f
        mockDisplay.stub {
            on { brightnessInfo } doReturn
                BrightnessInfo(
                    brightnessValue,
                    /* brightnessMinimum= */ 0.0f,
                    /* brightnessMaximum= */ 1.0f,
                    BrightnessInfo.HIGH_BRIGHTNESS_MODE_OFF,
                    /* highBrightnessTransitionPoint= */ 0.5f,
                    BrightnessInfo.BRIGHTNESS_MAX_REASON_NONE,
                )
        }
        val captor = argumentCaptor<KeyedObserver<String>>()
        controller.onStart()
        verify(mockDataStore)
            .addObserver(eq(BrightnessLevelPreference.KEY), captor.capture(), any())
        reset(mockItem)

        captor.firstValue.onKeyChanged(BrightnessLevelPreference.KEY, brightnessPercentage)

        verify(mockItem).summary = expected
    }

    @Test
    fun onItemSelected_startsBrightnessDialog() {
        activityScenarioRule.scenario.onActivity { activity ->
            controller.onItemSelected(activity)

            val nextIntent = shadowOf(activity).nextStartedActivity

            assertThat(nextIntent.action).isEqualTo(Intent.ACTION_SHOW_BRIGHTNESS_DIALOG)
        }
    }

    companion object {
        private var systemProperty: SystemProperty? = null

        @BeforeClass
        @JvmStatic
        fun setup() {
            // Robolectric by default creates Activity contexts without an associated Display.
            // This property ensures the Activity context is attached to a simulated display,
            // preventing UnsupportedOperationException when code calls Context#getDisplay().
            systemProperty =
                SystemProperty().apply { override("robolectric.createActivityContexts", "true") }
        }

        @AfterClass
        @JvmStatic
        fun cleanUp() {
            systemProperty?.let {
                it.close()
                systemProperty = null
            }
        }
    }
}
