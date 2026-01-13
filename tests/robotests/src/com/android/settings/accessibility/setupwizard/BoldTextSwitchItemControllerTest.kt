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

package com.android.settings.accessibility.setupwizard

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.android.settings.Settings.TestingSettingsActivity
import com.android.settings.accessibility.textreading.ui.BoldTextPreference
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.google.android.setupdesign.items.SwitchItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

/** Tests for [BoldTextSwitchItemController]. */
@RunWith(RobolectricTestRunner::class)
class BoldTextSwitchItemControllerTest {

    @get:Rule val activityScenarioRule = ActivityScenarioRule(TestingSettingsActivity::class.java)

    private val mockSwitchItem = mock<SwitchItem>()
    private val mockDataStore = mock<KeyValueStore>()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val controller: BoldTextSwitchItemController by lazy {
        BoldTextSwitchItemController(context, mockSwitchItem, mockDataStore)
    }

    @Test
    fun onStart_registersObserverWithCorrectKey() {
        controller.onStart()

        verify(mockDataStore)
            .addObserver(eq(BoldTextPreference.KEY), any<KeyedObserver<String>>(), any())
    }

    @Test
    fun onStop_removesRegisteredObserver() {
        val captor = argumentCaptor<KeyedObserver<String>>()
        controller.onStart()
        verify(mockDataStore).addObserver(any(), captor.capture(), any())
        val capturedObserver = captor.firstValue

        controller.onStop()

        verify(mockDataStore).removeObserver(eq(BoldTextPreference.KEY), eq(capturedObserver))
    }

    @Test
    fun onStop_withoutStart_doesNotAttemptRemoval() {
        controller.onStop()
        verify(mockDataStore, never()).removeObserver(any(), any())
    }

    @Test
    fun bindData_setsInitialSwitchStateFromDataStore() {
        mockDataStore.stub { on { getBoolean(eq(BoldTextPreference.KEY)) } doReturn true }

        controller.bindData(mockSwitchItem)

        verify(mockSwitchItem).isChecked = true
    }

    @Test
    fun onItemSelected_togglesSwitchState() {
        activityScenarioRule.scenario.onActivity { activity ->
            mockSwitchItem.stub { on { isChecked } doReturn false }

            controller.onItemSelected(activity)

            verify(mockDataStore).setBoolean(eq(BoldTextPreference.KEY), eq(true))
        }
    }

    @Test
    fun dataStoreChange_triggersRebind() {
        val captor = argumentCaptor<KeyedObserver<String>>()
        controller.onStart()
        verify(mockDataStore).addObserver(any(), captor.capture(), any())

        captor.firstValue.onKeyChanged(BoldTextPreference.KEY, 0)

        verify(mockDataStore, atLeastOnce()).getBoolean(eq(BoldTextPreference.KEY))
    }

    @Test
    fun switchToggle_updatesDataStore() {
        val listenerCaptor = argumentCaptor<SwitchItem.OnCheckedChangeListener>()

        controller.bindData(mockSwitchItem)
        verify(mockSwitchItem).setOnCheckedChangeListener(listenerCaptor.capture())

        val capturedListener = listenerCaptor.firstValue
        capturedListener.onCheckedChange(mockSwitchItem, true)

        verify(mockDataStore).setBoolean(eq(BoldTextPreference.KEY), eq(true))
    }
}
