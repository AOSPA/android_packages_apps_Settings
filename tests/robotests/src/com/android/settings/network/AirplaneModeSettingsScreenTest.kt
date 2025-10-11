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

package com.android.settings.network

import android.app.settings.SettingsEnums.AIRPLANE_MODE_SETTINGS
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.CompanionDeviceManager
import android.content.Context
import android.os.PersistableBundle
import android.os.UserHandle
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings.Global
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.server.connectivity.Flags
import com.android.settings.R
import com.android.settings.Settings.AirplaneModeSettingsActivity
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.TestScope
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class AirplaneModeSettingsScreenTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val screen = AirplaneModeSettingsScreen(context)
    private val companionDeviceManager =
        shadowOf(context.getSystemService(CompanionDeviceManager::class.java))
    private val lifecycleContext =
        mock<PreferenceLifecycleContext> {
            on { requirePreference<Preference>(AirplaneModeSettingsScreen.KEY) } doReturn
                mock<Preference>()
        }

    @Test
    fun key() {
        assertThat(screen.key).isEqualTo(AirplaneModeSettingsScreen.KEY)
    }

    @Test
    fun getTitle_isAirplaneMode() {
        assertThat(screen.title).isEqualTo(R.string.airplane_mode)
    }

    @Test
    fun getHighlightMenuKey() {
        assertThat(screen.highlightMenuKey).isEqualTo(R.string.menu_key_network)
    }

    @Test
    fun getMetricsCategory() {
        assertThat(screen.metricsCategory).isEqualTo(AIRPLANE_MODE_SETTINGS)
    }

    @Test
    @EnableFlags(Flags.FLAG_SYNC_AIRPLANE_MODE_WITH_WATCHES)
    fun hasPairedWatch_isAvailableAndIndexable() {
        companionDeviceManager.addAssociation(
            AssociationInfo.Builder(1, UserHandle.myUserId(), context.packageName)
                .setDeviceProfile(AssociationRequest.DEVICE_PROFILE_WATCH)
                .setDisplayName("Smart Watch")
                .setMetadata(PersistableBundle())
                .build()
        )

        assertThat(screen.isAvailable(context)).isTrue()
        assertThat(screen.isIndexable(context)).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_SYNC_AIRPLANE_MODE_WITH_WATCHES)
    fun noPairedWatch_isNotAvailableOrIndexable() {
        assertThat(screen.isAvailable(context)).isFalse()
        assertThat(screen.isIndexable(context)).isFalse()
    }

    @Test
    @DisableFlags(Flags.FLAG_SYNC_AIRPLANE_MODE_WITH_WATCHES)
    fun flagDisabled_isNotAvailableOrIndexable() {
        companionDeviceManager.addAssociation(
            AssociationInfo.Builder(1, UserHandle.myUserId(), context.packageName)
                .setDeviceProfile(AssociationRequest.DEVICE_PROFILE_WATCH)
                .setDisplayName("Smart Watch")
                .setMetadata(PersistableBundle())
                .build()
        )

        assertThat(screen.isAvailable(context)).isFalse()
        assertThat(screen.isIndexable(context)).isFalse()
    }

    @Test
    fun getLaunchIntent_correctActivity() {
        val intent = screen.getLaunchIntent(context, null)

        assertThat(intent.component?.className)
            .isEqualTo(AirplaneModeSettingsActivity::class.java.name)
    }

    @Test
    fun getPreferenceHierarchy_returnsCorrectHierarchy() {
        val hierarchy = screen.getPreferenceHierarchy(context, TestScope())

        val keys = mutableListOf<String>()
        hierarchy.forEach { preference -> keys.add(preference.metadata.key) }

        assertThat(keys)
            .containsExactly(
                AirplaneModePreference.KEY,
                AirplaneModeSyncPreference.KEY,
                AirplaneModeSettingsFooter.KEY,
            )
            .inOrder()
    }

    @Test
    fun storage_returnsDataStore() {
        assertThat(screen.storage(context)).isNotNull()
    }

    @Test
    fun onCreate_isEntryPoint_notifiedForAirplaneModeChanges() {
        lifecycleContext.stub { on { preferenceScreenKey } doReturn NetworkDashboardScreen.KEY }

        screen.onCreate(lifecycleContext)
        shadowOf(context.contentResolver).getContentObservers(Global.getUriFor("")).forEach {
            it.onChange(true, Global.getUriFor(AirplaneModePreference.KEY))
        }

        verify(lifecycleContext).notifyPreferenceChange(AirplaneModeSettingsScreen.KEY)
    }

    @Test
    fun onCreate_isEntryPoint_notNotifiedForNonAirplaneModeChanges() {
        lifecycleContext.stub { on { preferenceScreenKey } doReturn NetworkDashboardScreen.KEY }

        screen.onCreate(lifecycleContext)
        shadowOf(context.contentResolver).getContentObservers(Global.getUriFor("")).forEach {
            it.onChange(true, Global.getUriFor("some_other_setting"))
        }

        verify(lifecycleContext, never()).notifyPreferenceChange(AirplaneModeSettingsScreen.KEY)
    }

    @Test
    fun onCreate_isNotEntryPoint_notNotifiedForAirplaneModeChanges() {
        lifecycleContext.stub { on { preferenceScreenKey } doReturn AirplaneModeSettingsScreen.KEY }

        screen.onCreate(lifecycleContext)
        shadowOf(context.contentResolver).getContentObservers(Global.getUriFor("")).forEach {
            it.onChange(true, Global.getUriFor(AirplaneModePreference.KEY))
        }

        verify(lifecycleContext, never()).notifyPreferenceChange(AirplaneModeSettingsScreen.KEY)
    }

    @Test
    fun onDestroy_isEntryPoint_observerRemoved() {
        lifecycleContext.stub { on { preferenceScreenKey } doReturn NetworkDashboardScreen.KEY }
        // Call onCreate to add the observer first
        screen.onCreate(lifecycleContext)

        screen.onDestroy(lifecycleContext)
        shadowOf(context.contentResolver).getContentObservers(Global.getUriFor("")).forEach {
            it.onChange(true, Global.getUriFor(AirplaneModePreference.KEY))
        }

        verify(lifecycleContext, never()).notifyPreferenceChange(AirplaneModeSettingsScreen.KEY)
    }

    @Test
    fun onDestroy_isNotEntryPoint_doesNothing() {
        lifecycleContext.stub { on { preferenceScreenKey } doReturn AirplaneModeSettingsScreen.KEY }
        // Call onCreate, which should do nothing
        screen.onCreate(lifecycleContext)

        // Call onDestroy, which should also do nothing and not crash
        screen.onDestroy(lifecycleContext)
    }

    @Test
    fun footer_key_returnsCorrectKey() {
        val footer = AirplaneModeSettingsFooter()

        assertThat(footer.key).isEqualTo(AirplaneModeSettingsFooter.KEY)
    }

    @Test
    fun footer_title_returnsCorrectTitle() {
        val footer = AirplaneModeSettingsFooter()

        assertThat(footer.title).isEqualTo(R.string.airplane_mode_sync_description)
    }
}
