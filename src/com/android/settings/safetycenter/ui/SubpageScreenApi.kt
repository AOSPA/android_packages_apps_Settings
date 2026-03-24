/*
 * Copyright (C) 2026 The Android Open Source Project
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
import android.os.Process
import android.safetycenter.SafetyCenterManager
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Custom
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.safetycenter.SafetyCenterDataTransformer
import kotlin.reflect.KClass

/**
 * Base class for Safety Center subpage APIs.
 *
 * Encapsulates common logic for flag gating, availability preconditions, and helper methods for
 * visibility checks.
 */
abstract class SubpageScreenApi(
    key: String,
    topLevelSettingsCategory: Category,
    fragment: KClass<out Fragment>,
    @StringRes purpose: Int,
    private val subpageRegistryKey: String,
) :
    PreferencesApiScreen(
        key = key,
        topLevelSettingsCategory = topLevelSettingsCategory,
        fragment = fragment,
        purpose = purpose,
    ) {

    init {
        flag { Flags.catalystMigration26q2() && Flags.enableSafetyCenterNewUi() }

        preconditions("There must be safety information made available for this screen from on-device safety sources.") {
            if (SafetyCenterSubpageRegistry.isSubpageAvailable(context, subpageRegistryKey)) {
                Allowed
            } else {
                Custom(
                    R.string.safety_source_unavailable,
                    stability = PreconditionStability.UNSTABLE,
                )
            }
        }
    }

    /**
     * Checks if a specific Safety Center source entry is visible for the current user.
     *
     * @param context The context.
     * @param sourceId The ID of the safety source.
     * @return `true` if the entry exists in the current Safety Center data, `false` otherwise.
     */
    protected fun isSafetySourceVisible(context: Context, sourceId: String): Boolean {
        val safetyCenterManager =
            context.getSystemService(SafetyCenterManager::class.java) ?: return false
        val uiData = SafetyCenterDataTransformer.transform(safetyCenterManager.safetyCenterData)
        return uiData.getEntry(Process.myUserHandle().identifier, sourceId) != null
    }
}
