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

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.android.internal.accessibility.AccessibilityShortcutController.ACCESSIBILITY_HEARING_AIDS_COMPONENT_NAME
import com.android.internal.accessibility.AccessibilityShortcutController.AUTOCLICK_COMPONENT_NAME
import com.android.internal.accessibility.AccessibilityShortcutController.COLOR_INVERSION_COMPONENT_NAME
import com.android.internal.accessibility.AccessibilityShortcutController.DALTONIZER_COMPONENT_NAME
import com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME
import com.android.internal.accessibility.AccessibilityShortcutController.MOUSE_KEYS_COMPONENT_NAME
import com.android.internal.accessibility.AccessibilityShortcutController.ONE_HANDED_COMPONENT_NAME
import com.android.internal.accessibility.AccessibilityShortcutController.REDUCE_BRIGHT_COLORS_COMPONENT_NAME
import com.android.settings.R
import com.android.settings.accessibility.Flags
import com.android.settings.accessibility.PreferredShortcuts
import com.android.settings.accessibility.extensions.toParameterStringArray
import com.android.settings.accessibility.shared.utils.getA11yActivityFeatureName
import com.android.settings.accessibility.shared.utils.getA11yServiceFeatureName
import com.android.settings.accessibility.shortcuts.EditShortcutsPreferenceFragment
import com.android.settings.core.PreferenceScreenMixin
import com.android.settingslib.metadata.CatalystFlagProviderFactory
import com.android.settingslib.metadata.KeyParametersSchema
import com.android.settingslib.metadata.ParameterizedPreferenceScreenArgumentsFactory
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.metadata.ValidatedKeyParameters
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.metadata.preferencesapi.types.AnyString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_UNCATEGORIZED

