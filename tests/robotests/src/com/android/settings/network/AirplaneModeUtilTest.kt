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

import android.app.Application
import android.companion.AssociationInfo
import android.companion.AssociationRequest.DEVICE_PROFILE_WATCH
import android.companion.CompanionDeviceManager
import android.companion.CompanionDeviceManager.FEATURE_CROSS_DEVICE_SYNC
import android.companion.CompanionDeviceManager.FLAG_AIRPLANE_MODE
import android.companion.Flags.FLAG_ENABLE_DATA_SYNC
import android.content.Context.COMPANION_DEVICE_SERVICE
import android.content.pm.PackageManager
import android.os.PersistableBundle
import android.os.UserHandle
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.server.connectivity.Flags.FLAG_SYNC_AIRPLANE_MODE_WITH_WATCHES
import com.android.settings.R
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowContextImpl

@RunWith(AndroidJUnit4::class)
@Config(shadows = [SettingsShadowResources::class])
class AirplaneModeUtilTest {

    @get:Rule val setFlagsRule = SetFlagsRule()

    private val associationsList = mutableListOf<AssociationInfo>()
    private var selfSupportsApmSync = true
    private val mockCompanionDeviceManager =
        mock<CompanionDeviceManager> {
            on { getAllAssociations(UserHandle.USER_ALL) } doReturn associationsList
            on { getLocalMetadata(UserHandle.USER_ALL) } doAnswer
                {
                    getMetadata(selfSupportsApmSync)
                }
        }
    private val context = ApplicationProvider.getApplicationContext<Application>().baseContext
    private val shadowPackageManager = shadowOf(context.packageManager)

    @Before
    fun setUp() {
        Shadow.extract<ShadowContextImpl>(context)
            .setSystemService(COMPANION_DEVICE_SERVICE, mockCompanionDeviceManager)
    }

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
    @EnableFlags(FLAG_SYNC_AIRPLANE_MODE_WITH_WATCHES, FLAG_ENABLE_DATA_SYNC)
    fun hasPairedWatchForAirplaneModeSync_noAssociations_returnsFalse() {
        assertThat(context.hasPairedWatchForAirplaneModeSync()).isFalse()
    }

    @Test
    @EnableFlags(FLAG_SYNC_AIRPLANE_MODE_WITH_WATCHES, FLAG_ENABLE_DATA_SYNC)
    fun hasPairedWatchForAirplaneModeSync_hasNonWatchAssociation_returnsFalse() {
        addAssociation("some_other_profile")

        assertThat(context.hasPairedWatchForAirplaneModeSync()).isFalse()
    }

    @Test
    @EnableFlags(FLAG_SYNC_AIRPLANE_MODE_WITH_WATCHES, FLAG_ENABLE_DATA_SYNC)
    fun hasPairedWatchForAirplaneModeSync_hasWatchAndNonWatchAssociation_returnsTrue() {
        addAssociation("some_other_profile")
        addAssociation(DEVICE_PROFILE_WATCH)

        assertThat(context.hasPairedWatchForAirplaneModeSync()).isTrue()
    }

    @Test
    @EnableFlags(FLAG_SYNC_AIRPLANE_MODE_WITH_WATCHES, FLAG_ENABLE_DATA_SYNC)
    fun hasPairedWatchForAirplaneModeSync_hasWatchAssociation_apmSyncUnsupported_returnsFalse() {
        addAssociation(DEVICE_PROFILE_WATCH, apmSyncSupported = false)

        assertThat(context.hasPairedWatchForAirplaneModeSync()).isFalse()
    }

    @Test
    @EnableFlags(FLAG_SYNC_AIRPLANE_MODE_WITH_WATCHES, FLAG_ENABLE_DATA_SYNC)
    fun hasPairedWatchForAirplaneModeSync_hasWatchAssociation_apmSyncSupported_returnsTrue() {
        addAssociation(DEVICE_PROFILE_WATCH)

        assertThat(context.hasPairedWatchForAirplaneModeSync()).isTrue()
    }

    @Test
    @EnableFlags(FLAG_SYNC_AIRPLANE_MODE_WITH_WATCHES, FLAG_ENABLE_DATA_SYNC)
    fun hasPairedWatchForAirplaneModeSync_selfDoesNotSupportApmSync_returnsFalse() {
        addAssociation(DEVICE_PROFILE_WATCH)
        selfSupportsApmSync = false

        assertThat(context.hasPairedWatchForAirplaneModeSync()).isFalse()
    }

    @Test
    @EnableFlags(FLAG_SYNC_AIRPLANE_MODE_WITH_WATCHES, FLAG_ENABLE_DATA_SYNC)
    fun hasPairedWatchForAirplaneModeSync_hasMultipleWatchAssociations_oneApmSyncSupported_returnsTrue() {
        addAssociation(DEVICE_PROFILE_WATCH, apmSyncSupported = false)
        addAssociation(DEVICE_PROFILE_WATCH, apmSyncSupported = true)

        assertThat(context.hasPairedWatchForAirplaneModeSync()).isTrue()
    }

    @Test
    @EnableFlags(FLAG_SYNC_AIRPLANE_MODE_WITH_WATCHES)
    @DisableFlags(FLAG_ENABLE_DATA_SYNC)
    fun hasPairedWatchForAirplaneModeSync_dataSyncFlagDisabled_returnsFalse() {
        addAssociation(DEVICE_PROFILE_WATCH)

        assertThat(context.hasPairedWatchForAirplaneModeSync()).isFalse()
    }

    @Test
    @EnableFlags(FLAG_ENABLE_DATA_SYNC)
    @DisableFlags(FLAG_SYNC_AIRPLANE_MODE_WITH_WATCHES)
    fun hasPairedWatchForAirplaneModeSync_apmFlagDisabled_returnsFalse() {
        addAssociation(DEVICE_PROFILE_WATCH)

        assertThat(context.hasPairedWatchForAirplaneModeSync()).isFalse()
    }

    private fun addAssociation(deviceProfile: String, apmSyncSupported: Boolean = true) {
        associationsList.add(
            AssociationInfo.Builder(1, UserHandle.myUserId(), "packageName")
                .setDeviceProfile(deviceProfile)
                .setDisplayName("Smart Device")
                .setMetadata(getMetadata(apmSyncSupported))
                .setSystemDataSyncFlags(if (apmSyncSupported) FLAG_AIRPLANE_MODE else 0)
                .build()
        )
    }

    private fun getMetadata(apmSyncSupported: Boolean) =
        PersistableBundle().apply {
            putPersistableBundle(
                FEATURE_CROSS_DEVICE_SYNC,
                PersistableBundle().apply { putBoolean(APM_SYNC_SUPPORTED, apmSyncSupported) },
            )
        }
}
