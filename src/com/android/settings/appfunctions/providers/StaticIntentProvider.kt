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

package com.android.settings.appfunctions.providers

import androidx.annotation.Keep
import com.android.settings.appfunctions.DeviceStateCategory
import com.android.settings.appfunctions.intents.getAccessibilityIntents
import com.android.settings.appfunctions.intents.getModesIntents
import com.android.settings.appfunctions.intents.getOtherIntents
import com.android.settings.appfunctions.intents.getSecurityIntents
import com.google.android.appfunctions.schema.common.v1.devicestate.PerScreenDeviceStates

/**
 * A simple data class to represent a static Intent URI + description.
 */
@Keep
data class StaticIntent(
    val description: String,
    val intentUri: String,
)

/**
 * A [DeviceStateProvider] that provides a static list of device states from a list of
 * [StaticIntent]s.
 *
 * This provider is useful for adding simple, non-dynamic states that consist only of a
 * description and an intent URI. It will only provide states if the [requestCategory]
 * matches the category this provider is configured with.
 *
 * @param staticIntents The list of [StaticIntent]s to provide.
 * @param category The [DeviceStateCategory] this provider is associated with.
 */
@Keep
class StaticIntentProvider(
    val staticIntents: List<StaticIntent>,
    private val category: DeviceStateCategory,
) : DeviceStateProvider {
    override suspend fun provide(requestCategory: DeviceStateCategory): DeviceStateProviderResult {
        if (requestCategory != category) {
            return DeviceStateProviderResult(emptyList())
        }

        val states = staticIntents.map {
            PerScreenDeviceStates(
                description = it.description,
                intentUri = it.intentUri,
                deviceStateItems = emptyList(),
            )
        }
        return DeviceStateProviderResult(states)
    }
}

fun getAllIntents(): List<StaticIntent> =
    getModesIntents() + getSecurityIntents() + getAccessibilityIntents() + getOtherIntents()
