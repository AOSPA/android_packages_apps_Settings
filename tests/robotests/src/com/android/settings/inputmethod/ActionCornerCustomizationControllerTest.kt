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

package com.android.settings.inputmethod

import android.app.ActivityManager
import android.app.role.RoleManager
import android.app.settings.SettingsEnums
import android.content.Context
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import android.provider.Settings.Secure.ACTION_CORNER_ACTION_HOME
import android.provider.Settings.Secure.ACTION_CORNER_ACTION_LOCKSCREEN
import android.provider.Settings.Secure.ACTION_CORNER_ACTION_NONE
import android.provider.Settings.Secure.ACTION_CORNER_ACTION_NOTE
import android.provider.Settings.Secure.ACTION_CORNER_ACTION_NOTIFICATIONS
import android.provider.Settings.Secure.ACTION_CORNER_ACTION_OVERVIEW
import android.provider.Settings.Secure.ACTION_CORNER_ACTION_PEEK
import android.provider.Settings.Secure.ACTION_CORNER_ACTION_QUICK_SETTINGS
import android.provider.Settings.Secure.ACTION_CORNER_BOTTOM_LEFT_ACTION
import androidx.preference.ListPreference
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.testutils.MetricsRule
import com.android.settings.testutils.shadow.ShadowSystemSettings
import com.android.systemui.Flags
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config

/** Test for [ActionCornerCustomizationController] */
@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowSystemSettings::class])
class ActionCornerCustomizationControllerTest {
    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()
    @get:Rule val setFlagsRule = SetFlagsRule()
    @get:Rule val metricsRule = MetricsRule()

    private val context = spy(ApplicationProvider.getApplicationContext<Context>())
    private val preferenceManager = PreferenceManager(context)
    private val preferenceScreen = preferenceManager.createPreferenceScreen(context)
    private lateinit var preference: ListPreference
    private lateinit var controller: ActionCornerCustomizationController

    @Mock private lateinit var roleManager: RoleManager

    @Before
    fun setUp() {
        whenever(context.getSystemService(RoleManager::class.java)).thenReturn(roleManager)
        controller = ActionCornerCustomizationController(context, PREF_KEY_BOTTOM_LEFT)
        preference =
            ListPreference(context).apply {
                key = PREF_KEY_BOTTOM_LEFT
                setEntries(R.array.action_corner_action_titles)
                entryValues = entryValueArray
            }
        preferenceScreen.addPreference(preference)
        controller.displayPreference(preferenceScreen)
    }

    @Test
    fun displayPreference_hasCorrectDialogTitle() {
        val dialogTitle = getDialogTitle()
        assertThat(preference.dialogTitle).isEqualTo(dialogTitle)
    }

    private fun getDialogTitle(): String {
        val name = context.getString(R.string.action_corner_bottom_left_name)
        return context.getString(R.string.action_corner_action_dialog_title, name)
    }

    @Test
    fun updateState_default_showNone() {
        controller.updateState(preference)

        assertThat(preference.value).isEqualTo(ACTION_CORNER_ACTION_NONE.toString())
        assertThat(preference.summary).isEqualTo(NONE_TITLE)
    }

    @Test
    fun updateState_actionIsHome_showHome() {
        Settings.Secure.putIntForUser(
            context.contentResolver,
            TARGET,
            ACTION_CORNER_ACTION_HOME,
            ActivityManager.getCurrentUser(),
        )
        controller.updateState(preference)

        assertThat(preference.value).isEqualTo(ACTION_CORNER_ACTION_HOME.toString())
        assertThat(preference.summary).isEqualTo(HOME_TITLE)
    }

    @Test
    fun onPreferenceChange_setToHome() {
        controller.onPreferenceChange(preference, ACTION_CORNER_ACTION_HOME)

        val action =
            Settings.Secure.getIntForUser(
                context.contentResolver,
                TARGET,
                ACTION_CORNER_ACTION_NONE,
                ActivityManager.getCurrentUser(),
            )
        assertThat(action).isEqualTo(ACTION_CORNER_ACTION_HOME)
        verify(metricsRule.metricsFeatureProvider).action(any(), eq(METRICS), eq(action))
    }

