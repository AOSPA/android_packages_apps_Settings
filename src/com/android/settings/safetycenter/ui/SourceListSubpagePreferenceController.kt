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

package com.android.settings.safetycenter.ui

import android.content.Context
import android.icu.text.ListFormatter
import android.icu.util.ULocale
import android.safetycenter.SafetyCenterEntry
import com.android.settingslib.safetycenter.SafetySourcePreference

/**
 * Controls the subpage preferences for subpages that need to have a dynamic list of safety source
 * titles as the default summary.
 */
class SourceListSubpagePreferenceController(context: Context, preferenceKey: String) :
    SubpagePreferenceController(context, preferenceKey) {

    override fun getDefaultSubpageSummary(
        safetySourceEntries: List<SafetyCenterEntry>
    ): CharSequence {
        // Only list main/personal profile items, to avoid duplication of information about
        // what's the content of the subpage.
        val titles =
            safetySourceEntries
                .filter {
                    UserProfilesUtils.getUserProfile(mContext, it.user!!) ==
                        SafetySourcePreference.Profile.PERSONAL
                }
                .map { it.title }

        if (titles.isEmpty()) {
            return super.getDefaultSubpageSummary(safetySourceEntries)
        }

        return ListFormatter.getInstance(
                ULocale.getDefault(ULocale.Category.DISPLAY),
                ListFormatter.Type.AND,
                ListFormatter.Width.NARROW,
            )
            .format(titles)
    }
}
