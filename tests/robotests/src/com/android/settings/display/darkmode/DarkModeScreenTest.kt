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

package com.android.settings.display.darkmode

import android.app.settings.SettingsEnums
import android.content.ContextWrapper
import android.os.PowerManager
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.Settings
import com.android.settings.SettingsActivity.EXTRA_FRAGMENT_ARG_KEY
import com.android.settings.accessibility.Flags
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.android.settingslib.metadata.PreferenceMetadata
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

class DarkModeScreenTest : SettingsCatalystTestCase() {
    private val mockPowerManager = mock<PowerManager>()
    private val context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getSystemService(name: String): Any? =
                when (name) {
                    getSystemServiceName(PowerManager::class.java) -> mockPowerManager
                    else -> super.getSystemService(name)
                }
        }

    override val preferenceScreenCreator = DarkModeScreen(context)
    override val flagName: String
        get() = Flags.FLAG_CATALYST_DARK_UI_MODE

    override fun migration() {}

    @Test
    fun key() {
        assertThat(preferenceScreenCreator.key).isEqualTo("dark_ui_mode")
    }

    @Test
    fun getTitle() {
        assertThat(preferenceScreenCreator.title).isEqualTo(R.string.dark_ui_mode)
    }

    @Test
    fun getKeywords() {
        assertThat(preferenceScreenCreator.keywords).isEqualTo(R.string.keywords_dark_ui_mode)
    }

    @Test
    fun getHighlightMenuKey() {
        assertThat(preferenceScreenCreator.highlightMenuKey).isEqualTo(R.string.menu_key_display)
    }

    @Test
    fun getMetricsCategory() {
        assertThat(preferenceScreenCreator.metricsCategory)
            .isEqualTo(SettingsEnums.DARK_UI_SETTINGS)
    }

    @Test
    fun isEnabled_isTrue() {
        mockPowerManager.stub { on { isPowerSaveMode } doReturn false }

        assertThat(preferenceScreenCreator.isEnabled(context)).isTrue()
    }

    @Test
    fun isEnabled_isFalse() {
        mockPowerManager.stub { on { isPowerSaveMode } doReturn true }

        assertThat(preferenceScreenCreator.isEnabled(context)).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_DARK_UI_MODE)
    fun isIndexable_flagOn_isTrue() {
        mockPowerManager.stub { on { isPowerSaveMode } doReturn false }

        assertThat(preferenceScreenCreator.isIndexable(context)).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_DARK_UI_MODE)
    fun isIndexable_flagOn_isFalse() {
        mockPowerManager.stub { on { isPowerSaveMode } doReturn true }

        assertThat(preferenceScreenCreator.isIndexable(context)).isFalse()
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_DARK_UI_MODE)
    fun isIndexable_flagOFF_isFalse() {
        mockPowerManager.stub { on { isPowerSaveMode } doReturn false }

        assertThat(preferenceScreenCreator.isIndexable(context)).isFalse()
    }

    @Test
    fun hasCompleteHierarchy_isFalse() {
        assertThat(preferenceScreenCreator.hasCompleteHierarchy()).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_DARK_UI_MODE)
    fun isFlagEnabled_isTrue() {
        assertThat(preferenceScreenCreator.isFlagEnabled(context)).isTrue()
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_DARK_UI_MODE)
    fun isFlagEnabled_isFalse() {
        assertThat(preferenceScreenCreator.isFlagEnabled(context)).isFalse()
    }

    @Test
    fun getLaunchIntent_noMetadata_correctActivity() {
        val underTest = preferenceScreenCreator.getLaunchIntent(context, null)!!

        assertThat(underTest.getComponent()?.getClassName())
            .isEqualTo(Settings.DarkThemeSettingsActivity::class.java.getName())
        assertThat(underTest.hasExtra(EXTRA_FRAGMENT_ARG_KEY)).isFalse()
    }

    @Test
    fun getLaunchIntent_metadata_correctActivityWithExtraKey() {
        val underTest =
            preferenceScreenCreator.getLaunchIntent(context, TestMetadata("preference_key"))!!

        assertThat(underTest.hasExtra(EXTRA_FRAGMENT_ARG_KEY)).isTrue()
        assertThat(underTest.getStringExtra(EXTRA_FRAGMENT_ARG_KEY)).isEqualTo("preference_key")
    }
}

private data class TestMetadata(override val key: String) : PreferenceMetadata
