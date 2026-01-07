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
import android.os.UserManager
import android.safetycenter.SafetyCenterManager
import android.util.Log
import androidx.annotation.StringRes
import androidx.annotation.XmlRes
import com.android.settingslib.safetycenter.SafetyCenterDataTransformer
import com.android.settingslib.safetycenter.SafetyCenterUiData
import com.android.settingslib.safetycenter.SafetySourcePreference
import com.android.settingslib.search.SearchIndexableRaw

/** Utility object for Safety Center search indexing. */
object SafetyCenterSearchIndexUtils {
    private const val TAG = "SCSearchIndexUtils"

    /** Fetches and transforms the current SafetyCenterData. */
    fun getCurrentSafetyCenterData(context: Context): SafetyCenterUiData? {
        val safetyCenterManager = context.getSystemService(SafetyCenterManager::class.java)
        return safetyCenterManager?.let {
            SafetyCenterDataTransformer.transform(it.safetyCenterData)
        }
    }

    /**
     * Generates a list of [SearchIndexableRaw] for dynamic indexing of [SafetySourcePreference]s
     * for a given subpage key.
     *
     * @param context The Context.
     * @param preferenceKey The string preference key of the subpage.
     * @return A list of [SearchIndexableRaw] for preferences with available data.
     */
    fun getDynamicRawDataForIndexingSubpage(
        context: Context,
        preferenceKey: String,
    ): List<SearchIndexableRaw> {
        val titleResId = SafetyCenterSubpageRegistry.getTitleResId(preferenceKey)
        val prefs =
            SafetyCenterSubpageRegistry.getXmlSafetySourcePrefConfigs(context, preferenceKey)
        return getDynamicRawDataForIndexingSafetySourcePrefs(context, prefs, titleResId)
    }

    /**
     * Generates a list of [SearchIndexableRaw] for dynamic indexing of [SafetySourcePreference]s
     * within a specific XML resource.
     *
     * @param context The Context.
     * @param xmlResId The XML resource ID to parse.
     * @param screenTitleResId The string resource for the screen title.
     * @return A list of [SearchIndexableRaw] for preferences with available data.
     */
    fun getDynamicRawDataForIndexingFromXml(
        context: Context,
        @XmlRes xmlResId: Int,
        @StringRes screenTitleResId: Int,
    ): List<SearchIndexableRaw> {
        val prefs =
            SafetyCenterSubpageRegistry.parseSafetySourcePreferencesFromXml(context, xmlResId)
        return getDynamicRawDataForIndexingSafetySourcePrefs(context, prefs, screenTitleResId)
    }

    private fun getDynamicRawDataForIndexingSafetySourcePrefs(
        context: Context,
        prefConfigs: List<SafetySourcePrefConfig>,
        @StringRes screenTitleResId: Int?,
    ): List<SearchIndexableRaw> {
        val uiData = getCurrentSafetyCenterData(context)
        val userManager = context.getSystemService(UserManager::class.java)

        if (uiData == null || userManager == null || screenTitleResId == null) {
            Log.w(TAG, "Missing data for dynamic indexing")
            return emptyList()
        }
        val screenTitle = context.getString(screenTitleResId)
        val rawData = mutableListOf<SearchIndexableRaw>()

        for (pref in prefConfigs) {
            val userHandle =
                UserProfilesUtils.getUserHandleForProfile(context, pref.profile) ?: continue
            val entry = uiData.getEntry(userHandle.identifier, pref.sourceId)
            if (entry != null) {
                val raw = SearchIndexableRaw(context)
                raw.key = pref.key
                raw.title = entry.title.toString()
                raw.summaryOn = entry.summary?.toString()
                raw.screenTitle = screenTitle
                raw.keywords = pref.keywords
                rawData.add(raw)
            }
        }
        return rawData
    }
}
