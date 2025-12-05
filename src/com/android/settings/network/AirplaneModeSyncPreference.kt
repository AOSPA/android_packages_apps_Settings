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

import android.app.settings.SettingsEnums.ACTION_AIRPLANE_MODE_SYNC_TOGGLE
import android.bluetooth.BluetoothManager
import android.content.Context
import android.provider.Settings
import androidx.annotation.DrawableRes
import com.android.settings.R
import com.android.settings.metrics.PreferenceActionMetricsProvider
import com.android.settingslib.datastore.SettingsGlobalStore
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.SwitchPreference

class AirplaneModeSyncPreference :
    SwitchPreference(KEY, R.string.sync_across_devices_title), PreferenceActionMetricsProvider {

    override val icon: Int
        @DrawableRes get() = R.drawable.ic_sync

    override fun isEnabled(context: Context) =
        context.getSystemService(BluetoothManager::class.java).adapter.isEnabled

    override fun getReadPermissions(context: Context) = SettingsGlobalStore.getReadPermissions()

    override fun getWritePermissions(context: Context) = SettingsGlobalStore.getWritePermissions()

    // TODO(b/420946599): Check with PWG for the read/write permit and sensitivityLevel.
    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun storage(context: Context) = SettingsGlobalStore.get(context)

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    override val preferenceActionMetrics: Int
        get() = ACTION_AIRPLANE_MODE_SYNC_TOGGLE

    companion object {
        const val KEY = Settings.Global.AIRPLANE_MODE_SYNC
    }
}
