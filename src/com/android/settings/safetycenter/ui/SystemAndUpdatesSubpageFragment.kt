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

/** Fragment for displaying System and Updates subpage within the Safety Center in Settings. */
@SearchIndexable
class SystemAndUpdatesSubpageFragment : SafetyCenterSubpageFragment() {

    override val subpageKey = SafetyCenterSubpageRegistry.SYSTEM_AND_UPDATES_SUBPAGE_KEY

    override fun getLogTag(): String {
        return TAG
    }

    companion object {
        private const val TAG = "SystemUpdatesSubpage"

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER: BaseSearchIndexProvider =
            object : BaseSearchIndexProvider(R.xml.safety_center_system_and_updates_subpage) {
                override fun isPageSearchEnabled(context: Context?): Boolean {
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
                            SafetyCenterSubpageRegistry.SYSTEM_AND_UPDATES_SUBPAGE_KEY,
                        )
                    )
                    return rawData
                }
            }
    }
}
