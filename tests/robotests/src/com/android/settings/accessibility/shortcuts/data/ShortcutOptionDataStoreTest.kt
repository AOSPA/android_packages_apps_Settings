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

package com.android.settings.accessibility.shortcuts.data

import android.content.Context
import android.os.UserHandle
import android.view.accessibility.AccessibilityManager
import androidx.test.core.app.ApplicationProvider
import com.android.internal.accessibility.AccessibilityShortcutController.COLOR_INVERSION_COMPONENT_NAME
import com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType
import com.android.internal.accessibility.util.ShortcutUtils
import com.android.settings.accessibility.data.AccessibilityRepositoryProvider
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils.shadow.ShadowAccessibilityManager
import com.android.settingslib.datastore.DataChangeReason
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameters
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestParameterInjector::class)
class ShortcutOptionDataStoreTest {
    @get:Rule val settingStoreRule = SettingsStoreRule()
    private lateinit var appContext: Context
    private lateinit var a11yManager: ShadowAccessibilityManager
    private lateinit var settingsSecureStore: SettingsSecureStore
    private lateinit var dataStore: ShortcutOptionDataStore

    @Before
    fun setUp() {
        appContext = ApplicationProvider.getApplicationContext()
        settingsSecureStore = SettingsSecureStore.get(appContext)
        a11yManager = Shadow.extract(appContext.getSystemService(AccessibilityManager::class.java))
    }

    @After
    fun cleanUp() {
        AccessibilityRepositoryProvider.resetInstanceForTesting()
    }

    @Test
    fun getValue_shortcutOn_returnTrue() {
        val shortcutType = UserShortcutType.SOFTWARE
        val targets =
            setOf(MAGNIFICATION_CONTROLLER_NAME, COLOR_INVERSION_COMPONENT_NAME.flattenToString())
        a11yManager.enableShortcutsForTargets(
            /* enable= */ true,
            shortcutType,
            targets,
            UserHandle.myUserId(),
        )
        dataStore = createDataStore(shortcutType, targets)

        assertThat(dataStore.getBoolean(PREF_KEY)).isTrue()
    }

    @Test
    fun getValue_shortcutOff_returnFalse() {
        val shortcutType = UserShortcutType.SOFTWARE
        val targets =
            setOf(MAGNIFICATION_CONTROLLER_NAME, COLOR_INVERSION_COMPONENT_NAME.flattenToString())
        a11yManager.enableShortcutsForTargets(
            /* enable= */ false,
            shortcutType,
            targets,
            UserHandle.myUserId(),
        )
        dataStore = createDataStore(shortcutType, targets)

        assertThat(dataStore.getBoolean(PREF_KEY)).isFalse()
    }

    @Test
    fun getValue_partialTargetsOn_returnFalse() {
        val shortcutType = UserShortcutType.SOFTWARE
        val targets =
            setOf(MAGNIFICATION_CONTROLLER_NAME, COLOR_INVERSION_COMPONENT_NAME.flattenToString())
        a11yManager.enableShortcutsForTargets(
            /* enable= */ false,
            shortcutType,
            setOf(MAGNIFICATION_CONTROLLER_NAME),
            UserHandle.myUserId(),
        )
        dataStore = createDataStore(shortcutType, targets)

        assertThat(dataStore.getBoolean(PREF_KEY)).isFalse()
    }

    @Test
    fun setValue_false_turnOffShortcutForAllTargets() {
        val shortcutType = UserShortcutType.SOFTWARE
        val targets =
            setOf(MAGNIFICATION_CONTROLLER_NAME, COLOR_INVERSION_COMPONENT_NAME.flattenToString())
        a11yManager.enableShortcutsForTargets(
            /* enable= */ true,
            shortcutType,
            targets,
            UserHandle.myUserId(),
        )
        dataStore = createDataStore(shortcutType, targets)

        dataStore.setBoolean(PREF_KEY, false)

        assertThat(a11yManager.getAccessibilityShortcutTargets(shortcutType)).isEmpty()
    }

