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

package com.android.settings.accessibility.autoclick.ui

import android.app.settings.SettingsEnums
import android.content.Context
import android.provider.Settings
import android.view.InputDevice.SOURCE_MOUSE
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.accessibility.AutoclickUtils
import com.android.settings.accessibility.Flags
import com.android.settings.accessibility.ToggleAutoclickPreferenceFragment
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils.shadow.ShadowInputDevice
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/** Tests for [AutoclickScreen]. */
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowInputDevice::class])
class AutoclickScreenTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()
    private val appContext: Context = ApplicationProvider.getApplicationContext()
    private val secureStore by lazy { SettingsSecureStore.get(appContext) }
    private lateinit var screen: AutoclickScreen

    @Before
    fun setUp() {
        screen = AutoclickScreen()
        ShadowInputDevice.reset()
    }

    @After
    fun cleanUp() {
        ShadowInputDevice.reset()
    }

    @Test
    fun key_isAutoclickScreenKey() {
        assertThat(screen.key).isEqualTo(AutoclickScreen.KEY)
    }

    @Test
    fun title_isAutoclickPreferenceTitle() {
        assertThat(appContext.getString(screen.title))
            .isEqualTo(appContext.getString(R.string.accessibility_autoclick_preference_title))
    }

    @Test
    fun keywords_areAutoClick() {
        assertThat(appContext.getString(screen.keywords))
            .isEqualTo(appContext.getString(R.string.keywords_auto_click))
    }

    @Test
    fun metricsCategory_isAccessibilityToggleAutoclick() {
        assertThat(screen.getMetricsCategory())
            .isEqualTo(SettingsEnums.ACCESSIBILITY_TOGGLE_AUTOCLICK)
    }

    @Test
    fun highlightMenuKey_isAccessibility() {
        assertThat(screen.highlightMenuKey).isEqualTo(R.string.menu_key_accessibility)
    }

    @Test
    fun isFlagEnabled_returnsCatalystAutoclickScreenFlag() {
        assertThat(screen.isFlagEnabled(appContext)).isEqualTo(Flags.catalystAutoclickScreen())
    }

    @Test
    fun fragmentClass_isToggleAutoclickPreferenceFragment() {
        assertThat(screen.fragmentClass()).isEqualTo(ToggleAutoclickPreferenceFragment::class.java)
    }

    @Test
    fun isIndexable_returnsTrue() {
        assertThat(screen.indexable).isTrue()
    }

    @Test
    fun isAvailable_noInputDevices_returnsFalse() {
        assertThat(screen.isAvailable(appContext)).isFalse()
    }

    @Test
    fun isAvailable_hasMouse_returnsTrue() {
        addInputDevice(deviceId = 1, SOURCE_MOUSE)

        assertThat(screen.isAvailable(appContext)).isTrue()
    }

    @Test
    fun getSummary_autoclickDisabled_returnsDisabledString() {
        secureStore.setBoolean(Settings.Secure.ACCESSIBILITY_AUTOCLICK_ENABLED, false)

        assertThat(screen.getSummary(appContext))
            .isEqualTo(appContext.getString(R.string.autoclick_disabled))
    }

    @Test
    fun getSummary_autoclickEnabled_returnsSummaryWithDelayedTime() {
        val delayInMs = 600
        secureStore.apply {
            setBoolean(Settings.Secure.ACCESSIBILITY_AUTOCLICK_ENABLED, true)
            setInt(Settings.Secure.ACCESSIBILITY_AUTOCLICK_DELAY, delayInMs)
        }

        val expectedSummary =
            AutoclickUtils.getAutoclickDelaySummary(
                appContext,
                R.string.accessibility_autoclick_delay_unit_second,
                delayInMs,
            )
        assertThat(screen.getSummary(appContext).toString()).isEqualTo(expectedSummary.toString())
    }

    @Test
    fun onCreate_shownAsEntrypoint_updatesOnSettingChange() {
        val mockLifecycleContext =
            mock<PreferenceLifecycleContext> {
                on { applicationContext } doReturn appContext
                on { preferenceScreenKey } doReturn "other_screen_key"
            }

        screen.onCreate(mockLifecycleContext)
        secureStore.setBoolean(Settings.Secure.ACCESSIBILITY_AUTOCLICK_ENABLED, true)
        ShadowLooper.idleMainLooper()

        verify(mockLifecycleContext).notifyPreferenceChange(screen.bindingKey)
    }

    @Test
    fun onDestroy_removesObserver() {
        val mockLifecycleContext =
            mock<PreferenceLifecycleContext> {
                on { applicationContext } doReturn appContext
                on { preferenceScreenKey } doReturn "other_screen_key"
            }
        screen.onCreate(mockLifecycleContext)

        screen.onDestroy(mockLifecycleContext)
        secureStore.setBoolean(Settings.Secure.ACCESSIBILITY_AUTOCLICK_ENABLED, true)
        ShadowLooper.idleMainLooper()

        verify(mockLifecycleContext, never()).notifyPreferenceChange(any())
    }

    private fun addInputDevice(deviceId: Int, source: Int) {
        ShadowInputDevice.addDevice(
            deviceId,
            ShadowInputDevice.makeInputDevicebyIdWithSources(deviceId, source),
        )
    }
}
