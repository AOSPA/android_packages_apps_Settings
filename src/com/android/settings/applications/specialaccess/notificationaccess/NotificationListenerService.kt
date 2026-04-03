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

package com.android.settings.applications.specialaccess.notificationaccess

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.ApplicationInfoFlags
import android.multiuser.Flags
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import androidx.annotation.RequiresApi
import com.android.settings.accessibility.data.AccessibilityRepositoryProvider
import com.android.settingslib.metadata.R
import com.android.settingslib.metadata.preferencesapi.types.DirectFiniteOptionsType
import com.android.settingslib.metadata.preferencesapi.types.EType
import kotlinx.coroutines.flow.first
import com.android.settingslib.metadata.preferencesapi.safe

/**
 * The flattened string representation of a NotificationListenerService
 */
object NotificationListenerService : DirectFiniteOptionsType<String> {
    override val externalType: EType<String> = EType.String

    override fun getDescription(context: Context): String =
        "The flattened string representation of a NotificationListenerService"

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override suspend fun getOptions(context: Context) =
      AppsNotificationAccessScreen.loadNotificationListenerServices(context).map {
        val flattened = (it.packageName + "/" + it.name).safe()
        Pair(flattened, flattened)
      }.toList()

    override fun getKey() = "NotificationListenerService"
}