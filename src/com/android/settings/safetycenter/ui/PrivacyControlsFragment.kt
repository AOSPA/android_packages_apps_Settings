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
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable
import com.android.settingslib.search.SearchIndexableRaw

/**
 * Fragment that displays various privacy controls. This fragment is a sub-page of the main Safety
 * Center UI. It hosts preferences for privacy hosts preferences for privacy-related settings`
 */
@SearchIndexable
class PrivacyControlsFragment : SafetyCenterSubpageFragment() {

    override val subpageKey = SafetyCenterSubpageRegistry.PRIVACY_CONTROLS_SUBPAGE_KEY

    override fun getLogTag(): String {
        return TAG
    }

    override fun redirectIfEmpty() {
        // Privacy controls subpage shouldn't be hidden even if safety source preferences in it are
        // not present, because it contains a lot of other preferences.
        return
    }

    companion object {
        private const val TAG = "PrivacyControlsFragment"

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER =
            object : BaseSearchIndexProvider(R.xml.safety_center_privacy_controls_settings) {
                public override fun isPageSearchEnabled(context: Context): Boolean {
                    return Flags.enableSafetyCenterNewUi()
                }

                override fun getDynamicRawDataToIndex(
                    context: Context,
                    enabled: Boolean,
                ): List<SearchIndexableRaw> {
                    val rawData = super.getDynamicRawDataToIndex(context, enabled).toMutableList()
                    rawData.addAll(
                        SafetyCenterSearchIndexUtils.getDynamicRawDataForIndexingSubpage(
                            context,
                            SafetyCenterSubpageRegistry.PRIVACY_CONTROLS_SUBPAGE_KEY,
                        )
                    )
                    return rawData
                }
            }
    }
}
