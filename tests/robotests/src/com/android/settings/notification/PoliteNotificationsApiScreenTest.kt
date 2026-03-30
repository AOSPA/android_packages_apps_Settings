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

package com.android.settings.notification

import android.Manifest.permission.WRITE_SETTINGS
import android.app.Application
import android.content.Context
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.server.notification.Flags as NotificationFlags
import com.android.settings.flags.Flags
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.MissingPermissionException
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class PoliteNotificationsApiScreenTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private val tester = ApiTester(PoliteNotificationsApiScreen())
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val shadowApplication = shadowOf(context as Application)

    @Before
    fun setUp() {
        shadowApplication.grantPermissions(WRITE_SETTINGS)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2, NotificationFlags.FLAG_POLITE_NOTIFICATIONS)
    fun getScreen_allFlagsEnabled_isNotNull() {
        assertThat(tester.getScreen()).isNotNull()
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_catalystFlagDisabled_isNull() {
        assertThat(tester.getScreen()).isNull()
    }

    @Test
    @DisableFlags(NotificationFlags.FLAG_POLITE_NOTIFICATIONS)
    fun getScreen_notificationFlagsDisabled_isNull() {
        assertThat(tester.getScreen()).isNull()
    }

    @Test
    fun getLaunchIntent_hasIntent() {
        assertThat(tester.getLaunchIntent()).isNotNull()
    }

    @Test
    fun getMainSwitchPreference_defaultOn_returnTrue() {
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.NOTIFICATION_COOLDOWN_ENABLED,
            PoliteNotificationsApiScreen.ON,
        )

        assertThat(tester.get<Boolean>(PoliteNotificationsApiScreen.MAIN_SWITCH_KEY)).isTrue()
    }

    @Test
    fun getMainSwitchPreference_defaultOff_returnFalse() {
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.NOTIFICATION_COOLDOWN_ENABLED,
            PoliteNotificationsApiScreen.OFF,
        )

        assertThat(tester.get<Boolean>(PoliteNotificationsApiScreen.MAIN_SWITCH_KEY)).isFalse()
    }

    @Test
    fun setMainSwitchPreference_asTrue_returnOn() {
        tester.set(PoliteNotificationsApiScreen.MAIN_SWITCH_KEY, true)

        assertThat(
                Settings.System.getInt(
                    context.contentResolver,
                    Settings.System.NOTIFICATION_COOLDOWN_ENABLED,
                    PoliteNotificationsApiScreen.OFF,
                )
            )
            .isEqualTo(PoliteNotificationsApiScreen.ON)
    }

    @Test
    fun setMainSwitchPreference_asFalse_returnOff() {
        tester.set(PoliteNotificationsApiScreen.MAIN_SWITCH_KEY, false)

        assertThat(
                Settings.System.getInt(
                    context.contentResolver,
                    Settings.System.NOTIFICATION_COOLDOWN_ENABLED,
                    PoliteNotificationsApiScreen.ON,
                )
            )
            .isEqualTo(PoliteNotificationsApiScreen.OFF)
    }

    @Test
    fun setMainSwitchPreference_noPermission_throwsException() {
        shadowApplication.denyPermissions(WRITE_SETTINGS)

        assertFailsWith<MissingPermissionException> {
            tester.set(PoliteNotificationsApiScreen.MAIN_SWITCH_KEY, true)
        }
    }
}
