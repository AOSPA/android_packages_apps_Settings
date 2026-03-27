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
package com.android.settings.datetime

import android.Manifest.permission.MANAGE_TIME_AND_ZONE_DETECTION
import android.app.time.TimeConfiguration
import android.app.time.TimeManager
import android.app.time.UnixEpochTime
import android.os.SystemClock
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.InvalidPreference
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean
import com.android.settingslib.metadata.preferencesapi.types.Date
import com.android.settingslib.metadata.preferencesapi.types.TimeOfDay

// LINT.IfChange
@ProvidePreferenceScreen(DateTimeSettingsApiScreen.KEY)
class DateTimeSettingsApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.SYSTEM,
        purpose = R.string.date_time_settings_purpose,
        fragment = DateTimeSettings::class,
        alreadyPartiallyMigrated = DateTimeSettingsScreen::class,
    ) {

    init {
        flag { Flags.catalystMigration26q2() }

        buildAutoTimePreferences()
        buildManualDateTimePreferences()
    }

    private fun buildAutoTimePreferences() {
        preference(
            key = AUTO_TIME_KEY,
            type = AnyBoolean,
            purpose = R.string.auto_date_time_purpose,
        ) {
            get {
                permissions(MANAGE_TIME_AND_ZONE_DETECTION)
                execute {
                    val timeManager =
                        context.getSystemService(TimeManager::class.java)
                            ?: error(context.getString(R.string.time_manager_service_not_available))
                    timeManager.timeCapabilitiesAndConfig.configuration.isAutoDetectionEnabled
                }
            }
            set {
                permissions(MANAGE_TIME_AND_ZONE_DETECTION)
                execute { value ->
                    val timeManager =
                        context.getSystemService(TimeManager::class.java)
                            ?: error(context.getString(R.string.time_manager_service_not_available))

                    val newTimeConfig =
                        TimeConfiguration.Builder().setAutoDetectionEnabled(value).build()

                    val success = timeManager.updateTimeConfiguration(newTimeConfig)
                    if (!success) {
                        error("TimeManager.updateTimeConfiguration() failed")
                    }
                }
            }
        }
    }

    private fun buildManualDateTimePreferences() {
        preference(key = DATE_KEY, type = Date, purpose = R.string.manual_date_purpose) {
            permissions(MANAGE_TIME_AND_ZONE_DETECTION)

            get {
                execute {
                    val timeManager =
                        context.getSystemService(TimeManager::class.java)
                            ?: error(context.getString(R.string.time_manager_service_not_available))
                    val currentInstant =
                        java.time.Instant.ofEpochMilli(
                            timeManager.timeState.unixEpochTime.unixEpochTimeMillis
                        )
                    val zoneId = java.time.ZoneId.of(timeManager.timeZoneState.id)
                    currentInstant.atZone(zoneId).toLocalDate()
                }
            }
            set {
                preconditions(R.string.manual_date_precondition) {
                    val timeManager =
                        context.getSystemService(TimeManager::class.java)
                            ?: error(context.getString(R.string.time_manager_service_not_available))
                    if (
                        timeManager.timeCapabilitiesAndConfig.configuration.isAutoDetectionEnabled
                    ) {
                        InvalidPreference(
                            KEY,
                            AUTO_TIME_KEY,
                            context.getString(R.string.manual_date_precondition),
                        )
                    } else {
                        Allowed
                    }
                }
                execute { value ->
                    val timeManager =
                        context.getSystemService(TimeManager::class.java)
                            ?: error(context.getString(R.string.time_manager_service_not_available))

                    val timeZoneState = timeManager.timeZoneState
                    val currentInstant =
                        java.time.Instant.ofEpochMilli(
                            timeManager.timeState.unixEpochTime.unixEpochTimeMillis
                        )
                    val zoneId = java.time.ZoneId.of(timeZoneState.id)
                    val currentZonedDateTime = currentInstant.atZone(zoneId)
                    val newZonedDateTime =
                        currentZonedDateTime
                            .withYear(value.year)
                            .withMonth(value.monthValue)
                            .withDayOfMonth(value.dayOfMonth)
                    val timeMillis = newZonedDateTime.toInstant().toEpochMilli()

                    val unixEpochTime =
                        UnixEpochTime(
                            /* elapsedRealtimeMillis= */ SystemClock.elapsedRealtime(),
                            /* unixEpochTimeMillis= */ timeMillis,
                        )

                    val success = timeManager.setManualTime(unixEpochTime)
                    if (success) {
                        timeManager.confirmTime(unixEpochTime)
                    } else {
                        error("TimeManager.setManualTime() failed")
                    }
                }
            }
        }

        preference(key = TIME_KEY, type = TimeOfDay, purpose = R.string.manual_time_purpose) {
            permissions(MANAGE_TIME_AND_ZONE_DETECTION)

            get {
                execute {
                    val timeManager =
                        context.getSystemService(TimeManager::class.java)
                            ?: error(context.getString(R.string.time_manager_service_not_available))
                    val currentInstant =
                        java.time.Instant.ofEpochMilli(
                            timeManager.timeState.unixEpochTime.unixEpochTimeMillis
                        )
                    val zoneId = java.time.ZoneId.of(timeManager.timeZoneState.id)
                    currentInstant.atZone(zoneId).toLocalTime().withSecond(0).withNano(0)
                }
            }
            set {
                permissions(MANAGE_TIME_AND_ZONE_DETECTION)
                preconditions(R.string.manual_time_precondition) {
                    val timeManager =
                        context.getSystemService(TimeManager::class.java)
                            ?: error(context.getString(R.string.time_manager_service_not_available))
                    if (
                        timeManager.timeCapabilitiesAndConfig.configuration.isAutoDetectionEnabled
                    ) {
                        InvalidPreference(
                            KEY,
                            AUTO_TIME_KEY,
                            context.getString(R.string.manual_time_precondition),
                        )
                    } else {
                        Allowed
                    }
                }

                execute { value ->
                    val timeManager =
                        context.getSystemService(TimeManager::class.java)
                            ?: error(context.getString(R.string.time_manager_service_not_available))

                    val timeZoneState = timeManager.timeZoneState
                    val currentInstant =
                        java.time.Instant.ofEpochMilli(
                            timeManager.timeState.unixEpochTime.unixEpochTimeMillis
                        )
                    val zoneId = java.time.ZoneId.of(timeZoneState.id)
                    val currentZonedDateTime = currentInstant.atZone(zoneId)
                    val newZonedDateTime =
                        currentZonedDateTime
                            .withHour(value.hour)
                            .withMinute(value.minute)
                            .withSecond(0)
                            .withNano(0)
                    val timeMillis = newZonedDateTime.toInstant().toEpochMilli()

                    val unixEpochTime =
                        UnixEpochTime(
                            /* elapsedRealtimeMillis= */ SystemClock.elapsedRealtime(),
                            /* unixEpochTimeMillis= */ timeMillis,
                        )

                    val success = timeManager.setManualTime(unixEpochTime)
                    if (success) {
                        timeManager.confirmTime(unixEpochTime)
                    } else {
                        error("TimeManager.setManualTime() failed")
                    }
                }
            }
        }
    }

    companion object {
        const val KEY = "api_date_time_settings"
        const val AUTO_TIME_KEY = "auto_time"
        const val DATE_KEY = "date"
        const val TIME_KEY = "time"
    }
}
// LINT.ThenChange(DateTimeSettings.java, DateTimeSettingsScreen.kt)
