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

package com.android.settings.accessibility.shortcuts.ui

import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener
import androidx.preference.Preference
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityUtil
import com.android.settings.accessibility.extensions.isInSetupWizard
import com.android.settings.accessibility.shortcuts.ShortcutOptionPreference as ShortcutOptionWidget
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.utils.StringUtil

class GestureShortcutPreference(context: Context, targets: Set<String>) :
    ShortcutOptionPreference(context, UserShortcutType.GESTURE, targets),
    PreferenceSummaryProvider,
    PreferenceAvailabilityProvider,
    PreferenceLifecycleProvider {
    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.shortcut_gesture_pref_purpose

    override val title: Int
        get() = R.string.accessibility_shortcut_edit_dialog_title_software_by_gesture

    private var touchExplorationListener: TouchExplorationStateChangeListener? = null
    private var observer: KeyedObserver<String>? = null

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        if (preference is ShortcutOptionWidget) {
            val resId =
                if (AccessibilityUtil.isTouchExploreEnabled(preference.context))
                    R.drawable.accessibility_shortcut_type_gesture_touch_explore_on
                else R.drawable.accessibility_shortcut_type_gesture
            preference.setIntroImageResId(resId)
        }
    }

    override fun getSummary(context: Context): CharSequence? {
        val numFingers = if (AccessibilityUtil.isTouchExploreEnabled(context)) 3 else 2
        return StringUtil.getIcuPluralsString(
            context,
            numFingers,
            R.string.accessibility_shortcut_edit_dialog_summary_gesture,
        )
    }

    override val availabilityDescription =
        "The device must not be during setup, must be in gesture navigation mode, and must have a touch screen."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override fun isAvailable(context: Context): Boolean {
        return !context.isInSetupWizard() &&
            AccessibilityUtil.isGestureNavigateEnabled(context) &&
            AccessibilityUtil.isTouchShortcutAvailable(context)
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        touchExplorationListener =
            TouchExplorationStateChangeListener { context.notifyPreferenceChange(key) }
                .also {
                    val a11yManager =
                        context.getSystemService(AccessibilityManager::class.java) ?: return@also
                    a11yManager.addTouchExplorationStateChangeListener(it)
                }
        observer =
            KeyedObserver<String> { _, _ -> context.notifyPreferenceChange(key) }
                .also {
                    SettingsSecureStore.get(context)
                        .addObserver(
                            key = NAVIGATION_MODE_SETTING,
                            observer = it,
                            executor = HandlerExecutor.main,
                        )
                }
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        super.onDestroy(context)
        touchExplorationListener?.let {
            val a11yManager =
                context.getSystemService(AccessibilityManager::class.java) ?: return@let
            a11yManager.removeTouchExplorationStateChangeListener(it)
        }
        observer?.let {
            SettingsSecureStore.get(context).removeObserver(NAVIGATION_MODE_SETTING, it)
        }
    }

    companion object {
        private const val KEY = "shortcut_gesture_pref"
        private const val NAVIGATION_MODE_SETTING = Settings.Secure.NAVIGATION_MODE
    }
}
