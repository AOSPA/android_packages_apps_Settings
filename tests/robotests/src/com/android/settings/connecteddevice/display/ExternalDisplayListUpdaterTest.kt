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

package com.android.settings.connecteddevice.display

import android.content.Context
import android.os.Bundle
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.connecteddevice.DevicePreferenceCallback
import com.android.settings.flags.Flags
import com.android.settingslib.RestrictedLockUtils
import com.android.settingslib.RestrictedPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

@RunWith(AndroidJUnit4::class)
class ExternalDisplayListUpdaterTest : ExternalDisplayTestBase() {

    @Mock private lateinit var callback: DevicePreferenceCallback
    private lateinit var updater: TestExternalDisplayListUpdater

    @Before
    override fun setUp() {
        super.setUp()
        updater = TestExternalDisplayListUpdater(callback, /* metricsCategory= */ 0)
        updater.initPreference(mContext, mMockedInjector)
    }

    @Test
    fun update_addsPreferenceForConnectedDisplay() {
        updateDisplaysAndTopology(listOf(createExternalDisplay(DisplayIsEnabled.YES)))

        // Trigger update
        updater.refreshPreference()
        mHandler.flush()

        val prefCaptor = ArgumentCaptor.forClass(Preference::class.java)
        verify(callback).onDeviceAdded(prefCaptor.capture())

        val addedPref = prefCaptor.value
        assertThat(addedPref.title).isEqualTo("HDMI")
        assertThat(addedPref.key).isEqualTo("external_display_1")
        assertThat(addedPref.summary).isNull()
    }

    @Test
    fun update_doesNotAddPreferenceForDisabledDisplay() {
        updateDisplaysAndTopology(listOf(createExternalDisplay(DisplayIsEnabled.NO)))

        updater.refreshPreference()
        mHandler.flush()

        verify(callback, never()).onDeviceAdded(any())
    }

    @Test
    fun update_removesPreferenceWhenDisplayDisabled() {
        updateDisplaysAndTopology(listOf(createExternalDisplay(DisplayIsEnabled.YES)))
        updater.refreshPreference()
        mHandler.flush()

        val prefCaptor = ArgumentCaptor.forClass(Preference::class.java)
        verify(callback).onDeviceAdded(prefCaptor.capture())
        val addedPref = prefCaptor.value
        clearInvocations(callback)

        // Change display state
        updateDisplaysAndTopology(listOf(createExternalDisplay(DisplayIsEnabled.NO)))
        updater.refreshPreference()
        mHandler.flush()

        verify(callback).onDeviceRemoved(addedPref)
    }

    @Test
    fun update_removesPreferenceForDisconnectedDisplay() {
        updateDisplaysAndTopology(listOf(createExternalDisplay(DisplayIsEnabled.YES)))

        // First add
        updater.refreshPreference()
        mHandler.flush()

        val prefCaptor = ArgumentCaptor.forClass(Preference::class.java)
        verify(callback).onDeviceAdded(prefCaptor.capture())
        val addedPref = prefCaptor.value

        // Now remove display from injector
        updateDisplaysAndTopology(emptyList())

        // Trigger update again
        updater.refreshPreference()
        mHandler.flush()

        verify(callback).onDeviceRemoved(addedPref)
    }

    @Test
    fun preferenceClick_launchesSettings() {
        val display = createExternalDisplay(DisplayIsEnabled.YES)
        updateDisplaysAndTopology(listOf(display))
        updater.refreshPreference()
        mHandler.flush()

        val prefCaptor = ArgumentCaptor.forClass(Preference::class.java)
        verify(callback).onDeviceAdded(prefCaptor.capture())
        val addedPref = prefCaptor.value

        addedPref.performClick()

        assertThat(updater.launchSettingsCalled).isTrue()
        assertThat(updater.launchSettingsArgs).isNotNull()
        assertThat(updater.launchSettingsArgs?.getInt("display_id")).isEqualTo(display.id)
    }

    @Test
    fun update_summaryNull() {
        updateDisplaysAndTopology(listOf(createExternalDisplay(DisplayIsEnabled.YES)))

        updater.refreshPreference()
        mHandler.flush()

        val prefCaptor = ArgumentCaptor.forClass(Preference::class.java)
        verify(callback).onDeviceAdded(prefCaptor.capture())
        assertThat(prefCaptor.value.summary).isNull()
    }

    class TestExternalDisplayListUpdater(
        callback: DevicePreferenceCallback,
        metricsCategory: Int
    ) : ExternalDisplayListUpdater(callback, metricsCategory) {

        var launchSettingsCalled = false
        var launchSettingsArgs: Bundle? = null

        override fun launchSettings(context: Context, args: Bundle?) {
            launchSettingsCalled = true
            launchSettingsArgs = args
        }
    }
}
