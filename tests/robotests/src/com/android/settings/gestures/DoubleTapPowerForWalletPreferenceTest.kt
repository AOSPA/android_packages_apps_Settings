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

package com.android.settings.gestures

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings.Secure.DOUBLE_TAP_POWER_BUTTON_GESTURE_ENABLED
import android.service.quickaccesswallet.QuickAccessWalletClientImpl
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.internal.R as IR
import com.android.settings.R
import com.android.settings.gestures.DoubleTapPowerSettingsUtils.DOUBLE_TAP_POWER_LAUNCH_CAMERA_MODE
import com.android.settings.gestures.DoubleTapPowerSettingsUtils.DOUBLE_TAP_POWER_MULTI_TARGET_MODE
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

// LINT.IfChange
@RunWith(AndroidJUnit4::class)
class DoubleTapPowerForWalletPreferenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preference = DoubleTapPowerForWalletPreference(DoubleTapPowerStorage(context))
    private val storage = DoubleTapPowerMainSwitchPreference.createDataStore(context)

    @Test
    fun keyAndTitle_areCorrect() {
        assertThat(preference.key).isEqualTo(DoubleTapPowerForWalletPreference.KEY)
        assertThat(preference.title).isEqualTo(R.string.double_tap_power_wallet_action_title)
        assertThat(preference.keywords).isEqualTo(R.string.keywords_double_tap_power_wallet_action)
    }

    @Test
    @Config(shadows = [SettingsShadowResources::class])
    fun isAvailable_allConditionsMet_returnsTrue() {
        SettingsShadowResources.overrideResource(
            IR.integer.config_doubleTapPowerGestureMode,
            DOUBLE_TAP_POWER_MULTI_TARGET_MODE,
        )

        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_NFC, true)

        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    @Config(shadows = [SettingsShadowResources::class])
    fun isAvailable_multiTargetNotAvailable_returnsFalse() {
        SettingsShadowResources.overrideResource(
            IR.integer.config_doubleTapPowerGestureMode,
            DOUBLE_TAP_POWER_LAUNCH_CAMERA_MODE,
        )

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    @Config(shadows = [SettingsShadowResources::class])
    fun isAvailable_nfcNotAvailable_returnsFalse() {
        SettingsShadowResources.overrideResource(
            IR.integer.config_doubleTapPowerGestureMode,
            DOUBLE_TAP_POWER_MULTI_TARGET_MODE,
        )

        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_NFC, false)

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    @Config(shadows = [ShadowQuickAccessWalletClientImpl::class])
    fun isEnabled_allConditionsMet_returnsTrue() {
        ShadowQuickAccessWalletClientImpl.setWalletServiceAvailable(true)

        storage.setBoolean(DOUBLE_TAP_POWER_BUTTON_GESTURE_ENABLED, true)

        assertThat(preference.isEnabled(context)).isTrue()
    }

    @Test
    @Config(shadows = [ShadowQuickAccessWalletClientImpl::class])
    fun isEnabled_gestureDisabled_returnsFalse() {
        ShadowQuickAccessWalletClientImpl.setWalletServiceAvailable(true)

        storage.setBoolean(DOUBLE_TAP_POWER_BUTTON_GESTURE_ENABLED, false)

        assertThat(preference.isEnabled(context)).isFalse()
    }

    @Test
    @Config(shadows = [ShadowQuickAccessWalletClientImpl::class])
    fun isEnabled_walletNotAvailable_returnsFalse() {
        ShadowQuickAccessWalletClientImpl.setWalletServiceAvailable(false)

        storage.setBoolean(DOUBLE_TAP_POWER_BUTTON_GESTURE_ENABLED, true)

        assertThat(preference.isEnabled(context)).isFalse()
    }
}

@Implements(QuickAccessWalletClientImpl::class)
internal class ShadowQuickAccessWalletClientImpl {

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
// LINT.ThenChange(DoubleTapPowerForWalletPreferenceControllerTest.java)
