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

package com.android.settings.appfunctions

import android.content.Context
import android.content.Intent
import com.android.settingslib.graph.PreferenceGetterApiDescriptor
import com.android.settingslib.graph.PreferenceGetterRequest
import com.android.settingslib.graph.PreferenceGetterResponse
import com.android.settingslib.graph.PreferenceSetterApiDescriptor
import com.android.settingslib.graph.PreferenceSetterRequest
import com.android.settingslib.ipc.MessengerServiceClient
import com.android.settingslib.service.PREFERENCE_SERVICE_ACTION
import kotlinx.coroutines.Deferred

/** Manages the connection to the PreferenceService. */
class PreferenceServiceClient(context: Context) : MessengerServiceClient(context) {
    override val serviceIntentFactory = { Intent(PREFERENCE_SERVICE_ACTION) }

    /**
     * The id for get preference graph API. Corresponds to API_GET_PREFERENCE_GRAPH in SettingsLib
     * ServiceApiConstants.
     */
    private val API_GET_PREFERENCE_GRAPH = 1

    /**
     * The id for preference setter API. Corresponds to API_PREFERENCE_SETTER in SettingsLib
     * ServiceApiConstants.
     */
    private val API_PREFERENCE_SETTER = 2

    /**
     * The id for preference getter API. Corresponds to API_PREFERENCE_GETTER in SettingsLib
     * ServiceApiConstants.
     */
    private val API_PREFERENCE_GETTER = 3

    /** Invokes the preference getter API. */
    fun getPreferences(
        packageName: String,
        request: PreferenceGetterRequest,
    ): Deferred<PreferenceGetterResponse> {
        return invoke(packageName, PreferenceGetterApiDescriptor(API_PREFERENCE_GETTER), request)
    }

    /** Invokes the preference setter API. */
    fun setPreferenceValue(packageName: String, request: PreferenceSetterRequest): Deferred<Int> {
        return invoke(packageName, PreferenceSetterApiDescriptor(API_PREFERENCE_SETTER), request)
    }
}
