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

package com.android.settings.accessibility.data

import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.AccessibilityShortcutInfo
import android.content.ComponentName
import android.content.Context
import android.os.UserHandle
import android.view.accessibility.AccessibilityManager

/** A repository that let the caller perform get/set for accessibility-related settings */
interface AccessibilityRepository {
    fun getAccessibilityShortcutInfo(componentName: ComponentName): AccessibilityShortcutInfo?

    fun getAccessibilityServiceInfo(componentName: ComponentName): AccessibilityServiceInfo?
}

private class AccessibilityRepositoryImpl(context: Context) : AccessibilityRepository {
    private val applicationContext: Context = context.applicationContext
    private val a11yManager: AccessibilityManager =
        applicationContext.getSystemService(AccessibilityManager::class.java)!!

    override fun getAccessibilityShortcutInfo(
        componentName: ComponentName
    ): AccessibilityShortcutInfo? {
        return a11yManager
            .getInstalledAccessibilityShortcutListAsUser(applicationContext, UserHandle.myUserId())
            .firstOrNull { shortcutInfo ->
                componentName == shortcutInfo.componentName
            }
    }

    override fun getAccessibilityServiceInfo(
        componentName: ComponentName
    ): AccessibilityServiceInfo? {
        return a11yManager.getInstalledAccessibilityServiceList().firstOrNull { serviceInfo ->
            componentName == serviceInfo.componentName
        }
    }
}

class AccessibilityRepositoryProvider {
    companion object {
        @Volatile private var instance: AccessibilityRepository? = null

        @JvmStatic
        fun get(context: Context): AccessibilityRepository =
            instance
                ?: synchronized(this) {
                    instance ?: AccessibilityRepositoryImpl(context).also { instance = it }
                }

        @JvmStatic
        fun resetInstanceForTesting() {
            instance = null
        }
    }
}