// TODO(b/440383851): Figure out if this screen can be merged with
// [AccessibilityShortcutPreference] and its subclasses.
// If we do, make sure we don't accidentally allow outsider to change individual shortcuts.
@ProvidePreferenceScreen(EditShortcutsScreen.KEY, parameterized = true)
open class EditShortcutsScreen
private constructor(
    @Deprecated(
        "This property will be removed once the catalyst framework stops passing the arguments as a bundle. Use the keyParameters instead."
    )
    final override val arguments: Bundle?,
    final override val keyParameters: ValidatedKeyParameters?,
) : PreferenceScreenMixin, PreferenceLifecycleProvider {
    // Tagged as UI-ONLY since due to it having no parameters, it is not meaningful for now for
    // graph callers.
    override fun tags(context: Context) = arrayOf(APP_FUNCTION_UNCATEGORIZED, UI_ONLY_PREFERENCE)

    private val shortcutTargets: Set<String> by lazy {
        if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
            // TODO(b/440383851): understand whether we should provide parameters to any caller and
            // retrieve the shortcutTargets from keyParameters
            keyParameters
                ?.get(EditShortcutsPreferenceFragment.ARG_KEY_SHORTCUT_TARGETS)
                ?.toParameterStringArray()
                ?.toSet() ?: emptySet()
        } else {
            arguments
                ?.getStringArray(EditShortcutsPreferenceFragment.ARG_KEY_SHORTCUT_TARGETS)
                ?.toSet() ?: emptySet()
        }
    }

    @Deprecated(
        "This constructor will be removed once the catalyst framework stops passing the arguments as a bundle. Use the other constructor instead."
    )
    constructor(args: Bundle) : this(args, null)

    constructor(keyParameters: ValidatedKeyParameters) : this(null, keyParameters)

    override fun getScreenTitle(context: Context): CharSequence? {
        if (shortcutTargets.size == 1) {
            val target = shortcutTargets.first()
            return getFrameworkFeatureShortcutTitle(context, target)
                ?: getA11yServiceShortcutTitle(context, target)
                ?: getA11yActivityShortcutTitle(context, target)
        }
        return context.getString(R.string.accessibility_shortcut_edit_screen_title)
    }

    private fun getFrameworkFeatureShortcutTitle(
        context: Context,
        featureComponentName: String,
    ): CharSequence? {
        return FEATURE_SHORTCUT_TITLE_MAP[featureComponentName]?.let { context.getString(it) }
    }

    private fun getA11yServiceShortcutTitle(
        context: Context,
        a11yServiceComponentName: String,
    ): CharSequence? {
        val a11yServiceLabel =
            getA11yServiceFeatureName(context, a11yServiceComponentName) ?: return null
        return context.getString(R.string.accessibility_shortcut_title, a11yServiceLabel)
    }

    private fun getA11yActivityShortcutTitle(
        context: Context,
        a11yActivityComponentName: String,
    ): CharSequence? {
        val a11yActivityLabel =
            getA11yActivityFeatureName(context, a11yActivityComponentName) ?: return null
        return context.getString(R.string.accessibility_shortcut_title, a11yActivityLabel)
    }

    override val highlightMenuKey: Int
        get() = R.string.menu_key_accessibility

    override fun isFlagEnabled(context: Context): Boolean {
        return Flags.catalystEditShortcuts()
    }

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?): Intent? {
        // TODO(b/331487112): Allow deep linking into edit shortcuts screen once we have settled on
        // the user flow regarding permission dialogs.
        return null
    }

    override fun onResume(context: PreferenceLifecycleContext) {
        super.onResume(context)
        PreferredShortcuts.updatePreferredShortcutsFromSettings(context, shortcutTargets)
    }

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            if (shortcutTargets.isEmpty()) return@preferenceHierarchy
            val advancedPreference = AdvancedPreference(shortcutTargets)

            // LINT.IfChange(shortcut_type_ui_order)
            +IntroPreference(shortcutTargets)
            +KeyboardShortcutPreference(context, shortcutTargets)
            +QuickSettingsShortcutPreference(context, shortcutTargets)
            +FloatingButtonShortcutPreference(context, shortcutTargets)
            +GestureShortcutPreference(context, shortcutTargets)
            +NavButtonShortcutPreference(context, shortcutTargets)
            +VolumeKeysShortcutPreference(context, shortcutTargets)
            +TopRowKeyShortcutPreference(context, shortcutTargets)
            +advancedPreference
            +TripleTapShortcutPreference(
                context,
                shortcutTargets,
                expandableStateProvider = { advancedPreference.expandableState },
            )
            // LINT.ThenChange(/src/com/android/settings/accessibility/AccessibilityUtil.java:shortcut_type_ui_order)
        }

    override fun getMetricsCategory(): Int {
        return SettingsEnums.DIALOG_ACCESSIBILITY_SERVICE_EDIT_SHORTCUT
    }

    override fun fragmentClass(): Class<out Fragment>? {
        return EditShortcutsPreferenceFragment::class.java
    }

    override val key: String
        get() = KEY

    // TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.edit_shortcuts_screen_purpose

    companion object : ParameterizedPreferenceScreenArgumentsFactory {
        const val KEY = "edit_shortcuts_screen"

        @JvmStatic
        // TODO(b/440383851): understand whether we should provide parameters to any caller and
        // update the parameters schema
        override val parametersSchema = KeyParametersSchema {
            parameter(
                EditShortcutsPreferenceFragment.ARG_KEY_SHORTCUT_TARGETS,
                "A string array of targets",
                required = true,
                type = AnyString, // TODO(scottjonathan):Is there a better type to use here
            )
        }

        @JvmStatic
        override fun keyParameters(context: Context): Flow<ValidatedKeyParameters> {
            // TODO (b/457649430): when the catalyst framework stops passing the arguments as a
            // bundle: replace the parameters(context) call to the actual implementation,
            // or make this function the primary implementation and the legacy parameters() should
            // call this one.
            return parameters(context).map { bundle -> parametersSchema.prepare(bundle) }
        }

        @Deprecated(
            "This method will be removed once the catalyst framework stops passing the arguments as a bundle. Use keyParameters instead."
        )
        @OptIn(ExperimentalCoroutinesApi::class)
        @JvmStatic
        fun parameters(context: Context): Flow<Bundle> {
            // TODO(b/440383851): understand whether we should provide parameters to any caller
            return emptyFlow()
        }

        val FEATURE_SHORTCUT_TITLE_MAP: Map<String, Int> =
            mapOf(
                ACCESSIBILITY_HEARING_AIDS_COMPONENT_NAME.flattenToString() to
                    R.string.accessibility_hearing_device_shortcut_title,
                COLOR_INVERSION_COMPONENT_NAME.flattenToString() to
                    R.string.accessibility_display_inversion_shortcut_title,
                DALTONIZER_COMPONENT_NAME.flattenToString() to
                    R.string.accessibility_daltonizer_shortcut_title,
                MAGNIFICATION_CONTROLLER_NAME to
                    R.string.accessibility_screen_magnification_shortcut_title,
                ONE_HANDED_COMPONENT_NAME.flattenToString() to
                    R.string.one_handed_mode_shortcut_title,
                REDUCE_BRIGHT_COLORS_COMPONENT_NAME.flattenToString() to
                    R.string.reduce_bright_colors_shortcut_title,
                AUTOCLICK_COMPONENT_NAME.flattenToString() to
                    R.string.accessibility_autoclick_shortcut_title,
                MOUSE_KEYS_COMPONENT_NAME.flattenToString() to
                    R.string.accessibility_mouse_keys_shortcut_title,
            )
    }
}
