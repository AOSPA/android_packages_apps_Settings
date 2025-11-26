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

package com.android.settings.accessibility.extensions

import org.junit.Assert
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class ParameterStringArrayUtilsTest {
    @Test
    fun toParameterString_emptyArray_returnsEmptyBrackets() {
        val array = emptyArray<String>()
        Assert.assertEquals("[]", array.toParameterString())
    }

    @Test
    fun toParameterString_singleElement_returnsElementInBrackets() {
        val array = arrayOf("one")
        Assert.assertEquals("[one]", array.toParameterString())
    }

    @Test
    fun toParameterString_multipleElements_returnsCommaSeparatedStringInBrackets() {
        val array = arrayOf("one", "two", "three")
        Assert.assertEquals("[one,two,three]", array.toParameterString())
    }

    @Test
    fun toParameterString_arrayWithEmptyString_correctlyFormats() {
        val array = arrayOf("one", "", "three")
        Assert.assertEquals("[one,,three]", array.toParameterString())
    }

    @Test
    fun toParameterStringArray_emptyString_returnsEmptyArray() {
        val str = ""
        assertArrayEquals(emptyArray<String>(), str.toParameterStringArray())
    }

    @Test
    fun toParameterStringArray_emptyBrackets_returnsEmptyArray() {
        val str = "[]"
        assertArrayEquals(emptyArray<String>(), str.toParameterStringArray())
    }

    @Test
    fun toParameterStringArray_singleElement_returnsArrayWithOneElement() {
        val str = "[one]"
        assertArrayEquals(arrayOf("one"), str.toParameterStringArray())
    }

    @Test
    fun toParameterStringArray_multipleElements_returnsArrayWithMultipleElements() {
        val str = "[one,two,three]"
        assertArrayEquals(arrayOf("one", "two", "three"), str.toParameterStringArray())
    }

    @Test
    fun toParameterStringArray_stringWithEmptyElement_returnsArrayWithEmptyString() {
        val str = "[one,,three]"
        assertArrayEquals(arrayOf("one", "", "three"), str.toParameterStringArray())
    }
}