    @Test
    fun onPreferenceChange_setToPeek() {
        controller.onPreferenceChange(preference, ACTION_CORNER_ACTION_PEEK)

        val action =
            Settings.Secure.getIntForUser(
                context.contentResolver,
                TARGET,
                ACTION_CORNER_ACTION_NONE,
                ActivityManager.getCurrentUser(),
            )
        assertThat(action).isEqualTo(ACTION_CORNER_ACTION_PEEK)
        verify(metricsRule.metricsFeatureProvider).action(any(), eq(METRICS), eq(action))
    }

    @Test
    @EnableFlags(com.android.window.flags.Flags.FLAG_ENABLE_HOME_SCREEN_PEEKING)
    fun displayPreference_peekFlagEnabled_showPeek() {
        controller.displayPreference(preferenceScreen)

        assertThat(preference.entries).asList().contains(PEEK_TITLE)
        assertThat(preference.entryValues).asList().contains(ACTION_CORNER_ACTION_PEEK.toString())
    }

    @Test
    @DisableFlags(com.android.window.flags.Flags.FLAG_ENABLE_HOME_SCREEN_PEEKING)
    fun displayPreference_peekFlagDisabled_hidePeek() {
        controller.displayPreference(preferenceScreen)

        assertThat(preference.entries).asList().doesNotContain(PEEK_TITLE)
        assertThat(preference.entryValues)
            .asList()
            .doesNotContain(ACTION_CORNER_ACTION_PEEK.toString())
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_NOTE_IN_ACTION_CORNER)
    fun displayPreference_noteActionCornerFlagEnabled_roleAvailable_showNotes() {
        whenever(roleManager.isRoleAvailable(RoleManager.ROLE_NOTES)).thenReturn(true)
        controller.displayPreference(preferenceScreen)

        assertThat(preference.entries).asList().contains(NOTE_TITLE)
        assertThat(preference.entryValues).asList().contains(ACTION_CORNER_ACTION_NOTE.toString())
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_NOTE_IN_ACTION_CORNER)
    fun displayPreference_notesFlagDisabled_roleAvailable_hideNotes() {
        whenever(roleManager.isRoleAvailable(RoleManager.ROLE_NOTES)).thenReturn(true)
        controller.displayPreference(preferenceScreen)

        assertThat(preference.entries).asList().doesNotContain(NOTE_TITLE)
        assertThat(preference.entryValues)
            .asList()
            .doesNotContain(ACTION_CORNER_ACTION_NOTE.toString())
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_NOTE_IN_ACTION_CORNER)
    fun displayPreference_noteActionCornerFlagEnabled_roleNotAvailable_hideNotes() {
        whenever(roleManager.isRoleAvailable(RoleManager.ROLE_NOTES)).thenReturn(false)
        controller.displayPreference(preferenceScreen)

        assertThat(preference.entries).asList().doesNotContain(NOTE_TITLE)
        assertThat(preference.entryValues)
            .asList()
            .doesNotContain(ACTION_CORNER_ACTION_NOTE.toString())
    }

    private companion object {
        const val PREF_KEY_BOTTOM_LEFT = "action_corner_bottom_left"
        const val TARGET = ACTION_CORNER_BOTTOM_LEFT_ACTION
        const val NONE_TITLE = "None"
        const val HOME_TITLE = "Home"
        const val NOTE_TITLE = "Note"
        const val PEEK_TITLE = "Peek/unpeek desktop"
        const val METRICS = SettingsEnums.ACTION_BOTTOM_LEFT_ACTION_CORNER_SHORTCUT_CHANGED

        val entryValueArray =
            arrayOf<CharSequence>(
                ACTION_CORNER_ACTION_NONE.toString(),
                ACTION_CORNER_ACTION_HOME.toString(),
                ACTION_CORNER_ACTION_OVERVIEW.toString(),
                ACTION_CORNER_ACTION_NOTIFICATIONS.toString(),
                ACTION_CORNER_ACTION_QUICK_SETTINGS.toString(),
                ACTION_CORNER_ACTION_LOCKSCREEN.toString(),
            )
    }
}
