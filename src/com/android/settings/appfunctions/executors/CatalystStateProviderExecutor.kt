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
import android.os.BaseBundle
import android.os.Process
import android.provider.Settings
import android.util.Log
import com.android.settings.appfunctions.CatalystConfig
import com.android.settings.appfunctions.DeviceStateAppFunctionType
import com.android.settings.appfunctions.DeviceStateProviderExecutorResult
import com.android.settings.deviceinfo.imei.ImeiPreference
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceHierarchyNode
import com.android.settingslib.metadata.PreferenceScreenMetadata
import com.android.settingslib.metadata.PreferenceScreenRegistry
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.ValidatedKeyParameters
import com.android.settingslib.metadata.ReadWritePermit.Companion.ALLOW
import com.android.settingslib.metadata.accessPreconditionsAsString
import com.android.settingslib.metadata.getPreferencePurpose
import com.android.settingslib.metadata.getPreferenceScreenTitle
import com.android.settingslib.metadata.getPreferenceSummary
import com.android.settingslib.metadata.getPreferenceTitle
import com.android.settingslib.metadata.getTrampolinedLaunchIntent
import com.android.settingslib.metadata.isExposable
import com.android.settingslib.metadata.isUiOnlyPreference
import com.android.settingslib.metadata.resolvedAccessAndGetPreconditionsAsString
import com.android.settingslib.metadata.resolvedSetPreconditionsAsString
import com.android.settingslib.metadata.setPreconditionsAsString
import com.android.settingslib.metadata.preferencesapi.ApiPreference
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.spaprivileged.model.app.AppListRepositoryImpl
import com.android.settingslib.utils.applications.AppUtils
import com.google.android.appfunctions.schema.common.v1.devicestate.DeviceStateItem
import com.google.android.appfunctions.schema.common.v1.devicestate.LocalizedString
import com.google.android.appfunctions.schema.common.v1.devicestate.PerScreenDeviceStates
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import com.android.settingslib.metadata.preferencesapi.markStringAsExternalData

