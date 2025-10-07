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

package com.android.settings.accessibility.shortcuts.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType
import com.android.settings.accessibility.shortcuts.ShortcutOptionPreference as ShortcutOptionWidget
import com.android.settings.accessibility.shortcuts.data.ShortcutOptionDataStore
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.ReadWritePermit
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShortcutOptionPreferenceTest {

    private lateinit var context: Context
    private lateinit var preference: ShortcutOptionPreference

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        preference =
            object :
                ShortcutOptionPreference(
                    context = context,
                    shortcutType = UserShortcutType.SOFTWARE,
                    targets = setOf("com.test.Target1", "com.test.Target2"),
                ) {
                override val key: String
                    get() = "test_shortcut_option_pref"
            }
    }

    @Test
    fun indexable_isFalse() {
        assertThat(preference.indexable).isFalse()
    }

    @Test
    fun createWidget_returnsInstanceOfShortcutOptionWidget() {
        val widget = preference.createWidget(context)

        assertThat(widget).isInstanceOf(ShortcutOptionWidget::class.java)
    }

    @Test
    fun storage_returnsShortcutOptionDataStore() {
        val storage = preference.storage(context)

        assertThat(storage).isInstanceOf(ShortcutOptionDataStore::class.java)
    }

    @Test
    fun getReadPermissions_returnsSecureStorePermissions() {
        val expectedPermissions = SettingsSecureStore.getReadPermissions()
        assertThat(preference.getReadPermissions(context)).isEqualTo(expectedPermissions)
    }

    @Test
    fun getWritePermissions_returnsSecureStorePermissions() {
        val expectedPermissions = SettingsSecureStore.getWritePermissions()
        assertThat(preference.getWritePermissions(context)).isEqualTo(expectedPermissions)
    }

    @Test
    fun getReadPermit_alwaysReturnsAllow() {
        val permit = preference.getReadPermit(context, callingPid = 0, callingUid = 0)

        assertThat(permit).isEqualTo(ReadWritePermit.ALLOW)
    }

    @Test
    fun getWritePermit_withTrueValue_returnsDisallow() {
        val permit =
            preference.getWritePermit(
                context = context,
                value = true,
                callingPid = 0,
                callingUid = 0,
            )

        assertThat(permit).isEqualTo(ReadWritePermit.DISALLOW)
    }

    @Test
    fun getWritePermit_withFalseValue_returnsAllow() {
        val permit =
            preference.getWritePermit(
                context = context,
                value = false,
                callingPid = 0,
                callingUid = 0,
            )

        assertThat(permit).isEqualTo(ReadWritePermit.ALLOW)
    }

    @Test
    fun getWritePermit_withNullValue_returnsAllow() {
        val permit =
            preference.getWritePermit(
                context = context,
                value = null,
                callingPid = 0,
                callingUid = 0,
            )

        assertThat(permit).isEqualTo(ReadWritePermit.ALLOW)
    }
}
