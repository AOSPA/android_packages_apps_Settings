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

package com.android.settings.appfunctions.executors

import com.android.settings.appfunctions.PreferenceServiceClient
import com.android.settingslib.graph.PreferenceGetterRequest
import com.android.settingslib.graph.PreferenceGetterResponse
import com.android.settingslib.graph.PreferenceSetterRequest
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@Implements(PreferenceServiceClient::class)
class ShadowPreferenceServiceClient {
    companion object {
        var mockGetterResponse: PreferenceGetterResponse? = null
        var mockSetterResponse: Int? = null
        var getterException: Exception? = null
        var setterException: Exception? = null
        var lastRequest: Any? = null

        fun reset() {
            mockGetterResponse = null
            mockSetterResponse = null
            getterException = null
            setterException = null
            lastRequest = null
        }
    }

    @Implementation
    suspend fun getPreferences(request: PreferenceGetterRequest): PreferenceGetterResponse {
        getterException?.let { throw it }
        lastRequest = request
        return mockGetterResponse!!
    }

    @Implementation
    suspend fun setPreferenceValue(request: PreferenceSetterRequest): Int {
        setterException?.let { throw it }
        lastRequest = request
        return mockSetterResponse!!
    }

    @Implementation fun close() {}
}
