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

import android.app.Application
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.CompanionDeviceManager
import android.content.Intent
import android.os.PersistableBundle
import android.os.UserHandle
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.ext.truth.content.IntentSubject.assertThat
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(shadows = [SettingsShadowResources::class])
class SettingsTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private val app: Application = ApplicationProvider.getApplicationContext()

    @Test
    @EnableFlags(com.android.server.connectivity.Flags.FLAG_SYNC_AIRPLANE_MODE_WITH_WATCHES)
    fun networkDashboardActivity_airplaneModeIntent_redirects() {
        val intent = Intent(android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS)
        SettingsShadowResources.overrideResource(R.bool.config_show_toggle_airplane, true)
        val cdm = shadowOf(app.getSystemService(CompanionDeviceManager::class.java))
        cdm.addAssociation(
            AssociationInfo.Builder(1, UserHandle.myUserId(), app.packageName)
                .setDeviceProfile(AssociationRequest.DEVICE_PROFILE_WATCH)
                .setDisplayName("Smart Watch")
                .setMetadata(PersistableBundle())
                .build()
        )

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
    @DisableFlags(com.android.server.connectivity.Flags.FLAG_SYNC_AIRPLANE_MODE_WITH_WATCHES)
    fun networkDashboardActivity_airplaneModeIntent_flagDisabled_doesNotRedirect() {
        val intent = Intent(android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS)
        SettingsShadowResources.overrideResource(R.bool.config_show_toggle_airplane, true)
        val cdm = shadowOf(app.getSystemService(CompanionDeviceManager::class.java))
        cdm.addAssociation(
            AssociationInfo.Builder(1, UserHandle.myUserId(), app.packageName)
                .setDeviceProfile(AssociationRequest.DEVICE_PROFILE_WATCH)
                .setDisplayName("Smart Watch")
                .setMetadata(PersistableBundle())
                .build()
        )

        val activity =
            Robolectric.buildActivity(Settings.NetworkDashboardActivity::class.java, intent)
                .create()
                .get()

        val shadowActivity = shadowOf(activity)
        assertThat(shadowActivity.nextStartedActivity).isNull()
        assertThat(activity.isFinishing).isFalse()
    }

    @Test
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
}
