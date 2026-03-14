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
import com.android.settingslib.metadata.preferencesapi.types.FiniteOptionsType
import com.android.settingslib.notification.modes.ZenModesBackend

/** Provides a list of zen modes on the device. */
object ZenModes : FiniteOptionsType<String> {
    override fun getType(): Class<String> = String::class.java

    override fun getDescription(context: Context): String =
        context.getString(R.string.zen_mode_type_description)

    override suspend fun getOptions(context: Context): List<Pair<String, String>> =
        ZenModesBackend.getInstance(context).modes.map { mode -> mode.name to mode.name }

    override fun getKey(): String = "ZenModes"
}
