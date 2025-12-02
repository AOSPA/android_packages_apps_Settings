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
package com.android.settings.spa.accessibility

import android.content.pm.ApplicationInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ForceDarkAppExceptionsController(
    private val app: ApplicationInfo,
    private val repository: ForceDarkAppExceptionsRepository,
) {
    private val isForceDarkExceptionMutableStateFlow =
        MutableStateFlow<Boolean>(repository.isAppForceDarkAlwaysDisable(app))

    // Expose isForceDarkExceptionMutableStateFlow as read-only StateFlow
    val isException: StateFlow<Boolean> = isForceDarkExceptionMutableStateFlow.asStateFlow()

    fun setException(isException: Boolean) {
        if (repository.setIsAppForceDarkAlwaysDisable(app, isException)) {
            isForceDarkExceptionMutableStateFlow.value = isException
        }
    }
}
