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
import android.provider.Settings
import androidx.preference.Preference
import com.android.internal.accessibility.AccessibilityShortcutController.COLOR_INVERSION_COMPONENT_NAME
import com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType
import com.android.internal.accessibility.util.ShortcutUtils
import com.android.server.accessibility.Flags
import com.android.settings.R
import com.android.settings.accessibility.shared.utils.getAccessibilityFeatureName
import com.android.settings.accessibility.shortcuts.ShortcutOptionPreference as ShortcutOptionWidget
import com.android.settings.inputmethod.InputPeripheralsSettingsUtils
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider

class KeyboardShortcutPreference(context: Context, targets: Set<String>) :
    ShortcutOptionPreference(context, UserShortcutType.KEY_GESTURE, targets),
    PreferenceSummaryProvider,
    PreferenceAvailabilityProvider,
    PreferenceLifecycleProvider {
    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.shortcut_keyboard_pref_purpose

    override val title: Int
        get() = R.string.accessibility_shortcut_edit_dialog_title_keyboard

    private var observer: KeyedObserver<String>? = null

    override fun getSummary(context: Context): CharSequence? {
        if (targets.size > 1) {
            return null
        }

        val targetName = targets.single()

        val shortcutKeyCode: String? = ShortcutUtils.getKeyCodeLabelFromTarget(context, targetName)
        val shortcutFeatureName: CharSequence? = getAccessibilityFeatureName(context, targetName)

        val meta: String? = context.getString(R.string.modifier_keys_meta)
        val alt: String? = context.getString(R.string.modifier_keys_alt)

        return context.getString(
            R.string.accessibility_shortcut_edit_dialog_summary_keyboard,
            meta,
            alt,
            shortcutKeyCode,
            shortcutFeatureName,
        )
    }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)

        if (targets.size > 1 || preference !is ShortcutOptionWidget) {
            return
        }

        val imageResId = getIconResId(context)
        if (imageResId != 0) {
            preference.setIntroImageResId(imageResId)
        }
    }

    override val availabilityDescription =
        "There must be a keyboard connected. There must be exactly one target configured for this shortcut. The device must support Key gesture shortcut settings. The target must be Magnification, Voice Access, TalkBack, Color Inversion, or Select to Speak. "

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override fun isAvailable(context: Context): Boolean {
        if (
            !Flags.enableKeyGestureShortcutSettings() ||
                targets.size > 1 ||
                !InputPeripheralsSettingsUtils.isHardKeyboard()
        ) {
            return false
        }

        // Keyboard shortcut is only currently available for Magnification, Voice Access, TalkBack,
        // and Select to Speak.
        return targetsContainsMagnification() ||
            targetsContainsVoiceAccess(context) ||
            targetsContainsScreenReader(context) ||
            (com.android.hardware.input.Flags.enableColorInversionKeyGestures() &&
                targetsContainsColorInversion()) ||
            (com.android.hardware.input.Flags.enableSelectToSpeakKeyGestures() &&
                targetsContainsSelectToSpeak(context))
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        observer =
            KeyedObserver<String> { _, _ -> context.notifyPreferenceChange(key) }
                .also {
                    SettingsSecureStore.get(context)
                        .addObserver(
                            key = KEYBOARD_SHORTCUT_SETTING,
                            observer = it,
                            executor = HandlerExecutor.main,
                        )
                }
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        super.onDestroy(context)
        observer?.let {
            SettingsSecureStore.get(context).removeObserver(KEYBOARD_SHORTCUT_SETTING, it)
        }
    }

    fun getIconResId(context: Context): Int =
        when {
            targets.size > 1 -> 0
            targetsContainsColorInversion() ->
                R.drawable.accessibility_shortcut_type_keyboard_colorinversion
            targetsContainsMagnification() ->
                R.drawable.accessibility_shortcut_type_keyboard_magnification
            targetsContainsScreenReader(context) ->
                R.drawable.accessibility_shortcut_type_keyboard_screenreader
            com.android.hardware.input.Flags.enableSelectToSpeakKeyGestures() &&
                targetsContainsSelectToSpeak(context) ->
                R.drawable.accessibility_shortcut_type_keyboard_selecttospeak
            targetsContainsVoiceAccess(context) ->
                R.drawable.accessibility_shortcut_type_keyboard_voiceaccess
            else -> 0
        }

    private fun targetsContainsColorInversion(): Boolean {
        return targets.contains(COLOR_INVERSION_COMPONENT_NAME.flattenToString())
    }

    private fun targetsContainsMagnification(): Boolean {
        return targets.contains(MAGNIFICATION_CONTROLLER_NAME)
    }

    private fun targetsContainsScreenReader(context: Context): Boolean {
        val screenReaderTargetName = ShortcutUtils.getScreenReaderTargetName(context)
        val screenReaderComponentName = ComponentName.unflattenFromString(screenReaderTargetName)

        return targets.contains(screenReaderTargetName) ||
            (screenReaderComponentName != null &&
                targets.contains(screenReaderComponentName.flattenToString()))
    }

    private fun targetsContainsSelectToSpeak(context: Context): Boolean {
        val selectToSpeakTargetName = ShortcutUtils.getSelectToSpeakTargetName(context)
        return targets.contains(selectToSpeakTargetName)
    }

    private fun targetsContainsVoiceAccess(context: Context): Boolean {
        val voiceAccessTargetName = ShortcutUtils.getVoiceAccessTargetName(context)
        return targets.contains(voiceAccessTargetName)
    }

    companion object {
        private const val KEY = "shortcut_keyboard_pref"
        private const val KEYBOARD_SHORTCUT_SETTING =
            Settings.Secure.ACCESSIBILITY_KEY_GESTURE_TARGETS
    }
}
