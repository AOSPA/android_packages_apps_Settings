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

package com.android.settings.accessibility.detail.a11yservice

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.os.Build
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.HARDWARE
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.QUICK_SETTINGS
import com.android.settings.R
import com.android.settings.accessibility.PreferredShortcut
import com.android.settings.accessibility.PreferredShortcuts
import com.android.settings.accessibility.ShortcutPreference
import com.android.settings.accessibility.ToggleShortcutPreferenceController
import com.android.settings.accessibility.extensions.getFeatureName
import com.android.settings.accessibility.extensions.targetSdkIsAtLeast

class ShortcutPreferenceController(context: Context, prefKey: String) :
    ToggleShortcutPreferenceController(context, prefKey) {
    private var shortcutTitle: CharSequence? = null
    private var serviceInfo: AccessibilityServiceInfo? = null

    fun initialize(serviceInfo: AccessibilityServiceInfo) {
        super.initialize(serviceInfo.componentName)
        this.serviceInfo = serviceInfo
        shortcutTitle =
            mContext.getString(
                R.string.accessibility_shortcut_title,
                serviceInfo.getFeatureName(mContext),
            )
        setAllowedPreferredShortcutType()
    }

    override fun displayPreference(screen: PreferenceScreen?) {
        super.displayPreference(screen)
        shortcutTitle?.let { screen?.findPreference<Preference>(preferenceKey)?.title = it }
        shortcutPreference?.isSettingsEditable =
            serviceInfo?.targetSdkIsAtLeast(Build.VERSION_CODES.R) == true
    }

    override fun getDefaultShortcutTypes(): Int {
        val hasQsTile = serviceInfo?.tileServiceName?.isNotEmpty() == true
        val isAccessibilityTool = serviceInfo?.isAccessibilityTool == true
        return if (serviceInfo?.targetSdkIsAtLeast(Build.VERSION_CODES.R) == true) {
            if (isAccessibilityTool && hasQsTile) {
                QUICK_SETTINGS
            } else {
                super.getDefaultShortcutTypes()
            }
        } else {
            HARDWARE
        }
    }

    override fun onToggleClicked(preference: ShortcutPreference) {
        preference.preferenceManager.showDialog(preference)
    }

    private fun setAllowedPreferredShortcutType() {
        serviceInfo?.let {
            if (it.targetSdkIsAtLeast(Build.VERSION_CODES.R) != true) {
                PreferredShortcuts.saveUserShortcutType(
                    mContext,
                    PreferredShortcut(it.componentName.flattenToString(), HARDWARE),
                )
            }
        }
    }
}
