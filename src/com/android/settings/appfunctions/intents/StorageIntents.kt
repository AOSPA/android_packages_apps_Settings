/*
 * Copyright 2025 The Android Open Source Project
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
package com.android.settings.appfunctions.intents

import android.os.storage.StorageManager
import com.android.settings.appfunctions.DeviceStateAppFunctionType.GET_STORAGE
import com.android.settings.appfunctions.providers.StaticIntent
import com.android.settings.appfunctions.providers.StaticIntents

fun getStorageIntents() =
    StaticIntents(
        GET_STORAGE,
        listOf(
            StaticIntent(
                description = "Free up space",
                intentUri = "intent:#Intent;action=${StorageManager.ACTION_MANAGE_STORAGE};end",
            )
        ),
    )
