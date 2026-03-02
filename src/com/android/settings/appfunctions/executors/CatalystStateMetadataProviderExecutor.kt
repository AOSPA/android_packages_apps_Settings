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

import android.app.appsearch.GenericDocument
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Binder
import android.provider.Settings
import android.util.Log
import com.android.settings.appfunctions.CatalystConfig
import com.android.settings.appfunctions.DeviceStateAppFunctionType
import com.android.settings.appfunctions.DeviceStateMetadataProviderExecutorResult
import com.android.settingslib.graph.PreferenceGetterFlags
import com.android.settingslib.graph.proto.PreferenceProto
import com.android.settingslib.graph.proto.PreferenceValueDescriptorProto
import com.android.settingslib.graph.proto.PreferenceValueProto
import com.android.settingslib.graph.toProto
import com.android.settingslib.metadata.PreferenceHierarchyNode
import com.android.settingslib.metadata.PreferenceScreenMetadata
import com.android.settingslib.metadata.PreferenceScreenRegistry
import com.android.settingslib.metadata.accessPreconditionsAsString
import com.android.settingslib.metadata.getPreconditionsAsString
import com.android.settingslib.metadata.preferencesapi.types.ApiType
import com.android.settingslib.metadata.preferencesapi.types.FiniteOptionsType
import com.android.settingslib.metadata.setPreconditionsAsString
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.getPreferencePurpose
import com.android.settingslib.metadata.getPreferenceScreenTitle
import com.android.settingslib.metadata.getPreferenceTitle
import com.android.settingslib.metadata.isExposable
import com.android.settingslib.metadata.preferencesapi.ApiPreference
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.setWarningAsString
import com.android.settingslib.utils.applications.AppUtils
import com.google.android.appfunctions.schema.common.v1.devicestate.DeviceStateItemMetadata
import com.google.android.appfunctions.schema.common.v1.devicestate.ItemizationDetail
import com.google.android.appfunctions.schema.common.v1.devicestate.ItemizationType
import com.google.android.appfunctions.schema.common.v1.devicestate.LocalizedString
import com.google.android.appfunctions.schema.common.v1.devicestate.PerScreenMetadata
import com.google.android.appfunctions.schema.common.v1.devicestate.Sensitivity
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeout

