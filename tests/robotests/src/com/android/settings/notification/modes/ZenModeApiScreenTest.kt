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

package com.android.settings.notification.modes

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.app.Application
import android.content.Context
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import android.service.notification.ZenModeConfig.MANUAL_RULE_ID
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.flags.Flags
import com.android.settings.notification.modes.ZenModeApiScreen.Companion.MODE_NAME
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.FailedPreconditionException
import com.android.settings.testutils2.MissingPermissionException
import com.android.settings.testutils2.Parameters
import com.android.settingslib.notification.modes.ZenMode
import com.android.settingslib.notification.modes.ZenModesBackend
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class ZenModeApiScreenTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private val tester = ApiTester(ZenModeApiScreen())

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val shadowApplication = shadowOf(context as Application)
    private val zenModesBackend = mock<ZenModesBackend>()

    @Before
    fun setUp() {
        shadowApplication.grantPermissions(WRITE_SECURE_SETTINGS)
        ZenModesBackend.setInstance(zenModesBackend)
        val dndMode =
            mock<ZenMode> {
                on { name } doReturn DND_MODE_NAME
                on { id } doReturn MANUAL_RULE_ID
            }
        val bedtimeMode =
            mock<ZenMode> {
                on { name } doReturn BEDTIME_MODE_NAME
                on { id } doReturn "bedtime_mode_id"
            }
        zenModesBackend.stub { on { modes } doReturn listOf(dndMode, bedtimeMode) }
        tester.initializeScreenParameters(Parameters(MODE_NAME to DND_MODE_NAME))
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagEnabled_isNotNull() {
        assertThat(tester.getScreen()).isNotNull()
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagDisabled_isNull() {
        assertThat(tester.getScreen()).isNull()
    }

    @Test
    fun getLaunchIntent_hasIntent() {
        assertThat(tester.getLaunchIntent()).isNotNull()
    }

    @Test
    fun getRadioPreference_passBedtimeModeName_throwsFailedPreconditionException() {
        tester.initializeScreenParameters(Parameters(MODE_NAME to BEDTIME_MODE_NAME))

        assertFailsWith<FailedPreconditionException> {
            tester.get<DurationType>(ZenModeApiScreen.RADIO_SELECTOR_KEY)
        }
    }

    @Test
    fun getRadioPreference_forever_returnForeverType() {
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.ZEN_DURATION,
            Settings.Secure.ZEN_DURATION_FOREVER,
        )

        assertThat(tester.get<DurationType>(ZenModeApiScreen.RADIO_SELECTOR_KEY))
            .isEqualTo(DurationType.FOREVER.asApiValue)
    }

    @Test
    fun getRadioPreference_alwaysAsk_returnAlwaysAskType() {
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.ZEN_DURATION,
            Settings.Secure.ZEN_DURATION_PROMPT,
        )

        assertThat(tester.get<DurationType>(ZenModeApiScreen.RADIO_SELECTOR_KEY))
            .isEqualTo(DurationType.ALWAYSASK.asApiValue)
    }

    @Test
    fun getRadioPreference_countdown_15_returnCountdown_15Type() {
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.ZEN_DURATION,
            ZenModeApiScreen.COUNTDOWN_15,
        )

        assertThat(tester.get<DurationType>(ZenModeApiScreen.RADIO_SELECTOR_KEY))
            .isEqualTo(DurationType.COUNTDOWN_15.asApiValue)
    }

    @Test
    fun getRadioPreference_countdown_45_returnCountdown_45Type() {
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.ZEN_DURATION,
            ZenModeApiScreen.COUNTDOWN_45,
        )

        assertThat(tester.get<DurationType>(ZenModeApiScreen.RADIO_SELECTOR_KEY))
            .isEqualTo(DurationType.COUNTDOWN_45.asApiValue)
    }

    @Test
    fun getRadioPreference_countdown_1_HR_returnCountdown_1_HRType() {
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.ZEN_DURATION,
            ZenModeApiScreen.COUNTDOWN_1_HR,
        )

        assertThat(tester.get<DurationType>(ZenModeApiScreen.RADIO_SELECTOR_KEY))
            .isEqualTo(DurationType.COUNTDOWN_1_HR.asApiValue)
    }

    @Test
    fun getRadioPreference_countdown_12_HRS_returnCountdown_12_HRSType() {
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.ZEN_DURATION,
            ZenModeApiScreen.COUNTDOWN_12_HRS,
        )

        assertThat(tester.get<DurationType>(ZenModeApiScreen.RADIO_SELECTOR_KEY))
            .isEqualTo(DurationType.COUNTDOWN_12_HRS.asApiValue)
    }

    @Test
    fun setRadioPreference_noPermission_throwsException() {
        shadowApplication.denyPermissions(WRITE_SECURE_SETTINGS)

        assertFailsWith<MissingPermissionException> {
            tester.set(ZenModeApiScreen.RADIO_SELECTOR_KEY, DurationType.FOREVER.asApiValue)
        }
    }

    @Test
    fun setRadioPreference_passBedtimeModeName_throwsFailedPreconditionException() {
        tester.initializeScreenParameters(Parameters(MODE_NAME to BEDTIME_MODE_NAME))

        assertFailsWith<FailedPreconditionException> {
            tester.set(ZenModeApiScreen.RADIO_SELECTOR_KEY, DurationType.FOREVER.asApiValue)
        }
    }

    @Test
    fun setRadioPreference_asForeverType_returnForever() {
        tester.set(ZenModeApiScreen.RADIO_SELECTOR_KEY, DurationType.FOREVER.asApiValue)

        assertThat(Settings.Secure.getInt(context.contentResolver, Settings.Secure.ZEN_DURATION))
            .isEqualTo(Settings.Secure.ZEN_DURATION_FOREVER)
    }

    @Test
    fun setRadioPreference_asAlwaysAskType_returnAlwaysAsk() {
        tester.set(ZenModeApiScreen.RADIO_SELECTOR_KEY, DurationType.ALWAYSASK.asApiValue)

        assertThat(Settings.Secure.getInt(context.contentResolver, Settings.Secure.ZEN_DURATION))
            .isEqualTo(Settings.Secure.ZEN_DURATION_PROMPT)
    }

    @Test
    fun setRadioPreference_asCountdown_15Type_returnCountdown_15() {
        tester.set(ZenModeApiScreen.RADIO_SELECTOR_KEY, DurationType.COUNTDOWN_15.asApiValue)

        assertThat(Settings.Secure.getInt(context.contentResolver, Settings.Secure.ZEN_DURATION))
            .isEqualTo(ZenModeApiScreen.COUNTDOWN_15)
    }

    @Test
    fun setRadioPreference_asCountdown_30Type_returnCountdown_30() {
        tester.set(ZenModeApiScreen.RADIO_SELECTOR_KEY, DurationType.COUNTDOWN_30.asApiValue)

        assertThat(Settings.Secure.getInt(context.contentResolver, Settings.Secure.ZEN_DURATION))
            .isEqualTo(ZenModeApiScreen.COUNTDOWN_30)
    }

    @Test
    fun setRadioPreference_asCountdown_1HRType_returnCountdown_1_HR() {
        tester.set(ZenModeApiScreen.RADIO_SELECTOR_KEY, DurationType.COUNTDOWN_1_HR.asApiValue)

        assertThat(Settings.Secure.getInt(context.contentResolver, Settings.Secure.ZEN_DURATION))
            .isEqualTo(ZenModeApiScreen.COUNTDOWN_1_HR)
    }

    @Test
    fun setRadioPreference_asCountdown_6HRSType_returnCountdown_6_HRS() {
        tester.set(ZenModeApiScreen.RADIO_SELECTOR_KEY, DurationType.COUNTDOWN_6_HRS.asApiValue)

        assertThat(Settings.Secure.getInt(context.contentResolver, Settings.Secure.ZEN_DURATION))
            .isEqualTo(ZenModeApiScreen.COUNTDOWN_6_HRS)
    }

    companion object {
        private const val DND_MODE_NAME = "Do Not Disturb"
        private const val BEDTIME_MODE_NAME = "Bedtime"
    }
}
