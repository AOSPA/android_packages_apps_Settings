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

package com.android.settings.wifi.utils

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import com.google.android.material.textfield.TextInputLayout

/** A widget that wraps the relationship work between a TextInputLayout and an EditText. */
open class TextInputGroup(
    private val view: View,
    private val layoutId: Int,
    private val editTextId: Int,
) {

    private val View.layout: TextInputLayout?
        get() = findViewById(layoutId)

    private val View.editText: EditText?
        get() = findViewById(editTextId)

    private val textWatcher =
        object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                view.layout?.isErrorEnabled = false
            }
        }

    init {
        addTextChangedListener(textWatcher)
    }

    fun addTextChangedListener(watcher: TextWatcher) {
        view.editText?.addTextChangedListener(watcher)
    }

    fun getText(): String {
        return view.editText?.text?.toString() ?: ""
    }

    fun setText(text: String) {
        view.editText?.setText(text)
    }

    fun setError(errorMessage: String?) {
        view.layout?.apply { error = errorMessage }
    }
}
