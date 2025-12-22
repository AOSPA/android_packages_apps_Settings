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

package com.android.settings.accessibility.buttonshortcutsetting.ui

import android.content.Context
import android.provider.Settings.Secure
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.IllustrationPreference as IllustrationWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper

/** Test for [ButtonShortcutIllustrationPreference] */
@RunWith(RobolectricTestParameterInjector::class)
class ButtonShortcutIllustrationPreferenceTest {

    @get:Rule val settingsStoreRule = SettingsStoreRule()
    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()
    @Mock lateinit var mockPrefLifecycleContext: PreferenceLifecycleContext
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preference = ButtonShortcutIllustrationPreference()

    @Test
    fun key() {
        assertThat(preference.key).isEqualTo(ButtonShortcutIllustrationPreference.KEY)
    }

    @Test
    fun isIndexable() {
        assertThat(preference.indexable).isFalse()
    }

    @Test
    fun createWidget_returnsIllustrationWidgetWithSelectableFalse() {
        val widget = preference.createWidget(context)
        assertThat(widget).isInstanceOf(IllustrationWidget::class.java)
        assertThat(widget.isSelectable).isFalse()
    }

    @Test
    fun purpose_returnsCorrectResourceId() {
        assertThat(preference.purpose).isEqualTo(R.string.animated_image_purpose)
    }

    @Test
    fun bind_useNavButtonShortcut_showsNavButtonShortcutPreview() {
        setButtonMode(useNavButton = true)

        val widget = preference.createAndBindWidget<IllustrationWidget>(context)

        val shadowDrawable = shadowOf(widget.imageDrawable)
        assertThat(shadowDrawable.createdFromResId)
            .isEqualTo(R.drawable.accessibility_shortcut_type_navbar_preview)
    }

    @Test
    fun bind_useLargeFloatingButton_showLargeButtonPreview() {
        setButtonMode(useNavButton = false)
        setFloatingButtonSize(large = true)

        val widget = preference.createAndBindWidget<IllustrationWidget>(context)

        val actualDrawable = widget.imageDrawable
        val shadowDrawable = shadowOf(actualDrawable)
        assertThat(shadowDrawable.createdFromResId)
            .isEqualTo(R.drawable.accessibility_shortcut_type_fab_size_large_preview)
    }

    @Test
    fun bind_useSmallFloatingButton_showSmallButtonPreview() {
        setButtonMode(useNavButton = false)
        setFloatingButtonSize(large = false)

        val widget = preference.createAndBindWidget<IllustrationWidget>(context)

        val actualDrawable = widget.imageDrawable
        val shadowDrawable = shadowOf(actualDrawable)
        assertThat(shadowDrawable.createdFromResId)
            .isEqualTo(R.drawable.accessibility_shortcut_type_fab_size_small_preview)
    }

    @Test
    fun onCreate_buttonModeSettingChanged_callsNotifyPreferenceChange() {
        setButtonMode(useNavButton = false)

        preference.onCreate(mockPrefLifecycleContext)
        setButtonMode(useNavButton = true)

        verify(mockPrefLifecycleContext).notifyPreferenceChange(preference.key)
    }

    @Test
    fun onDestroy_buttonModeSettingChanged_shouldNotCallNotifyPreferenceChange() {
        setButtonMode(useNavButton = false)

        preference.run {
            onCreate(mockPrefLifecycleContext)
            onDestroy(mockPrefLifecycleContext)
        }
        setButtonMode(useNavButton = true)

        verify(mockPrefLifecycleContext, never()).notifyPreferenceChange(preference.key)
    }

    @Test
    fun onCreate_menuSizeSettingChanged_callsNotifyPreferenceChange() {
        setFloatingButtonSize(large = true)

        preference.onCreate(mockPrefLifecycleContext)
        setFloatingButtonSize(large = false)

        verify(mockPrefLifecycleContext).notifyPreferenceChange(preference.key)
    }

    @Test
    fun onDestroy_menuSizeSettingChanged_shouldNotCallNotifyPreferenceChange() {
        setFloatingButtonSize(large = true)

        preference.run {
            onCreate(mockPrefLifecycleContext)
            onDestroy(mockPrefLifecycleContext)
        }
        setFloatingButtonSize(large = false)

        verify(mockPrefLifecycleContext, never()).notifyPreferenceChange(preference.key)
    }

    @Test
    fun onCreate_menuOpacitySettingChanged_callsNotifyPreferenceChange() {
        setFloatingButtonOpacity(0.2f)

        preference.onCreate(mockPrefLifecycleContext)
        setFloatingButtonOpacity(0.5f)

        verify(mockPrefLifecycleContext).notifyPreferenceChange(preference.key)
    }

    @Test
    fun onDestroy_menuOpacitySettingChanged_shouldNotCallNotifyPreferenceChange() {
        setFloatingButtonOpacity(0.2f)

        preference.run {
            onCreate(mockPrefLifecycleContext)
            onDestroy(mockPrefLifecycleContext)
        }
        setFloatingButtonOpacity(0.5f)

        verify(mockPrefLifecycleContext, never()).notifyPreferenceChange(preference.key)
    }

    private fun setButtonMode(useNavButton: Boolean) {
        SettingsSecureStore.get(context)
            .setInt(
                Secure.ACCESSIBILITY_BUTTON_MODE,
                if (useNavButton) Secure.ACCESSIBILITY_BUTTON_MODE_NAVIGATION_BAR
                else Secure.ACCESSIBILITY_BUTTON_MODE_FLOATING_MENU,
            )
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    }

    private fun setFloatingButtonSize(large: Boolean) {
        SettingsSecureStore.get(context)
            .setInt(Secure.ACCESSIBILITY_FLOATING_MENU_SIZE, if (large) 1 else 0)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    }

    private fun setFloatingButtonOpacity(opacity: Float) {
        SettingsSecureStore.get(context)
            .setFloat(Secure.ACCESSIBILITY_FLOATING_MENU_OPACITY, opacity)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    }
}
