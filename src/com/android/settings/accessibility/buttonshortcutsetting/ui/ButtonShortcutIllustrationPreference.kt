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

package com.android.settings.accessibility.buttonshortcutsetting.ui

import android.content.Context
import android.provider.Settings
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityUtil
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.IllustrationPreference as IllustrationWidget

/** Preference to display an illustration of the accessibility button shortcut. */
class ButtonShortcutIllustrationPreference :
    PreferenceMetadata, PreferenceBinding, PreferenceLifecycleProvider {
    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.animated_image_purpose

    override val indexable
        get() = false

    private var buttonSettingsObserver: KeyedObserver<String>? = null

    override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)

    override fun createWidget(context: Context) =
        IllustrationWidget(context).apply { isSelectable = false }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        val illustrationWidget = preference as? IllustrationWidget ?: return
        val context = illustrationWidget.context
        val secureStore = SettingsSecureStore.get(context)

        illustrationWidget.imageDrawable =
            if (AccessibilityUtil.isFloatingMenuEnabled(context)) {
                secureStore.setDefaultValue(
                    Settings.Secure.ACCESSIBILITY_FLOATING_MENU_OPACITY,
                    DEFAULT_OPACITY,
                )

                val size = secureStore.getInt(Settings.Secure.ACCESSIBILITY_FLOATING_MENU_SIZE)
                // The alpha range when set on a drawable is [0-255]
                val opacity: Int =
                    ((secureStore.getFloat(Settings.Secure.ACCESSIBILITY_FLOATING_MENU_OPACITY)
                            ?: DEFAULT_OPACITY) * 255)
                        .toInt()

                val drawableId =
                    if (size == SMALL_SIZE) {
                        R.drawable.accessibility_shortcut_type_fab_size_small_preview
                    } else {
                        R.drawable.accessibility_shortcut_type_fab_size_large_preview
                    }
                context.getDrawable(drawableId)?.apply { alpha = opacity }
            } else {
                context.getDrawable(R.drawable.accessibility_shortcut_type_navbar_preview)
            }
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        val secureStore = SettingsSecureStore.get(context)
        buttonSettingsObserver = KeyedObserver { _, _ -> context.notifyPreferenceChange(key) }

        buttonSettingsObserver?.let {
            RELATED_SETTINGS_KEY.forEach { settingKey ->
                secureStore.addObserver(settingKey, observer = it, executor = HandlerExecutor.main)
            }
        }
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        super.onDestroy(context)
        val secureStore = SettingsSecureStore.get(context)
        buttonSettingsObserver?.let {
            RELATED_SETTINGS_KEY.forEach { settingKey ->
                secureStore.removeObserver(settingKey, observer = it)
            }
        }
    }

    companion object {
        const val KEY = "accessibility_button_preview"
        const val DEFAULT_OPACITY = 0.55f
        private const val SMALL_SIZE = 0
        private val RELATED_SETTINGS_KEY =
            arrayOf(
                Settings.Secure.ACCESSIBILITY_BUTTON_MODE,
                Settings.Secure.ACCESSIBILITY_FLOATING_MENU_SIZE,
                Settings.Secure.ACCESSIBILITY_FLOATING_MENU_OPACITY,
            )
    }
}
