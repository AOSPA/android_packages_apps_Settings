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
import android.graphics.drawable.Drawable
import androidx.preference.Preference

/** Controls the "Privacy controls" subpage, mostly just to calculate the summary. */
class PrivacyControlsPreferenceController(context: Context, preferenceKey: String) :
    SubpagePreferenceController(context, preferenceKey) {

    override fun setIcon(preference: Preference, icon: Drawable?) {
        // Icon for the privacy controls subpage should never be shown. So we can reuse all the
        // summary calculation logic, but we prevent the icon from being set.
        preference.icon = null
    }

    override fun setVisibility(preference: Preference, isVisible: Boolean) {
        // Privacy controls subpage shouldn't be hidden even if safety source preferences in it are
        // not present, because it contains a lot of other preferences.
        preference.isVisible = true
    }

    override fun updateNonIndexableKeys(keys: MutableList<String>) {
        // Privacy controls subpage shouldn't be hidden from search as the subpage preference is
        // always visible
        return
    }
}
