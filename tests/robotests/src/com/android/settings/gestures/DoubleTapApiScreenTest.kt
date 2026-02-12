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

package com.android.settings.gestures

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.app.Application
import android.content.Context
import android.os.UserHandle
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import android.provider.Settings.Secure.DOZE_DOUBLE_TAP_GESTURE
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.internal.R
import com.android.settings.flags.Flags
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.HardwareUnsupportedException
import com.android.settings.testutils2.MissingPermissionException
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@Config(shadows = [SettingsShadowResources::class])
@RunWith(AndroidJUnit4::class)
class DoubleTapApiScreenTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private val tester = ApiTester(DoubleTapApiScreen())

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val shadowApplication = shadowOf(context as Application)

    @Before
    fun setUp() {
        shadowApplication.grantPermissions(WRITE_SECURE_SETTINGS)
        SettingsShadowResources.overrideResource(
            R.string.config_dozeDoubleTapSensorType,
            "FakeDoubleTapSensorType",
        )
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
    fun getLaunchIntent_withDoubleTapSensor_hasIntent() {
        assertThat(tester.getLaunchIntent()).isNotNull()
    }

    @Test
    fun getLaunchIntent_withoutDoubleTapSensor_throwsHardwareUnsupportedException() {
        SettingsShadowResources.overrideResource(R.string.config_dozeDoubleTapSensorType, "")

        assertFailsWith<HardwareUnsupportedException> { tester.getLaunchIntent() }
    }

    @Test
    fun getMainSwitchPreference_defaultOn_returnTrue() {
        Settings.Secure.putInt(
            context.contentResolver,
            DOZE_DOUBLE_TAP_GESTURE,
            DoubleTapApiScreen.ON,
        )

        assertThat(tester.get<Boolean>(DoubleTapApiScreen.MAIN_SWITCH_KEY)).isTrue()
    }

    @Test
    fun getMainSwitchPreference_defaultOff_returnFalse() {
        Settings.Secure.putInt(
            context.contentResolver,
            DOZE_DOUBLE_TAP_GESTURE,
            DoubleTapApiScreen.OFF,
        )

        assertThat(tester.get<Boolean>(DoubleTapApiScreen.MAIN_SWITCH_KEY)).isFalse()
    }

    @Test
    fun getMainSwitchPreference_withoutDoubleTapSensor_throwsHardwareUnsupportedException() {
        SettingsShadowResources.overrideResource(R.string.config_dozeDoubleTapSensorType, "")
        Settings.Secure.putInt(
            context.contentResolver,
            DOZE_DOUBLE_TAP_GESTURE,
            DoubleTapApiScreen.ON,
        )

        assertFailsWith<HardwareUnsupportedException> {
            tester.get<Boolean>(DoubleTapApiScreen.MAIN_SWITCH_KEY)
        }
    }

    @Test
    fun setMainSwitchPreference_asTrue_returnOn() {
        tester.set(DoubleTapApiScreen.MAIN_SWITCH_KEY, true)

        assertThat(
                Settings.Secure.getIntForUser(
                    context.contentResolver,
                    DOZE_DOUBLE_TAP_GESTURE,
                    DoubleTapApiScreen.OFF,
                    UserHandle.myUserId(),
                )
            )
            .isEqualTo(DoubleTapApiScreen.ON)
    }

    @Test
    fun setMainSwitchPreference_asFalse_returnOff() {
        tester.set(DoubleTapApiScreen.MAIN_SWITCH_KEY, false)

        assertThat(
                Settings.Secure.getIntForUser(
                    context.contentResolver,
                    DOZE_DOUBLE_TAP_GESTURE,
                    DoubleTapApiScreen.ON,
                    UserHandle.myUserId(),
                )
            )
            .isEqualTo(DoubleTapApiScreen.OFF)
    }

    @Test
    fun setMainSwitchPreference_noPermission_throwsException() {
        shadowApplication.denyPermissions(WRITE_SECURE_SETTINGS)

        assertFailsWith<MissingPermissionException> {
            tester.set(DoubleTapApiScreen.MAIN_SWITCH_KEY, true)
        }
    }
}
