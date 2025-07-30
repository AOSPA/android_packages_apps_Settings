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

package com.android.settings.accessibility.shared.data

import android.content.ComponentName
import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.test.core.app.ApplicationProvider
import com.android.internal.accessibility.AccessibilityShortcutController.ACCESSIBILITY_HEARING_AIDS_COMPONENT_NAME
import com.android.internal.accessibility.common.ShortcutConstants
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.HARDWARE
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.QUICK_SETTINGS
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.SOFTWARE
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.TRIPLETAP
import com.android.settings.accessibility.PreferredShortcut
import com.android.settings.accessibility.PreferredShortcuts
import com.android.settings.testutils.shadow.ShadowAccessibilityManager
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadow.api.Shadow

const val TEST_KEY = "testKey"

@RunWith(RobolectricTestRunner::class)
class AccessibilityShortcutDataStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val testComponentName: ComponentName = ACCESSIBILITY_HEARING_AIDS_COMPONENT_NAME
    private val testComponentString: String = testComponentName.flattenToString()
    private val a11yManager: ShadowAccessibilityManager =
        Shadow.extract(context.getSystemService(AccessibilityManager::class.java))

    @Test
    fun setValue_true_preferredShortcutEnabled() = runTest {
        assertThat(a11yManager.getAccessibilityShortcutTargets(SOFTWARE)).isEmpty()
        assertThat(a11yManager.getAccessibilityShortcutTargets(HARDWARE)).isEmpty()
        setPreferredShortcuts(SOFTWARE or HARDWARE)
        val dataStore = createDataStoreFromTestScope(this)

        dataStore.setBoolean(TEST_KEY, true)

        assertThat(a11yManager.getAccessibilityShortcutTargets(SOFTWARE))
            .contains(testComponentString)
        assertThat(a11yManager.getAccessibilityShortcutTargets(HARDWARE))
            .contains(testComponentString)
    }

    @Test
    fun setValue_false_preferredShortcutDisabled() = runTest {
        enableShortcuts(true, SOFTWARE or HARDWARE)
        assertThat(a11yManager.getAccessibilityShortcutTargets(SOFTWARE))
            .contains(testComponentString)
        assertThat(a11yManager.getAccessibilityShortcutTargets(HARDWARE))
            .contains(testComponentString)
        val dataStore = createDataStoreFromTestScope(this)

        dataStore.setBoolean(TEST_KEY, false)

        assertThat(a11yManager.getAccessibilityShortcutTargets(SOFTWARE)).isEmpty()
        assertThat(a11yManager.getAccessibilityShortcutTargets(HARDWARE)).isEmpty()
    }

    @Test
    fun getValue_assertValueCorrect() = runTest {
        val dataStore = createDataStoreFromTestScope(this)

        enableShortcuts(true, QUICK_SETTINGS)
        assertThat(dataStore.getBoolean(TEST_KEY)).isTrue()

        enableShortcuts(false, QUICK_SETTINGS)
        assertThat(dataStore.getBoolean(TEST_KEY)).isFalse()
    }

    @Test
    fun addObserver_settingsStoreAddObserver() = runTest {
        val mockSettingsStore = mock<KeyValueStore>()
        val dataStore = createDataStoreFromTestScope(this, mockSettingsStore)

        dataStore.addObserver(mock<KeyedObserver<String?>>(), mock<Executor>())

        for (settingsKey in ShortcutConstants.GENERAL_SHORTCUT_SETTINGS.toList()) {
            verify(mockSettingsStore).addObserver(settingsKey, dataStore, HandlerExecutor.main)
        }
    }

    @Test
    fun removeObserver_settingsStoreRemoveObserver() = runTest {
        val mockSettingsStore = mock<KeyValueStore>()
        val dataStore = createDataStoreFromTestScope(this, mockSettingsStore)

        val mockObserver = mock<KeyedObserver<String?>>()
        dataStore.addObserver(mockObserver, mock<Executor>())
        dataStore.removeObserver(mockObserver)

        for (settingsKey in ShortcutConstants.GENERAL_SHORTCUT_SETTINGS.toList()) {
            verify(mockSettingsStore).removeObserver(settingsKey, dataStore)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onKeyChanged_afterDebounceTime_settingsUpdated() = runTest {
        val originalShortcut = TRIPLETAP
        a11yManager.setAccessibilityShortcutTargets(TRIPLETAP, listOf(testComponentString))
        val dataStore = createDataStoreFromTestScope(this)
        // Make sure the scope.launch in AccessibilityShortcutDataStore.init block is executed
        advanceUntilIdle()
        assertThat(dataStore.getUserShortcutTypes()).isEqualTo(originalShortcut)

        val updatedShortcuts = SOFTWARE or TRIPLETAP
        a11yManager.setAccessibilityShortcutTargets(updatedShortcuts, listOf(testComponentString))
        dataStore.onKeyChanged("key", 0)
        advanceTimeBy(10L)
        // Debounce time isn't passed, shortcut shouldn't be updated
        assertThat(dataStore.getUserShortcutTypes()).isEqualTo(originalShortcut)

        dataStore.onKeyChanged("key", 0)
        advanceTimeBy(200L)
        // Debounce time is passed, shortcut should be updated
        assertThat(dataStore.getUserShortcutTypes()).isEqualTo(updatedShortcuts)
    }

    private fun enableShortcuts(enable: Boolean, types: Int) {
        a11yManager.enableShortcutsForTargets(
            enable,
            types,
            setOf(testComponentString),
            context.userId,
        )
        if (types != ShortcutConstants.UserShortcutType.DEFAULT) {
            PreferredShortcuts.saveUserShortcutType(
                context,
                PreferredShortcut(testComponentString, types),
            )
        }
    }

    private fun setPreferredShortcuts(types: Int) {
        PreferredShortcuts.saveUserShortcutType(
            context,
            PreferredShortcut(testComponentString, types),
        )
    }

    private fun createDataStoreFromTestScope(
        testScope: TestScope,
        settingsStore: KeyValueStore = SettingsSecureStore.get(context),
    ): AccessibilityShortcutDataStore =
        AccessibilityShortcutDataStore(
            context,
            testComponentName,
            testScope.backgroundScope,
            StandardTestDispatcher(testScope.testScheduler),
            settingsStore,
        )
}
