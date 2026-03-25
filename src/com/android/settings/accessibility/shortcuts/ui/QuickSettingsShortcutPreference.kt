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

import android.content.ComponentName
import android.content.Context
import android.os.UserHandle
import android.service.quicksettings.TileService
import android.util.ArraySet
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener
import androidx.core.content.getSystemService
import androidx.preference.Preference
import com.android.internal.accessibility.common.ShortcutConstants.AccessibilityFragmentType
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType
import com.android.internal.accessibility.util.AccessibilityUtils
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityUtil
import com.android.settings.accessibility.extensions.isInSetupWizard
import com.android.settings.accessibility.shortcuts.ShortcutOptionPreference as ShortcutOptionWidget
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.utils.StringUtil

class QuickSettingsShortcutPreference(context: Context, targets: Set<String>) :
    ShortcutOptionPreference(context, UserShortcutType.QUICK_SETTINGS, targets),
    PreferenceSummaryProvider,
    PreferenceAvailabilityProvider,
    PreferenceLifecycleProvider {
    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.shortcut_quick_settings_pref_purpose

    override val title: Int
        get() = R.string.accessibility_shortcut_edit_dialog_title_quick_settings

    private var touchExplorationListener: TouchExplorationStateChangeListener? = null

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        if (preference is ShortcutOptionWidget) {
            preference.setIntroImageResId(R.drawable.accessibility_shortcut_type_quick_settings)
        }
    }

    override fun getSummary(context: Context): CharSequence? {
        val numFingers = if (AccessibilityUtil.isTouchExploreEnabled(context)) 2 else 1
        return StringUtil.getIcuPluralsString(
            context,
            numFingers,
            if (context.isInSetupWizard())
                R.string.accessibility_shortcut_edit_dialog_summary_quick_settings_suw
            else R.string.accessibility_shortcut_edit_dialog_summary_quick_settings,
        )
    }

    override val availabilityDescription =
        "The device must support Quick Settings. All shortcut targets must have a Quick Settings tile and must be pre-defined as supporting Quick Settings."

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context): Boolean {
        return TileService.isQuickSettingsSupported() &&
            allTargetsHasQsTile(context) &&
            allTargetsHasValidQsTileUseCase(context)
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        touchExplorationListener =
            TouchExplorationStateChangeListener { context.notifyPreferenceChange(key) }
                .also {
                    val a11yManager =
                        context.getSystemService<AccessibilityManager>() ?: return@also
                    a11yManager.addTouchExplorationStateChangeListener(it)
                }
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        super.onDestroy(context)
        touchExplorationListener?.let {
            val a11yManager = context.getSystemService<AccessibilityManager>() ?: return@let
            a11yManager.removeTouchExplorationStateChangeListener(it)
        }
    }

    private fun allTargetsHasQsTile(context: Context): Boolean {
        val accessibilityManager =
            context.getSystemService(AccessibilityManager::class.java) ?: return false

        val a11yFeatureToTileMap =
            accessibilityManager.getA11yFeatureToTileMap(UserHandle.myUserId())
        if (a11yFeatureToTileMap.isEmpty()) {
            return false
        }
        for (target in targets) {
            val targetComponentName = ComponentName.unflattenFromString(target)
            if (
                targetComponentName == null ||
                    !a11yFeatureToTileMap.containsKey(targetComponentName)
            ) {
                return false
            }
        }

        return true
    }

    /**
     * Returns true if all targets have valid QS Tile shortcut use case.
     *
     * Note: We don't want to promote the qs option in the edit shortcuts screen for a standard
     * AccessibilityService, because the Tile is provided by the owner of the AccessibilityService,
     * and they don't have control to enable the A11yService themselves which makes the TileService
     * not acting as the other a11y shortcut like FAB where the user can turn on/off the feature by
     * toggling the shortcut.
     *
     * A standard AccessibilityService normally won't create a TileService because the above
     * mentioned reason. In any case where the standard AccessibilityService provides a tile, we'll
     * hide it from the Setting's UI.
     */
    private fun allTargetsHasValidQsTileUseCase(context: Context): Boolean {
        val accessibilityManager =
            context.getSystemService(AccessibilityManager::class.java) ?: return false

        val installedServices = accessibilityManager.getInstalledAccessibilityServiceList()
        val standardA11yServices: MutableSet<String?> = ArraySet<String?>()
        for (serviceInfo in installedServices) {
            if (
                AccessibilityUtils.getAccessibilityServiceFragmentType(serviceInfo) !=
                    AccessibilityFragmentType.INVISIBLE_TOGGLE
            ) {
                standardA11yServices.add(serviceInfo!!.componentName.flattenToString())
            }
        }

        for (target in targets) {
            if (standardA11yServices.contains(target)) {
                return false
            }
        }

        return true
    }

    companion object {
        private const val KEY = "shortcut_quick_settings_pref"
    }
}
