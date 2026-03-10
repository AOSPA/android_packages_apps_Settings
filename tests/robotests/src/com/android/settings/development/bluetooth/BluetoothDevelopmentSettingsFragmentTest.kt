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

package com.android.settings.development.bluetooth

import android.app.Application
import android.content.Context
import android.os.PowerManager
import androidx.fragment.app.testing.FragmentScenario
import androidx.test.core.app.ApplicationProvider
import com.android.settingslib.development.DevelopmentSettingsEnabler
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class BluetoothDevelopmentSettingsFragmentTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun searchIndexProvider_isPageSearchEnabled_returnsTrueWhenDevelopmentSettingsEnabled() {
        DevelopmentSettingsEnabler.setDevelopmentSettingsEnabled(context, true)

        val nonIndexableKeys =
            BluetoothDevelopmentSettingsFragment.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(
                context
            )

        // The entire screen shouldn't be hidden by default just based on the master toggle here
        assertThat(nonIndexableKeys).doesNotContain("bluetooth_development_settings_screen")
    }

    @Test
    fun searchIndexProvider_isPageSearchEnabled_returnsFalseWhenDevelopmentSettingsDisabled() {
        DevelopmentSettingsEnabler.setDevelopmentSettingsEnabled(context, false)

        val nonIndexableKeys =
            BluetoothDevelopmentSettingsFragment.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(
                context
            )

        // The entire screen should be hidden
        assertThat(nonIndexableKeys).contains("bluetooth_development_settings_screen")
    }

    @Test
    fun searchIndexProvider_createPreferenceControllers_returnsNonEmptyList() {
        val controllers =
            BluetoothDevelopmentSettingsFragment.SEARCH_INDEX_DATA_PROVIDER
                .createPreferenceControllers(context)
        assertThat(controllers).isNotEmpty()
    }

    @Test
    fun onRebootDialogConfirmed_callsPowerManagerReboot() {
        val powerManager = mock(PowerManager::class.java)
        val shadowApplication = Shadows.shadowOf(context)
        shadowApplication.setSystemService(Context.POWER_SERVICE, powerManager)

        FragmentScenario.launch(BluetoothDevelopmentSettingsFragment::class.java).onFragment {
            attachedFragment ->
            attachedFragment.onRebootDialogConfirmed()

            verify(powerManager).reboot(null)
        }
    }
}
