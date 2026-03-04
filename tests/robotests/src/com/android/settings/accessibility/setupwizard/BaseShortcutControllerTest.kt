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

import android.app.Application
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import com.android.settings.accessibility.setupwizard.items.IllustrationCheckBoxItem
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.google.android.setupdesign.items.Item
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

/** Tests for [BaseShortcutController]. */
@RunWith(RobolectricTestRunner::class)
class BaseShortcutControllerTest {

    private val appContext: Application = ApplicationProvider.getApplicationContext()
    private val mockDataStore = mock<KeyValueStore>()
    private val item = IllustrationCheckBoxItem()
    private val controller = TestShortcutController(appContext, item, mockDataStore, TEST_KEY)

    @Test
    fun bindData_setsSummary() {
        controller.bindData(item)

        assertThat(item.summary).isEqualTo(TEST_SUMMARY)
    }

    @Test
    fun bindData_setsCheckedStateFromStore() {
        mockDataStore.stub { on { getBoolean(eq(TEST_KEY)) } doReturn true }

        controller.bindData(item)

        assertThat(item.isChecked).isTrue()
    }

    @Test
    fun onItemSelected_togglesValueInStore() {
        item.isChecked = false

        controller.onItemSelected(FragmentActivity())

        verify(mockDataStore).setBoolean(TEST_KEY, true)
    }

    @Test
    fun onStart_registersObserver() {
        controller.onStart()

        verify(mockDataStore).addObserver(eq(TEST_KEY), any<KeyedObserver<String>>(), any())
    }

    @Test
    fun onStop_removesObserver() {
        val captor = argumentCaptor<KeyedObserver<String>>()

        controller.onStart()
        verify(mockDataStore).addObserver(any(), captor.capture(), any())

        controller.onStop()
        verify(mockDataStore).removeObserver(eq(TEST_KEY), eq(captor.firstValue))
    }

    @Test
    fun onKeyChanged_triggersRebind() {
        val captor = argumentCaptor<KeyedObserver<String>>()
        controller.onStart()
        verify(mockDataStore).addObserver(any(), captor.capture(), any())
        reset(mockDataStore)

        captor.firstValue.onKeyChanged(TEST_KEY, 0)

        verify(mockDataStore).getBoolean(TEST_KEY)
    }

    companion object {
        private const val TEST_KEY = "test_shortcut_key"

        private const val TEST_SUMMARY = "test_summary"

        /** Concrete implementation for testing the Base class logic. */
        private class TestShortcutController(
            context: android.content.Context,
            item: Item,
            dataStore: KeyValueStore,
            key: String,
        ) : BaseShortcutController(context, item, dataStore, key) {
            override fun updateItemVisuals(item: IllustrationCheckBoxItem) {
                item.summary = TEST_SUMMARY
            }
        }
    }
}
