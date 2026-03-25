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
package com.android.settings.supervision.ipc

import org.junit.Assert.assertEquals
import org.junit.Test

class IsSupervisorAccountApiTest {
    private val isSupervisorAccountApi = IsSupervisorAccountApi()

    @Test
    fun testRequestCodec_decodedRequestMatchesOriginalRequest() {
        val request = Unit
        val encoded = isSupervisorAccountApi.requestCodec.encode(request)
        val decoded = isSupervisorAccountApi.requestCodec.decode(encoded)
        assertEquals(request, decoded)
    }

    @Test
    fun testResponseCodec_decodedResponseMatchesOriginalResponse() {
        val response = true
        val encoded = isSupervisorAccountApi.responseCodec.encode(response)
        val decoded = isSupervisorAccountApi.responseCodec.decode(encoded)
        assertEquals(response, decoded)
    }
}