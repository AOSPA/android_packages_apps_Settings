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

package com.android.settings.display

import android.content.Context
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import androidx.test.core.app.ApplicationProvider
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.connecteddevice.display.ConnectedDisplayInjector
import com.android.settings.connecteddevice.display.ExternalDisplayListUpdater
import com.android.settings.core.BasePreferenceController
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExternalDisplayPreferenceControllerTest {

    @Mock private lateinit var updater: ExternalDisplayListUpdater

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private lateinit var preferenceManager: PreferenceManager

    private lateinit var preference: Preference
    private lateinit var preference2: Preference

    private lateinit var screen: PreferenceScreen
    private lateinit var context: Context
    private lateinit var controller: ExternalDisplayPreferenceController

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()

        screen = spy(PreferenceScreen(context, null))
        `when`(screen.preferenceManager).thenReturn(preferenceManager)

        controller = ExternalDisplayPreferenceController(context, updater, /* lifecycle= */ null)
        preference = Preference(context)
        preference.key = "pref_key"
        preference2 = Preference(context)
        preference2.key = "pref_key_2"
    }

    @After
    fun tearDown() {
        org.mockito.Mockito.framework().clearInlineMocks()
    }

    @Test
    fun getAvailabilityStatus_returnsAvailable() {
        assertThat(controller.getAvailabilityStatus())
            .isEqualTo(BasePreferenceController.AVAILABLE)
    }

    @Test
    fun onStart_shouldRegisterCallback() {
        controller.onStart(mock(androidx.lifecycle.LifecycleOwner::class.java))
        verify(updater).registerCallback()
        verify(updater).refreshPreference()
    }

    @Test
    fun onStop_shouldUnregisterCallback() {
        controller.onStop(mock(androidx.lifecycle.LifecycleOwner::class.java))
        verify(updater).unregisterCallback()
    }

    @Test
    fun displayPreference_shouldInitPreference() {
        controller.displayPreference(screen)
        verify(updater)
            .initPreference(
                safeEq(context),
                safeAny(ConnectedDisplayInjector::class.java, ConnectedDisplayInjector(context))
            )
    }

    @Test
    fun onDeviceAdded_shouldAddPreferenceCategoryAndPreference() {
        controller.displayPreference(screen)

        controller.onDeviceAdded(preference)

        val captor = ArgumentCaptor.forClass(Preference::class.java)
        verify(screen).addPreference(captor.capture())
        val addedPref = captor.value
        assertThat(addedPref).isInstanceOf(PreferenceCategory::class.java)
        val category = addedPref as PreferenceCategory
        assertThat(category.preferenceCount).isEqualTo(1)
        assertThat(category.getPreference(0)).isEqualTo(preference)
    }

    @Test
    fun onDeviceAdded_shouldScrollToPreferenceCategory() {
        val fragment = mock(SettingsPreferenceFragment::class.java)
        controller.setFragment(fragment)
        controller.displayPreference(screen)

        controller.onDeviceAdded(preference)

        val captor = ArgumentCaptor.forClass(Preference::class.java)
        verify(screen).addPreference(captor.capture())
        val category = captor.value as PreferenceCategory
        verify(fragment).scrollToPreference(category)
    }

    @Test
    fun onDeviceAdded_secondDevice_shouldAddToSameCategory() {
        controller.displayPreference(screen)

        // Add first device
        controller.onDeviceAdded(preference)

        val captor = ArgumentCaptor.forClass(Preference::class.java)
        verify(screen).addPreference(captor.capture())
        val category = captor.value as PreferenceCategory

        // Mock screen to return the existing category
        `when`(
                screen.findPreference<PreferenceCategory>(
                    ExternalDisplayPreferenceController.PREF_KEY_EXTERNAL_DISPLAY_CATEGORY
                )
            )
            .thenReturn(category)

        // Add second device
        controller.onDeviceAdded(preference2)

        assertThat(category.preferenceCount).isEqualTo(2)
        assertThat(category.getPreference(0)).isAnyOf(preference, preference2)
        assertThat(category.getPreference(1)).isAnyOf(preference, preference2)
        assertThat(category.getPreference(0)).isNotEqualTo(category.getPreference(1))
    }

    @Test
    fun onDeviceRemoved_shouldRemovePreferenceFromCategory() {
        val mockCategory = mock(PreferenceCategory::class.java)
        `when`(
                screen.findPreference<PreferenceCategory>(
                    ExternalDisplayPreferenceController.PREF_KEY_EXTERNAL_DISPLAY_CATEGORY
                )
            )
            .thenReturn(mockCategory)
        `when`(mockCategory.preferenceCount).thenReturn(1)

        controller.displayPreference(screen)
        controller.onDeviceRemoved(preference)

        verify(mockCategory).removePreference(preference)
        verify(screen, never()).removePreference(mockCategory)
    }

    @Test
    fun onDeviceRemoved_shouldRemoveCategoryIfEmpty() {
        val mockCategory = mock(PreferenceCategory::class.java)
        `when`(
                screen.findPreference<PreferenceCategory>(
                    ExternalDisplayPreferenceController.PREF_KEY_EXTERNAL_DISPLAY_CATEGORY
                )
            )
            .thenReturn(mockCategory)
        `when`(mockCategory.preferenceCount).thenReturn(0)

        controller.displayPreference(screen)
        controller.onDeviceRemoved(preference)

        verify(mockCategory).removePreference(preference)
        verify(screen).removePreference(mockCategory)
    }

    @Test
    fun onDeviceRemoved_oneOfTwoDevices_shouldNotRemoveCategory() {
        val mockCategory = mock(PreferenceCategory::class.java)
        `when`(
                screen.findPreference<PreferenceCategory>(
                    ExternalDisplayPreferenceController.PREF_KEY_EXTERNAL_DISPLAY_CATEGORY
                )
            )
            .thenReturn(mockCategory)

        // Simulate category having 1 preference remaining after removal
        `when`(mockCategory.preferenceCount).thenReturn(1)

        controller.displayPreference(screen)
        controller.onDeviceRemoved(preference)

        verify(mockCategory).removePreference(preference)
        verify(screen, never()).removePreference(mockCategory)
    }

    private fun <T> safeEq(value: T): T {
        ArgumentMatchers.eq(value)
        return value
    }

    private fun <T> safeAny(type: Class<T>, default: T): T {
        ArgumentMatchers.any(type)
        return default
    }
}
