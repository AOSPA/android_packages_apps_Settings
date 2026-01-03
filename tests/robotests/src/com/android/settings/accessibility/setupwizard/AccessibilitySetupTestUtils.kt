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

package com.android.settings.accessibility.setupwizard

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.ColorDrawable
import com.android.settings.testutils.AccessibilityTestUtils
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.spy
import org.mockito.kotlin.whenever

/** Utility for creating mocked Accessibility Services for Setup Wizard tests. */
fun createMockServiceInfo(
    context: Context,
    componentName: ComponentName,
    label: String,
    description: String,
    isAlwaysOnService: Boolean = false,
): AccessibilityServiceInfo {
    return spy(
            AccessibilityTestUtils.createAccessibilityServiceInfo(
                context,
                componentName,
                isAlwaysOnService,
            )
        )
        .apply {
            isAccessibilityTool = true
            doReturn(description).whenever(this).loadDescription(any())
            resolveInfo =
                resolveInfo?.let {
                    spy(it).apply {
                        doReturn(label).whenever(this).loadLabel(any())
                        doReturn(ColorDrawable(0)).whenever(this).loadIcon(any())
                    }
                }
        }
}
