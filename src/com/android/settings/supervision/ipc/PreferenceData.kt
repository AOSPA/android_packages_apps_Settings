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
package com.android.settings.supervision.ipc

import android.os.Bundle

/**
 * Data class representing preference information, used for displaying preference items.
 *
 * This class encapsulates data such as icons, titles, summaries, actions, action icons, and target
 * packages. It provides constructors for creating instances from a Bundle and for converting
 * instances back to a Bundle.
 *
 * @property icon Optional icon resource ID for the preference.
 * @property title Optional title text for the preference.
 * @property summary Optional summary text for the preference.
 * @property action Optional {@link Intent} action to be performed when the preference is clicked,
 *   such as {@link #ACTION_VIEW}.
 * @property trailingIcon Optional trailing icon resource ID.
 * @property targetPackage Optional target package name for limiting the applications that can
 *   perform the action.
 * @property isVisible Optional boolean indicating whether the preference should be visible,
 *   defaults to true.
 * @property learnMoreLink Optional link to a help center article for more information.
 */
data class PreferenceData(
    val icon: Int? = null,
    val title: CharSequence? = null,
    val summary: CharSequence? = null,
    var action: String? = null,
    var trailingIcon: Int? = null,
    var targetPackage: String? = null,
    var isVisible: Boolean = true,
    var learnMoreLink: String? = null,
) {
    constructor(
        bundle: Bundle
    ) : this(
        icon = bundle.getInt(ICON, -1).takeIf { it != -1 },
        title = bundle.getCharSequence(TITLE),
        summary = bundle.getCharSequence(SUMMARY),
        action = bundle.getString(ACTION),
        trailingIcon = bundle.getInt(ACTION_ICON, -1).takeIf { it != -1 },
        targetPackage = bundle.getString(TARGET_PACKAGE),
        isVisible = bundle.getBoolean(IS_VISIBLE, true),
        learnMoreLink = bundle.getString(LEARN_MORE_LINK),
    )

    fun toBundle(): Bundle {
        return Bundle().apply {
            icon?.let { putInt(ICON, it) }
            title?.let { putCharSequence(TITLE, it) }
            summary?.let { putCharSequence(SUMMARY, it) }
            action?.let { putString(ACTION, it) }
            trailingIcon?.let { putInt(ACTION_ICON, it) }
            targetPackage?.let { putString(TARGET_PACKAGE, it) }
            putBoolean(IS_VISIBLE, isVisible)
            learnMoreLink?.let { putString(LEARN_MORE_LINK, it) }
        }
    }

    companion object {
        private const val ICON = "icon"
        private const val TITLE = "title"
        private const val SUMMARY = "summary"
        private const val ACTION = "action"
        private const val ACTION_ICON = "trailing_icon"
        private const val TARGET_PACKAGE = "target_package"
        private const val IS_VISIBLE = "is_visible"
        private const val LEARN_MORE_LINK = "learn_more_link"
    }
}
