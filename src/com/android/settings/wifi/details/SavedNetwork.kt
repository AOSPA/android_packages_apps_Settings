/*
 * Copyright 2026 The Android Open Source Project
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

package com.android.settings.wifi.details

import android.content.Context
import com.android.settings.wifi.repository.SavedNetworkInfo
import com.android.settingslib.metadata.preferencesapi.types.FiniteOptionsType
import com.android.settingslib.metadata.preferencesapi.unsafe
import com.android.settingslib.metadata.preferencesapi.SafetyAnnotated
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settingslib.metadata.preferencesapi.types.EType

/** A saved network. */
// This is only open to allow the companion object to be created. Do not subclass.
open class SavedNetwork(
) : FiniteOptionsType<SavedNetworkInfo, String> {
    private val repository = featureFactory.wifiFeatureProvider.savedNetworkRepository

    override val externalType: EType<String> = EType.String

    override fun getDescription(context: Context): String = "A saved network"

    override fun getKey(): String = "SavedNetwork"

    override suspend fun getOptions(context: Context): List<Pair<SafetyAnnotated<String>, SafetyAnnotated<String>>> {
        return repository.fetchSavedNetworksInfo().map {
            it.lookupKey.unsafe() to it.ssid.unsafe()
        }.toList()
    }

    override fun convertInternalToExternal(internalValue: SavedNetworkInfo): String =
        internalValue.lookupKey
    override fun convertExternalToInternal(externalValue: String): SavedNetworkInfo =
        repository.findSavedNetworkInfo(externalValue) ?: error("Saved network info not found!")

    companion object : SavedNetwork()
}
