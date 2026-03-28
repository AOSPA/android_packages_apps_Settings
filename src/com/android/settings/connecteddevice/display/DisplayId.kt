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

package com.android.settings.connecteddevice.display

import android.content.Context
import android.hardware.display.DisplayManager
import com.android.settingslib.metadata.R
import com.android.settingslib.metadata.preferencesapi.types.DirectFiniteOptionsType
import com.android.settingslib.metadata.preferencesapi.types.EType
import com.android.settingslib.metadata.preferencesapi.safe
import com.android.settingslib.metadata.preferencesapi.unsafe
import com.android.settingslib.metadata.preferencesapi.SafetyAnnotated

/**
 * A system display identifier.
 *
 * @param allowInternal Whether to include the internal display.
 * @param allowExternal Whether to include connected external displays.
 */
open class DisplayId(
    private val allowInternal: Boolean = true,
    private val allowExternal: Boolean = true,
) : DirectFiniteOptionsType<String> {

    override fun getDescription(context: Context): String =
        context.getString(R.string.display_id_type_description)

    override suspend fun getOptions(context: Context): List<Pair<SafetyAnnotated<String>, SafetyAnnotated<String>>> {
        val displayManager =
            context.getSystemService(DisplayManager::class.java) ?: return emptyList()
        val displayInjector = ConnectedDisplayInjector(context)

        return displayManager.displays
            .filter { display ->
                val isInternal = display.isInternal

                val isConnectedExternal = displayInjector.isConnectedDisplay(display)

                (isInternal && allowInternal) || (isConnectedExternal && allowExternal)
            }
            .map { display ->
                val isInternal = display.isInternal
                val description =
                    if (isInternal) {
                        "${display.name} (internal)"
                    } else {
                        "${display.name} (external)"
                    }

                display.displayId.toString().safe() to description.unsafe()
            }
    }

    override fun getKey(): String = when {
        allowInternal && allowExternal -> "DisplayId_InternalAndExternal"
        allowInternal -> "DisplayId_InternalOnly"
        allowExternal -> "DisplayId_ExternalOnly"
        else -> "DisplayId_NoDisplay"
    }

    override val externalType: EType<String> = EType.String

    companion object : DisplayId()
}
