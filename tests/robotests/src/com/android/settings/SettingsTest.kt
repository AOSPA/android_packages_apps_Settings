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
package com.android.settings

import android.app.Activity
import android.app.Application
import android.companion.AssociationInfo
import android.companion.AssociationRequest.DEVICE_PROFILE_WATCH
import android.companion.CompanionDeviceManager
import android.companion.CompanionDeviceManager.FEATURE_CROSS_DEVICE_SYNC
import android.companion.CompanionDeviceManager.FLAG_AIRPLANE_MODE
import android.companion.Flags.FLAG_ENABLE_DATA_SYNC
import android.content.Context.COMPANION_DEVICE_SERVICE
import android.content.Intent
import android.os.PersistableBundle
import android.os.UserHandle
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.ext.truth.content.IntentSubject.assertThat
import com.android.server.connectivity.Flags.FLAG_SYNC_AIRPLANE_MODE_WITH_WATCHES
import com.android.settings.network.APM_SYNC_SUPPORTED
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowContextImpl
import org.robolectric.shadows.ShadowLooper

@RunWith(AndroidJUnit4::class)
@Config(shadows = [SettingsShadowResources::class])
class SettingsTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private val mContext = ApplicationProvider.getApplicationContext<Application>().baseContext
    private val mMockCompanionDeviceManager =
        mock<CompanionDeviceManager> {
            on { getLocalMetadata(UserHandle.USER_ALL) } doReturn PersistableBundle()
        }

    @Before
    fun setUp() {
        Shadow.extract<ShadowContextImpl>(mContext)
            .setSystemService(COMPANION_DEVICE_SERVICE, mMockCompanionDeviceManager)
    }

    @Test
    @EnableFlags(FLAG_SYNC_AIRPLANE_MODE_WITH_WATCHES, FLAG_ENABLE_DATA_SYNC)
    fun networkDashboardActivity_airplaneModeIntent_redirects() {
        val intent = Intent(android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS)
        SettingsShadowResources.overrideResource(R.bool.config_show_toggle_airplane, true)
        addWatchAssociation()

        val activity =
            Robolectric.buildActivity(Settings.NetworkDashboardActivity::class.java, intent)
                .create()
                .get()

        val shadowActivity = shadowOf(activity)
        val nextIntent = shadowActivity.nextStartedActivity
        assertThat(nextIntent).isNotNull()
        assertThat(nextIntent.component?.className)
            .isEqualTo(Settings.AirplaneModeSettingsActivity::class.java.name)
        assertThat(activity.isFinishing).isTrue()
    }

    @Test
    @EnableFlags(FLAG_ENABLE_DATA_SYNC)
    @DisableFlags(FLAG_SYNC_AIRPLANE_MODE_WITH_WATCHES)
    fun networkDashboardActivity_airplaneModeIntent_apmFlagDisabled_doesNotRedirect() {
        val intent = Intent(android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS)
        SettingsShadowResources.overrideResource(R.bool.config_show_toggle_airplane, true)
        addWatchAssociation()

        val activity =
            Robolectric.buildActivity(Settings.NetworkDashboardActivity::class.java, intent)
                .create()
                .get()
        ShadowLooper.idleMainLooper()

        val shadowActivity = shadowOf(activity)
        assertThat(shadowActivity.nextStartedActivity).isNull()
        assertThat(activity.isFinishing).isFalse()
    }

    @Test
    @EnableFlags(FLAG_SYNC_AIRPLANE_MODE_WITH_WATCHES)
    @DisableFlags(FLAG_ENABLE_DATA_SYNC)
    fun networkDashboardActivity_airplaneModeIntent_dataSyncFlagDisabled_doesNotRedirect() {
        val intent = Intent(android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS)
        SettingsShadowResources.overrideResource(R.bool.config_show_toggle_airplane, true)
        addWatchAssociation()

        val activity =
            Robolectric.buildActivity(Settings.NetworkDashboardActivity::class.java, intent)
                .create()
                .get()
        ShadowLooper.idleMainLooper()

        val shadowActivity = shadowOf(activity)
        assertThat(shadowActivity.nextStartedActivity).isNull()
        assertThat(activity.isFinishing).isFalse()
    }

    @Test
    @EnableFlags(FLAG_SYNC_AIRPLANE_MODE_WITH_WATCHES, FLAG_ENABLE_DATA_SYNC)
    fun networkDashboardActivity_notAirplaneModeIntent_doesNotRedirect() {
        val intent = Intent("android.intent.action.VIEW")

        val activity =
            Robolectric.buildActivity(Settings.NetworkDashboardActivity::class.java, intent)
                .create()
                .get()
        val shadowActivity = shadowOf(activity)

        assertThat(shadowActivity.nextStartedActivity).isNull()
        assertThat(activity.isFinishing).isFalse()
    }

    @Test
    @EnableFlags(FLAG_SYNC_AIRPLANE_MODE_WITH_WATCHES, FLAG_ENABLE_DATA_SYNC)
    fun networkDashboardActivity_airplaneModeNotEligible_doesNotRedirect() {
        SettingsShadowResources.overrideResource(R.bool.config_show_toggle_airplane, false)
        val intent = Intent(android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS)

        val activity =
            Robolectric.buildActivity(Settings.NetworkDashboardActivity::class.java, intent)
                .create()
                .get()

        val shadowActivity = shadowOf(activity)
        assertThat(shadowActivity.nextStartedActivity).isNull()
        assertThat(activity.isFinishing).isFalse()
    }

    @Test
    @EnableFlags(FLAG_SYNC_AIRPLANE_MODE_WITH_WATCHES, FLAG_ENABLE_DATA_SYNC)
    fun networkDashboardActivity_noPairedWatch_doesNotRedirect() {
        SettingsShadowResources.overrideResource(R.bool.config_show_toggle_airplane, true)
        val intent = Intent(android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS)

        val activity =
            Robolectric.buildActivity(Settings.NetworkDashboardActivity::class.java, intent)
                .create()
                .get()

        val shadowActivity = shadowOf(activity)
        assertThat(shadowActivity.nextStartedActivity).isNull()
        assertThat(activity.isFinishing).isFalse()
    }

    fun startNetworkDashboardActivity(
        intent: Intent = Intent(android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS)
    ): Activity =
        Robolectric.buildActivity(Settings.NetworkDashboardActivity::class.java, intent)
            .create()
            .get()

    private fun addWatchAssociation() {
        val metadata =
            PersistableBundle().apply {
                putPersistableBundle(
                    FEATURE_CROSS_DEVICE_SYNC,
                    PersistableBundle().apply { putBoolean(APM_SYNC_SUPPORTED, true) },
                )
            }
        mMockCompanionDeviceManager.stub {
            on { getAllAssociations(UserHandle.USER_ALL) } doReturn
                listOf(
                    AssociationInfo.Builder(1, UserHandle.myUserId(), mContext.packageName)
                        .setDeviceProfile(DEVICE_PROFILE_WATCH)
                        .setDisplayName("Smart Watch")
                        .setMetadata(metadata)
                        .setSystemDataSyncFlags(FLAG_AIRPLANE_MODE)
                        .build()
                )
            on { getLocalMetadata(UserHandle.USER_ALL) } doReturn metadata
        }
    }
}