/* A [DeviceStateProvider] that provides device state information for Settings that are
exposed using Catalyst framework. Configured in [CatalystStateProviderConfig]. */
class CatalystStateProviderExecutor(
    val config: CatalystConfig,
    private val context: Context,
    private val englishContext: Context,
) : DeviceStateExecutor {
    private val perScreenConfigMap = config.screenConfigs.associateBy { it.screenKey }
    private val screenKeyList = perScreenConfigMap.keys.toList()

    override suspend fun execute(
        appFunctionType: DeviceStateAppFunctionType,
        params: GenericDocument?,
    ): DeviceStateProviderExecutorResult {
        // Cache the app list as it is used for multiple screens and is expensive to compute.
        AppListRepositoryImpl.useCaching = true
        try {
            val perScreenDeviceStatesList = mutableListOf<PerScreenDeviceStates>()
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
            val maxExecutionTimeMs = Settings.Global.getLong(
                context.contentResolver,
                SETTING_MAX_EXECUTION_TIME_MS,
                DEFAULT_MAX_EXECUTION_TIME_MS
            ).milliseconds
            val logAppFunctionTime = Settings.Global.getInt(
                context.contentResolver,
                SETTING_LOG_APPFUNCTION_TIME,
                0
            ) == 1
            val shouldIncludeScreenKey = AppUtils.isDebuggable() && Settings.Global.getInt(
                context.contentResolver,
                APP_FUNCTION_INCLUDE_SCREEN_KEY_IN_DESCRIPTION,
                0
            ) == 1

            val semaphore = Semaphore(maxParallelism)
            var deferredList = emptyList<Pair<String, Deferred<List<PerScreenDeviceStates>?>>>()
            val timeLogs = ConcurrentHashMap<String, Long>()

            val startTimeAll = android.os.SystemClock.elapsedRealtime()
            withTimeoutOrNull(maxExecutionTimeMs) {
                coroutineScope {
                    deferredList = screenKeyList.map { screenKey ->
                        screenKey to async {
                            var executionTime = -1L
                            try {
                                semaphore.withPermit {
                                    val startTime = android.os.SystemClock.elapsedRealtime()
                                    try {
                                        withTimeout(perScreenTimeoutMs) {
                                            try {
                                                val hierarchyMap = getEnabledPreferencesHierarchy(config, context, appFunctionType, screenKey)
                                                if (hierarchyMap.isEmpty()) return@withTimeout null

                                                val firstInstance = hierarchyMap.entries.first()
                                                val firstScreenMetadata = firstInstance.key
                                                val firstPreferences = firstInstance.value

                                                if (!firstScreenMetadata.isExposable(context) || !firstScreenMetadata.isFlagEnabled(context)) return@withTimeout null

                                                val hasNonStaticInfo = firstScreenMetadata.accessPreconditionsAsString(context) != null ||
                                                        firstScreenMetadata.setPreconditionsAsString(context) != null ||
                                                        firstScreenMetadata.getEnabledDescription() != null ||
                                                        (firstScreenMetadata as? PreferenceAvailabilityProvider)?.availabilityDescription != null

                                                val hasStateProvidingPreference = firstPreferences.any {
                                                    it.metadata.isExposable(context)
                                                    }

                                                    if (hasNonStaticInfo || hasStateProvidingPreference) {
                                                    hierarchyMap.mapNotNull { (screenMetadata, preferences) ->
                                                        buildPerScreenDeviceStates(screenMetadata, preferences, shouldIncludeScreenKey)
                                                    }
                                                } else null
                                            } catch (e: Exception) {
                                                Log.e(TAG, "error building $screenKey", e)
                                                null
                                            }
                                        }
                                    } finally {
                                        executionTime = android.os.SystemClock.elapsedRealtime() - startTime
                                    }
                                }
                            } catch (e: TimeoutCancellationException) {
                                Log.e(TAG, "Timed out building screen: $screenKey", e)
                                if (logAppFunctionTime) {
                                    timeLogs[screenKey] = -1L
                                }
                                null
                            } finally {
                                if (logAppFunctionTime && !timeLogs.containsKey(screenKey)) {
                                    if (executionTime == -1L) {
                                        Log.e(TAG, "executionTime is -1 for screen: $screenKey")
                                        timeLogs[screenKey] = -1L
                                    } else {
                                        timeLogs[screenKey] = executionTime
                                    }
                                }
                            }
                        }
                    }
                    deferredList.map { it.second }.awaitAll()
                }
            } ?: Log.w(TAG, "Max execution time of $maxExecutionTimeMs exceeded.")

            if (logAppFunctionTime) {
                val totalTime = android.os.SystemClock.elapsedRealtime() - startTimeAll
                Log.d(TAG, "AppFunction $appFunctionType took ${totalTime}ms")
                timeLogs.entries
                    .sortedByDescending { if (it.value == -1L) Long.MAX_VALUE else it.value }
                    .forEach { entry ->
                        val timeStr = if (entry.value == -1L) "timed out" else "${entry.value}ms"
                        Log.d(TAG, "Screen ${entry.key} took $timeStr")
                    }
            }

            val completedKeys = mutableSetOf<String>()
            val results = mutableListOf<List<PerScreenDeviceStates>>()

            for ((screenKey, deferred) in deferredList) {
                if (deferred.isCompleted && !deferred.isCancelled) {
                    completedKeys.add(screenKey)
                    val res = deferred.getCompleted()
                    if (res != null) {
                        results.add(res)
                    }
                }
            }

            val incompleteKeys = screenKeyList - completedKeys
            if (incompleteKeys.isNotEmpty()) {
                Log.w(TAG, "Screens not processed due to max execution time: $incompleteKeys")
            }

            perScreenDeviceStatesList.addAll(results.flatten())
            return DeviceStateProviderExecutorResult(states = perScreenDeviceStatesList)
        } finally {
            // Disable caching for the next execution to avoid stale data.
            AppListRepositoryImpl.useCaching = false
        }
    }


    private suspend fun CoroutineScope.buildPerScreenDeviceStates(
        screenMetaData: PreferenceScreenMetadata,
        preferencesHierarchy: List<PreferenceHierarchyNode>,
        shouldIncludeScreenKey: Boolean,
    ): PerScreenDeviceStates? {
        val deviceStateItemList = mutableListOf<DeviceStateItem>()
        preferencesHierarchy.forEach {
            val metadata = it.metadata
            val jsonValue =
                when {
                    // TODO(b/444419242): Handle IMEI redaction properly.
                    isImeiPreference(metadata.key) -> "REDACTED"
                    metadata is PersistentPreference<*> -> {
                        getDeviceStateItemValueForPreference(metadata)
                    }
                    else -> null
                }
            jsonValue?.let {
                deviceStateItemList.add(
                    DeviceStateItem(
                        // Binding key is either equal to the key or contains the package name or
                        // other item specific id necessary to distinguish the items.
                        key = "${screenMetaData.key}/${metadata.bindingKey}",
                        purpose = metadata.getPreferencePurpose(context).toString(),
                        name = if (metadata is PreferencesApiScreen || metadata is ApiPreference<*, *>) null
                            else {
                                val englishString = metadata.getPreferenceTitle(englishContext).toString()
                                val localizedString = metadata.getPreferenceTitle(context).toString()
                                if (englishString != "null" || localizedString != "null") {
                                    LocalizedString(
                                        english = metadata.getPreferenceTitle(englishContext).toString(),
                                        localized = metadata.getPreferenceTitle(context).toString(),
                                    )
                                } else null
                            },
                        jsonValue = it,
                        hintText = listOfNotNull(
                            metadata.resolvedAccessAndGetPreconditionsAsString(context),
                            metadata.resolvedSetPreconditionsAsString(context),
                        ).joinToString(". "),
                    )
                )
            }
        }

        val basicDescription = listOfNotNull(
            screenMetaData.getPreferenceScreenTitle(context)?.toString(),
            screenMetaData.getPreferencePurpose(context).toString(),
            screenMetaData.resolvedAccessAndGetPreconditionsAsString(context),
            screenMetaData.resolvedSetPreconditionsAsString(context),
        ).filter { it.isNotBlank() }
            .joinToString(". ")
            .replace("..", ".")

        val keyParameters = screenMetaData.keyParameters

        val descriptionSuffix = if (keyParameters != null && keyParameters != ValidatedKeyParameters.EMPTY) {
             "${keyParameters.toParametersString()}"
        } else {
            val arguments = screenMetaData.arguments?.clone() as? BaseBundle
            arguments?.remove("source")
            if (arguments == null) {
                ""
            } else {
                ". " + arguments.keySet().joinToString(", ") { "$it=${arguments.get(it)}" }
            }
        }
        val descriptionPrefix = if (shouldIncludeScreenKey) "[key=${screenMetaData.key}]" else ""
        val description = descriptionPrefix + basicDescription + descriptionSuffix

        val intentUri =
            screenMetaData
                .getTrampolinedLaunchIntent(null)
                .apply {
                    if (keyParameters != null && keyParameters != ValidatedKeyParameters.EMPTY) {
                        putExtra(PreferenceScreenMetadata.EXTRA_ITEMIZATION, keyParameters.values.values.joinToString(","))
                    }
                }
                .toUri(Intent.URI_INTENT_SCHEME)

        val states =
            PerScreenDeviceStates(
                description = description,
                deviceStateItems = deviceStateItemList,
                intentUri = intentUri,
            )

        return states
    }

    private fun getDeviceStateItemValueForPreference(metadata: PersistentPreference<*>): String? {
        val allowedRead = metadata.getReadPermit(
            context, Process.myPid(),
            Process.myUid()
        ) == ALLOW && metadata.isExposable(context) && (metadata as? PreferenceAvailabilityProvider)?.isAvailable(context) ?: true
        return if (allowedRead) {
            if (metadata.valueType == String::class.java) {
                // We should be smarter here and only mark external if the data is
                // actually unsafe.
                metadata.storage(context)
                    .getValue(metadata.key, metadata.valueType as Class<Any>)
                    ?.toString()?.let { markStringAsExternalData(it) }
            } else {
                metadata.storage(context)
                    .getValue(metadata.key, metadata.valueType as Class<Any>)
                    ?.toString()
            }
        } else {
            "Error: Not allowed to read value"
        }
    }

    private fun isImeiPreference(prefKey: String): Boolean {
        return prefKey.startsWith(ImeiPreference.KEY_PREFIX)
    }

    companion object {
        private const val TAG = "CatalystStateProviderExecutor"
        private const val DEFAULT_MAX_PARALLELISM = 3
        private const val DEFAULT_PER_SCREEN_TIMEOUT_MS = 20000L
        private const val DEFAULT_MAX_EXECUTION_TIME_MS = 25000L
        private const val SETTING_MAX_PARALLELISM = "com.android.settings.APP_FUNCTION_MAX_PARALLELISM"
        private const val SETTING_PER_SCREEN_TIMEOUT_MS = "com.android.settings.APP_FUNCTION_PER_SCREEN_TIMEOUT_MS"
        private const val SETTING_MAX_EXECUTION_TIME_MS = "com.android.settings.APP_FUNCTION_MAX_EXECUTION_TIME_MS"
        private const val SETTING_LOG_APPFUNCTION_TIME = "com.android.settings.APP_FUNCTION_LOG_TIME"
        private const val APP_FUNCTION_INCLUDE_SCREEN_KEY_IN_DESCRIPTION = "com.android.settings.APP_FUNCTION_INCLUDE_SCREEN_KEY_IN_DESCRIPTION"
    }
}
