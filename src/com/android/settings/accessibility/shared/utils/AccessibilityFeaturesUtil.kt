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

package com.android.settings.accessibility.shared.utils

import android.content.ComponentName
import android.content.Context
import com.android.internal.accessibility.AccessibilityShortcutController.ACCESSIBILITY_HEARING_AIDS_COMPONENT_NAME
import com.android.internal.accessibility.AccessibilityShortcutController.AUTOCLICK_COMPONENT_NAME
import com.android.internal.accessibility.AccessibilityShortcutController.COLOR_INVERSION_COMPONENT_NAME
import com.android.internal.accessibility.AccessibilityShortcutController.DALTONIZER_COMPONENT_NAME
import com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME
import com.android.internal.accessibility.AccessibilityShortcutController.MOUSE_KEYS_COMPONENT_NAME
import com.android.internal.accessibility.AccessibilityShortcutController.ONE_HANDED_COMPONENT_NAME
import com.android.internal.accessibility.AccessibilityShortcutController.REDUCE_BRIGHT_COLORS_COMPONENT_NAME
import com.android.settings.R
import com.android.settings.accessibility.data.AccessibilityRepositoryProvider
import com.android.settings.accessibility.extensions.getFeatureName
import com.android.settingslib.R as SettingsLibR

/**
 * Returns the accessibility feature name for the given component name.
 *
 * @param context the context.
 * @param featureComponentName the flattened component name of the accessibility feature.
 */
fun getAccessibilityFeatureName(context: Context, featureComponentName: String): CharSequence? {
    return FRAMEWORK_ACCESSIBILITY_FEATURE_NAME[featureComponentName]?.let { context.getText(it) }
        ?: getA11yServiceFeatureName(context, featureComponentName)
        ?: getA11yActivityFeatureName(context, featureComponentName)
}

/**
 * Returns the accessibility service feature name for the given component name.
 *
 * @param context the context.
 * @param featureComponentName the flattened component name of the accessibility service.
 */
fun getA11yServiceFeatureName(context: Context, featureComponentName: String): CharSequence? {
    val a11yService = ComponentName.unflattenFromString(featureComponentName) ?: return null
    return AccessibilityRepositoryProvider.get(context)
        .getAccessibilityServiceInfo(a11yService)
        ?.getFeatureName(context)
}

/**
 * Returns the accessibility activity feature name for the given component name.
 *
 * @param context the context.
 * @param featureComponentName the flattened component name of the accessibility activity.
 */
fun getA11yActivityFeatureName(context: Context, featureComponentName: String): CharSequence? {
    val a11yActivity = ComponentName.unflattenFromString(featureComponentName) ?: return null
    return AccessibilityRepositoryProvider.get(context)
        .getAccessibilityShortcutInfo(a11yActivity)
        ?.activityInfo
        ?.loadLabel(context.packageManager)
}

/**
 * A map of framework accessibility feature component names to their corresponding string resource
 * IDs.
 */
val FRAMEWORK_ACCESSIBILITY_FEATURE_NAME =
    mapOf(
        ACCESSIBILITY_HEARING_AIDS_COMPONENT_NAME.flattenToString() to
            R.string.accessibility_hearingaid_title,
        COLOR_INVERSION_COMPONENT_NAME.flattenToString() to
            R.string.accessibility_display_inversion_preference_title,
        DALTONIZER_COMPONENT_NAME.flattenToString() to
            SettingsLibR.string.accessibility_display_daltonizer_preference_title,
        MAGNIFICATION_CONTROLLER_NAME to R.string.accessibility_screen_magnification_title,
        ONE_HANDED_COMPONENT_NAME.flattenToString() to R.string.one_handed_title,
        REDUCE_BRIGHT_COLORS_COMPONENT_NAME.flattenToString() to
            R.string.reduce_bright_colors_preference_title,
        AUTOCLICK_COMPONENT_NAME.flattenToString() to
            R.string.accessibility_autoclick_preference_title,
        MOUSE_KEYS_COMPONENT_NAME.flattenToString() to R.string.mouse_keys,
    )
