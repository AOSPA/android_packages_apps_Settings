/*
 * Copyright 2025 The Android Open Source Project
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

package com.android.settings.appfunctions.executors

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.android.settings.appfunctions.CatalystConfig
import com.android.settings.appfunctions.DeviceStateAppFunctionType
import com.android.settingslib.metadata.CatalystFlagProviderFactory
import com.android.settingslib.metadata.KeyParametersSchema
import com.android.settingslib.metadata.PreferenceHierarchy
import com.android.settingslib.metadata.PreferenceHierarchyNode
import com.android.settingslib.metadata.PreferenceScreenMetadata
import com.android.settingslib.metadata.PreferenceScreenRegistry
import com.android.settingslib.metadata.ValidatedKeyParameters
import com.android.settingslib.metadata.isExposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList

/**
 * A generic helper function to process preferences for a given screen. It handles the common logic
 * of fetching, validating, and iterating over preferences.
 *
 * @param screenKey The key of the preference screen to process.
 * @return A Map containing the screen's metadata and the list of processed items, or empty map if the
 *   screen is invalid or disabled.
 */
suspend fun CoroutineScope.getEnabledPreferencesHierarchy(
    config: CatalystConfig,
    context: Context,
    appFunctionType: DeviceStateAppFunctionType? = null,
    screenKey: String,
    removeDuplicates: Boolean = false,
    includeAtLeastOne: Boolean = false,
): Map<PreferenceScreenMetadata, List<PreferenceHierarchyNode>> {
    val perScreenConfigMap = config.screenConfigs.associateBy { it.screenKey }
    val perScreenConfig = perScreenConfigMap[screenKey]
    if (
        perScreenConfig == null ||
            !perScreenConfig.enabled ||
            (appFunctionType != null && appFunctionType !in perScreenConfig.appFunctionTypes)
    ) {
        return mapOf()
    }

    val hierarchies =
        if (PreferenceScreenRegistry.isParameterized(context, screenKey)) {
            val paramsFlow = if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
                PreferenceScreenRegistry.getKeyParameters(context, screenKey)
            } else {
                PreferenceScreenRegistry.getParameters(context, screenKey)
            }

            val targetParams = if (removeDuplicates) {
                paramsFlow.take(1).toList()
            } else {
                paramsFlow.toList()
            }

            val modifiedTargetParams = if (includeAtLeastOne && targetParams.isEmpty()) {
                if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
                        listOf(ValidatedKeyParameters(PreferenceScreenRegistry.getScreenParametersSchema(screenKey) ?: KeyParametersSchema { }, emptyMap()))
                    } else {
                        listOf(Bundle())
                    }
            } else {
                targetParams
            }

            modifiedTargetParams.map { param ->
                getPreferenceHierarchy(
                    context,
                    screenKey,
                    args = param as? Bundle,
                    keyParameters = param as? ValidatedKeyParameters,
                )
            }
        } else {
            listOf(
                getPreferenceHierarchy(
                    context,
                    screenKey,
                    args = null,
                    keyParameters = null,
                )
            )
        }

    return hierarchies.filterNotNull().toMap()
}

fun CoroutineScope.getEnabledPreferencesHierarchy(
    context: Context,
    screenMetadata: PreferenceScreenMetadata,
): Map<PreferenceScreenMetadata, List<PreferenceHierarchyNode>> {
    val preferenceHierarchy = mutableListOf<PreferenceHierarchyNode>()
    screenMetadata.getPreferenceHierarchy(
        context,
        this
    ).forEach { preferenceHierarchy.add(it) }

    return mapOf(screenMetadata to preferenceHierarchy)
}

private suspend fun CoroutineScope.getPreferenceHierarchy(
    context: Context,
    screenKey: String,
    args: Bundle?,
    keyParameters: ValidatedKeyParameters?,
): Pair<PreferenceScreenMetadata, List<PreferenceHierarchyNode>>? {
    val screenMetaData =
        if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
            PreferenceScreenRegistry.createWithKeyParameters(context, screenKey, keyParameters)
        } else {
            PreferenceScreenRegistry.create(context, screenKey, args)
        } ?: return null

    val preferenceHierarchy = mutableListOf<PreferenceHierarchyNode>()
    screenMetaData.getPreferenceHierarchy(context, this).forEachRecursivelyAsyncExceptRoot {
        val metadata = it.metadata
        if (!metadata.isExposable(context)) return@forEachRecursivelyAsyncExceptRoot
        if(metadata is PreferenceScreenMetadata) return@forEachRecursivelyAsyncExceptRoot
        preferenceHierarchy.add(it)
    }
    return screenMetaData to preferenceHierarchy
}

/**
 * Runs an action over a preference hierarchy, except the root node of the hierarchy, which contains
 * the screen metadata when called on a screen hierarchy.
 */
private suspend fun PreferenceHierarchy.forEachRecursivelyAsyncExceptRoot(isRoot: Boolean = true, action: suspend (PreferenceHierarchyNode) -> Unit) {
    if(!isRoot)
        action(this)
    // async hierarchy is included by forEachAsync
    forEachAsync {
        when (it) {
            is PreferenceHierarchy -> it.forEachRecursivelyAsyncExceptRoot(false, action)
            else -> action(it)
        }
    }
}