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

package com.android.settings.testutils.shadow

import android.content.Context
import com.google.android.setupdesign.util.ThemeHelper
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.Resetter

/** Shadow for [ThemeHelper] to control GLIF expressive style states. */
@Implements(ThemeHelper::class)
class ShadowThemeHelper {

    companion object {
        private var expressiveStyle = false

        @JvmStatic
        @Implementation
        fun shouldApplyGlifExpressiveStyle(context: Context): Boolean = expressiveStyle

        /** Sets whether the expressive style should be applied. */
        @JvmStatic
        fun setShouldApplyGlifExpressiveStyle(shouldApply: Boolean) {
            expressiveStyle = shouldApply
        }

        /** Resets the shadow state. */
        @JvmStatic
        @Resetter
        fun reset() {
            expressiveStyle = false
        }
    }
}