/* A [DeviceStateExecutor] that provides device state metadata information for Settings that are
exposed using Catalyst framework. Configured in [CatalystStateProviderConfig]. */
class CatalystStateMetadataProviderExecutor(
    val config: CatalystConfig,
    private val context: Context,
    private val englishContext: Context,
) : DeviceStateExecutor {
    private val perScreenConfigMap = config.screenConfigs.associateBy { it.screenKey }
    private val screenKeyList = perScreenConfigMap.keys.toList()

    override suspend fun execute(
        appFunctionType: DeviceStateAppFunctionType,
        params: GenericDocument?,
    ): DeviceStateMetadataProviderExecutorResult {
        val perScreenDeviceStatesList = mutableListOf<PerScreenMetadata>()
        val itemizationTypes = mutableSetOf<ItemizationType>()
        coroutineScope {
            val semaphore = Semaphore(MAX_PARALLELISM)
            val deferredList =
                screenKeyList.map { screenKey ->
                    async {
                        try {
                            withTimeout(PER_SCREEN_TIMEOUT_MS) {
                                semaphore.withPermit {
                                    try {
                                        val screenMetadata = PreferenceScreenRegistry.createScreenInstanceForMetadata(context, screenKey)
                                        if(screenMetadata != null && screenMetadata.isExposable(context)) {
                                            buildPerScreenDeviceStatesMetadata(screenKey)
                                        } else null
                                    } catch (e: Exception) {
                                        Log.e(TAG, "error building $screenKey", e)
                                        null
                                    }
                                }
                            }
                        } catch (e: TimeoutCancellationException) {
                            Log.e(TAG, "Timed out building screen: $screenKey", e)
                            null
                        }
                    }
                }
            val results = deferredList.awaitAll().filterNotNull()
            perScreenDeviceStatesList.addAll(results.flatMap { it.metadata })
            itemizationTypes.addAll(results.flatMap { it.itemizationTypes })
        }
        return DeviceStateMetadataProviderExecutorResult(
            metadata = perScreenDeviceStatesList,
            itemizationTypes = itemizationTypes,
        )
    }

    private suspend fun CoroutineScope.buildPerScreenDeviceStatesMetadata(
        screenKey: String
    ): DeviceStateMetadataProviderExecutorResult {
        val isParameterized = PreferenceScreenRegistry.isParameterized(context, screenKey)
        val hierarchy =
            getEnabledPreferencesHierarchy(
                config,
                context,
                appFunctionType = null,
                screenKey,
                removeDuplicates = isParameterized,
            )

        val metadata =
            hierarchy.map { entry ->
                val screenMetaData = entry.key
                val preferencesHierarchy = entry.value
                buildPerScreenDeviceStatesMetadata(
                    screenMetaData,
                    preferencesHierarchy,
                    isParameterized,
                )
            }

        val types = mutableSetOf<ItemizationType>()
        hierarchy.values.flatten().forEach { node ->
            (node.metadata as? ApiPreference<*>)?.let { types.add(it.type.toItemizationType(context)) }
        }
        hierarchy.keys.forEach { screenMetaData ->
            screenMetaData.keyParametersSchema?.getParameters()?.values?.forEach { param ->
                types.add(param.type.toItemizationType(context))
            }
        }

        return DeviceStateMetadataProviderExecutorResult(
            metadata = metadata,
            itemizationTypes = types,
        )
    }

    private suspend fun CoroutineScope.buildPerScreenDeviceStatesMetadata(
        screenMetaData: PreferenceScreenMetadata,
        preferencesHierarchy: List<PreferenceHierarchyNode>,
        isParameterized: Boolean,
    ): PerScreenMetadata {
        val deviceStateItemMetadataList = mutableListOf<DeviceStateItemMetadata>()
        preferencesHierarchy.forEach {
            val metadata = it.metadata
            // skip if metadata is screen
            if(metadata is PreferenceScreenMetadata)
                return@forEach
            // skip over explicitly disabled preferences
            val metadataProto = try {
                metadata.toProto(
                    context,
                    Binder.getCallingPid(),
                    Binder.getCallingUid(),
                    screenMetaData,
                    isRoot = false,
                    flags = PreferenceGetterFlags.METADATA or PreferenceGetterFlags.VALUE_DESCRIPTOR,
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to convert preference to proto: ${metadata.key}", e)
                return@forEach
            }

            val sensitivityLevel =
                when (metadataProto.sensitivityLevel) {
                    SensitivityLevel.MUST_PROVIDE_UNDO -> Sensitivity.MUST_PROVIDE_UNDO
                    SensitivityLevel.REQUIRES_CONFIRMATION -> Sensitivity.REQUIRES_CONFIRMATION
                    else -> null
                }

            val writable = if (metadata is ApiPreference<*>) {
                metadata.set != null
            } else {
                false // Legacy preferences are not writable
            }

            // We replace .. with . because sometimes the strings
            // contain a full stop at the end and this joins with
            // the separator
            val hintText = listOfNotNull(
                        metadata.accessPreconditionsAsString(context),
                        metadata.getPreconditionsAsString(context),
                        metadata.setPreconditionsAsString(context),
                        metadata.setWarningAsString(context),
                    ).joinToString(separator = "\n").replace("..", ".")
            deviceStateItemMetadataList.add(
                DeviceStateItemMetadata(
                    // TODO: Expose parameterization
                    key = "${screenMetaData.key}/${metadataProto.key}",
                    purpose = metadataProto.getPurposeString(),
                    // Currently api-first screens and preferences do not have
                    // titles
                    name = if (metadata is PreferencesApiScreen || metadata is ApiPreference<*>) null
                    else LocalizedString(
                            english = metadata.getPreferenceTitle(englishContext).toString(),
                            localized = metadata.getPreferenceTitle(context).toString(),
                        ),
                    sensitivity = sensitivityLevel,
                    writable = writable,
                    possibleValues = if (metadata is ApiPreference<*>) {
                        val type = metadata.type
                        if (type is FiniteOptionsType) {
                            type.getOptions(context).map {
                                "${it.first.toString()} (${it.second.toString()})"
                            }.joinToString(", ")
                        } else {
                            null
                        }
                    } else {
                        val str = metadataProto.valueDescriptor.toDeviceStateString()
                        if (str.isEmpty()) null else str
                    },

                    hintText = if (hintText.isNotEmpty()) { hintText } else { null },
                )
            )
        }

        val launchingIntent = screenMetaData.getLaunchIntent(context, null)
        return PerScreenMetadata(
            description = (
                    listOfNotNull(
                        if (shouldIncludeScreenKey()) "[key=${screenMetaData.key}]" else "",
                        // This is a hack to remove the title from parametrised screens as it may contain
                        // some text referring to that specific parameter which could confuse the agent.
                        if (isParameterized) ""
                            else screenMetaData.getPreferenceScreenTitle(context)?.toString() ?: "",
                        screenMetaData.getPreferencePurpose(context),
                        screenMetaData.accessPreconditionsAsString(context),
                    ).filter{it.isNotBlank()}.joinToString(". ").replace("..", ".")
                ),
            deviceStateItemsMetadata = deviceStateItemMetadataList,
            // intentUri = launchingIntent?.toUri(Intent.URI_INTENT_SCHEME),

            // Ideally itemizationTypes should be 1) nullable and 2) more
            // complex than a string so we can communicate more detail
            itemizationTypes = screenMetaData.keyParametersSchema?.getParameters()?.values?.map {
                    val type = it.type
                    if (type is FiniteOptionsType) {
                        val optionsString = type.getOptions(context).map { "${it.first} (${it.second})" }.joinToString(",")
                        "${type.getKey()} - ${type.getDescription(context)} - options: $optionsString"
                    } else {
                        "${type.getKey()} - ${type.getDescription(context)}"
                    }
                }?.toList() ?: emptyList(),
        )
    }

    /**
     * Returns true if the screen key should be included in the description for debugging.
     *
     * This should never be used in production.
     */
    private fun shouldIncludeScreenKey(): Boolean {
        return AppUtils.isDebuggable() && Settings.Global.getInt(
            context.contentResolver,
            "com.android.settings.APP_FUNCTION_INCLUDE_SCREEN_KEY_IN_DESCRIPTION",
            0
        ) == 1
    }

    private fun PreferenceProto.getPurposeString(): String =
        if (this.purpose != 0) {
            try {
                context.getString(this.purpose)
            } catch (e: Resources.NotFoundException) {
                Log.w(TAG, "Cannot get purpose for: ${this.key}", e)
                this.key
            }
        } else {
            this.key
        }

    companion object {
        private const val TAG = "CatalystStateMetadataProviderExecutor"
        private const val MAX_PARALLELISM = 3
        private val PER_SCREEN_TIMEOUT_MS = 5.seconds

        /** Returns an LLM readable string describing the value type. */
        internal fun PreferenceValueDescriptorProto.toDeviceStateString(): String {
            val typeString = if (possibleValuesCount > 0) {
                possibleValuesList.joinToString(separator = ", ") {
                    "${it.value.toValueString()} (${it.description})"
                }
            } else {
                when (typeCase) {
                    PreferenceValueDescriptorProto.TypeCase.BOOLEAN_TYPE -> "BOOL"
                    PreferenceValueDescriptorProto.TypeCase.FLOAT_TYPE -> "FLOAT"
                    PreferenceValueDescriptorProto.TypeCase.LONG_TYPE -> "LONG"
                    PreferenceValueDescriptorProto.TypeCase.RANGE_VALUE -> {
                        val range = rangeValue
                        val filter = listOf(
                            if (range.hasMin()) "min=${range.min}" else null,
                            if (range.hasMax()) "max=${range.max}" else null,
                            if (range.hasStep() && range.step > 1) "step=${range.step}" else null,
                        ).filterNotNull().joinToString(separator = ", ")

                        if (filter.isEmpty()) {
                            "INTEGER"
                        } else {
                            "INTEGER($filter)"
                        }
                    }
                    PreferenceValueDescriptorProto.TypeCase.STRING_TYPE -> "STRING"
                else -> ""
                }
            }
            val parametersString = if (hasParameters() && parameters.valuesMap.isNotEmpty()) {
                " [" + parameters.valuesMap.entries.sortedBy { it.key }.joinToString(",") { "${it.key}=${it.value}" } + "]"
            } else {
                ""
            }
            return typeString + parametersString
        }

        private fun PreferenceValueProto.toValueString(): String =
            when {
                hasBooleanValue() -> booleanValue.toString()
                hasFloatValue() -> floatValue.toString()
                hasIntValue() -> intValue.toString()
                hasLongValue() -> longValue.toString()
                hasStringValue() -> stringValue
                else -> ""
            }
    }
}

suspend fun ApiType<*>.toItemizationType(context: Context): ItemizationType {
    return ItemizationType(
        key = getKey(),
        hintText = getDescription(context),
        values = if (this is FiniteOptionsType) {
            getOptions(context).map {
                ItemizationDetail(key = it.first.toString(), value = it.second.toString())
            }.toList()
        } else {
            emptyList()
        },
    )
}
