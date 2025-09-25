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

package com.android.settings.appfunctions.metadataprovidersources

import android.content.Context
import androidx.annotation.Keep
import com.android.settings.appfunctions.stateprovidersources.SharedDeviceStateData
import com.google.android.appfunctions.schema.common.v1.devicestate.PerScreenMetadata

/** Retrieves a specific aspect of the device's state metadata directly from Android APIs. */
@Keep
interface DeviceStateMetadataSource {

    /**
     * Retrieves the [PerScreenMetadata] for the current context.
     *
     * @param context The Android [android.content.Context] which might be needed to access system
     *   services or resources.
     * @param sharedDeviceStateData Data shared by multiple [DeviceStateMetadataSource]s, which is
     *   computed lazily.
     * @return A [PerScreenMetadata] object.
     */
    suspend fun get(
        context: Context,
        sharedDeviceStateData: SharedDeviceStateData,
    ): List<PerScreenMetadata>
}
