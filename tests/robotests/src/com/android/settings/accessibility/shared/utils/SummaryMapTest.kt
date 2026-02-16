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

package com.android.settings.accessibility.shared.utils

import android.content.Context
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class SummaryMapTest {
    private var resources: Resources = mock()
    private val context =
        spy(ApplicationProvider.getApplicationContext<Context>()) {
            on { resources } doReturn resources
        }

    private val valuesRes = 101
    private val entriesRes = 102

    @Test
    fun getSummary_returnsCorrectEntry() {
        val summaryMap = SummaryMap(valuesRes, entriesRes) { _, v -> v as Int }
        val values = intArrayOf(0, 1, 2)
        val entries = arrayOf("Zero", "One", "Two")

        resources.stub {
            on { getIntArray(valuesRes) } doReturn values
            on { getStringArray(entriesRes) } doReturn entries
        }

        assertThat(summaryMap.getSummary(context, 0)).isEqualTo("Zero")
        assertThat(summaryMap.getSummary(context, 1)).isEqualTo("One")
        assertThat(summaryMap.getSummary(context, 2)).isEqualTo("Two")
        assertThat(summaryMap.getSummary(context, 3)).isNull()
    }

    @Test
    fun getSummary_stringValues_returnsCorrectEntry() {
        val summaryMap =
            SummaryMap(valuesRes, entriesRes, useIntValues = false) { _, v -> v as String }

        val values = arrayOf("a", "b", "c")
        val entries = arrayOf("Alpha", "Beta", "Gamma")

        resources.stub {
            on { getStringArray(valuesRes) } doReturn values
            on { getStringArray(entriesRes) } doReturn entries
        }

        assertThat(summaryMap.getSummary(context, "a")).isEqualTo("Alpha")
        assertThat(summaryMap.getSummary(context, "b")).isEqualTo("Beta")
        assertThat(summaryMap.getSummary(context, "d")).isNull()
    }

    @Test
    fun getSummary_nullValue_returnsNull() {
        val summaryMap = SummaryMap(valuesRes, entriesRes) { _, v -> v as Int }
        assertThat(summaryMap.getSummary(context, null)).isNull()
    }
}
