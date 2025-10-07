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
import android.view.View
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityButtonFragment
import com.android.settings.accessibility.shortcuts.ShortcutOptionPreference as ShortcutOptionWidget
import com.android.settings.accessibility.shortcuts.data.ShortcutOptionDataStore
import com.android.settings.core.SubSettingLauncher
import com.android.settings.utils.AnnotationSpan
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.preference.BooleanValuePreferenceBinding

/**
 * Abstract base class for a preference that represents a shortcut option.
 *
 * This class handles the data storage and permission checks for shortcut options.
 *
 * @param context The context.
 * @param shortcutType The type of the user shortcut, e.g., [UserShortcutType.SOFTWARE].
 * @param targets The set of accessibility features associated with this shortcut option.
 */
abstract class ShortcutOptionPreference(
    protected val context: Context,
    @param:UserShortcutType private val shortcutType: Int,
    protected val targets: Set<String>,
) : BooleanValuePreference, BooleanValuePreferenceBinding {

    override val indexable: Boolean
        get() = false

    private val dataStore: ShortcutOptionDataStore by lazy {
        ShortcutOptionDataStore(context.applicationContext, shortcutType, targets)
    }

    override fun createWidget(context: Context): ShortcutOptionWidget {
        return ShortcutOptionWidget(context)
    }

    override fun storage(context: Context): KeyValueStore = dataStore

    override fun getReadPermissions(context: Context) = SettingsSecureStore.getReadPermissions()

    override fun getWritePermissions(context: Context) = SettingsSecureStore.getWritePermissions()

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(
        context: Context,
        value: Boolean?,
        callingPid: Int,
        callingUid: Int,
    ): @ReadWritePermit Int {
        // Don't allow user enable shortcut outside of the Settings app
        return when (value) {
            true -> ReadWritePermit.DISALLOW
            else -> ReadWritePermit.ALLOW
        }
    }

    companion object {
        @JvmStatic
        fun getCustomizeAccessibilityButtonLink(context: Context): CharSequence {
            val linkListener =
                View.OnClickListener { v: View? ->
                    SubSettingLauncher(context)
                        .setDestination(AccessibilityButtonFragment::class.java.getName())
                        .setSourceMetricsCategory(
                            SettingsEnums.DIALOG_ACCESSIBILITY_SERVICE_EDIT_SHORTCUT
                        )
                        .launch()
                }
            val linkInfo =
                AnnotationSpan.LinkInfo(AnnotationSpan.LinkInfo.DEFAULT_ANNOTATION, linkListener)
            return AnnotationSpan.linkify(
                context.getText(
                    R.string.accessibility_shortcut_edit_dialog_summary_software_floating
                ),
                linkInfo,
            )
        }
    }
}
