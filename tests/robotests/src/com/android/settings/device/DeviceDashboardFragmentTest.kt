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

package com.android.settings.device

import android.app.settings.SettingsEnums
import android.content.Context
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.testutils.shadow.ShadowDesktopSettingsUtils
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.androidx.fragment.FragmentController
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowDesktopSettingsUtils::class])
class DeviceDashboardFragmentTest {

    private lateinit var fragment: DeviceDashboardFragment
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        fragment = FragmentController.of(DeviceDashboardFragment()).create().visible().get()
    }

    @Test
    fun fragment_shouldBeCreated() {
        assertThat(fragment).isNotNull()
    }

    @Test
    fun preferenceScreen_shouldBeInflated() {
        assertThat(fragment.preferenceScreen).isNotNull()
        assertThat(fragment.preferenceScreen.title).isEqualTo(
            fragment.context?.getString(R.string.device_dashboard_title)
        )
    }

    @Test
    fun getMetricsCategory_shouldReturnCorrectEnum() {
        assertThat(fragment.getMetricsCategory()).isEqualTo(SettingsEnums.SETTINGS_DEVICE_CATEGORY)
    }

    @Test
    fun displayPreference_shouldBeConfiguredCorrectly() {
        val displayPreference = fragment.findPreference<Preference>("device_dashboard_display")
        assertThat(displayPreference).isNotNull()
        assertThat(displayPreference?.fragment).isEqualTo("com.android.settings.DisplaySettings")
        assertThat(displayPreference?.title).isEqualTo(fragment.context?.getString(R.string.display_settings))
        assertThat(displayPreference?.summary).isEqualTo(fragment.context?.getString(R.string.device_display_dashboard_summary))
        val icon = displayPreference?.icon
        assertThat(Shadows.shadowOf(icon).createdFromResId)
            .isEqualTo(R.drawable.ic_settings_tv_displays)
    }

    @Test
    fun soundPreference_shouldBeConfiguredCorrectly() {
        val soundPreference = fragment.findPreference<Preference>("device_dashboard_sound")
        assertThat(soundPreference).isNotNull()
        assertThat(soundPreference?.fragment).isEqualTo("com.android.settings.notification.SoundSettings")
        assertThat(soundPreference?.title).isEqualTo(fragment.context?.getString(R.string.sound_settings))
        assertThat(soundPreference?.summary).isEqualTo(fragment.context?.getString(R.string.device_sound_dashboard_summary))
        val icon = soundPreference?.icon
        assertThat(Shadows.shadowOf(icon).createdFromResId).isEqualTo(R.drawable.ic_settings_mic)
    }

    @Test
    fun keyboardPreference_shouldBeConfiguredCorrectly() {
        ShadowDesktopSettingsUtils.setShouldShow(true)
        ReflectionHelpers.callInstanceMethod<Unit>(fragment, "updatePreferenceStates")

        val keyboardPreference = fragment.findPreference<Preference>("keyboard_settings")
        assertThat(keyboardPreference).isNotNull()
        assertThat(keyboardPreference?.fragment).isEqualTo("com.android.settings.inputmethod.KeyboardSettings")
        assertThat(keyboardPreference?.title).isEqualTo(fragment.context?.getString(R.string.keyboard_settings))
        assertThat(keyboardPreference?.summary).isEqualTo(fragment.context?.getString(R.string.device_keyboard_settings_summary))
        val icon = keyboardPreference?.icon
        assertThat(Shadows.shadowOf(icon).createdFromResId).isEqualTo(R.drawable.ic_settings_keyboards)
    }

    @Test
    fun mousePreference_shouldBeConfiguredCorrectly() {
        val mousePreference = fragment.findPreference<Preference>("mouse_settings")
        assertThat(mousePreference).isNotNull()
        assertThat(mousePreference?.fragment).isEqualTo("com.android.settings.inputmethod.MouseSettingFragment")
        assertThat(mousePreference?.title).isEqualTo(fragment.context?.getString(R.string.mouse_settings))
        assertThat(mousePreference?.summary).isEqualTo(fragment.context?.getString(R.string.device_mouse_settings_summary))
        val icon = mousePreference?.icon
        assertThat(Shadows.shadowOf(icon).createdFromResId).isEqualTo(R.drawable.ic_settings_mouse)
    }

    @Test
    fun trackpadPreference_shouldBeConfiguredCorrectly() {
        val trackpadPreference = fragment.findPreference<Preference>("trackpad_settings")
        assertThat(trackpadPreference).isNotNull()
        assertThat(trackpadPreference?.fragment).isEqualTo("com.android.settings.inputmethod.TouchpadAndMouseSettings")
        assertThat(trackpadPreference?.title).isEqualTo(fragment.context?.getString(R.string.trackpad_settings))
        assertThat(trackpadPreference?.summary).isEqualTo(fragment.context?.getString(R.string.device_trackpad_settings_summary))
        val icon = trackpadPreference?.icon
        assertThat(Shadows.shadowOf(icon).createdFromResId).isEqualTo(R.drawable.ic_settings_touchpad_mouse)
    }

    @Test
    fun touchpadPreference_shouldBeConfiguredCorrectly() {
        val touchpadPreference = fragment.findPreference<Preference>("touchpad_settings")
        assertThat(touchpadPreference).isNotNull()
        assertThat(touchpadPreference?.fragment).isEqualTo("com.android.settings.inputmethod.TouchpadSettingFragment")
        assertThat(touchpadPreference?.title).isEqualTo(fragment.context?.getString(R.string.trackpad_settings))
        assertThat(touchpadPreference?.summary).isEqualTo(fragment.context?.getString(R.string.device_trackpad_settings_summary))
        val icon = touchpadPreference?.icon
        assertThat(Shadows.shadowOf(icon).createdFromResId).isEqualTo(R.drawable.ic_settings_touchpad_mouse)
    }

    @Test
    fun getXmlResourcesToIndex_enabled_returnsResId() {
        ShadowDesktopSettingsUtils.setShouldShow(true)

        val result = DeviceDashboardFragment.SEARCH_INDEX_DATA_PROVIDER.getXmlResourcesToIndex(context, true)

        assertThat(result).hasSize(1)
        assertThat(result[0].xmlResId).isEqualTo(R.xml.device_dashboard_fragment)
    }

    @Test
    fun getXmlResourcesToIndex_disabled_returnsEmpty() {
        ShadowDesktopSettingsUtils.setShouldShow(false)

        val result = DeviceDashboardFragment.SEARCH_INDEX_DATA_PROVIDER.getXmlResourcesToIndex(context, true)

        assertThat(result).isEmpty()
    }

    @Test
    fun isPageSearchEnabled_enabled_returnsTrue() {
        ShadowDesktopSettingsUtils.setShouldShow(true)

        val provider = DeviceDashboardFragment.SEARCH_INDEX_DATA_PROVIDER
        val result = ReflectionHelpers.callInstanceMethod<Boolean>(
            provider, "isPageSearchEnabled",
            ReflectionHelpers.ClassParameter(Context::class.java, context)
        )
        assertThat(result).isTrue()
    }

    @Test
    fun isPageSearchEnabled_disabled_returnsFalse() {
        ShadowDesktopSettingsUtils.setShouldShow(false)

        val provider = DeviceDashboardFragment.SEARCH_INDEX_DATA_PROVIDER
        val result = ReflectionHelpers.callInstanceMethod<Boolean>(
            provider, "isPageSearchEnabled",
            ReflectionHelpers.ClassParameter(Context::class.java, context)
        )
        assertThat(result).isFalse()
    }
}
