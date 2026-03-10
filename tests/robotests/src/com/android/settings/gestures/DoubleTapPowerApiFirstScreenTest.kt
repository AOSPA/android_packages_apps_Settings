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
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import android.provider.Settings.Secure.DOUBLE_TAP_POWER_BUTTON_GESTURE
import android.service.quickaccesswallet.Flags as WalletFlag
import android.service.quickaccesswallet.QuickAccessWalletClientImpl
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.internal.R
import com.android.settings.flags.Flags
import com.android.settings.gestures.DoubleTapPowerSettingsUtils.DOUBLE_TAP_POWER_DISABLED_MODE
import com.android.settings.gestures.DoubleTapPowerSettingsUtils.DOUBLE_TAP_POWER_MULTI_TARGET_MODE
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.FailedPreconditionException
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
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@Config(shadows = [SettingsShadowResources::class])
@RunWith(AndroidJUnit4::class)
class DoubleTapPowerApiFirstScreenTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private val tester = ApiTester(DoubleTapPowerApiFirstScreen())

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val shadowApplication = shadowOf(context as Application)

    @Before
    fun setUp() {
        shadowApplication.grantPermissions(WRITE_SECURE_SETTINGS)
        SettingsShadowResources.overrideResource(
            R.integer.config_doubleTapPowerGestureMode,
            DOUBLE_TAP_POWER_MULTI_TARGET_MODE,
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
    fun getLaunchIntent_hasIntent() {
        assertThat(tester.getLaunchIntent()).isNotNull()
    }

    @Test
    @DisableFlags(WalletFlag.FLAG_LAUNCH_WALLET_OPTION_ON_POWER_DOUBLE_TAP)
    fun getLaunchIntent_disabledFlag_throwsHardwareUnsupportedException() {
        assertFailsWith<HardwareUnsupportedException> { tester.getLaunchIntent() }
    }

    @Test
    fun getLaunchIntent_withoutDoubleTapGesture_throwsHardwareUnsupportedException() {
        SettingsShadowResources.overrideResource(
            R.integer.config_doubleTapPowerGestureMode,
            DOUBLE_TAP_POWER_DISABLED_MODE,
        )

        assertFailsWith<HardwareUnsupportedException> { tester.getLaunchIntent() }
    }

    @Test
    fun getRadioPreference_defaultLaunchCamera_returnCameraLaunch() {
        Settings.Secure.putInt(
            context.contentResolver,
            DOUBLE_TAP_POWER_BUTTON_GESTURE,
            DoubleTapPowerApiFirstScreen.CAMERA_LAUNCH_VALUE,
        )

        assertThat(
                tester.get<PowerButtonLaunchApp>(DoubleTapPowerApiFirstScreen.RADIO_PREFERENCE_KEY)
            )
            .isEqualTo(PowerButtonLaunchApp.CAMERA.asApiValue)
    }

    @Test
    fun getRadioPreference_defaultLaunchWallet_returnWalletLaunch() {
        Settings.Secure.putInt(
            context.contentResolver,
            DOUBLE_TAP_POWER_BUTTON_GESTURE,
            DoubleTapPowerApiFirstScreen.WALLET_LAUNCH_VALUE,
        )

        assertThat(
                tester.get<PowerButtonLaunchApp>(DoubleTapPowerApiFirstScreen.RADIO_PREFERENCE_KEY)
            )
            .isEqualTo(PowerButtonLaunchApp.WALLET.asApiValue)
    }

    @Test
    fun getRadioPreference_withoutDoubleTapGesture_throwsHardwareUnsupportedException() {
        SettingsShadowResources.overrideResource(
            R.integer.config_doubleTapPowerGestureMode,
            DOUBLE_TAP_POWER_DISABLED_MODE,
        )

        Settings.Secure.putInt(
            context.contentResolver,
            DOUBLE_TAP_POWER_BUTTON_GESTURE,
            DoubleTapPowerApiFirstScreen.CAMERA_LAUNCH_VALUE,
        )

        assertFailsWith<HardwareUnsupportedException> {
            tester.get<PowerButtonLaunchApp>(DoubleTapPowerApiFirstScreen.RADIO_PREFERENCE_KEY)
        }
    }

    @Test
    @Config(shadows = [MyShadowQuickAccessWalletClientImpl::class])
    fun setRadioPreference_asLaunchCamera_returnCameraLaunchValue() {
        MyShadowQuickAccessWalletClientImpl.setWalletServiceAvailable(true)

        tester.set(
            DoubleTapPowerApiFirstScreen.RADIO_PREFERENCE_KEY,
            PowerButtonLaunchApp.CAMERA.asApiValue,
        )

        assertThat(Settings.Secure.getInt(context.contentResolver, DOUBLE_TAP_POWER_BUTTON_GESTURE))
            .isEqualTo(DoubleTapPowerApiFirstScreen.CAMERA_LAUNCH_VALUE)
    }

    @Test
    @Config(shadows = [MyShadowQuickAccessWalletClientImpl::class])
    fun setRadioPreference_asLaunchWallet_returnWalletLaunchValue() {
        MyShadowQuickAccessWalletClientImpl.setWalletServiceAvailable(true)

        tester.set(
            DoubleTapPowerApiFirstScreen.RADIO_PREFERENCE_KEY,
            PowerButtonLaunchApp.WALLET.asApiValue,
        )

        assertThat(Settings.Secure.getInt(context.contentResolver, DOUBLE_TAP_POWER_BUTTON_GESTURE))
            .isEqualTo(DoubleTapPowerApiFirstScreen.WALLET_LAUNCH_VALUE)
    }

    @Test
    @Config(shadows = [MyShadowQuickAccessWalletClientImpl::class])
    fun setRadioPreference_walletServiceNotAvailable__throwsFailedPreconditionException() {
        MyShadowQuickAccessWalletClientImpl.setWalletServiceAvailable(false)

        assertFailsWith<FailedPreconditionException> {
            tester.set(
                DoubleTapPowerApiFirstScreen.RADIO_PREFERENCE_KEY,
                PowerButtonLaunchApp.CAMERA.asApiValue,
            )
        }
    }

    @Test
    @Config(shadows = [MyShadowQuickAccessWalletClientImpl::class])
    fun setRadioPreference_noPermission_throwsException() {
        MyShadowQuickAccessWalletClientImpl.setWalletServiceAvailable(true)
        shadowApplication.denyPermissions(WRITE_SECURE_SETTINGS)
        assertFailsWith<MissingPermissionException> {
            tester.set(
                DoubleTapPowerApiFirstScreen.RADIO_PREFERENCE_KEY,
                PowerButtonLaunchApp.CAMERA.asApiValue,
            )
        }
    }
}

@Implements(QuickAccessWalletClientImpl::class)
internal class MyShadowQuickAccessWalletClientImpl {

    @Implementation
    fun isWalletServiceAvailable(): Boolean {
        return hasWalletServiceAvailable
    }

    companion object {
        private var hasWalletServiceAvailable = false

        @JvmStatic
        internal fun setWalletServiceAvailable(value: Boolean) {
            hasWalletServiceAvailable = value
        }
    }
}
