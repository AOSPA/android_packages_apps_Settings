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

package com.android.settings.sound

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.provider.Settings
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import com.android.settingslib.Utils

/** Utilities for populating the suggestion preference content. */
object SuggestionsPreferenceUtils {
    private val SUGGESTION_PROVIDER_TARGET = "media_device_suggestions_target"
    private val SUGGESTION_PROVIDER_DESCRIPTION = "media_device_suggestions_description"

    fun getSuggestionIntent(context: Context): Intent? {
        val targetAction =
            Settings.Secure.getString(context.contentResolver, SUGGESTION_PROVIDER_TARGET)
              ?: return null
        val intent = Intent(targetAction)
        val activityInfo =
            intent.resolveActivityInfo(context.packageManager, PackageManager.MATCH_ALL)
                ?: return null
        if (activityInfo.applicationInfo?.isSystemApp != true) {
            return null
        }
        return intent
    }

    fun getSuggestionDescription(context: Context) =
        Settings.Secure.getString(
            context.contentResolver,
            SUGGESTION_PROVIDER_DESCRIPTION
        )
}
