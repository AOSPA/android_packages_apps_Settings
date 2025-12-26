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

package com.android.settings.display.darkmode

import android.Manifest
import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.location.LocationManager
import android.provider.Settings
import com.android.settings.R
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.DataChangeReason
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.datastore.SettingsSecureStore

@Suppress("UNCHECKED_CAST")
class DarkModeScheduleStorage(private val context: Context) :
    AbstractKeyedDataObservable<String>(), KeyedObserver<String>, KeyValueStore {
    private val settingsStore = SettingsSecureStore.get(context)

    private val uiModeManager = context.getSystemService(UiModeManager::class.java)!!

    override fun contains(key: String) = true

    override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
        return context.getString(
            when (uiModeManager.nightMode) {
                UiModeManager.MODE_NIGHT_AUTO -> R.string.dark_ui_auto_mode_auto
                UiModeManager.MODE_NIGHT_CUSTOM -> {
                    val isCustomBedtime =
                        BedtimeSettings(context).getBedtimeSettingsIntent() != null &&
                            (uiModeManager.nightModeCustomType ==
                                UiModeManager.MODE_NIGHT_CUSTOM_TYPE_BEDTIME)
                    if (isCustomBedtime) R.string.dark_ui_auto_mode_custom_bedtime
                    else R.string.dark_ui_auto_mode_custom
                }

                else -> R.string.dark_ui_auto_mode_never
            }
        ) as? T
    }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        when (value) {
            context.getString(R.string.dark_ui_auto_mode_never) -> {
                val isNightMode =
                    (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_YES) !=
                        0
                uiModeManager.nightMode =
                    if (isNightMode) UiModeManager.MODE_NIGHT_YES else UiModeManager.MODE_NIGHT_NO
            }
            context.getString(R.string.dark_ui_auto_mode_auto) -> {
                val locationManager = context.getSystemService(LocationManager::class.java)
                if (
                    locationManager?.isLocationEnabled == true &&
                        locationManager.lastLocation != null
                ) {
                    uiModeManager.nightMode = UiModeManager.MODE_NIGHT_AUTO
                }
            }
            context.getString(R.string.dark_ui_auto_mode_custom) -> {
                uiModeManager.nightMode = UiModeManager.MODE_NIGHT_CUSTOM
            }
            context.getString(R.string.dark_ui_auto_mode_custom_bedtime) -> {
                uiModeManager.nightModeCustomType = UiModeManager.MODE_NIGHT_CUSTOM_TYPE_BEDTIME
            }
        }
    }

    override fun onFirstObserverAdded() {
        settingsStore.addObserver(KEY, this, HandlerExecutor.main)
    }

    override fun onLastObserverRemoved() {
        settingsStore.removeObserver(KEY, this)
    }

    override fun onKeyChanged(key: String, reason: Int) {
        notifyChange(DataChangeReason.UPDATE)
    }

    companion object {
        const val KEY = Settings.Secure.UI_NIGHT_MODE

        fun getReadPermissions() = Permissions.EMPTY

        fun getWritePermissions() = Permissions.allOf(Manifest.permission.MODIFY_DAY_NIGHT_MODE)
    }
}
