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

@file:Suppress("ktlint:standard:filename")

package com.android.settings.accessibility.captionpreferences.ui

import android.content.Context
import androidx.preference.Preference
import com.android.settings.accessibility.ColorPreference

/** Helper for creating a [ColorPreference] widget. */
fun createColorWidget(context: Context, valuesRes: Int, titlesRes: Int): Preference =
    ColorPreference(context, null).apply {
        setTitles(context.resources.getStringArray(titlesRes))
        setValues(context.resources.getIntArray(valuesRes))
    }
