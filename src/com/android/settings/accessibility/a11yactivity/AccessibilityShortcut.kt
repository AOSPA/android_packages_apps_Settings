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

package com.android.settings.accessibility.a11yactivity

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
import com.android.settingslib.metadata.preferencesapi.unsafe
import com.android.settingslib.metadata.preferencesapi.SafetyAnnotated

/**
 * The flattened string representation of the ComponentName of an Activity implementing an accessibility feature
 */
object AccessibilityShortcut : DirectFiniteOptionsType<String> {
    override val externalType: EType<String> = EType.String

    override fun getDescription(context: Context): String =
        "The flattened string representation of an Activity implementing an accessibility feature"

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override suspend fun getOptions(context: Context): List<Pair<SafetyAnnotated<String>, SafetyAnnotated<String>>> {
            return AccessibilityRepositoryProvider.get(context)
                    .accessibilityShortcutInfos
                    .first()
                    .map { a11yShortcutInfo ->
                      val componentName = a11yShortcutInfo.componentName.flattenToString().safe()
                      Pair(componentName, a11yShortcutInfo.loadIntro(context.packageManager)?.toString()?.unsafe() ?: componentName)
                    }.toList()
    }

    override fun getKey() = "AccessibilityShortcut"
}