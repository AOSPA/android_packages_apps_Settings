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
package com.android.settings.supervision.ipc

import org.junit.Assert.assertEquals
import org.junit.Test

class PreferenceDataApiTest {

    private val api = PreferenceDataApi()

    @Test
    fun testRequestCodec() {
        val request = PreferenceDataRequest(listOf("key1", "key2"))
        val encoded = api.requestCodec.encode(request)
        val decoded = api.requestCodec.decode(encoded)
        assertEquals(request, decoded)
    }

    @Test
    fun testResponseCodec() {
        val response = mapOf(
            "key1" to PreferenceData(icon = 1, title = "Title 1", summary = "Summary 1"),
            "key2" to PreferenceData(title = "Title 2"),
            "key3" to PreferenceData(icon = 3, summary = "Summary 3"),
            "key4" to PreferenceData()
        )
        val encoded = api.responseCodec.encode(response)
        val decoded = api.responseCodec.decode(encoded)
        assertEquals(response, decoded)
    }

    @Test
    fun testRequestCodec_emptyList() {
        val request = PreferenceDataRequest(emptyList())
        val encoded = api.requestCodec.encode(request)
        val decoded = api.requestCodec.decode(encoded)
        assertEquals(request, decoded)
    }

    @Test
    fun testResponseCodec_emptyMap() {
        val response = emptyMap<String, PreferenceData>()
        val encoded = api.responseCodec.encode(response)
        val decoded = api.responseCodec.decode(encoded)
        assertEquals(response, decoded)
    }
}
