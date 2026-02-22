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

package com.android.settings.inputmethod

import android.content.Context
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import android.view.inputmethod.Flags
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import androidx.test.core.app.ApplicationProvider
import com.android.settings.inputmethod.ImeSwitcherButtonInNavBarPreferenceController.Companion.SETTING_VALUE_OFF
import com.android.settings.inputmethod.ImeSwitcherButtonInNavBarPreferenceController.Companion.SETTING_VALUE_ON
import com.android.settings.testutils.shadow.ShadowSecureSettings
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowSecureSettings::class])
class ImeSwitcherButtonInNavBarPreferenceControllerTest {

    @get:Rule val mMockitoRule: MockitoRule = MockitoJUnit.rule()

    @get:Rule val mSetFlagsRule = SetFlagsRule()

    private lateinit var mContext: Context
    private lateinit var mController: ImeSwitcherButtonInNavBarPreferenceController

    @Before
    fun setUp() {
        mContext = ApplicationProvider.getApplicationContext()
        mController = ImeSwitcherButtonInNavBarPreferenceController(mContext, PREF_KEY)
    }

    @Test
    fun isChecked_default_returnsTrue() {
        assertThat(mController.isChecked).isTrue()
    }

    @Test
    fun isChecked_settingOff_returnsFalse() {
        Settings.Secure.putInt(
            mContext.contentResolver,
            Settings.Secure.IME_SWITCHER_BUTTON_IN_NAVBAR_ENABLED,
            SETTING_VALUE_OFF,
        )

        assertThat(mController.isChecked).isFalse()
    }

    @Test
    fun isChecked_settingOn_returnsTrue() {
        Settings.Secure.putInt(
            mContext.contentResolver,
            Settings.Secure.IME_SWITCHER_BUTTON_IN_NAVBAR_ENABLED,
            SETTING_VALUE_ON,
        )

        assertThat(mController.isChecked).isTrue()
    }

    @Test
    fun setChecked_true_updatesSetting() {
        mController.isChecked = true

        assertThat(
                Settings.Secure.getInt(
                    mContext.contentResolver,
                    Settings.Secure.IME_SWITCHER_BUTTON_IN_NAVBAR_ENABLED,
                    SETTING_VALUE_OFF,
                )
            )
            .isEqualTo(SETTING_VALUE_ON)
    }

    @Test
    fun setChecked_false_updatesSetting() {
        mController.isChecked = false

        assertThat(
                Settings.Secure.getInt(
                    mContext.contentResolver,
                    Settings.Secure.IME_SWITCHER_BUTTON_IN_NAVBAR_ENABLED,
                    SETTING_VALUE_ON,
                )
            )
            .isEqualTo(SETTING_VALUE_OFF)
    }

    @Test
    @EnableFlags(Flags.FLAG_IME_SWITCHER_BUTTON_IN_NAVBAR_SETTING)
    fun updateState_available_preferenceVisible() {
        val preference = Preference(mContext)
        preference.key = PREF_KEY
        val screen = mock(PreferenceScreen::class.java)
        `when`(screen.findPreference<Preference>(PREF_KEY)).thenReturn(preference)

        mController.displayPreference(screen)
        mController.updateState(preference)

        assertThat(preference.isVisible).isTrue()
    }

    @Test
    @DisableFlags(Flags.FLAG_IME_SWITCHER_BUTTON_IN_NAVBAR_SETTING)
    fun updateState_unavailable_preferenceInvisible() {
        val preference = Preference(mContext)
        preference.key = PREF_KEY
        val screen = mock(PreferenceScreen::class.java)
        `when`(screen.findPreference<Preference>(PREF_KEY)).thenReturn(preference)

        mController.displayPreference(screen)
        mController.updateState(preference)

        assertThat(preference.isVisible).isFalse()
    }

    companion object {
        private const val PREF_KEY = "ime_switcher_in_navbar_enabled"
    }
}
