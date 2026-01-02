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

package com.android.settings.testutils

import com.google.android.material.slider.Slider
import org.robolectric.util.ReflectionHelpers

/**
 * Sets the value of the Slider and triggers the OnChangeListener callbacks as if the user
 * interacted with it.
 *
 * This method uses reflection to access private fields of the Slider.
 */
fun Slider.setValueFromUser(newValue: Float) {
    val listeners =
        ReflectionHelpers.getField<List<Slider.OnChangeListener>>(this, "changeListeners")
    if (value != newValue) {
        value = newValue
        listeners.forEach { listener ->
            listener.onValueChange(/* slider= */ this, newValue, /* fromUser= */ true)
        }
    }
}
