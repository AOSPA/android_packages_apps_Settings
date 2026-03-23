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
import android.os.Bundle
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
import com.android.settingslib.metadata.CatalystFlagProviderFactory
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.PreferenceHierarchyNode
import com.android.settingslib.metadata.PreferenceScreenMetadata
import com.android.settingslib.metadata.PreferenceScreenMetadataParameterizedFactory
import com.android.settingslib.metadata.PreferenceScreenRegistry
import com.android.settingslib.metadata.accessPreconditionsAsString
import com.android.settingslib.metadata.getPreconditionsAsString
import com.android.settingslib.metadata.preferencesapi.types.ApiType
import com.android.settingslib.metadata.preferencesapi.types.FiniteOptionsType
import com.android.settingslib.metadata.setPreconditionsAsString
import com.android.settingslib.metadata.stableAccessPreconditionFailuresAsString
import com.android.settingslib.metadata.stableSetPreconditionFailuresAsString
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.getPreferencePurpose
import com.android.settingslib.metadata.getPreferenceScreenTitle
import com.android.settingslib.metadata.getTrampolinedLaunchIntent
import com.android.settingslib.metadata.isExposable
import com.android.settingslib.metadata.preferencesapi.ApiPreference
import com.android.settingslib.metadata.setWarningAsString
import com.android.settingslib.utils.applications.AppUtils
import com.google.android.appfunctions.schema.common.v1.devicestate.DeviceStateItemMetadata
import com.google.android.appfunctions.schema.common.v1.devicestate.ItemizationDetail
import com.google.android.appfunctions.schema.common.v1.devicestate.ItemizationType
import com.google.android.appfunctions.schema.common.v1.devicestate.PerScreenMetadata
import com.google.android.appfunctions.schema.common.v1.devicestate.Sensitivity
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeout
import com.android.settingslib.metadata.preferencesapi.extractSafety

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
        val itemizationTypes = mutableMapOf<String, ItemizationType>()
        coroutineScope {
            val maxParallelism = Settings.Global.getInt(
                context.contentResolver,
                SETTING_MAX_PARALLELISM,
                DEFAULT_MAX_PARALLELISM
            )
            val perScreenTimeoutMs = Settings.Global.getLong(
                context.contentResolver,
                SETTING_PER_SCREEN_TIMEOUT_MS,
                DEFAULT_PER_SCREEN_TIMEOUT_MS
            ).milliseconds
            val semaphore = Semaphore(maxParallelism)
            val deferredList =
                screenKeyList.map { screenKey ->
                    async {
                        try {
                            withTimeout(perScreenTimeoutMs) {
                                semaphore.withPermit {
                                    try {
                                        val screenMetadata = PreferenceScreenRegistry.createScreenInstanceForMetadata(context, screenKey)
                                        if (screenMetadata != null && screenMetadata.isExposable(context)) {
                                            buildPerScreenDeviceStatesMetadata(screenKey)
                                        } else {
                                            val factory = PreferenceScreenRegistry.preferenceScreenMetadataFactories[screenKey]
                                            val isParameterized = factory is PreferenceScreenMetadataParameterizedFactory
                                            if (isParameterized) {
                                                // Try and build the metadata with no itemization entries
                                                buildPerScreenDeviceStatesMetadata(factory, screenKey )
                                            } else {
                                                null
                                            }
                                        }
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
            results.flatMap { it.itemizationTypes }.forEach { itemizationTypes[it.key] = it }
        }
        return DeviceStateMetadataProviderExecutorResult(
            metadata = perScreenDeviceStatesList,
            itemizationTypes = itemizationTypes.values.toSet(),
            hintText = "When an intentUri includes '%24itemization', that must be replaced by an actual itemization value before launching.",
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

        return buildHierarchyMetadata(hierarchy, isParameterized)
    }

    private suspend fun CoroutineScope.buildPerScreenDeviceStatesMetadata(
        parameterizedFactory: PreferenceScreenMetadataParameterizedFactory,
        screenKey: String
    ): DeviceStateMetadataProviderExecutorResult? {
        val screenMetadata =
            if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
                PreferenceScreenRegistry.createWithKeyParameters(
                    context,
                    screenKey,
                    parameterizedFactory.parametersSchema.prepareEmpty()
                )
            } else {
                PreferenceScreenRegistry.create(context, screenKey, Bundle.EMPTY)
            }
        if (screenMetadata != null && screenMetadata.isExposable(context)) {
            val hierarchy = getEnabledPreferencesHierarchy(context, screenMetadata)
            return buildHierarchyMetadata(hierarchy, isParameterized = true)
        }

        return null
    }

    private suspend fun CoroutineScope.buildHierarchyMetadata(
        hierarchy: Map<PreferenceScreenMetadata, List<PreferenceHierarchyNode>>,
        isParameterized: Boolean,
    ): DeviceStateMetadataProviderExecutorResult {
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

        val types = mutableMapOf<String, ItemizationType>()
        hierarchy.values.flatten().forEach { node ->
            (node.metadata as? ApiPreference<*, *>)?.let { types[it.type.toItemizationType(context).key] = it.type.toItemizationType(context) }
        }
        hierarchy.keys.forEach { screenMetaData ->
            screenMetaData.keyParametersSchema?.getParameters()?.values?.forEach { param ->
                types[param.type.toItemizationType(context).key] = param.type.toItemizationType(context)
            }
        }

        return DeviceStateMetadataProviderExecutorResult(
            metadata = metadata,
            itemizationTypes = types.values.toSet(),
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

            val writable =
            if (metadataProto.sensitivityLevel > 2) { // requires confirmation or do not expose
                false
            } else if (metadata is ApiPreference<*, *>) {
                metadata.set != null
            } else if (metadata is PersistentPreference<*>) {
                metadata.supportsWrite
            } else {
                false
            }

            // We replace .. with . because sometimes the strings
            // contain a full stop at the end and this joins with
            // the separator
            val hintText = listOfNotNull(
                        metadata.accessPreconditionsAsString(context),
                        metadata.getPreconditionsAsString(context),
                        metadata.setPreconditionsAsString(context),
                        metadata.setWarningAsString(context),
                        metadata.stableAccessPreconditionFailuresAsString(context),
                        metadata.stableSetPreconditionFailuresAsString(context),
                    ).joinToString(separator = "\n").replace("..", ".")
            deviceStateItemMetadataList.add(
                DeviceStateItemMetadata(
                    key = "${screenMetaData.key}/${metadataProto.key}",
                    purpose = metadataProto.getPurposeString(),
                    // Name contains values so should not be in metadata. The
                    // valuable information here is now in the purpose.
                    name = null,
                    sensitivity = sensitivityLevel,
                    writable = writable,
                    possibleValues = if (metadata is ApiPreference<*, *>) {
                        val type = metadata.type

                        val str = "itemization:${type.getKey()}"
                        val parameters = type.getParameters()

                        if (parameters != null) {
                            str + " ${parameters.toParametersString()}"
                        } else {
                            str
                        }
                    } else {
                        val str = metadataProto.valueDescriptor.toDeviceStateString()
                        if (str.isEmpty()) null else str
                    },

                    hintText = if (hintText.isNotEmpty()) { hintText } else { null },
                )
            )
        }

        val launchingIntent = screenMetaData.getTrampolinedLaunchIntent(null).apply {
            // .toUri() will drop the parameter's bundle. In the end, this
            // launchingIntent will contain only the screenKey and (if parameterized) the
            // itemization extra. The SettingsLaunchpadActivity is able to launch the correct screen
            // based on this.
            if (isParameterized) {
                // Add the literal '$itemization' string as the value for the itemization extra
                putExtra(PreferenceScreenMetadata.EXTRA_ITEMIZATION, $$"$itemization")
            }
        }.toUri(Intent.URI_INTENT_SCHEME)

        return PerScreenMetadata(
            description = (
                    listOfNotNull(
                        if (shouldIncludeScreenKey()) "[key=${screenMetaData.key}]" else "",
                        // This is a hack to remove the title from parameterised screens as it may contain
                        // some text referring to that specific parameter which could confuse the agent.
                        if (isParameterized) ""
                            else screenMetaData.getPreferenceScreenTitle(context)?.toString() ?: "",
                        screenMetaData.getPreferencePurpose(context),
                        screenMetaData.accessPreconditionsAsString(context),
                    ).filter{it.isNotBlank()}.joinToString(". ").replace("..", ".")
                ),
            deviceStateItemsMetadata = deviceStateItemMetadataList,
            intentUri = launchingIntent,

            // Ideally itemizationTypes should be 1) nullable and 2) more
            // complex than a string so we can communicate more detail
            itemizationTypes = screenMetaData.keyParametersSchema?.getParameters()?.values?.map {
                    val type = it.type
                    "${type.getKey()}"
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
        private const val DEFAULT_MAX_PARALLELISM = 3
        private const val DEFAULT_PER_SCREEN_TIMEOUT_MS = 5000L
        private const val SETTING_MAX_PARALLELISM = "com.android.settings.APP_FUNCTION_MAX_PARALLELISM"
        private const val SETTING_PER_SCREEN_TIMEOUT_MS = "com.android.settings.APP_FUNCTION_PER_SCREEN_TIMEOUT_MS"

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

suspend fun ApiType<*, *>.toItemizationType(context: Context): ItemizationType {
    return ItemizationType(
        key = getKey(),
        hintText = getDescription(context),
        values = if (this is FiniteOptionsType) {
            getOptions(context).map {
                ItemizationDetail(key = extractSafety(it.first).toString(), value = extractSafety(it.second).toString())
            }.toList()
        } else {
            emptyList()
        },
    )
}
