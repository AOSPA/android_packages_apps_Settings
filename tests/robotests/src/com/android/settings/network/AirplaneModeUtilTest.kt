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

package com.android.settings.network

import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.PersistableBundle
import android.os.UserHandle
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(shadows = [SettingsShadowResources::class])
class AirplaneModeUtilTest {

    @get:Rule val setFlagsRule = SetFlagsRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val shadowPackageManager = shadowOf(context.packageManager)

    @Test
    fun isAirplaneModeEligible_configIsTrueAndNotLeanback_returnsTrue() {
        SettingsShadowResources.overrideResource(R.bool.config_show_toggle_airplane, true)
        shadowPackageManager.setSystemFeature(PackageManager.FEATURE_LEANBACK, false)

        assertThat(context.isAirplaneModeEligible()).isTrue()
    }

    @Test
    fun isAirplaneModeEligible_configIsFalse_returnsFalse() {
        SettingsShadowResources.overrideResource(R.bool.config_show_toggle_airplane, false)
        shadowPackageManager.setSystemFeature(PackageManager.FEATURE_LEANBACK, false)

        assertThat(context.isAirplaneModeEligible()).isFalse()
    }

    @Test
    fun isAirplaneModeEligible_isLeanback_returnsFalse() {
        SettingsShadowResources.overrideResource(R.bool.config_show_toggle_airplane, true)
        shadowPackageManager.setSystemFeature(PackageManager.FEATURE_LEANBACK, true)

        assertThat(context.isAirplaneModeEligible()).isFalse()
    }

    @Test
    @EnableFlags("com.android.server.connectivity.sync_airplane_mode_with_watches")
    fun hasPairedWatchForAirplaneModeSync_noAssociations_returnsFalse() {
        assertThat(context.hasPairedWatchForAirplaneModeSync()).isFalse()
    }

    @Test
    @EnableFlags("com.android.server.connectivity.sync_airplane_mode_with_watches")
    fun hasPairedWatchForAirplaneModeSync_hasNonWatchAssociation_returnsFalse() {
        context.addAssociation("some_other_profile")

        assertThat(context.hasPairedWatchForAirplaneModeSync()).isFalse()
    }

    @Test
    @EnableFlags("com.android.server.connectivity.sync_airplane_mode_with_watches")
    fun hasPairedWatchForAirplaneModeSync_hasWatchAndNonWatchAssociation_returnsTrue() {
        context.addAssociation("some_other_profile")
        context.addAssociation(AssociationRequest.DEVICE_PROFILE_WATCH)

        assertThat(context.hasPairedWatchForAirplaneModeSync()).isTrue()
    }

    @Test
    @EnableFlags("com.android.server.connectivity.sync_airplane_mode_with_watches")
    fun hasPairedWatchForAirplaneModeSync_hasWatchAssociation_returnsTrue() {
        context.addAssociation(AssociationRequest.DEVICE_PROFILE_WATCH)

        assertThat(context.hasPairedWatchForAirplaneModeSync()).isTrue()
    }

    @Test
    @DisableFlags("com.android.server.connectivity.sync_airplane_mode_with_watches")
    fun hasPairedWatchForAirplaneModeSync_flagDisabled_returnsFalse() {
        context.addAssociation(AssociationRequest.DEVICE_PROFILE_WATCH)

        assertThat(context.hasPairedWatchForAirplaneModeSync()).isFalse()
    }

    private fun Context.addAssociation(deviceProfile: String) {
        shadowOf(getSystemService(CompanionDeviceManager::class.java))
            .addAssociation(
                AssociationInfo.Builder(1, UserHandle.myUserId(), context.packageName)
                    .setDeviceProfile(deviceProfile)
                    .setDisplayName("Smart Device")
                    .setMetadata(PersistableBundle())
                    .build()
            )
    }
}
