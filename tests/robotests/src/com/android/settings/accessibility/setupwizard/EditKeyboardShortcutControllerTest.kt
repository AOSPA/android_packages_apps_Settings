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
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import com.android.internal.accessibility.AccessibilityShortcutController.COLOR_INVERSION_COMPONENT_NAME
import com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME
import com.android.settings.R
import com.android.settings.accessibility.setupwizard.items.IllustrationCheckBoxItem
import com.android.settings.accessibility.shortcuts.ui.KeyboardShortcutPreference
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for [EditKeyboardShortcutController]. */
@Config(shadows = [SettingsShadowResources::class])
@RunWith(RobolectricTestRunner::class)
class EditKeyboardShortcutControllerTest {

    @get:Rule val setFlagsRule = SetFlagsRule()
    private val appContext: Application = ApplicationProvider.getApplicationContext()
    private val mockDataStore = mock<KeyValueStore>()
    private val item = IllustrationCheckBoxItem()

    @Before
    fun setUp() {
        SettingsShadowResources.overrideResource(
            com.android.internal.R.string.config_defaultAccessibilityService,
            "talkback",
        )
        SettingsShadowResources.overrideResource(
            com.android.internal.R.string.config_defaultSelectToSpeakService,
            "select_to_speak",
        )
    }

    @Test
    fun bindData_multipleTargets_setsNullSummaryAndNoIcon() {
        val (controller, _) = createController(setOf(MAGNIFICATION_CONTROLLER_NAME, "talkback"))

        controller.bindData(item)

        assertThat(item.summary).isNull()
        assertThat(item.icon).isNull()
    }

    @Test
    fun bindData_magnificationTarget_setsSummaryAndIcon() {
        val (controller, _) = createController(setOf(MAGNIFICATION_CONTROLLER_NAME))

        controller.bindData(item)

        assertSummaryContainsMetaKey()
        assertThat(item.icon).isNotNull()
    }

    @Test
    fun bindData_colorInversionTarget_setsSummaryAndIcon() {
        val target = COLOR_INVERSION_COMPONENT_NAME.flattenToString()
        val (controller, _) = createController(setOf(target))

        controller.bindData(item)

        assertSummaryContainsMetaKey()
        assertThat(item.icon).isNotNull()
    }

    @Test
    @EnableFlags(com.android.hardware.input.Flags.FLAG_ENABLE_SELECT_TO_SPEAK_KEY_GESTURES)
    fun bindData_selectToSpeakTarget_withFlagEnabled_setsSummaryAndIcon() {
        val (controller, _) = createController(setOf("select_to_speak"))

        controller.bindData(item)

        assertSummaryContainsMetaKey()
        assertThat(item.icon).isNotNull()
    }

    @Test
    fun bindData_unknownTarget_setsSummaryButNoIcon() {
        val (controller, _) = createController(setOf("unknown_target"))

        controller.bindData(item)

        assertSummaryContainsMetaKey()
        assertThat(item.icon).isNull()
    }

    @Test
    fun bindData_isCheckedInStore_setsItemCheckedTrue() {
        val (controller, store) = createController(setOf(MAGNIFICATION_CONTROLLER_NAME))
        store.setBoolean(EditKeyboardShortcutController.KEY, true)

        controller.bindData(item)

        assertThat(item.isChecked).isTrue()
    }

    @Test
    fun onItemSelected_togglesCheckedStateAndUpdatesStore() {
        val (controller, store) = createController(setOf(MAGNIFICATION_CONTROLLER_NAME))
        store.setBoolean(EditKeyboardShortcutController.KEY, false)
        controller.bindData(item)

        controller.onItemSelected(FragmentActivity())

        assertThat(store.getBoolean(EditKeyboardShortcutController.KEY)).isTrue()
    }

    @Test
    fun onStart_registersObserver() {
        val (controller, store) =
            createController(setOf(MAGNIFICATION_CONTROLLER_NAME), mockDataStore)

        controller.onStart()

        verify(store)
            .addObserver(
                eq(EditKeyboardShortcutController.KEY),
                any<KeyedObserver<String>>(),
                any(),
            )
    }

    @Test
    fun onStop_removesRegisteredObserver() {
        val (controller, store) =
            createController(setOf(MAGNIFICATION_CONTROLLER_NAME), mockDataStore)
        val captor = argumentCaptor<KeyedObserver<String>>()

        controller.onStart()
        verify(store).addObserver(any(), captor.capture(), any())

        controller.onStop()
        verify(store).removeObserver(eq(EditKeyboardShortcutController.KEY), eq(captor.firstValue))
    }

    @Test
    fun onStop_withoutStart_doesNotAttemptRemoval() {
        val (controller, store) =
            createController(setOf(MAGNIFICATION_CONTROLLER_NAME), mockDataStore)

        controller.onStop()

        verify(store, never()).removeObserver(any(), any())
    }

    @Test
    fun onKeyChanged_triggersRebindFromStore() {
        val (controller, store) =
            createController(setOf(MAGNIFICATION_CONTROLLER_NAME), mockDataStore)
        val captor = argumentCaptor<KeyedObserver<String>>()
        controller.onStart()
        verify(store).addObserver(any(), captor.capture(), any())
        reset(mockDataStore)

        captor.firstValue.onKeyChanged(EditKeyboardShortcutController.KEY, 0)

        verify(mockDataStore).getBoolean(EditKeyboardShortcutController.KEY)
    }

    /** Creates the controller and its associated store. */
    private fun createController(
        targets: Set<String>,
        dateStore: KeyValueStore? = null,
    ): Pair<EditKeyboardShortcutController, KeyValueStore> {
        val metadata = KeyboardShortcutPreference(appContext, targets)
        val store = dateStore ?: metadata.storage(appContext)
        val controller =
            EditKeyboardShortcutController(
                appContext,
                item,
                keyboardShortcutMetadata = metadata,
                keyboardShortcutMetadataDataStore = store,
            )
        return controller to store
    }

    private fun assertSummaryContainsMetaKey() {
        val metaKeyLabel = appContext.getString(R.string.modifier_keys_meta)
        assertThat(item.summary.toString()).contains(metaKeyLabel)
    }
}
