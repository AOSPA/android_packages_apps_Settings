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

package com.android.settings.accessibility.setupwizard

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.android.settings.EmptySetupWizardActivity
import com.android.settings.accessibility.setupwizard.items.TwoTargetItem
import com.android.settings.accessibility.shared.ui.AccessibilityShortcutPreference
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.google.common.truth.Truth.assertThat
import org.junit.Before
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
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

/** Tests for [MagnificationShortcutTwoTargetItemController]. */
@RunWith(RobolectricTestRunner::class)
class MagnificationShortcutTwoTargetItemControllerTest {

    @get:Rule val activityScenarioRule = ActivityScenarioRule(EmptySetupWizardActivity::class.java)

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val mockDataStore = mock<KeyValueStore>()
    private val mockMetadata = mock<AccessibilityShortcutPreference>()
    private val twoTargetItem = TwoTargetItem()

    private lateinit var controller: MagnificationShortcutTwoTargetItemController

    @Before
    fun setUp() {
        mockMetadata.stub { on { getSummary(any()) } doReturn SUMMARY }

        controller =
            MagnificationShortcutTwoTargetItemController(
                context,
                twoTargetItem,
                magnificationShortcutMetadata = mockMetadata,
                magnificationShortcutMetadataDataStore = mockDataStore,
            )
    }

    @Test
    fun bindData_setsItemPropertiesFromMetadata() {
        mockDataStore.stub { on { getBoolean(eq(KEY)) } doReturn true }

        controller.bindData(twoTargetItem)

        assertThat(twoTargetItem.summary.toString()).isEqualTo(SUMMARY)
        assertThat(twoTargetItem.isChecked).isTrue()
    }

    @Test
    fun onStart_registersObserver() {
        controller.onStart()

        verify(mockDataStore).addObserver(eq(KEY), any<KeyedObserver<String>>(), any())
    }

    @Test
    fun onStop_removesRegisteredObserver() {
        val captor = argumentCaptor<KeyedObserver<String>>()
        controller.onStart()
        verify(mockDataStore).addObserver(any(), captor.capture(), any())

        val capturedObserver = captor.firstValue
        controller.onStop()

        verify(mockDataStore).removeObserver(eq(KEY), eq(capturedObserver))
    }

    @Test
    fun onStop_withoutStart_doesNotAttemptRemoval() {
        controller.onStop()
        verify(mockDataStore, never()).removeObserver(any(), any())
    }

    @Test
    fun switchToggled_updatesDataStore() {
        controller.bindData(twoTargetItem)

        twoTargetItem.isChecked = true

        verify(mockDataStore).setBoolean(eq(KEY), eq(true))
    }

    @Test
    fun dataStoreChange_triggersRebind() {
        val captor = argumentCaptor<KeyedObserver<String>>()
        controller.onStart()
        verify(mockDataStore).addObserver(any(), captor.capture(), any())
        reset(mockDataStore)

        captor.firstValue.onKeyChanged(KEY, 0)

        verify(mockDataStore).getBoolean(eq(KEY))
    }

    companion object {
        private const val KEY = "magnification_shortcut_preference"
        private const val SUMMARY = "Shortcut summary"
    }
}
