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
@file:JvmName("ParameterStringArrayUtils")

package com.android.settings.accessibility.extensions

/**
 * Converts an array of strings into a single parameter string.
 *
 * The elements of the array are joined by a comma and the resulting string is enclosed in square
 * brackets. For example, `["a", "b", "c"]` becomes `"[a,b,c]"`.
 *
 * @return A single string representation of the array, formatted as `"[element1,element2,...]"`.
 */
fun Array<String>.toParameterString(): String {
    return "[${this.joinToString(",")}]"
}

/**
 * Converts a parameter string back into an array of strings.
 *
 * This is the reverse operation of [toParameterString]. It expects a string formatted as
 * `"[element1,element2,...]"`. An empty string or `[]` will result in an empty array.
 *
 * @return An array of strings parsed from the parameter string.
 */
fun String.toParameterStringArray(): Array<String> {
    if (this.isEmpty() || this == "[]") {
        return emptyArray()
    }

    return this.substring(1, this.length - 1).split(",").toTypedArray()
}
