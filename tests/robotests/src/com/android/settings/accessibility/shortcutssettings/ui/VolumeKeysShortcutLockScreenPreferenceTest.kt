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

package com.android.settings.accessibility.shortcutssettings.ui

import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.HARDWARE
import com.android.settings.R
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils.shadow.ShadowAccessibilityManager
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow

/** Test for [VolumeKeysShortcutLockScreenPreference] */
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowAccessibilityManager::class])
class VolumeKeysShortcutLockScreenPreferenceTest {

    @get:Rule val settingsStoreRule = SettingsStoreRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preference = VolumeKeysShortcutLockScreenPreference()
    private val shadowA11yManager: ShadowAccessibilityManager =
        Shadow.extract(context.getSystemService(AccessibilityManager::class.java))

    @Test
    fun key() {
        assertThat(preference.key).isEqualTo(Settings.Secure.ACCESSIBILITY_SHORTCUT_ON_LOCK_SCREEN)
    }

    @Test
    fun purpose() {
        assertThat(preference.purpose)
            .isEqualTo(R.string.a11y_volume_keys_shortcut_setting_on_lock_screen_purpose)
    }

    @Test
    fun title() {
        assertThat(preference.title)
            .isEqualTo(R.string.accessibility_shortcut_service_on_lock_screen_title)
    }

    @Test
    fun storage_dialogShown_defaultValueIsTrue() {
        setShortcutDialogShown(true)

        val store = preference.storage(context)

        assertThat(store).isInstanceOf(SettingsSecureStore::class.java)
        assertThat(store.getBoolean(preference.key)).isTrue()
    }

    @Test
    fun storage_dialogNotShown_defaultValueIsFalse() {
        setShortcutDialogShown(false)

        val store = preference.storage(context)

        assertThat(store).isInstanceOf(SettingsSecureStore::class.java)
        assertThat(store.getBoolean(preference.key)).isFalse()
    }

    @Test
    fun isEnabled_hasHardwareShortcutTargets_isTrue() {
        shadowA11yManager.setAccessibilityShortcutTargets(HARDWARE, listOf("foo"))

        assertThat(preference.isEnabled(context)).isTrue()
    }

    @Test
    fun isEnabled_noHardwareShortcutTargets_isFalse() {
        shadowA11yManager.setAccessibilityShortcutTargets(HARDWARE, emptyList())

        assertThat(preference.isEnabled(context)).isFalse()
    }

    @Test
    fun getSummary_enabled_returnsCorrectSummary() {
        shadowA11yManager.setAccessibilityShortcutTargets(HARDWARE, listOf("foo"))

        assertThat(preference.getSummary(context))
            .isEqualTo(context.getString(R.string.accessibility_shortcut_description))
    }

    @Test
    fun getSummary_disabled_returnsCorrectSummary() {
        shadowA11yManager.setAccessibilityShortcutTargets(HARDWARE, emptyList())

        assertThat(preference.getSummary(context))
            .isEqualTo(
                context.getString(
                    R.string.a11y_volume_keys_shortcut_unassigned_setting_unavailable_summary
                )
            )
    }

    @Test
    fun createWidget_returnsSwitchPreference() {
        val widget: Preference = preference.createAndBindWidget(context)

        assertThat(widget).isInstanceOf(SwitchPreferenceCompat::class.java)
    }

    @Test
    fun bindWidget_valueIsTrue_isChecked() {
        preference.storage(context).setBoolean(preference.key, true)
        val widget = preference.createAndBindWidget(context) as SwitchPreferenceCompat

        assertThat(widget.isChecked).isTrue()
    }

    @Test
    fun bindWidget_valueIsFalse_isNotChecked() {
        preference.storage(context).setBoolean(preference.key, false)
        val widget = preference.createAndBindWidget(context) as SwitchPreferenceCompat

        assertThat(widget.isChecked).isFalse()
    }

    private fun setShortcutDialogShown(shown: Boolean) {
        SettingsSecureStore.get(context)
            .setBoolean(Settings.Secure.ACCESSIBILITY_SHORTCUT_DIALOG_SHOWN, shown)
    }
}
