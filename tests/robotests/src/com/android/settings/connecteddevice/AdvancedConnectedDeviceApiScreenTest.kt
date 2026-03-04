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

package com.android.settings.connecteddevice

import android.Manifest.permission
import android.app.Application
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.UserManager
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.flags.Flags.FLAG_CATALYST_MIGRATION_26Q2
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.EnterpriseRestrictionException
import com.android.settings.testutils2.HardwareUnsupportedException
import com.android.settings.testutils2.InvalidPreferenceException
import com.android.settings.testutils2.MissingPermissionException
import com.android.settings.testutils2.RegionalRestrictionException
import com.android.settings.uwb.UwbPreferenceController
import com.android.settings.uwb.UwbPreferenceController.AVAILABLE
import com.android.settings.uwb.UwbPreferenceController.UNSUPPORTED_ON_DEVICE
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowDevicePolicyManager

@RunWith(AndroidJUnit4::class)
@Config(shadows = [AdvancedConnectedDeviceApiScreenTest.ShadowUwbPreferenceController::class])
class AdvancedConnectedDeviceApiScreenTest {
    private lateinit var context: Application
    private lateinit var tester: ApiTester
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var shadowDevicePolicyManager: ShadowDevicePolicyManager
    private lateinit var userManager: UserManager
    private lateinit var adminComponent: ComponentName
    @get:Rule val setFlagsRule = SetFlagsRule()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        tester = ApiTester(AdvancedConnectedDeviceApiScreen())
        devicePolicyManager =
            context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        shadowDevicePolicyManager = shadowOf(devicePolicyManager)
        userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        adminComponent = ComponentName("testing", "testing.MyDeviceAdminReceiver")
        shadowDevicePolicyManager.setActiveAdmin(adminComponent)
        ShadowUwbPreferenceController.reset()
        Settings.Global.putInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0)
        shadowOf(context).grantPermissions(permission.UWB_PRIVILEGED)
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_isNotNull() {
        assertThat(tester.getScreen()).isNotNull()
    }

    @Test
    @DisableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagDisabled_isNull() {
        assertThat(tester.getScreen()).isNull()
    }

    @Test
    fun uwb_unsupported_throwsException() {
        ShadowUwbPreferenceController.availabilityStatus = UNSUPPORTED_ON_DEVICE

        assertThrows(HardwareUnsupportedException::class.java) {
            tester.get<Boolean>(AdvancedConnectedDeviceApiScreen.UWB_KEY)
        }
    }

    @Test
    fun uwb_regulatoryDisabled_throwsException() {
        ShadowUwbPreferenceController.availabilityStatus =
            UwbPreferenceController.CONDITIONALLY_UNAVAILABLE

        assertThrows(RegionalRestrictionException::class.java) {
            tester.get<Boolean>(AdvancedConnectedDeviceApiScreen.UWB_KEY)
        }
    }

    @Test
    fun uwb_userRestricted_throwsException() {
        devicePolicyManager.addUserRestriction(
            adminComponent,
            UserManager.DISALLOW_ULTRA_WIDEBAND_RADIO,
        )

        assertThrows(EnterpriseRestrictionException::class.java) {
            tester.get<Boolean>(AdvancedConnectedDeviceApiScreen.UWB_KEY)
        }
    }

    @Test
    fun getUwb_active_returnsTrue() {
        ShadowUwbPreferenceController.availabilityStatus = AVAILABLE
        ShadowUwbPreferenceController.isChecked = true

        assertThat(tester.get<Boolean>(AdvancedConnectedDeviceApiScreen.UWB_KEY)).isTrue()
    }

    @Test
    fun getUwb_disabled_returnsFalse() {
        ShadowUwbPreferenceController.availabilityStatus = AVAILABLE
        ShadowUwbPreferenceController.isChecked = false

        assertThat(tester.get<Boolean>(AdvancedConnectedDeviceApiScreen.UWB_KEY)).isFalse()
    }

    @Test
    fun uwb_airplaneModeOn_getWorksSetThrows() {
        ShadowUwbPreferenceController.isChecked = true
        Settings.Global.putInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 1)

        // Get should still work (it returns false in shadow when airplane mode is on)
        assertThat(tester.get<Boolean>(AdvancedConnectedDeviceApiScreen.UWB_KEY)).isFalse()

        // Set should throw InvalidPreferenceException
        assertThrows(InvalidPreferenceException::class.java) {
            tester.set(AdvancedConnectedDeviceApiScreen.UWB_KEY, true)
        }
    }

    @Test
    fun setUwb_noPermission_throwsException() {
        shadowOf(context).denyPermissions(permission.UWB_PRIVILEGED)

        assertThrows(MissingPermissionException::class.java) {
            tester.set(AdvancedConnectedDeviceApiScreen.UWB_KEY, true)
        }
    }

    @Test
    fun setUwb_toTrue_updatesValue() {
        ShadowUwbPreferenceController.availabilityStatus = AVAILABLE
        ShadowUwbPreferenceController.isChecked = false

        tester.set(AdvancedConnectedDeviceApiScreen.UWB_KEY, true)

        assertThat(ShadowUwbPreferenceController.isChecked).isTrue()
        assertThat(tester.get<Boolean>(AdvancedConnectedDeviceApiScreen.UWB_KEY)).isTrue()
    }

    @Test
    fun setUwb_toFalse_updatesValue() {
        ShadowUwbPreferenceController.availabilityStatus = AVAILABLE
        ShadowUwbPreferenceController.isChecked = true

        tester.set(AdvancedConnectedDeviceApiScreen.UWB_KEY, false)

        assertThat(ShadowUwbPreferenceController.isChecked).isFalse()
        assertThat(tester.get<Boolean>(AdvancedConnectedDeviceApiScreen.UWB_KEY)).isFalse()
    }

    @Implements(UwbPreferenceController::class)
    class ShadowUwbPreferenceController {
        @Implementation
        fun getAvailabilityStatus(): Int {
            if (isAirplaneModeOn()) {
                return UwbPreferenceController.DISABLED_DEPENDENT_SETTING
            }
            return availabilityStatus
        }

        @Implementation
        fun isChecked(): Boolean {
            if (isAirplaneModeOn()) {
                return false
            }
            return isChecked
        }

        @Implementation
        fun setChecked(isChecked: Boolean): Boolean {
            if (isAirplaneModeOn()) {
                return false
            }
            ShadowUwbPreferenceController.isChecked = isChecked
            return true
        }

        @Implementation
        fun onStart() {
            // Do nothing to avoid NullPointerException from uninitialized mPreference
        }

        private fun isAirplaneModeOn(): Boolean {
            val context = ApplicationProvider.getApplicationContext<Context>()
            return Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.AIRPLANE_MODE_ON,
                0,
            ) == 1
        }

        companion object {
            var availabilityStatus = AVAILABLE
            var isChecked = false

            fun reset() {
                availabilityStatus = AVAILABLE
                isChecked = false
            }
        }
    }
}
