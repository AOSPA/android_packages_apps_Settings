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

package com.android.settings.notification.modes

import android.content.Context
import com.android.settings.R
import com.android.settingslib.metadata.preferencesapi.types.DirectFiniteOptionsType
import com.android.settingslib.notification.modes.ZenModesBackend
import com.android.settingslib.metadata.preferencesapi.unsafe
import com.android.settingslib.metadata.preferencesapi.types.EType

/** Provides a list of zen modes on the device. */
object ZenModes : DirectFiniteOptionsType<String> {
    override val externalType = EType.String

    override fun getDescription(context: Context): String =
        context.getString(R.string.zen_mode_type_description)

    override suspend fun getOptions(context: Context) =
        ZenModesBackend.getInstance(context).modes.map { mode -> mode.name.unsafe() to mode.name.unsafe() }

    override fun getKey(): String = "ZenModes"
}
