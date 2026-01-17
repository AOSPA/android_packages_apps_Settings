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

package com.android.settings.accessibility.captionpreferences.ui

import android.app.settings.SettingsEnums
import android.provider.Settings
import android.provider.Settings.ACTION_CAPTIONING_SETTINGS
import com.android.settings.R
import com.android.settings.SettingsActivity.EXTRA_FRAGMENT_ARG_KEY
import com.android.settings.accessibility.CaptioningPropertiesFragment
import com.android.settings.accessibility.Flags
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceMetadata
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.shadows.ShadowLooper

/** Tests for [CaptioningPropertiesScreen] */
class CaptioningPropertiesScreenTest : SettingsCatalystTestCase() {

    @get:Rule val settingsStoreRule = SettingsStoreRule()
    private lateinit var settingsSecureStore: SettingsSecureStore
    override val flagName: String
        get() = Flags.FLAG_CATALYST_CAPTION_PREFERENCES_SCREEN

    override val preferenceScreenCreator = CaptioningPropertiesScreen()

    @Before
    fun setUp() {
        settingsSecureStore = SettingsSecureStore.get(appContext)
        setCaptioningEnabled(false)
    }

    @Test
    fun key_returnsCorrectValue() {
        assertThat(preferenceScreenCreator.key).isEqualTo(CaptioningPropertiesScreen.KEY)
    }

    @Test
    fun title_returnsCorrectValue() {
        assertThat(preferenceScreenCreator.title).isEqualTo(R.string.accessibility_captioning_title)
    }

    @Test
    fun icon_returnsCorrectValue() {
        assertThat(preferenceScreenCreator.icon).isEqualTo(R.drawable.ic_captioning)
    }

    @Test
    fun purpose_returnsCorrectValue() {
        assertThat(preferenceScreenCreator.purpose)
            .isEqualTo(R.string.caption_preferences_screen_purpose)
    }

    @Test
    fun highlightMenuKey_returnsCorrectValue() {
        assertThat(preferenceScreenCreator.highlightMenuKey)
            .isEqualTo(R.string.menu_key_accessibility)
    }

    @Test
    fun getMetricsCategory_returnsCorrectValue() {
        assertThat(preferenceScreenCreator.getMetricsCategory())
            .isEqualTo(SettingsEnums.ACCESSIBILITY_CAPTION_PROPERTIES)
    }

    @Test
    fun fragmentClass_returnsCorrectValue() {
        assertThat(preferenceScreenCreator.fragmentClass())
            .isEqualTo(CaptioningPropertiesFragment::class.java)
    }

    @Test
    fun indexable_returnsTrue() {
        assertThat(preferenceScreenCreator.indexable).isTrue()
    }

    @Test
    fun getSummary_whenCaptioningEnabled_returnsEnabledSummary() {
        setCaptioningEnabled(true)

        val summary = preferenceScreenCreator.getSummary(appContext)

        assertThat(summary).isEqualTo(appContext.getString(R.string.show_captions_enabled))
    }

    @Test
    fun getSummary_whenCaptioningDisabled_returnsDisabledSummary() {
        setCaptioningEnabled(false)

        val summary = preferenceScreenCreator.getSummary(appContext)

        assertThat(summary).isEqualTo(appContext.getString(R.string.show_captions_disabled))
    }

    @Test
    fun onCreate_asEntryPoint_settingChanges_notifiesPreferenceChange() {
        val mockLifecycleContext =
            mock<PreferenceLifecycleContext> {
                on { preferenceScreenKey } doReturn "some_other_key"
            }

        preferenceScreenCreator.onCreate(mockLifecycleContext)
        setCaptioningEnabled(true)

        verify(mockLifecycleContext).notifyPreferenceChange(preferenceScreenCreator.bindingKey)
    }

    @Test
    fun onCreate_asContainer_settingChanges_doesNotNotifyPreferenceChange() {
        val mockLifecycleContext =
            mock<PreferenceLifecycleContext> {
                on { preferenceScreenKey } doReturn preferenceScreenCreator.key
            }

        preferenceScreenCreator.onCreate(mockLifecycleContext)
        setCaptioningEnabled(true)

        verify(mockLifecycleContext, never()).notifyPreferenceChange(any())
    }

    @Test
    fun onDestroy_removesObserver() {
        val mockLifecycleContext =
            mock<PreferenceLifecycleContext> {
                on { preferenceScreenKey } doReturn "some_other_key"
            }
        preferenceScreenCreator.onCreate(mockLifecycleContext)

        // Change setting once to confirm observer is active.
        setCaptioningEnabled(true)
        verify(mockLifecycleContext, times(1))
            .notifyPreferenceChange(preferenceScreenCreator.bindingKey)

        preferenceScreenCreator.onDestroy(mockLifecycleContext)
        clearInvocations(mockLifecycleContext)

        // Change setting again. Observer should be removed, so no notification.
        setCaptioningEnabled(false)

        verify(mockLifecycleContext, never())
            .notifyPreferenceChange(preferenceScreenCreator.bindingKey)
    }

    @Test
    fun getLaunchIntent_returnCaptioningSettingsIntent() {
        val prefKey = "fakePrefKey"
        val mockPrefMetadata = mock<PreferenceMetadata> { on { key } doReturn prefKey }
        val launchIntent = preferenceScreenCreator.getLaunchIntent(appContext, mockPrefMetadata)

        assertThat(launchIntent).isNotNull()
        assertThat(launchIntent.action).isEqualTo(ACTION_CAPTIONING_SETTINGS)
        assertThat(launchIntent.getStringExtra(EXTRA_FRAGMENT_ARG_KEY)).isEqualTo(prefKey)
    }

    override fun migration() {
        // Because we're planning to do full migration (not a hybrid migration),
        // temporarily disable the migration test until the full migration is done
    }

    private fun setCaptioningEnabled(enabled: Boolean) {
        settingsSecureStore.setBoolean(Settings.Secure.ACCESSIBILITY_CAPTIONING_ENABLED, enabled)

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    }
}