    @Test
    fun setValue_true_turnOnShortcutForAllTargets() {
        val shortcutType = UserShortcutType.SOFTWARE
        val targets =
            setOf(MAGNIFICATION_CONTROLLER_NAME, COLOR_INVERSION_COMPONENT_NAME.flattenToString())
        a11yManager.enableShortcutsForTargets(
            /* enable= */ false,
            shortcutType,
            targets,
            UserHandle.myUserId(),
        )
        dataStore = createDataStore(shortcutType, targets)

        dataStore.setBoolean(PREF_KEY, true)

        assertThat(a11yManager.getAccessibilityShortcutTargets(shortcutType))
            .containsExactlyElementsIn(targets)
    }

    @Test
    fun setValue_unsupportedType_throwsIllegalStateException() {
        dataStore = createDataStore(UserShortcutType.SOFTWARE, setOf(MAGNIFICATION_CONTROLLER_NAME))

        assertThrows(IllegalStateException::class.java) {
            dataStore.setValue(PREF_KEY, Int::class.javaObjectType, 1)
        }
    }

    @Test
    fun onFirstObserverAdded_addObserverToSettingStore() {
        val observer = KeyedObserver<String> { key, reason -> }
        dataStore = createDataStore(UserShortcutType.SOFTWARE, setOf(MAGNIFICATION_CONTROLLER_NAME))
        dataStore.addObserver(PREF_KEY, observer, HandlerExecutor.main)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertThat(dataStore.hasAnyObserver()).isTrue()
        assertThat(settingsSecureStore.hasAnyObserver()).isTrue()
    }

    @Test
    fun onLastObserverRemoved_removeObserver() {
        val observer = KeyedObserver<String> { key, reason -> }
        dataStore = createDataStore(UserShortcutType.SOFTWARE, setOf(MAGNIFICATION_CONTROLLER_NAME))
        dataStore.addObserver(PREF_KEY, observer, HandlerExecutor.main)
        dataStore.removeObserver(PREF_KEY, observer)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertThat(dataStore.hasAnyObserver()).isFalse()
        assertThat(settingsSecureStore.hasAnyObserver()).isFalse()
    }

    @TestParameters(
        customName = "SoftwareType",
        value = ["{shortcutType: ${UserShortcutType.SOFTWARE} }"],
    )
    @TestParameters(
        customName = "HardwareType",
        value = ["{shortcutType: ${UserShortcutType.HARDWARE} }"],
    )
    @TestParameters(
        customName = "TripleTapType",
        value = ["{shortcutType: ${UserShortcutType.TRIPLETAP} }"],
    )
    @TestParameters(
        customName = "QuickSettingsType",
        value = ["{shortcutType: ${UserShortcutType.QUICK_SETTINGS} }"],
    )
    @TestParameters(
        customName = "GestureType",
        value = ["{shortcutType: ${UserShortcutType.GESTURE} }"],
    )
    @TestParameters(
        customName = "KeyGestureType",
        value = ["{shortcutType: ${UserShortcutType.KEY_GESTURE} }"],
    )
    @TestParameters(
        customName = "TopRowKeyType",
        value = ["{shortcutType: ${UserShortcutType.TOP_ROW_KEY} }"],
    )
    @Test
    fun addObserver_settingChanged_notifyChange(shortcutType: Int) {
        var observerCalled = false
        val observer = KeyedObserver<String> { key, reason -> observerCalled = true }
        dataStore = createDataStore(shortcutType, setOf(MAGNIFICATION_CONTROLLER_NAME))
        dataStore.addObserver(PREF_KEY, observer, HandlerExecutor.main)

        val settingKey = ShortcutUtils.convertToKey(shortcutType)
        settingsSecureStore.notifyChange(settingKey, DataChangeReason.UPDATE)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertThat(observerCalled).isTrue()
    }

    private fun createDataStore(
        @UserShortcutType shortcutType: Int,
        targets: Set<String>,
    ): ShortcutOptionDataStore {
        return ShortcutOptionDataStore(appContext, shortcutType, targets, settingsSecureStore)
    }

    companion object {
        private const val PREF_KEY = "prefKey"
    }
}
